package traffic;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.Random;

/**
 * Builder for {@link SimulationEngine} to keep construction flexible and testable.
 */
public class SimulationEngineBuilder {
    private SimulationEngine.Road mainRoad;
    private SimulationEngine.Road crossRoad;
    private Point2D.Double intersectionPoint;
    private SpawnStrategy spawnStrategy = new BiasedSpawnStrategy();
    private ColorStrategy colorStrategy = new PaletteColorStrategy();
    private VehicleFactory vehicleFactory;
    private Random random = new Random();
    private double spawnPerMinute = SimulationConfig.getInstance().defaultSpawnPerMinute();
    private double targetSpeedLimit = SimulationConfig.getInstance().defaultSpeedLimit();
    private boolean laneClosure = SimulationConfig.getInstance().defaultLaneClosure();

    public static SimulationEngineBuilder defaults() {
        SimulationEngineBuilder builder = new SimulationEngineBuilder();
        builder.mainRoad = new SimulationEngine.Road(
                new Point2D.Double(80, 220),
                new Point2D.Double(720, 220),
                2,
                80,
                "Main Eastbound");
        builder.crossRoad = new SimulationEngine.Road(
                new Point2D.Double(400, 40),
                new Point2D.Double(400, 460),
                1,
                60,
                "Crossing");
        builder.intersectionPoint = new Point2D.Double(400, 220);
        return builder;
    }

    public SimulationEngineBuilder withSpawnStrategy(SpawnStrategy strategy) {
        this.spawnStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    public SimulationEngineBuilder withColorStrategy(ColorStrategy strategy) {
        this.colorStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    public SimulationEngineBuilder withVehicleFactory(VehicleFactory factory) {
        this.vehicleFactory = Objects.requireNonNull(factory);
        return this;
    }

    public SimulationEngineBuilder withRandom(Random random) {
        this.random = Objects.requireNonNull(random);
        return this;
    }

    public SimulationEngineBuilder withSpawnRate(double perMinute) {
        this.spawnPerMinute = perMinute;
        return this;
    }

    public SimulationEngineBuilder withSpeedLimit(double speed) {
        this.targetSpeedLimit = speed;
        return this;
    }

    public SimulationEngineBuilder withLaneClosure(boolean closed) {
        this.laneClosure = closed;
        return this;
    }

    public SimulationEngine build() {
        VehicleFactory resolvedFactory = vehicleFactory != null ? vehicleFactory : new VehicleFactory(colorStrategy);
        Objects.requireNonNull(mainRoad, "main road");
        Objects.requireNonNull(crossRoad, "cross road");
        Objects.requireNonNull(intersectionPoint, "intersection point");
        return new SimulationEngine(this);
    }

    SimulationEngine.Road getMainRoad() {
        return mainRoad;
    }

    SimulationEngine.Road getCrossRoad() {
        return crossRoad;
    }

    Point2D.Double getIntersectionPoint() {
        return intersectionPoint;
    }

    SpawnStrategy getSpawnStrategy() {
        return spawnStrategy;
    }

    VehicleFactory getVehicleFactory() {
        return vehicleFactory != null ? vehicleFactory : new VehicleFactory(colorStrategy);
    }

    Random getRandom() {
        return random;
    }

    double getSpawnPerMinute() {
        return spawnPerMinute;
    }

    double getTargetSpeedLimit() {
        return targetSpeedLimit;
    }

    boolean isLaneClosure() {
        return laneClosure;
    }
}
