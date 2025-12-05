# Traffic Simulator

Simple Swing-based traffic visualization that models a main road with a crossing intersection. You can adjust inflow, speed limits, and close a lane to see how throughput changes in real time.

## Requirements
- Java 21+ (tested with OpenJDK 24)
- Gradle (or use `./gradlew` if you add the wrapper)

## Running
With Gradle:
```bash
gradle run
```

With `make`:
```bash
make run
```

The window shows:
- Vehicles per minute slider (spawns across the main road and crossing road).
- Speed limit slider (caps vehicle speeds in the simulation).
- Lane closure toggle (removes one lane from the main road to model a work zone).
- Clickable traffic lights (above/right of the intersection) to manually set which road has green.
- Flow readout (vehicles exiting per second) and simulated time.

Hit **Reset traffic** to clear vehicles and restart the counters.

## Design patterns used
- Factory: `traffic.VehicleFactory` builds `SimulationEngine.Vehicle` instances with color policies.
- Builder: `traffic.SimulationEngineBuilder` assembles the engine, roads, and strategies.
- Strategy: `traffic.SpawnStrategy` (`BiasedSpawnStrategy`) chooses spawn roads; `ColorStrategy` (`PaletteColorStrategy`) picks vehicle colors.
- Observer: `traffic.SimulationObserver` delivers `SimulationSnapshot` updates to the UI (`TrafficSimulatorApp` and `TrafficPanel`).
- Command: `traffic.SimulationCommand` (`SimulationCommands.*`) encapsulates UI actions like changing speed/spawn/closure/reset.
- State + Template Method: `SimulationEngine.Intersection` hosts traffic light states (`MainGreenState`, etc.) with a timed update template driving phase changes.
- Singleton: `traffic.SimulationConfig` centralizes default simulation knobs.

## Next steps
- Add proper intersection logic (traffic lights or yielding rules) so cross traffic interacts with the main flow.
- Persist scenarios (save/load slider settings and lane closures).
- Add charts for historical throughput and queue lengths.
- Replace placeholder units with real-world units (mph) via scaling.
