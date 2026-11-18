# DIRU-NA-DEV-002C — State-aware Input Gate and Action Dispatch

**Status:** Core dispatcher and regression tests committed; CI execution pending  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA

## 1. Scope

This step models the in-stage input gate and separates ordinary movement, active interaction, and special movement input. The dispatcher is pure Java core logic. It returns a decision; it does not call gameplay actions, move the player, mutate stage grids, or access Android APIs.

## 2. Pinned source evidence

Read-only audit input:
- Repository: kubikaugustyn/DiamondRushSource
- Revision: 215c71d04057d66a1b08419b1a4b1043ae3e0881
- File: src/main/java/i.java
- Method: handleKeyPresses(), in-stage state branch, approximately lines 1190–1265.

Observed order:

1. If hBoolean OR ajBoolean, clear the shared key mask. Processing continues after this clear.
2. Call the cheat-key check (outside this dispatcher; no cheat implementation is added).
3. Apply the in-stage movement gate:
   (clInt != 0 AND NOT isKeyPressed(32944)) OR ayInt != 0 OR xBoolean OR nByte <= 0 OR playerAnimator.crtAnimationI == 19 OR EBoolean.
   If true, clear the key mask and leave the in-stage input branch.
4. If a contextual interaction object exists, center mask 32784 invokes its center action; otherwise broad confirm mask 32944 sets its confirm flag. The key mask is cleared regardless of whether either key was pressed.
5. If special movement timer aTInt > 0, use a separate input chain:
   - Up 4097 -> vertical offset -5
   - Down 262146 -> vertical offset +5
   - Left 16388 -> horizontal offset -5
   - Right 65544 -> horizontal offset +5
   - Center 32784 -> consume the special-movement tick without an offset
   - Broad confirm 32944 -> call special confirm action aeVoid and consume the tick
   - If none is pressed, do not decrement the timer and do not clear the key mask.
   Any recognized special-branch input consumes one timer unit and clears the key mask.
6. Otherwise use normal direction priority UP, DOWN, LEFT, RIGHT. Center is checked only after all four directions; it clears input and executes checkpoint/context-action logic. Broad confirm is not the normal center action.

## 3. Implemented core API

LegacyInputDispatcher.dispatch(HeldKeyState, Context) returns one of:
- BLOCKED
- NO_INPUT
- NORMAL_DIRECTION
- CENTER_ACTION
- INTERACTION_CENTER
- INTERACTION_CONFIRM
- INTERACTION_IDLE
- SPECIAL_MOVEMENT
- SPECIAL_CENTER_CONSUMED
- SPECIAL_CONFIRM_ACTION
- SPECIAL_WAIT

The result also carries the selected legacy direction, special offset delta, and whether one special-movement timer unit should be consumed.

The dispatcher clears the supplied held-key state only in source-observed clear paths. It leaves normal directional input and special-wait input intact, matching the source handler's local behavior. The caller remains responsible for applying returned actions and decrementing the external timer.

## 4. Regression coverage

Nineteen JUnit 4 test methods cover:
- early state lock and clear-before-gate ordering;
- confirm exception to the clInt lock;
- independent movement lock predicates;
- interaction center/confirm priority and unconditional input clear;
- special movement direction offsets and priority;
- special center and confirm action handling;
- special wait behavior;
- normal direction input preservation;
- normal center action clearing;
- broad confirm remaining distinct from normal center.

These tests validate the modeled dispatch contract only. They do not validate Android callbacks, real-time input timing, player movement, or original-device runtime behavior.

## 5. Non-goals and cautions

- No Android key-code or touchscreen mapping.
- No star-key/death/respawn action dispatch; star 256 remains separately audited.
- No cheat-key implementation.
- No direct execution of contextual object methods or aeVoid.
- No decrement of aTInt inside the core dispatcher; it reports whether the caller should consume one unit.
- No assumption that special movement is ordinary tile movement.
- Opaque source fields remain named by their decompiled identifiers until further call-site audits justify semantic names.

## 6. Next work item

**DIRU-NA-DEV-003A — Player Movement State and Interpolation Contract.** Map pending direction consumption, the 18 -> 12 -> 6 -> 0 interpolation countdown, tile coordinate updates, animation selection, and collision checks before implementing any player movement.
