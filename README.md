# Traffic Simulator

Simple Swing-based traffic visualization that models a main road, a crossing intersection, and an on-ramp. You can adjust inflow, speed limits, and close a lane to see how throughput changes in real time.

## Requirements
- Java 21+ (tested with OpenJDK 24)
- Gradle (or use `./gradlew` if you add the wrapper)

## Running
```bash
gradle run
```

The window shows:
- Vehicles per minute slider (spawns across the main road, crossing road, and on-ramp).
- Speed limit slider (caps vehicle speeds in the simulation).
- Lane closure toggle (removes one lane from the main road to model a work zone).
- Flow readout (vehicles exiting per second) and simulated time.

Hit **Reset traffic** to clear vehicles and restart the counters.

## Next steps
- Add proper intersection logic (traffic lights or yielding rules) so cross traffic interacts with the main flow.
- Persist scenarios (save/load slider settings and lane closures).
- Add charts for historical throughput and queue lengths.
- Replace placeholder units (px/s) with real-world units via scaling.
