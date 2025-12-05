package traffic;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Weighted random strategy that prefers the main road over others.
 */
public class BiasedSpawnStrategy implements SpawnStrategy {
    private final Map<String, Double> weights;

    public BiasedSpawnStrategy() {
        this(Map.of(
                "Main Eastbound", 0.7,
                "Crossing", 0.3
        ));
    }

    public BiasedSpawnStrategy(Map<String, Double> weights) {
        this.weights = weights;
    }

    @Override
    public SimulationEngine.Road chooseRoad(List<SimulationEngine.Road> roads, Random random) {
        if (roads.isEmpty()) {
            return null;
        }
        double total = roads.stream()
                .mapToDouble(r -> weights.getOrDefault(r.getName(), 1.0))
                .sum();
        double roll = random.nextDouble() * total;
        for (SimulationEngine.Road road : roads) {
            roll -= weights.getOrDefault(road.getName(), 1.0);
            if (roll <= 0) {
                return road;
            }
        }
        return roads.get(0);
    }
}
