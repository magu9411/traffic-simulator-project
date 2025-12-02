package traffic;

import java.util.List;
import java.util.Random;

/**
 * Strategy for choosing which road a new vehicle should spawn on.
 */
public interface SpawnStrategy {
    SimulationEngine.Road chooseRoad(List<SimulationEngine.Road> roads, Random random);
}
