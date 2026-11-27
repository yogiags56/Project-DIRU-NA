# DEV-003CS-3A — Stable GameSession Capture Seam

**Status:** Implemented as a narrow core-owner projection; not a production checkpoint provider.  
**Branch:** `dev-003cr-snapshot-provider-boundary`.

## Change

`GameSession.captureStableView()` now returns the current immutable `GameplayRenderSnapshot` only when called from the GameSession owner thread and ordinary movement interpolation is inactive.

It rejects:
- calls from a non-owner thread;
- calls while accepted movement is still interpolating.

Tests cover an initially settled state, rejection during interpolation, successful capture after the movement clock settles, and owner-thread enforcement.

## Exact scope

The returned projection contains the existing immutable `StageGrid`, `PlayerMotionState`, and simulation tick. `StageGrid` already defensively copies its three source layers. No mutable array alias is exposed through this view.

## Explicit non-claims

This is **not** a complete gameplay checkpoint, save-game schema, or implementation of `CheckpointSnapshotProvider`. It does not contain:
- packed background/runtime metadata grid and per-cell progress bytes;
- mutable object/auxiliary record tables and packed tile metadata;
- door-group controller state;
- mode-specific/moving-object state;
- all progression and retry counters;
- exact scheduler reconstruction or collision transaction state.

Those gaps are documented in `DEV-003CS-2_PERSISTED_FIELD_EVIDENCE_MATRIX.md` and its copy/restore source trace. The current `GameSession` still uses an injected collision policy and is not yet a complete playable gameplay owner.

## Next gate

Before a production provider can be written:
1. finish the CS-2 source-field and invariant closure;
2. implement the first reproducible gameplay owner and authoritative mutable stage model;
3. define an atomic restore candidate spanning all supported authoritative state;
4. add tests proving capture/restore leaves live state unchanged on every invalid path;
5. run core tests, Android storage tests, and APK assembly on the current branch head.

The stable view is a useful owner-thread seam for future work, but must not be passed to persistence as though it were a full checkpoint.
