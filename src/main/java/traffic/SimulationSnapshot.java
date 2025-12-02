package traffic;

import java.util.List;

/**
 * Immutable view of the simulation for observers and UI.
 */
public record SimulationSnapshot(
        List<SimulationEngine.RoadView> roadViews,
        List<SimulationEngine.VehicleView> vehicleViews,
        SimulationEngine.SignalView signalView,
        double throughputPerSecond,
        double simTimeSeconds
) {
}
