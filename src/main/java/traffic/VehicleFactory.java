package traffic;

import java.util.Objects;
import java.util.Random;

/**
 * Factory responsible for creating vehicles with the right defaults.
 */
public class VehicleFactory {
    private final ColorStrategy colorStrategy;

    public VehicleFactory(ColorStrategy colorStrategy) {
        this.colorStrategy = Objects.requireNonNull(colorStrategy);
    }

    public SimulationEngine.Vehicle createVehicle(SimulationEngine.Road road, int laneIndex, Random random) {
        return new SimulationEngine.Vehicle(road, laneIndex, colorStrategy.pickColor(random));
    }
}
