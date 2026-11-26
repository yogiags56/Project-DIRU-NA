# DIRU-NA-DEV-003R — Player-Layer Narrow Evaluator

**Status:** Initial pure evaluator and synthetic unit tests committed; no full player-layer simulator
**Source:** kubikaugustyn/DiamondRushSource/src/main/java/i.java
**Pinned revision:** 215c71d04057d66a1b08419b1a4b1043ae3e0881
**Method:** i.aBoolean(int,int,boolean)

## Scope

This evaluator handles only player-layer branches whose return flag and limited local effects can be represented without additional geometry or neighboring cells. It does not read a grid, mutate the stage, animate, push objects, or commit player state.

## Implemented cases

- 34/35: helper return flag equals probe mode.
- 31/49: helper return flag false.
- Explicit blocking set: 1,2,4,5,6,7,11,14,19,24,26,27,33,40,41,42,43,45,50,51,52,53 -> true.
- 10: if player type mode (xByte) equals 3, return true. For non-probe only, if current player metadata <= 0, return a metadata-write intent (set to 1). Otherwise preserve incoming return flag.
- 28: preserve incoming return flag and emit clear-active-direction intent.

## Explicitly not handled

- -1: requires stage-edge and neighbor/door conditions.
- 0/8/9/47: requires two-cell lookahead, geometry, movement counter and possible early return.
- 48: requires two-cell object-push geometry and multiple map writes.
- Unknown/default values: source default changes local delta and return flag, but animation depends on active direction; kept outside this narrow milestone until a richer intent model exists.

## Probe and mutation boundary

Probe mode suppresses the type-10 metadata-write intent. This evaluator only returns an intent; caller code must apply it transactionally after verifying probeMode == false.

## Tests

LegacyPlayerLayerEvaluatorTest covers probe-dependent values, explicit blocking/non-blocking sets, type-10 mode and probe behavior, type-28 direction-clear intent, and refusal to handle complex/unknown branches.

## Next

DEV-003S: model player-layer 0/8/9/47 lookahead as a pure geometry decision with explicit sampled cells, counter, direction, and kByte; preserve the source's immediate false early-return path and ensure probe mode never mutates world state. Then separately model 48 push geometry and -1 boundary/door conditions.