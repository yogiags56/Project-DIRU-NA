# DIRU-NA DEV-003W — Transactional collision mutation intents

## Purpose

Create a common immutable write-set vocabulary for the source-derived collision evaluators before connecting them to gameplay. This increment deliberately does not apply writes.

## API

- `LegacyCollisionMutationIntent` describes one typed layer write with coordinate, expected old value, and requested new value.
- `LegacyCollisionMutationPlan` holds an immutable ordered list of intents and the probe-mode marker.
- The builder rejects null intents, repeated destinations on the same layer, use after build, and every attempted world write in probe mode.
- The same coordinate on distinct layers is permitted because those are distinct storage cells.

## Atomicity boundary

The plan is a proposal only. It does not read current stage values, verify expected values, mutate StageGrid, update player motion, set animation, update foreground caches, or commit partial writes. The expected value is carried for a future applier to compare before any commit.

A future applier must validate every destination and expected value before changing any layer, then commit the complete write-set on the single gameplay-owner thread or commit nothing. Player-local state, animation state, and cache changes need explicit typed intents before they can be included in that transaction.

## Probe guarantee

A probe plan is empty by construction: its builder rejects all world-write intents. Evaluators must continue to suppress their own mutation requests as well; this plan is a second defensive boundary, not a replacement for source parity.

## Tests

Synthetic tests verify ordering and immutability, probe write rejection, duplicate destination rejection, cross-layer coordinate distinction, and one-shot builder use.

## Not yet implemented

- Mapping the player-layer 0/8/9/47, 48, and -1 evaluator decisions into this common plan.
- Player-local field, animation, movement-block, and foreground-cache intent types.
- Bounds and expected-value validation against a mutable gameplay-owned stage.
- Atomic apply/rollback semantics and owner-thread enforcement.
- Golden replay fixtures and Android gameplay integration.
