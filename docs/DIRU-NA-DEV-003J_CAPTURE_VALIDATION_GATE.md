# DIRU-NA-DEV-003J — Capture Validation Gate

**Status:** Capture-to-validator gate and synthetic unit tests committed; collision evaluator not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA

## 1. CI correction

Run #24 executed 94 core tests and failed one assertion. The type-19 downward foreground scan now reports the dedicated `TYPE19_FOREGROUND_RUN_NOT_TERMINATED` status when it reaches the stage edge without a non-19 terminator. The existing test still expected generic `OUT_OF_BOUNDS`; its expected status is corrected in the follow-up commit. Run #25 is the validation run for that correction.

## 2. Integration

`LegacyCollisionCaptureValidation` combines:
1. a `LegacyCollisionNeighborhoodCapture.Result`;
2. the selected collision branch;
3. the set of source context fields available to the caller.

It produces one of three outcomes:
- `VALID`: capture and branch input validation both pass; snapshot is exposed;
- `CAPTURE_INCOMPLETE`: capture failed bounds or dynamic scan; no snapshot is exposed;
- `INPUT_INCOMPLETE`: required branch samples or scalar context are missing; no snapshot is exposed.

The missing-sample and missing-context sets are immutable. This prevents downstream code from accidentally treating partial capture as valid input.

## 3. Tests

Synthetic tests cover a valid target capture, missing source context, out-of-bounds capture propagation, and immutable missing-context reporting.

## 4. Important boundary

This gate does not evaluate foreground/player-layer semantics, passability, movement direction, object push, scripts, animation, or mutations. It is only a strict input gate. No gameplay behavior is claimed as implemented.

## 5. Next milestone

**DEV-003K — First Pure-Core Collision Predicate.** Select one narrowly scoped, source-grounded predicate with no side effects, model its exact raw inputs and truth table, then add tests against synthetic fixtures. Do not implement a full movement result until the original helper's return flags and side effects are modeled independently.
