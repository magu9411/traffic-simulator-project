package traffic;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Small traffic simulation that updates on the Swing event thread.
 */
public class SimulationEngine {
    private final Road mainRoad;
    private final Road crossRoad;
    private final Road onRamp;
    private final Point2D.Double intersectionPoint;
    private final Intersection intersection;

    private final List<Road> roads;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Random random;
    private final SpawnStrategy spawnStrategy;
    private final VehicleFactory vehicleFactory;
    private final List<SimulationObserver> observers = new CopyOnWriteArrayList<>();

    private double spawnPerMinute;
    private double targetSpeedLimit;
    private boolean laneClosure;
    private double spawnAccumulator;
    private double simTimeSeconds;

    private final Deque<Double> departures = new ArrayDeque<>();
    private static final double THROUGHPUT_WINDOW_SECONDS = 10.0;
    private static final double MIN_GAP_PIXELS = 18.0;

    public SimulationEngine() {
        this(SimulationEngineBuilder.defaults());
    }

    SimulationEngine(SimulationEngineBuilder builder) {
        this.mainRoad = builder.getMainRoad();
        this.crossRoad = builder.getCrossRoad();
        this.onRamp = builder.getOnRamp();
        this.intersectionPoint = builder.getIntersectionPoint();
        this.intersection = new Intersection(mainRoad, crossRoad, intersectionPoint);
        this.roads = List.of(mainRoad, crossRoad, onRamp);
        this.spawnPerMinute = builder.getSpawnPerMinute();
        this.targetSpeedLimit = builder.getTargetSpeedLimit();
        this.laneClosure = builder.isLaneClosure();
        this.random = builder.getRandom();
        this.spawnStrategy = builder.getSpawnStrategy();
        this.vehicleFactory = builder.getVehicleFactory();
    }

    public void update(double deltaSeconds) {
        simTimeSeconds += deltaSeconds;
        intersection.update(deltaSeconds);
        spawnVehicles(deltaSeconds);
        moveVehicles(deltaSeconds);
        pruneDepartures();
        notifyObservers();
    }

    public void execute(SimulationCommand command) {
        command.execute(this);
    }

    public void setSpawnPerMinute(double perMinute) {
        spawnPerMinute = Math.max(0, perMinute);
    }

    public void setSpeedLimit(double speed) {
        targetSpeedLimit = Math.max(10, speed);
    }

    public void setLaneClosure(boolean closed) {
        laneClosure = closed;
    }

    public double getThroughputPerSecond() {
        return departures.isEmpty() ? 0.0 : departures.size() / THROUGHPUT_WINDOW_SECONDS;
    }

    public double getSimTimeSeconds() {
        return simTimeSeconds;
    }

    public void reset() {
        vehicles.clear();
        departures.clear();
        spawnAccumulator = 0;
        simTimeSeconds = 0;
        intersection.reset();
        SimulationConfig config = SimulationConfig.getInstance();
        spawnPerMinute = config.defaultSpawnPerMinute();
        targetSpeedLimit = config.defaultSpeedLimit();
        laneClosure = config.defaultLaneClosure();
        notifyObservers();
    }

    public List<RoadView> getRoadViews() {
        List<RoadView> views = new ArrayList<>();
        for (Road road : roads) {
            int open = openLanesFor(road);
            views.add(new RoadView(road.start, road.end, open, road.lanes, road.name));
        }
        return views;
    }

    public SignalView getSignalView() {
        Map<Road, Color> colors = intersection.signalColors();
        return new SignalView(intersectionPoint, colors.get(mainRoad), colors.get(crossRoad), intersection.currentPhase());
    }

    public List<VehicleView> getVehicleViews() {
        List<VehicleView> views = new ArrayList<>(vehicles.size());
        for (Vehicle vehicle : vehicles) {
            Point2D.Double position = vehicle.road.positionAlong(vehicle.position, vehicle.laneIndex, openLanesFor(vehicle.road));
            views.add(new VehicleView(position, vehicle.color, vehicle.road == onRamp));
        }
        return views;
    }

