# DIRU-NA-DEV-002A — Platform-independent Input State Capture Primitive

**Status:** Source-compatible input bitset and tests committed; CI execution pending  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA

## 1. Scope

Implement only the source-observed held-key bitset primitive in the core module. Do not implement Android key-code mapping, touch gesture mapping, direction priority, movement, or the full game loop in this step.

## 2. Static source evidence

Audit inputs:
- DIRU-NA-AUDIT-001 — main loop and key-state model.
- DIRU-NA-AUDIT-004 — key capture and directional query masks.
- Candidate source pinned at kubikaugustyn/DiamondRushSource commit 215c71d04057d66a1b08419b1a4b1043ae3e0881.

Observed capture behavior:
- key press ORs the normalized game-key bits into the shared integer mask;
- key release clears the corresponding bits;
- a query tests whether any bit in a requested mask is currently held;
- the capture layer permits multiple simultaneous held bits;
- gameplay branch priority is handled separately by ordered conditionals.

Audited direction query masks:

| Direction | Source mask | Meaning |
|---|---:|---|
| Up | 4097 | Query group for up |
| Right | 65544 | Query group for right |
| Down | 262146 | Query group for down |
| Left | 16388 | Query group for left |

These values are not Android key codes or movement velocities. They must not be mapped to touch positions or physical keys until the platform adapter is designed.

## 3. Implemented core types

- HeldKeyState: mutable integer bitset with press, release, any-bit query, snapshot, and clear operations.
- LegacyInputMasks: named constants for the four audited directional query masks.

The implementation contains no Android imports, Canvas dependencies, event queue, rendering calls, or timing logic.

## 4. Regression tests

Nine JUnit 4 tests cover:
- empty state;
- OR-combination of held bits;
- clearing only released bits;
- any-bit query semantics;
- full clear;
- zero query behavior;
- exact audited direction mask values;
- disjointness of the four direction masks;
- query integration between held state and a legacy direction mask.

The disjointness test records the audited constants' current bit layout; it does not establish that diagonal movement is supported.

## 5. Deliberate non-goals

- No key-code-to-mask mapping is added.
- No touch-to-mask mapping is added.
- No directional priority is guessed.
- No diagonal movement is inferred from simultaneous bit capture.
- No Android lifecycle or input callback is allowed to mutate game state directly.
- No game-loop timing or simulation tick is implemented here.

## 6. Validation

The code is committed with deterministic JUnit tests. Validation is pending the next GitHub Actions run. Do not mark this step complete until core:test passes on the commit.

## 7. Next work item

DIRU-NA-DEV-002B — Direction Query and Input Priority Trace. Reconfirm the exact source branch order and all action masks before adding a pure-core direction intent resolver. Keep key capture, input interpretation, and gameplay movement as separate layers.
