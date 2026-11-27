# DEV-003W10.11 — Demo interpreter tick and completion dispatch audit

## Evidence identity

Pinned decompiled source: `kubikaugustyn/DiamondRushSource/src/main/java/c.java`, revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This is a static control-flow audit. It does not implement or execute the interpreter. The purpose is to define a safe native execution contract before introducing stage or UI adapters.

## 1. Distinct lifecycle operations

| Legacy method | Native responsibility candidate | Evidence |
|---|---|---|
| `aByteArr()` | reset all record runtime fields and interpreter overlay state, then select first buffer | iterates all buffers through reset helper; clears overlay fields and selection index |
| `bByteArr()` | select next buffer; resets tick counter; returns null after last buffer | increments selected-buffer index and resets `bInt` |
| `bVoid()` | execute one frame/tick on the current ordinary record or each child of a group, then increment tick once | traverses current buffer, invokes `cVoid`, increments `bInt` |
| `aBoolean()` | query completion: ordinary completion byte or all group children complete | reads completion byte at offset +1 |
| `aVoid()` | time-gated update for text/line reveal records; separate from frame tick | checks 1000 ms interval and updates current record(s) |
| `aVoid(Graphics)` | render current buffer and overlay; includes legacy render-side state mutation | separate renderer traversal and overlay draw calls |

Do not collapse these methods into a generic update function. In particular, the 1000 ms text reveal update and per-frame executor have different clocks and side effects.

## 2. Per-frame executor: observed dispatch order

The executor first checks the interpreter's skip/abort flag. When set, only opcodes 5, 9, 10, 25, and 26 are allowed to continue; all other record completion bytes are set immediately and the handler returns.

The remaining switch is a legacy fall-through switch. The following table lists the ordered labels reached after entering each case; it is not a list of independent handlers.

| Entry opcode | Ordered labels reached | Observed effect |
|---:|---|---|
| 16, 17, 18 | 16/17/18 → 14/15 → 12 | parity-gated toggle/countdown; overlay state; position state and completion after tick > 5 |
| 14, 15 | 14/15 → 12 | enable/disable overlay, mark done, then update overlay position |
| 12 | 12 | copy x/y into overlay position; after tick > 5 mark done and force overlay active |
| 11 | 11 → 10 → 13 | set sprite/frame overlay identifiers and mark done; process control state; resolve type-13 sentinels and duration |
| 10 | 10 → 13 | update control bits on tick zero; later clear control when game state permits; invoke game update; then type-13 duration logic |
| 13 | 13 | resolve sentinel coordinates from interpreter overlay position; mark done when tick exceeds stored duration |
| 1 | 1 | resolve sentinel coordinates from player camera position; mark done when tick exceeds duration |
| 4 | 4 | mark done when tick exceeds stored duration |
| 6 | 6 | mark done when tick exceeds stored 32-bit threshold |
| 25 | 25 → 26 → 5 → 8 → 9 → 27 → 2 | write foreground grid cell; write background grid cell; invoke object callback; invoke second callback; update another object callback; process type-27 prompt completion; then process type-2 dialogue scroll |
| 26 | 26 → 5 → 8 → 9 → 27 → 2 | write background grid cell; invoke callbacks; prompt and dialogue follow-on behavior |
| 5 | 5 → 8 → 9 → 27 → 2 | invoke first object callback; then subsequent callbacks/prompt/dialogue behavior |
| 8 | 8 → 9 → 27 → 2 | no body at label 8; continue to later labels |
| 9 | 9 → 27 → 2 | invoke object callback; then prompt/dialogue behavior |
| 27 | 27 → 2 | if prompt reveal is complete, mark done; otherwise remain active; then dialogue handler is reached |
| 2 | 2 | update dialogue scroll/position and mark done when its threshold condition is met |
| 7 | 7 → 27 → 2 | mark done immediately, then continue through prompt/dialogue labels |

