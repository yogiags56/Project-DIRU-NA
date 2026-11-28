# DIRU-NA DEV-003W9.6 — Event queue consumers and alVoid side effects

## Source identity
Static source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

Static decompiled-source audit only; no retail-device equivalence is claimed.

## 1. Event ring state and producer

The three-argument `cVoid(x,y,type)` producer:
1. Uses `aeInt` as the tail index.
2. Writes X and Y as Java bytes into `aByteArr[tail*2]` and `aByteArr[tail*2+1]`.
3. Writes event type as a byte into `cByteArr[tail]`.
4. Resets animation-frame counter `bByteArr[tail]` to zero.
5. Advances tail: `aeInt = (aeInt + 1) & 7`.

There is no full-queue check in the producer.

## 2. Consumer and queue semantics

- `adInt` is the head/read cursor; `aeInt` is the tail/write cursor.
- Two render paths iterate from head to tail, wrapping each index with `& 7`. They draw each queued event at tile coordinates multiplied by 24 and offset by the camera.
- If `cByteArr[index] < 0`, the renderer uses sprite set 2, animation 0, with the frame selected from `bByteArr[index]`.
- Otherwise it uses sprite set 9, animation `cByteArr[index]`, frame `bByteArr[index]`.
- The update/tick path walks pending entries from head to tail and increments each entry's `bByteArr` frame counter.
- When a frame counter reaches the animation's frame count, the head advances with `adInt = (adInt + 1) & 7`. The loop continues over the entries that were pending at the start of that update.
- Thus entries remain queued/visible for their animation lifetime; the queue is both an animation work list and a render list, not a one-frame notification queue.

### Capacity and overflow inference
- The backing arrays have eight indexed slots, but head==tail is the empty condition.
- With no separate full flag or count, only seven entries can be unambiguously pending at once in the conventional ring interpretation.
- The producer does not reject or defer a write when full. If eight unconsumed writes wrap tail onto head, the cursors become equal and the queue appears empty; subsequent writes can overwrite unread slots.
- This is a source-derived overflow hazard, not a proven observed retail bug. Preserve legacy behavior if exact compatibility is required, but a native implementation should expose diagnostics and fixture this case rather than silently inventing a new overflow policy.

## 3. Cursor initialization/reset

- The inspected stage transition path sets both `aeInt` and `adInt` to zero together before invoking `saveCheckpoint()`.
- No other direct assignments to these cursors were found in the inspected source file. Recheck after any source-version change.
- Checkpoint restore does not establish a separate durable event queue snapshot in the audited path; queue lifecycle is reset around the stage transition rather than treated as checkpointed gameplay state.

## 4. Event producer call-site scope

The three-argument producer is not exclusive to special-object code 48. Call sites include:
- `aBoolean(x,y)` with event type 1 after recognized special-object mutation.
- Player/object interactions that enqueue type 5.
- Other gameplay events enqueue types derived from state/time, including negative type values.
- An object collection/removal path enqueues type 3.
- Additional movement and environment branches enqueue event types from local state.

Therefore the queue is a shared transient animation/event subsystem. It must not be named or scoped as a SpecialObject-only queue.

## 5. `alVoid()` exact side effects

The no-argument `alVoid()` is a different helper from the event ring and from the coordinate overload `jVoid(x,y)`.

Observed behavior:
1. Returns early when `kByte` is 3, 4, or 5 and `aqInt > 0`.
2. If `cmInt >= 0`, decrements `mByteArr[cmInt]`.
3. When that countdown reaches zero, plays sound ID 8.
4. Scans only interior cells: X from 1 through width-2, Y from 1 through height-2.
5. Selects foreground cells whose low byte is 0x11 and whose high portion equals the completed `cmInt` group.
6. Inspects the foreground tile immediately above:
   - Above code 7: if the upper payload's 0xF0 bits are clear, sets the 0x10 payload flag while retaining code 7 and writes cByte=24 at that above cell.
   - Above code 14 or 33: rewrites that foreground cell to the low-byte code only, clearing its packed high metadata.
7. It does not directly enqueue an event-ring item.

The coordinate helper `jVoid(x,y)` sets foreground metadata bit `0x10000000` at the specified cell and then calls `alVoid()`. Its callers often clear/mutate foreground or player cells before invoking it.

### Interpretation boundary
This behavior is consistent with a timed group/door or foreground-state completion mechanism, but the semantic name "door" should remain provisional until all cmInt/mByteArr producers and stage fixtures are traced. The code-level behavior above is the authoritative record.

## 6. Native architecture implications

- Name the subsystem `StageEventAnimationQueue` or equivalent, not `SpecialObjectEventQueue`.
- Keep head and tail as separate 3-bit cursors; preserve byte narrowing and animation frame-count selection.
- Model the eight physical slots and explicitly test the head==tail ambiguity and overflow hazard.
- Rendering must consume the same pending range as update; do not dequeue immediately after draw.
- Reset both cursors together at the observed stage transition.
- Keep `alVoid` group countdown/foreground completion logic separate from the event animation queue.
- Preserve the interior scan order and exact packed-bit operations in any future implementation.
- Before production implementation, trace every writer of `cmInt`, `mByteArr`, and `aqInt`; establish stage fixtures for zero countdown and each above-tile case.
- Stage border invariants remain unproven. The fact that `alVoid` scans interior cells does not prove every other helper's neighbor reads are safe for all assets.

## 7. Milestone boundary

W9.6 is source audit/documentation only. No production event queue, door/group countdown system, renderer, object runtime, collision activation, or GameSession integration is added.
