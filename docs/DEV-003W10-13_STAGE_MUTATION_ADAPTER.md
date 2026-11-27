# DEV-003W10.13 — Stage mutation adapter foundation

## Goal

Introduce a pure-Java, bounds-checked boundary for the immediate stage-cell writes identified in the W10.12 source audit. This layer is deliberately not an interpreter and does not implement opcode fall-through.

## Implemented boundary

`DemoStageMutationAdapter` receives a narrow `StageGrid` interface exposing dimensions and three explicit write operations:
- player-layer byte cell;
- background integer cell;
- foreground short cell.

Coordinates retain the legacy source convention `[x][y]`. All public mutation methods validate x and y before invoking the stage. The shared callback validates once, writes player-layer first, then background, matching the immediate callback order found in the pinned source.

The adapter does not clamp, silently ignore, or partially apply an invalid callback. An invalid coordinate raises `StageBoundsException` before either callback write occurs.

## Tests

`DemoStageMutationAdapterTest` uses an in-memory fake stage and verifies:
- player-layer then background write order;
- zero player-layer behavior used by opcode 8/9 callback arguments;
- independent foreground and background writes;
- invalid x and negative x rejection;
- no partial callback mutation after invalid coordinate;
- last valid cell acceptance;
- null stage rejection.

## Explicit limitations

- This adapter does not parse or dispatch opcodes.
- It does not implement opcode 25/26 fall-through into later callback labels. That ordered dispatch belongs to the interpreter execution layer and must be represented explicitly there.
- It does not resolve the full gameplay meaning of tile values or update entities.
- It does not perform camera updates, render, load sprites, or call Android APIs.
- The fake stage validates adapter contracts only; it is not a retail stage parity test.

## CI gate

Require the repository workflow to pass core JVM tests, Android storage unit tests, and Android debug APK assembly. A green workflow proves compilation and synthetic contract tests, not playable-game parity.

## Next

DEV-003W10.14 should introduce private mutable runtime state over immutable normalized records. The interpreter must then translate each verified legacy callback into adapter calls, preserving switch fall-through and ordering. Do not wire this adapter to the real game stage until the opcode execution tests cover those sequences.
