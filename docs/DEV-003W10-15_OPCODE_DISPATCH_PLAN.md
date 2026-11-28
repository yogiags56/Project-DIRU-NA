# DEV-003W10.15 — Opcode dispatch plan foundation

## Scope

Add a pure-Java control-flow planner that describes which legacy switch labels are reached for one leaf record. This is deliberately not an opcode executor: no operands are read, no completion state is changed, and no stage/game/camera/render callback is invoked.

## Verified dispatch paths

The plan preserves the pinned legacy switch fall-through paths:

| Entry opcode | Ordered labels |
|---:|---|
| 1, 2, 4, 6, 13 | entry only |
| 7 | 7 → 27 → 2 |
| 8 | 8 → 9 → 27 → 2 |
| 9 | 9 → 27 → 2 |
| 10 | 10 → 13 |
| 11 | 11 → 10 → 13 |
| 14, 15 | entry → 12 |
| 16, 17, 18 | entry → 14 → 12 |
| 25 | 25 → 26 → 5 → 8 → 9 → 27 → 2 |
| 26 | 26 → 5 → 8 → 9 → 27 → 2 |
| 5 | 5 → 8 → 9 → 27 → 2 |
| 27 | 27 → 2 |

The labels are ordered switch destinations, not independent handler invocations with implicit returns. The executor milestone must preserve this order.

## Skip/abort behavior

The pinned source allows only opcodes 5, 9, 10, 25, and 26 to continue through normal dispatch when skip is requested. Every other supported leaf opcode receives a skip plan with no reached labels; the executor will be responsible for marking its runtime record complete and returning.

Opcode 8 is intentionally not in the allow-list even though it normally falls through to opcode 9.

## Conservative support boundary

The planner rejects opcode 0 groups because groups are containers whose children are planned individually. It also rejects opcodes without a verified decoder/runtime contract rather than guessing their dispatch. This keeps the current support boundary explicit.

## Tests

Synthetic tests cover all audited fall-through families, the skip allow-list, unsupported opcode rejection, and group rejection.

## Important execution gate

This planner does not prove operand-layout safety. The normalized record model currently stores records as detached byte arrays, while legacy code sometimes indexes beyond the current record's declared size within a shared containing buffer. Before any stage-mutating executor is enabled, each candidate opcode must be audited for required trailing bytes and exact buffer-boundary behavior. In particular, the opcode-5 → 8 → 9 chain reads a u16 at offset +6 in label 9, while the current normalized opcode-5 layout is seven bytes long. That is an unresolved boundary hazard and must not be papered over by reading adjacent memory or inventing padding.

Likewise, opcodes 7/27/2 have chained text/prompt layouts and must not be executed until their shared-buffer layout is reconstructed.

## Next

DEV-003W10.16 should build an operand-access/record-window abstraction that makes legacy shared-buffer reads explicit and bounds-checked. Only after that can stage mutation callbacks be safely wired into an executor.
