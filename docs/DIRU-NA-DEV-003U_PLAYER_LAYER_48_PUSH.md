# DIRU-NA-DEV-003U — Player-layer 48 push evaluator

**Source:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
**Pinned revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`
**Method:** `i.aBoolean(int n, int n2, boolean probe)`

## Reconstructed branch

Player-layer 48 uses `targetX = playerX + 2*n` and `targetY = playerY + 2*n2`. The current metadata bit `0x8` selects the pair offsets: when set, offsets are 0 and +1; otherwise offsets are -1 and 0.

Destination player-layer cells are `(targetX,targetY+offsetA)` and `(targetX,targetY+offsetB)`. Both must be negative. The guard sample is `(playerX+n, playerY+n2+offsetB+1)` and must be non-negative.

The branch decrements the local movement counter first, then accepts only when `(counterAfter < 0 || probe) && n != 0` and all three sampled player-layer conditions pass.

## Accepted decision

- Return flag true and movement-block bit set.
- Outside probe, request two destination player-layer writes with the original player-layer value, two source-cell clears to -1, metadata copy/update intents, animation byte 18 at destination B, and foreground-cache write at `(targetX,targetY+offsetA-1)`.
- Destination metadata A copies source metadata A.
- Destination metadata B is `(sourceMetadataB & 0xFFFFFFF0) | direction`, where right is 2 and left is 4.
- Probe accepts the same collision decision but suppresses every world write.

## Rejected decision

- Preserve incoming return flag.
- If `n != 0`, request movement-block bit and clear local direction (`lByte=0`).
- No world mutation is requested.

## Implementation boundary

`LegacyPlayerLayer48Snapshot` is an immutable carrier for caller-sampled cells and exact named coordinates. `LegacyPlayerLayer48Evaluator` returns intents only; it does not read arrays, perform bounds checks, mutate state, call animation functions, or integrate with `GameSession`.

The source contains three identical writes to the same foreground-cache cell. The evaluator models the final observable value as one cache-write intent rather than reproducing redundant identical assignments.

## Validation status

Unit tests cover both offset layouts, right/left direction metadata, counter/probe gates, destination and guard blockers, and mutation suppression in probe mode. These are synthetic tests; CI and recorded gameplay replay must still confirm parity.