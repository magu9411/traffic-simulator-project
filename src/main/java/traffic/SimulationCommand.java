package traffic;

/**
 * Command pattern for mutating simulation knobs from the UI.
 */
public interface SimulationCommand {
    void execute(SimulationEngine engine);
}
