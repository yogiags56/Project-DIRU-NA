# DIRU-NA-DEV-003V — Player-layer -1 boundary and door evaluator

**Source:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
**Pinned revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`
**Method:** `i.aBoolean(int n, int n2, boolean probe)`

## Boundary rule

Target player cell is `(playerX+n, playerY+n2)`. If x or y is at either outermost stage coordinate (0 or dimension-1), player-layer -1 returns true and clears movement-block bit 0x8.

## Interior exceptions

All exceptions are suppressed in probe mode. Outside probe, any one of these permits the -1 branch to leave the incoming return flag unchanged (false in the original method) and clear local direction:

1. Horizontal movement (`n != 0`), sampled player-layer at `(targetX, playerY+1)` equals 0, and its metadata low three bits equal 3.
2. Door counter A >= 24 and either vertical neighbor at `(targetX,targetY-1)` or `(targetX,targetY+1)` is player-layer 28 with metadata bit 0x8 clear.
3. Door counter B >= 24 and either vertical neighbor is player-layer 28; this second condition does not inspect metadata bit 0x8.

If no exception matches, return true. The movement-block bit is cleared in every non-edge and edge path.

## Implementation

`LegacyPlayerLayerMinusOneSnapshot` carries caller-sampled neighbor values and exposes their source coordinates. `LegacyPlayerLayerMinusOneEvaluator` is pure and returns decision flags only. It performs no grid access or mutation.

## Tests and limitations

Synthetic tests cover all four stage edges, horizontal near-below exception, exact metadata low-bit match, door-counter thresholds, the distinct bit-0x8 rules, and probe suppression. The caller must supply correct stage dimensions and samples. Recorded gameplay replay and CI are still required.