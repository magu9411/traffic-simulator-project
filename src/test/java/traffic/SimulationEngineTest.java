package traffic;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.Random;

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
}
