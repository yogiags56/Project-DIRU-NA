# DEV-003W10.12 — Opcode callback and stage mutation audit

## Evidence identity

Pinned decompiled source: `kubikaugustyn/DiamondRushSource/src/main/java/c.java` and `src/main/java/i.java`, revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This is a source-backed static audit. It does not execute script records or mutate a native stage.

## Callback target used by opcodes 5, 8, and 9

The legacy target is `i.aVoid(short x, short y, byte playerLayer, int backgroundValue)`. Its body performs exactly two writes, in this order:

1. `crtStagePlayerLayer[x][y] = playerLayer`
2. `bIntArrArr[x][y] = backgroundValue`

Call-site arguments in `c.cVoid`:

| Opcode | x | y | player-layer byte | background int | Result |
|---:|---|---|---|---|---|
| 5 | signed-short at +2 | signed-short at +4 | byte at +6 | 0 | set player-layer cell and clear background value |
| 8 | signed-short at +2 | signed-short at +4 | 0 | signed-short at +6 | set player layer to 0 and set background value |
| 9 | signed-short at +2 | signed-short at +4 | 0 | signed-short at +6 | same callback as opcode 8 |

Opcode 8 has no independent switch body. It falls through into opcode 9, so it must carry the same three-u16 wire operands and normalized 8-byte record layout as opcode 9. The decoder and normalizer now implement that shape, with regression tests.

## Opcode 25 and 26 ordered writes

- Opcode 25 writes `crtStageForegrondLayer[x][y]` with `(record[+7] << 8) | record[+6]`, then sets its completion byte.
- It falls through into opcode 26.
- Opcode 26 writes `bIntArrArr[x][y]` from a little-endian u32 at +6, then sets completion.
- It then falls through to opcode 5, opcode 8, and opcode 9. Therefore the final callback also writes player layer 0 and background from opcode-9 operand at +6.
- The subsequent opcode-27 and opcode-2 labels are reached as well. Opcode 27 may return early when its completion/prompt state is already set; otherwise execution continues to dialogue handling.

This sequence must be represented as explicit ordered effects, not independent opcode handlers with early returns. Coordinate indexing in the source is `[x][y]`.

## Opcode 10 and camera update

Opcode 10's handler updates the player's control fields on tick zero; on later ticks it may clear the control byte and mark the record complete when the player's `jInt` reaches zero. It then calls `i.aVoid()`.

The no-argument `i.aVoid()` implementation updates the camera position from player coordinates, clamps the camera to stage bounds, and copies camera coordinates into render offsets. It is a camera-follow operation, not evidence of a general game-loop update. Native integration must keep it behind an explicit camera adapter and preserve its call ordering.

## Bounds and state ownership requirements

The source uses stage arrays in `[x][y]` order. Native adapters must validate x against width and y against height before any write. The source callback itself has no visible local bounds checks; invalid coordinates would fail at array access. Do not emulate silent clipping.

The initial native adapter should:
- receive explicit stage width/height and mutable foreground/background/player-layer storage;
- reject out-of-range coordinates deterministically before mutation;
- preserve the legacy write order for each opcode;
- keep the stage adapter independent of Android, rendering, sprite caches, and UI;
- expose a fake in-memory stage for unit tests;
- keep camera-follow behavior separate from stage-cell mutation.

## Deferred source trace

The full downstream semantics of opcode-5/8/9 cell writes are distributed across stage initialization and gameplay update routines. This audit confirms the exact immediate writes and their argument mapping, but does not claim complete semantic understanding of every affected tile or entity.

## Gate for DEV-003W10.13

Build a small, pure-Java stage mutation boundary and tests for:
- opcode-5 player-layer/background write order;
- opcode-8 and opcode-9 identical callback target but distinct record opcodes;
- opcode-25 then 26 then 5/8/9 fall-through sequence;
- signed-short coordinate interpretation;
- out-of-range x/y fail before any partial write;
- no aliasing or direct mutation of decoded wire records.

No interpreter tick engine, renderer, camera update, sprite cache, or full gameplay integration is introduced in this audit.
