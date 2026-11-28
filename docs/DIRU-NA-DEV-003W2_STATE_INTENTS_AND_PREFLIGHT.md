# DIRU-NA DEV-003W2 — State intents and preflight commit boundary

## Added

- `LegacyCollisionStateIntent` models typed player fields, animation fields, movement flags, and coordinate-scoped foreground-cache values. Invalid channel/field combinations are rejected.
- `LegacyCollisionMutationPlan` now holds both grid writes and state writes in immutable ordered lists. Probe mode rejects either kind of write. Duplicate destinations are rejected independently by grid layer/coordinate or state channel/field/coordinate.
- `LegacyCollisionMutationTarget` defines the owner-thread adapter contract: dimensions, reads for expected-value validation, and a single all-or-nothing commit method.
- `LegacyCollisionMutationApplier` checks owner-thread ownership, dimensions, all grid and coordinate-scoped state bounds, and every expected old value before it calls commit exactly once. Probe plans are empty no-ops.
- Tests cover state intent type safety, probe isolation, duplicate state fields, bounds, stale grid/state values, owner-thread rejection, commit rejection, and one atomic commit invocation.

## Critical atomicity contract

The applier itself cannot guarantee rollback for an arbitrary mutable implementation. Atomicity is delegated to `LegacyCollisionMutationTarget.commitAtomically(plan)`, whose implementation MUST commit every grid and state intent or none. The current target in tests is a recording fake; there is no production adapter for StageGrid/GameSession yet.

## Remaining work

1. Implement a production mutable stage/state owner compatible with the current immutable `StageGrid` model, without leaking mutable arrays.
2. Implement commit rollback or copy-on-write swap in that owner.
3. Map evaluator decisions for player-layer 0/8/9/47, 48, -1 and foreground branches into typed intents, preserving exact source write order and semantics.
4. Add race/stale-state tests and source-derived replay fixtures.
5. Integrate with GameSession only after production atomicity is proven.
