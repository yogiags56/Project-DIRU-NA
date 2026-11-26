# DIRU-NA-DEV-003S — Two-cell movement decision

**Status:** Pure decision model committed; caller sampling and world mutation remain out of scope.
**Pinned source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881
**Method:** i.aBoolean(int,int,boolean), player-layer cases 0/8/9/47.

## Evaluator contract

`LegacyTwoCellMovementEvaluator` accepts explicit sampled facts. It does not access stage arrays, call animation routines, or mutate world state.

### Early return

The caller supplies `earlyReturnConditionMet`, which must represent the original `kByte == 4` and coordinate/row predicate exactly. When true, the evaluator returns false before decrementing the movement counter. It returns animation intent 8 when `((movementStateBits | 0x8) & 0x7) == 0x2`, otherwise 9.

### Push acceptance

After the early-return gate, the counter is decremented. The two-cell push branch is accepted only when all are true:

- decremented counter < 0, or probe mode is active;
- movement direction is non-zero;
- destination cell passes the caller's `eBoolean`/passability sample;
- target foreground low byte is not 7, or its signed high-byte value has a non-zero 0xF0 mask;
- adjacent player-layer is not 19, 45, 49, or 43, unless the adjacent foreground low byte is 0x23.

On acceptance, return flag is true and movement-block bit intent is set. World-push intent is emitted only outside probe mode.

### Rejected push

When push is rejected, the incoming return flag is preserved; local direction clear and player-metadata low-three-bit clear intents are emitted. Movement-block intent is emitted only when direction is non-zero.

## Limits

- Coordinates and exact `eBoolean` destination sampling remain caller responsibilities.
- The class reports intents; it does not write the two player-layer cells, metadata, animation state, byte arrays, or call helper methods.
- The adjacent player-layer and foreground samples must use the exact source coordinates; this API deliberately does not calculate or fetch them.
- Player-layer 48 push geometry and -1 edge/door handling are separate milestones.
- Unit tests use synthetic samples and are not a substitute for replay against recorded game states.

## Next

DEV-003T: define an immutable sampling snapshot with named source coordinates for the two-cell target, adjacent player tile, adjacent foreground tile, destination passability, and movement state. Add fixture-driven parity tests before connecting any intents to GameSession.

## DEV-003T sampling snapshot follow-up

Added `LegacyTwoCellMovementSnapshot` as an immutable carrier for source-cell samples and movement state. Its coordinate formulas are kept separate:

- Target foreground / passability sample: `(playerX + 2*deltaX, playerY + 2*deltaY)`.
- Adjacent player-layer and foreground exception sample: `(playerX + deltaX, playerY + 1)`.
- Horizontal push destination: `(playerX + 2*deltaX, playerY + deltaY)`.
- Player cell cleared by the horizontal push: `(playerX + deltaX, playerY + deltaY)`.

The snapshot also computes the early-return gate from `kByte == 4`, targetX matching `dInt` or `dInt+1`, and targetY >= `eInt`. The evaluator now has an overload accepting this snapshot, reducing the chance that callers mix target and adjacent samples.

The snapshot is still populated by a caller. It does not perform bounds checks, access arrays, or call the legacy `eBoolean` helper. The horizontal push destination coordinates are exposed as decision context only; no grid writes occur.

Added fixture-style unit tests for coordinate formulas, early-return predicates, snapshot decoding, and snapshot-driven push acceptance/rejection. These are synthetic tests; recorded gameplay fixtures and CI remain outstanding.
