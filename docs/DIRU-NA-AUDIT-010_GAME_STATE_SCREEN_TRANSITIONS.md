# DIRU-NA-AUDIT-010 — Game State Machine & Screen Transition Reconstruction

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Primary inspected file: `src/main/java/i.java`, especially `handleKeyPresses()`, the subsequent `switch (i.bByte)` update dispatcher, paint dispatcher, lifecycle callbacks, and the static initializer.

This is a partial static-source reconstruction. The game uses a single global byte `i.bByte` as a major screen/game-flow state, but it also uses secondary state fields such as `bsInt`, `aRInt`, `kByte`, and `boInt` to represent sub-states. Numeric states are not given semantic names unless the inspected branch proves their role. Several transitions are conditional and additional branches remain to be audited.

## 1. Main loop and state ownership

The main loop runs input handling/update work, requests repaint, and sleeps around 50 ms per iteration. The loop uses `i.bByte` to choose behavior and suppresses/increments some global tick work depending on the active state. Rendering is separately dispatched by the same global state in the paint method.

The main state byte is initialized to 0 in the static initializer:

`i.mByte = (i.bByte = 0)`

The state byte is not the only state machine. Several screens use their own counters/sub-state fields, and the stage-loading state uses an incremental step counter so that resource and map loading are spread across update iterations.

## 2. Directly observed state roles

| State value | Directly observed behavior | Confidence |
|---:|---|---|
| 0 | Initial/global state; paint/update branches exist, but its complete screen role is not established by the inspected excerpts | Partial |
| 1 | Lifecycle callback has special handling; full screen role not established here | Partial |
| 2 | Active gameplay state: main loop has gameplay-specific tick/render behavior and several gameplay input/update paths test this value | High |
| 4 | Receives title/menu input handling through `TVoid()`; multiple transitions return to this state and title music is played on at least one path | High for menu/title role |
| 6 | Lifecycle callback groups it with non-resumable/ignored states; exact role unresolved | Low |
| 7 | Lifecycle callback handles a special substate; also shares some gameplay input routing | Partial |
| 9 | A sub-screen/state entered from state 31 and state 22 paths; exact screen role unresolved | Partial |
| 12 | Retry exhaustion path assigns this state; exact game-over/terminal screen presentation not fully traced | High for retry-exhausted transition |
| 15 | Entered after map/world selection/loading preparation; subsequent state 21 incremental load sequence is observed | High for transition/loading handoff |
| 18 | Entered from a gameplay-related branch; exact screen role unresolved | Partial |
| 20 | Update branch requests world/stage preparation when a global tick threshold is reached | Partial |
| 21 | Incremental world-map/stage preparation and resource loading; uses `bsInt` as a step counter | High |
| 22 | Credits/legal text screen: loads `cr.f`, scrolls text, can return to state 9 or 4 | High |
| 24 | Cheat/debug page, based on paint and input branches | High |
| 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 | Additional state-specific branches exist; not fully mapped in this pass | Incomplete |

This table is deliberately conservative. Numeric states with no clear inspected entry/exit path remain unnamed.

## 3. World-map preparation state (21)

State 21 is an incremental loader driven by `bsInt++`. Inspected steps include:

- Step 0: conditionally invokes `sVoid()`.
- Step 1: conditionally invokes `pVoid()`.
- Step 2: releases selected previously loaded sprite/texture resources under a mode flag.
- Step 3: conditionally invokes `auVoid()`.
- Step 4: chooses the world map resource by `crtWorldIndex`: Angkor, Scotland, or Tibet; allocates map-related arrays and reads the selected map.
- Step 5: loads map sprites from `ms.f`.
- Step 6: loads a world-specific map header sprite from `ms.f` chunk 2, 3, or 4.
- Step 7: loads/initializes the ice-crystal sprite from `mmv.f` chunk 1.
- Step 8: loads the fire-crystal sprite from `mmv.f` chunk 2.
- Step 9: loads the silver-diamond sprite from `mmv.f` chunk 3.
- Step 10: initializes a group of mode-specific fields when `acBoolean` is set.
- Step 14: reconciles stage completion/unlock data with map nodes, identifies current/last stage map positions, and transitions to state 15.

The sequence is not a single synchronous screen switch. It is a staged preparation process with resource allocation and cleanup. The skipped step numbers and conditional calls indicate that this is not yet a complete step map.

## 4. Credits state (22)

State 22 uses `aRInt` as a sub-state:

- 0 initializes scroll counters and advances to 1.
- 1 loads `/cr.f`, performs newline conversion, and replaces a marker with the MIDlet version bytes.
- 2 scrolls the credits text based on directional input and supports a key action that either transitions to state 9 or advances to cleanup.
- 3 releases the credits buffer, runs cleanup, returns to state 4, resets a sub-state, and plays title music.

This is direct evidence for a credits screen and its return-to-title/menu path.

## 5. Input and lifecycle transitions

The key handler routes input according to the active state. Examples:

- State 4 delegates to `TVoid()`, indicating a dedicated menu/title input routine.
- State 12 accepts confirmation to initialize retry-related values and either calls a mode-specific path or enters state 15 after preparation.
- State 22 handles credits scrolling and exit.
- State 24 handles debug/cheat controls.
- State 31 can transition to state 9 or call a separate `QVoid()` path.
- During gameplay (state 2), the respawn key can restore a checkpoint or request a death animation depending on hero state and checkpoint tile.

Lifecycle callbacks also branch on the global state. For example, `dVoid()` groups states 0, 6, 12, and 22 into an early-return path, treats state 7 specially, and has dedicated handling for states 15 and 1. This indicates that pause/resume and hide/show behavior must be modeled in relation to the current screen state rather than as one universal pause overlay.

## 6. Native Android architecture requirements

- Implement an explicit top-level application/game-flow state machine with typed states, not raw numeric bytes.
- Keep sub-state machines for incremental loading, menus, credits, and other multi-step screens.
- Keep gameplay simulation state separate from screen/navigation state.
- Make loading asynchronous or incremental without blocking the UI thread; preserve deterministic transition ordering and cancellation/cleanup.
- Define lifecycle behavior for foreground/background transitions per state, especially gameplay versus menus/loading/credits.
- Route physical and touch input through one action abstraction, then dispatch actions to the active state.
- Ensure screen rendering is a pure projection of state; resource loads, save writes, and game-state transitions belong to update/command handlers.
- Model retry exhaustion and game-over as separate named transitions only after the terminal state UI path is fully traced.
- Use a state-transition table and deterministic tests for every known transition, including cleanup on interrupted loading.

## 7. Open work

- Enumerate all writes to `i.bByte` and trace their caller context to identify every state.
- Map all state values 0–35 and identify unreachable/legacy/debug states.
- Trace state 4 menu options, world selection, stage selection, and transitions into gameplay.
- Trace states 9, 12, 15, 18, 20, 25–35 and their paint branches.
- Trace pause/resume, hide/show, and interrupted resource-loading behavior.
- Determine the exact state 12 presentation after retry exhaustion.
- Identify all save/load entry points associated with menu and stage transitions.
- Validate state transitions with original runtime evidence and synthetic state-machine tests.

## 8. Audit status

**Completed at static-source level:** main global state field and initialization; partial direct state-role inventory; state 21 incremental world-map/resource preparation; state 22 credits lifecycle; examples of input and lifecycle state routing.

**Not completed:** complete state inventory, full title/menu/world/stage navigation, full game-over flow, pause/resume semantics, all state transition edges, synthetic tests, runtime validation, and licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