    public void addObserver(SimulationObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(SimulationObserver observer) {
        observers.remove(observer);
    }

    public SimulationSnapshot createSnapshot() {
        return new SimulationSnapshot(
                getRoadViews(),
                getVehicleViews(),
                getSignalView(),
                getThroughputPerSecond(),
                getSimTimeSeconds()
        );
    }

    private void spawnVehicles(double deltaSeconds) {
        if (spawnPerMinute <= 0) {
            return;
        }
        double perSecond = spawnPerMinute / 60.0;
        spawnAccumulator += perSecond * deltaSeconds;
        while (spawnAccumulator >= 1.0) {
            trySpawn();
            spawnAccumulator -= 1.0;
        }
        if (spawnAccumulator > 0 && random.nextDouble() < spawnAccumulator) {
            trySpawn();
            spawnAccumulator = 0;
        }
    }

    private void trySpawn() {
        Road choice = spawnStrategy.chooseRoad(roads, random);
        if (choice == null) {
            return;
        }
        int openLanes = openLanesFor(choice);
        for (int lane = 0; lane < openLanes; lane++) {
            if (isLaneClear(choice, lane)) {
                vehicles.add(vehicleFactory.createVehicle(choice, lane, random));
                return;
            }
        }
    }

    private boolean isLaneClear(Road road, int lane) {
        double openLength = Math.min(road.length(), MIN_GAP_PIXELS * 2);
        for (Vehicle vehicle : vehicles) {
            if (vehicle.road == road && vehicle.laneIndex == lane && vehicle.position < openLength) {
                return false;
            }
        }
        return true;
    }

    private void moveVehicles(double deltaSeconds) {
        List<Vehicle> toRemove = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            Road road = vehicle.road;
            double speedLimit = Math.min(targetSpeedLimit, road.speedLimit);
            double desiredSpeed = speedLimit;

            double gap = findGapAhead(vehicle);
            double safeGap = vehicle.speed * 0.5 + MIN_GAP_PIXELS;
            if (gap >= 0 && gap < safeGap) {
                double factor = Math.max(0.2, gap / safeGap);
                desiredSpeed *= factor;
            }

            double distance = desiredSpeed * deltaSeconds;
            distance = applyIntersectionConstraint(vehicle, distance, desiredSpeed, deltaSeconds);
            vehicle.position += distance;
            vehicle.speed = distance <= 0 ? 0 : distance / Math.max(1e-6, deltaSeconds);

            if (vehicle.position > road.length()) {
                toRemove.add(vehicle);
                departures.addLast(simTimeSeconds);
            }
        }
        vehicles.removeAll(toRemove);
    }

    private double findGapAhead(Vehicle subject) {
        double closest = Double.MAX_VALUE;
        for (Vehicle other : vehicles) {
            if (other == subject || other.road != subject.road || other.laneIndex != subject.laneIndex) {
                continue;
            }
            if (other.position > subject.position && other.position < closest) {
                closest = other.position;
            }
        }
        return closest == Double.MAX_VALUE ? -1 : closest - subject.position;
    }

    private int openLanesFor(Road road) {
        if (!laneClosure || road.lanes <= 1 || road != mainRoad) {
            return road.lanes;
        }
        return Math.max(1, road.lanes - 1);
    }

    private double applyIntersectionConstraint(Vehicle vehicle, double desiredDistance, double desiredSpeed, double deltaSeconds) {
        if (!intersection.involves(vehicle.road)) {
            return desiredDistance;
        }
        double stopLine = intersection.positionAlong(vehicle.road);
        if (stopLine < 0 || vehicle.position >= stopLine) {
            return desiredDistance;
        }
        if (intersection.canProceed(vehicle.road)) {
            return desiredDistance;
        }
        double distanceToStop = stopLine - vehicle.position - MIN_GAP_PIXELS;
        double clamped = Math.max(0, Math.min(desiredDistance, distanceToStop));
        if (distanceToStop <= 0) {
            return 0;
        }
        double maxMove = desiredSpeed * deltaSeconds;
        return Math.min(clamped, maxMove);
    }

    private void pruneDepartures() {
        double cutoff = simTimeSeconds - THROUGHPUT_WINDOW_SECONDS;
        while (!departures.isEmpty() && departures.peekFirst() < cutoff) {
            departures.removeFirst();
        }
    }

    private void notifyObservers() {
        SimulationSnapshot snapshot = createSnapshot();
        for (SimulationObserver observer : observers) {
            observer.onUpdate(snapshot);
        }
    }

    public record RoadView(Point2D.Double start, Point2D.Double end, int openLanes, int totalLanes, String name) {
    }

    public record VehicleView(Point2D.Double position, Color color, boolean fromRamp) {
    }

    public record SignalView(Point2D.Double position, Color mainColor, Color crossColor, Intersection.Phase phase) {
    }

    static class Vehicle {
        private final Road road;
        private final int laneIndex;
        private final Color color;
        private double position;
        private double speed = 40;

        Vehicle(Road road, int laneIndex, Color color) {
            this.road = Objects.requireNonNull(road);
            this.laneIndex = laneIndex;
            this.color = Objects.requireNonNull(color);
        }
    }

    public static class Road {
        private final Point2D.Double start;
        private final Point2D.Double end;
        private final int lanes;
        private final double speedLimit;
        private final String name;
        private final double length;
        private final double laneWidth = 12;
        private final double unitX;
        private final double unitY;
        private final double normalX;
        private final double normalY;

