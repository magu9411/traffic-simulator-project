package traffic;

/**
 * Singleton holding default knobs for the simulation.
 */
public final class SimulationConfig {
    private static final SimulationConfig INSTANCE = new SimulationConfig();

    private final double defaultSpawnPerMinute = 40;
    private final double defaultSpeedLimit = 70;
    private final boolean defaultLaneClosure = false;

    private SimulationConfig() {
    }

    public static SimulationConfig getInstance() {
        return INSTANCE;
    }

    public double defaultSpawnPerMinute() {
        return defaultSpawnPerMinute;
    }

    public double defaultSpeedLimit() {
        return defaultSpeedLimit;
    }

    public boolean defaultLaneClosure() {
        return defaultLaneClosure;
    }
}
