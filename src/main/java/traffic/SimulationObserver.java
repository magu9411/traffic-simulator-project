package traffic;

/**
 * Observer notified after each simulation tick.
 */
public interface SimulationObserver {
    void onUpdate(SimulationSnapshot snapshot);
}