        public Road(Point2D.Double start, Point2D.Double end, int lanes, double speedLimit, String name) {
            this.start = Objects.requireNonNull(start);
            this.end = Objects.requireNonNull(end);
            this.lanes = lanes;
            this.speedLimit = speedLimit;
            this.name = Objects.requireNonNull(name);
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            this.length = Math.hypot(dx, dy);
            double mag = length == 0 ? 1 : length;
            this.unitX = dx / mag;
            this.unitY = dy / mag;
            this.normalX = -unitY;
            this.normalY = unitX;
        }

        public double length() {
            return length;
        }

        public String getName() {
            return name;
        }

        public Point2D.Double positionAlong(double distance, int laneIndex, int openLanes) {
            double clamped = Math.max(0, Math.min(distance, length));
            int laneOffsetIndex = Math.min(laneIndex, openLanes - 1);
            double offsetFromCenter = (laneOffsetIndex - (openLanes - 1) / 2.0) * laneWidth;
            double x = start.x + unitX * clamped + normalX * offsetFromCenter;
            double y = start.y + unitY * clamped + normalY * offsetFromCenter;
            return new Point2D.Double(x, y);
        }
    }

    public static class Intersection {
        public enum Phase {
            MAIN_GREEN,
            MAIN_YELLOW,
            CROSS_GREEN,
            CROSS_YELLOW
        }

        private final Road main;
        private final Road cross;
        private final Point2D.Double point;
        private double timeInPhase = 0;
        private TrafficLightState state = new MainGreenState();

        public Intersection(Road main, Road cross, Point2D.Double point) {
            this.main = Objects.requireNonNull(main);
            this.cross = Objects.requireNonNull(cross);
            this.point = point;
        }

        public void update(double deltaSeconds) {
            state.update(this, deltaSeconds);
        }

        public boolean involves(Road road) {
            return road == main || road == cross;
        }

        public double positionAlong(Road road) {
            if (road == main) {
                return distanceAlong(road, point);
            }
            if (road == cross) {
                return distanceAlong(road, point);
            }
            return -1;
        }

        public boolean canProceed(Road road) {
            return switch (state.phase()) {
                case MAIN_GREEN, MAIN_YELLOW -> road == main;
                case CROSS_GREEN, CROSS_YELLOW -> road == cross;
            };
        }

        public Phase currentPhase() {
            return state.phase();
        }

        public Map<Road, Color> signalColors() {
            Color red = new Color(0xE74C3C);
            Color yellow = new Color(0xF1C40F);
            Color green = new Color(0x27AE60);
            return switch (state.phase()) {
                case MAIN_GREEN -> Map.of(main, green, cross, red);
                case MAIN_YELLOW -> Map.of(main, yellow, cross, red);
                case CROSS_GREEN -> Map.of(main, red, cross, green);
                case CROSS_YELLOW -> Map.of(main, red, cross, yellow);
            };
        }

        public void reset() {
            state = new MainGreenState();
            timeInPhase = 0;
        }

        private double distanceAlong(Road road, Point2D.Double p) {
            double dx = p.x - road.start.x;
            double dy = p.y - road.start.y;
            return dx * road.unitX + dy * road.unitY;
        }

        private void changeState(TrafficLightState next) {
            state = next;
            timeInPhase = 0;
        }

        private interface TrafficLightState {
            Phase phase();

            double duration();

            TrafficLightState next();

            default void update(Intersection context, double deltaSeconds) {
                context.timeInPhase += deltaSeconds;
                if (context.timeInPhase >= duration()) {
                    context.changeState(next());
                }
            }
        }

        private abstract static class TimedPhaseState implements TrafficLightState {
            @Override
            public void update(Intersection context, double deltaSeconds) {
                TrafficLightState.super.update(context, deltaSeconds);
            }
        }

        private static class MainGreenState extends TimedPhaseState {
            @Override
            public Phase phase() {
                return Phase.MAIN_GREEN;
            }

            @Override
            public double duration() {
                return 12.0;
            }

            @Override
            public TrafficLightState next() {
                return new MainYellowState();
            }
        }

        private static class MainYellowState extends TimedPhaseState {
            @Override
            public Phase phase() {
                return Phase.MAIN_YELLOW;
            }

            @Override
            public double duration() {
                return 3.0;
            }

            @Override
            public TrafficLightState next() {
                return new CrossGreenState();
            }
        }

        private static class CrossGreenState extends TimedPhaseState {
            @Override
            public Phase phase() {
                return Phase.CROSS_GREEN;
            }

            @Override
            public double duration() {
                return 8.0;
            }

            @Override
            public TrafficLightState next() {
                return new CrossYellowState();
            }
        }

        private static class CrossYellowState extends TimedPhaseState {
            @Override
            public Phase phase() {
                return Phase.CROSS_YELLOW;
            }

            @Override
            public double duration() {
                return 3.0;
            }

            @Override
            public TrafficLightState next() {
                return new MainGreenState();
            }
        }
    }
}
