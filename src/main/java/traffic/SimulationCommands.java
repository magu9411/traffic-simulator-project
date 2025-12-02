package traffic;

/**
 * Common simulation commands used by the UI.
 */
public final class SimulationCommands {
    private SimulationCommands() {
    }

    public static class SetSpawnRateCommand implements SimulationCommand {
        private final double perMinute;

        public SetSpawnRateCommand(double perMinute) {
            this.perMinute = perMinute;
        }

        @Override
        public void execute(SimulationEngine engine) {
            engine.setSpawnPerMinute(perMinute);
        }
    }

    public static class SetSpeedLimitCommand implements SimulationCommand {
        private final double speed;

        public SetSpeedLimitCommand(double speed) {
            this.speed = speed;
        }

        @Override
        public void execute(SimulationEngine engine) {
            engine.setSpeedLimit(speed);
        }
    }

    public static class ToggleLaneClosureCommand implements SimulationCommand {
        private final boolean closed;

        public ToggleLaneClosureCommand(boolean closed) {
            this.closed = closed;
        }

        @Override
        public void execute(SimulationEngine engine) {
            engine.setLaneClosure(closed);
        }
    }

    public static class ResetCommand implements SimulationCommand {
        @Override
        public void execute(SimulationEngine engine) {
            engine.reset();
        }
    }
}
