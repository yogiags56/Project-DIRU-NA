# DIRU-NA-DEV-003E — Collision Input Snapshot and Typed Observation

**Status:** Immutable source-oriented snapshot and corrected trace invariants committed; evaluator not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource  
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881

## 1. Scope

This milestone defines immutable input data for a single collision-helper observation and corrects the trace model to distinguish local calculations from persisted state. It does not evaluate collisions, access stage arrays, mutate player state, or implement movement.

## 2. Why local and persisted state must be distinct

At the end of `aBoolean(dx,dy,probe)`, the source executes the `b2 && aWInt == 0` block before the final `if (!probe)` snapshot writeback. The block changes local copies of:
- `jInt` to 18;
- `crtPlayerX` by dx;
- `crtPlayerY` by dy.

These local changes can therefore occur in probe mode without being persisted to the player object. Separately, the special `kByte == 4` early return occurs before final writeback, even for non-probe calls.

The trace model now records:
- local tile delta applied;
- local progress initialized;
- helper returned normally;
- player snapshot written back;
- tile position committed to persistent player state.

Invariants:
1. local coordinate and local progress updates occur together;
2. they require `b2 == true` and `aWInt == 0`;
3. snapshot writeback occurs only for non-probe normal helper exit;
4. persisted tile commit occurs only when local update and snapshot writeback both occur.

## 3. Immutable input snapshot

`LegacyCollisionInputSnapshot` captures raw values required to reason about one target-cell check:
- player tile coordinate and stage dimensions;
- requested delta and probe mode;
- packed player state and active direction;
- movement/interpolation counters and scripted-movement flag;
- optional auxiliary-grid presence and target value;
- global gate, player type-10 mode, and boundary mode;
- raw foreground cell value;
- raw player-layer value and packed metadata;
- selected above-cell foreground/player-layer values.

Packed integers and byte-like values are retained as raw integers. The snapshot does not normalize, decode, or infer their semantics. Auxiliary-grid presence is separate from its integer value so absence is not confused with a real zero.

## 4. Test coverage

- trace local delta/progress calculation in non-probe mode and persisted writeback;
- trace local delta/progress calculation in probe mode without persisted writeback;
- early return with animation but no snapshot writeback;
- separate b2 and b3 flags;
- type-19 probe early exit without scripted world mutation;
- immutable snapshot raw-value preservation;
- target coordinate calculation;
- optional auxiliary-grid presence;
- positive stage-dimension validation.

These are synthetic unit fixtures, not a collision evaluator and not a test against proprietary stage assets.

## 5. Important boundaries

- No grid evaluator or collision decision algorithm.
- No typed gameplay outcome taxonomy beyond source-oriented observation fields.
- No map mutation or object push implementation.
- No coordinate commit in runtime.
- No animation calls.
- No Android dependencies.
- No claim that the current snapshot includes every neighbor read from every branch; additional branch-specific samples must be added when the evaluator contract is specified.

## 6. Next work item

**DEV-003F — Branch-complete neighborhood snapshot schema.** Enumerate all additional cell reads and metadata dependencies per source branch, especially two-cell lookahead, horizontal/vertical neighbors, player-layer 48 geometry, type-19 path search, and stage-edge guards. Extend the immutable snapshot using explicit named samples rather than a mutable grid or guessed defaults. Then define branch-specific evaluator fixtures.
