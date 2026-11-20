# DEV-003BR — Raw Object-Record Auxiliary-Link Field Audit

## Scope

This milestone adds a raw packed-field codec based on the pinned decompiled source. It does not integrate that codec into ObjectRecordTable or the lifecycle coordinator.

Source:
- `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- pinned revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`

## Source evidence

The six-argument object-record allocator at approximately lines 16719–16747:
- scans object record IDs 1..15;
- clears the selected raw `aLongArr` slot;
- for non-Type-7 records, writes type 1 at bits 28..30, writes the auxiliary link argument at bits 54..56, and writes packed tile metadata;
- for Type-7 records (argument `b == 7`), follows a separate branch that calls tile metadata cleanup and sets type 7 at bits 28..30, without writing bits 54..56.

The source's movement path around lines 16082–16148 reads the three-bit value at bits 54..56 and uses it as the zero-based auxiliary `bLongArr` index.

Other source facts:
- Object record IDs are one-based (1..15), while auxiliary indices are zero-based (0..14).
- The auxiliary link field is three bits wide and can represent only 0..7.
- The Type-7 creation branch is explicitly different and does not write the link field.
- The source does not establish that all 15 auxiliary slots are safely linkable from object records.
- Reclassification call sites can change type bits independently; the raw field's retention/clear policy must be decided per call path, not inferred from a generic type setter.

## Implementation

Added `LegacyObjectRecordLinkCodec.java`.

It provides:
- `LINK_BIT_OFFSET = 54`, `LINK_BIT_WIDTH = 3`, and a matching mask;
- raw extraction of bits 54..56;
- semantic read with an explicit Type-7 linkless result;
- safe write for non-Type-7 records, rejecting invalid or unrepresentable indices without modifying the original raw record;
- clearField that clears only bits 54..56 and preserves all other bits.

This is deliberately distinct from `AuxiliaryLinkCodec`: the latter models the value conversion, while this codec handles the actual packed `aLongArr` word.

## Tests

Added `LegacyObjectRecordLinkCodecTest.java` with seven test methods:
1. write/read for indices 0..7;
2. preservation of all non-link bits;
3. rejection of indices 8..14 without mutation;
4. invalid index and null type rejection;
5. explicit Type-7 linkless behavior;
6. clearing only the three-bit field;
7. explicit retention versus clearing policy during reclassification.

The focused object-record test source inventory increases from 38 to 45 methods. This is a source count only; the latest CI results must be checked independently.

## Important boundaries

- The codec is not yet used by ObjectRecordTable or CrossTableLifecycleCoordinator.
- The coordinator still maintains a logical sidecar association map.
- No raw legacy payload field is yet stored per ObjectRecordTable record.
- The codec does not assume whether a reclassified record should retain or clear its old link. The source has multiple transitions and this policy needs call-site-specific proof.
- The Type-7 exception is modeled explicitly as no link, rather than treating a zero field as a valid auxiliary slot.
- The exact original shipping JAR has not been provided; source-to-binary parity remains unverified.

## Next milestone — DEV-003BS: Raw Link Storage & Reclassification Integration

1. Decide a versioned raw payload representation for each ObjectRecordTable record.
2. Add the raw word to record snapshots and restore validation without changing current public gameplay semantics.
3. Integrate the packed codec behind explicit attach/read APIs.
4. Preserve Type-7's linkless state and define how reclassification handles retained legacy bits.
5. Test allocation, raw link write/read, reclassification, release/reuse, and checkpoint/restore.
6. Revisit the coordinator so its association map is validated against the packed raw field rather than acting as an independent authority.
7. Recheck GitHub Actions and resolve dependency access before claiming test pass.

No gameplay integration, exact shipping-JAR parity, passing CI, or APK build is claimed.