Important: the table is a control-flow summary, not an approved implementation specification for arbitrary input. Each opcode's normalized buffer layout and fall-through target must be preserved exactly. The original switch does not return at every case.

## 3. State mutations by family

### Interpreter-local state

- `bInt`: frame/tick counter; incremented exactly once after the complete current-buffer traversal.
- `bBoolean`: overlay enabled flag; affected by opcodes 14/15 and type-12 timeout.
- `cInt`, `dInt`: overlay position; written by type 12 and read by type 13 sentinel resolution.
- `eInt`, `fInt`: overlay sprite/frame selection; set by type 11.
- `gInt`: optional control icon; changed by types 16/17 and reset paths.
- `hInt`: text line boundary count; computed by text layout/reveal helper.

### Shared game/stage state

- Opcode 10 mutates shared control fields and invokes the game's update routine.
- Opcode 25 writes a foreground grid cell using two u16 coordinates and a packed two-byte value.
- Opcode 26 writes a background grid cell using two u16 coordinates and a u32 value.
- Opcodes 5, 8, and 9 invoke game callbacks with different argument layouts. Their precise gameplay meaning must be resolved from the target methods in `i.java` before a native adapter is designed.

The first native executor must not directly mutate global arrays or call Android/UI code. These operations need explicit interfaces with bounds checks and deterministic failure reporting.

## 4. Completion semantics

- Ordinary record completion is byte `record[offset + 1] == 1`.
- A group is complete only if every child completion byte is 1.
- An empty group is vacuously complete under the observed loop.
- The executor does not automatically select the next buffer after a record completes.
- Buffer selection is a separate operation.
- Skip/abort mode marks most records complete immediately, but the allow-list (5, 9, 10, 25, 26) continues through their normal fall-through behavior.
- The per-frame tick increments once even when the current buffer is a group and contains multiple children.

## 5. Reset semantics

Reset traverses all buffers, not only the selected one:
- ordinary record completion bytes are cleared;
- opcode 27 reveal counter and prompt scroll/position fields are reset;
- opcode 2 dialogue scroll/position fields are reset;
- group children are traversed using the packed u32 child-length table;
- interpreter overlay fields, selected-buffer index, skip flag, and tick counter are reset;
- first buffer is selected and the time-gate baseline is updated.

Reset must operate on private normalized copies. It must not mutate the immutable wire-decoder result or caller-owned input bytes.

## 6. Critical unresolved behavior

1. Exact semantics and argument effects of game callbacks invoked by opcodes 5, 8, and 9 remain to be traced in `i.java`.
2. Opcode 10 calls the game's update routine from inside the interpreter. Reentrancy and ordering relative to the outer gameplay update need call-site tracing.
3. Types 25/26 write stage grids and then fall through into callbacks. The grid mutation and callbacks must be treated as one ordered legacy sequence.
4. The text reveal helper uses a fixed short-array of line offsets and suppresses exceptions; native bounds and overflow behavior must be defined separately.
5. The renderer mutates control-icon state for some layouts. Render-side effects are separate from the tick executor.
6. Legacy signed byte/short casts affect group child counts, durations, positions, and text lengths. The parser currently validates wire boundaries but the executor compatibility layer still needs explicit signedness rules.

## 7. Implementation gate for DEV-003W10.12

Before implementing a native executor:
- trace the target methods called by opcodes 5, 8, 9, and 10 in `i.java`;
- identify exact stage-grid coordinate order and bounds assumptions;
- define immutable record descriptors plus mutable runtime-state storage, so reset/tick never mutate decoded wire records;
- define a fake stage/game adapter for tests;
- implement one opcode family at a time with fall-through preserved as explicit ordered transitions;
- test completion, skip allow-list, parity gating, tick increments, reset across all buffers, group traversal, and reentrancy;
- keep render and time-gated text reveal outside the first executor slice.

No interpreter execution, stage mutation, renderer, sprite cache, or gameplay integration is introduced by this audit.
