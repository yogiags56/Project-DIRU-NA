# DEV-003BO — Cross-Table Link Encoding & Transition Regression Model

## Scope

Introduce an isolated pure-Java model of the legacy three-bit object-record-to-auxiliary-slot link. Keep safe native behavior separate from explicit legacy truncation behavior.

This milestone does not connect the codec to gameplay, alter the object-record allocator, or claim original shipping-JAR runtime parity.

## Source-derived contract

The audited decompiled source has:
- a 15-entry auxiliary `bLongArr` indexed 0..14;
- a three-bit auxiliary slot field in `aLongArr` at bit offset 54;
- state-1 auxiliary allocation and a later state-1-to-state-3 transition through the linked slot.

A three-bit field can represent only values 0..7. The source's auxiliary allocator can allocate slots 8..14, but storing those values in the three-bit field loses upper bits. The exact runtime reachability and consequences of this mismatch remain unresolved pending matching original-JAR evidence.

## Implementation

Added:
`core/src/main/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/AuxiliaryLinkCodec.java`

The codec exposes three deliberately separate operations:

1. `encodeSafe(slot)`: accepts 0..7, rejects 8..14 as `UNREPRESENTABLE_SLOT`, and rejects values outside 0..14 as `INVALID_SLOT`.
2. `decode(raw)`: extracts the low three bits, returning 0..7.
3. `encodeLegacyTruncating(slot)`: explicitly models low-three-bit truncation for forensic tests only. This must not be used by the safe native path.

The codec also exposes `roundTripsExactly(slot)` as a convenience predicate.

## Regression tests

Added:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/objectrecord/AuxiliaryLinkCodecTest.java`

Five tests cover:
- exact round trips for all representable slots 0..7;
- safe rejection of slots 8..14;
- explicit legacy aliases (8→0, 9→1, ..., 14→6);
- invalid negative and out-of-range indices;
- three-bit decoder masking.

The existing raw 15-entry auxiliary checkpoint storage remains unchanged. No typed state machine or cross-table live reference was added.

## Safety decision

Do not silently truncate an auxiliary index in native code. Callers must use `encodeSafe` and handle `UNREPRESENTABLE_SLOT` explicitly. Use `encodeLegacyTruncating` only in tests or a future compatibility layer where reproducing the legacy behavior is intentional and documented.

## Limitations

- The codec models the width and truncation mechanics, not the original game allocator's occupancy policy.
- It does not establish whether slots 8..14 are reachable on real levels or whether they cause observable aliasing in the shipping game.
- It does not model state 1→3 transition ordering or state-3 consumption yet.
- No original shipping JAR is present or provenance-matched.
- No Android build, gameplay integration, or runtime trace is claimed.

## Next milestone — DEV-003BP: Auxiliary Transition State Machine & Cross-Table Guard Tests

1. Add a typed, isolated auxiliary-entry transition model only for source-proven fields: status, direction/subtype, X and Y.
2. Model the state-1→state-3 transition and state-3 consumption/clear ordering.
3. Require a valid representable cross-table link for every state transition initiated by an object record.
4. Test missing links, slot 8..14 rejection, stale link references, and atomic failure.
5. Preserve unknown packed bits unchanged through transitions.
6. Keep the legacy truncation codec separate from the safe model.

Do not integrate the transition model into gameplay until source and binary provenance are sufficient.
