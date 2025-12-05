package traffic;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    @Test
    void simulationConfigIsSingleton() {
        SimulationConfig first = SimulationConfig.getInstance();
        SimulationConfig second = SimulationConfig.getInstance();
        assertSame(first, second, "getInstance should return the same singleton");
        assertTrue(first.defaultSpeedLimit() > 0);
        assertTrue(first.defaultSpawnPerMinute() >= 0);
    }

    @Test
    void builderDefaultsProduceEngineWithRoads() {
        SimulationEngine engine = SimulationEngineBuilder.defaults().build();
        assertNotNull(engine);
        assertEquals(2, engine.getRoadViews().size(), "Expected main and cross roads");
        assertNotNull(engine.getSignalView(), "Signal view should be available");
    }

    @Test
    void vehicleFactoryCreatesVehicleWithColor() {
        VehicleFactory factory = new VehicleFactory(new PaletteColorStrategy());
        SimulationEngine.Road road = new SimulationEngine.Road(
                new Point2D.Double(0, 0),
                new Point2D.Double(100, 0),
                1,
                60,
                "Test");
        SimulationEngine.Vehicle vehicle = factory.createVehicle(road, 0, new Random(1));
        assertNotNull(vehicle);
    }

    @Test
    void laneClosureReducesOpenLanesForMainRoad() {
        SimulationEngine engine = SimulationEngineBuilder.defaults().build();
        engine.setLaneClosure(true);

        SimulationEngine.RoadView main = engine.getRoadViews().get(0);
        SimulationEngine.RoadView cross = engine.getRoadViews().get(1);

        assertEquals(1, main.openLanes(), "Main road should drop to a single open lane when closed");
        assertEquals(cross.totalLanes(), cross.openLanes(), "Cross road should remain fully open");
    }

    @Test
    void negativeSpawnRatePreventsSpawning() {
        SimulationEngine engine = SimulationEngineBuilder.defaults()
                .withSpawnStrategy(new FixedSpawnStrategy())
                .withRandom(new Random(7))
                .build();

        engine.setSpawnPerMinute(-5);
        engine.update(30);

        assertTrue(engine.getVehicleViews().isEmpty(), "Vehicles should not spawn when spawn rate is clamped to zero");
    }

    @Test
    void vehiclesStopAtRedAndProceedOnGreen() {
        SimulationEngine engine = SimulationEngineBuilder.defaults()
                .withSpawnStrategy(new FixedSpawnStrategy())
                .withRandom(new Random(1))
                .withSpawnRate(60)
                .build();

        engine.setManualPhase(SimulationEngine.Intersection.Phase.CROSS_GREEN);
        engine.update(1.0); // spawn a single vehicle on the main road
        engine.setSpawnPerMinute(0); // freeze additional spawns

        engine.update(5.0); // approach the intersection on red
        Point2D.Double positionBefore = engine.getVehicleViews().getFirst().position();
        assertTrue(positionBefore.x < engine.getSignalView().position().x,
                "Vehicle should stop before the intersection while facing red");

        engine.setManualPhase(SimulationEngine.Intersection.Phase.MAIN_GREEN);
        engine.update(2.0);
        Point2D.Double positionAfter = engine.getVehicleViews().getFirst().position();
        assertTrue(positionAfter.x > engine.getSignalView().position().x,
                "Vehicle should proceed through the intersection once green");
    }

    @Test
    void throughputIncreasesWhenVehiclesExit() {
        SimulationEngine engine = SimulationEngineBuilder.defaults()
                .withSpawnStrategy(new FixedSpawnStrategy())
                .withRandom(new Random(2))
                .withSpawnRate(60)
                .build();

        engine.update(1.0);
        engine.setSpawnPerMinute(0);
        engine.update(20.0);

        assertTrue(engine.getVehicleViews().isEmpty(), "Vehicle should have left the road after a long update");
        assertTrue(engine.getThroughputPerSecond() > 0, "Departure should be counted toward throughput");
    }

    @Test
    void observerReceivesSnapshotsOnUpdate() {
        SimulationEngine engine = SimulationEngineBuilder.defaults().build();
        AtomicInteger notifications = new AtomicInteger();
        SimulationObserver observer = snapshot -> {
            notifications.incrementAndGet();
            assertNotNull(snapshot.signalView());
            assertEquals(engine.getSimTimeSeconds(), snapshot.simTimeSeconds());
        };

        engine.addObserver(observer);
        engine.update(1.5);

        assertEquals(1, notifications.get(), "Observer should be notified once per update call");
    }

    private static class FixedSpawnStrategy implements SpawnStrategy {
        @Override
        public SimulationEngine.Road chooseRoad(java.util.List<SimulationEngine.Road> roads, Random random) {
            return roads.isEmpty() ? null : roads.get(0);
        }
    }
}
