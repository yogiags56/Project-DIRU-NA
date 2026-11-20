# DEV-003BS — Raw Link Storage & Reclassification Integration

## Scope

Integrated the packed legacy auxiliary-link field into the platform-independent ObjectRecordTable model and made the lifecycle coordinator validate that field. This remains a native safety model, not gameplay integration.

## Raw record payload

Each occupied ObjectRecordTable record now stores a 64-bit `rawLegacyPayload` representing its packed legacy `aLongArr` word.

- Allocation initializes the type bits (28..30) to the selected record type.
- Auxiliary link read/write delegates to `LegacyObjectRecordLinkCodec`.
- Link writes replace only bits 54..56 and preserve every other payload bit.
- Reclassification changes only bits 28..30; the auxiliary link and all other raw bits are retained.
- Release drops the record and therefore its payload; a reused slot receives a fresh payload initialized for the new type.
- Snapshots carry the raw payload per occupied record and restore validates that packed type bits agree with the record's typed RecordType.

The existing schema-version constant remains at 1 for this in-memory model iteration. A durable on-disk snapshot format has not been declared; before external persistence is introduced, the schema must be versioned and migration behavior specified.

## Coordinator integration

`CrossTableLifecycleCoordinator.attach` now writes the actual packed three-bit link after object allocation and tile linking. If the raw link cannot be written (including Type-7's linkless behavior), it restores the pre-operation object-table snapshot and returns failure.

`markReady` and `consume` verify that the packed object-record link matches the coordinator's logical association before touching the auxiliary entry.

Coordinator restore now validates:
- live object handle;
- auxiliary slot is representable;
- auxiliary state is 1 or 3;
- no duplicate auxiliary slot;
- packed record link equals the sidecar association.

The sidecar map remains as a lifecycle convenience, but it is no longer trusted independently of the raw packed field.

## Tests

Added four ObjectRecordTable tests:
1. raw link write/read and preservation through Type-2 -> Type-5 reclassification;
2. Type-7 rejects link writes and reports explicit linkless status;
3. raw payload round-trip through checkpoint/restore and stale-handle rejection;
4. corrupted type bits in snapshot are rejected without replacing live payload.

Added two coordinator tests:
1. packed link is written on attach and used as a guard for transitions;
2. restore rejects disagreement between sidecar mapping and packed field atomically.

The raw codec retains its seven focused tests. The focused object-record test source inventory is now 51 methods (prior 45 plus six). This is a count of authored test methods, not a claim that they all passed.

## Source and behavior boundaries

- Type bits are mapped to the enum's numbered Type-1..Type-7 values.
- The three-bit link is zero-based and only represents auxiliary indices 0..7.
- Type-7 is explicitly linkless in the audited allocator branch.
- Reclassification preserves the old link field because the source call sites write type bits independently; no generic rule to clear the field was established.
- Whether every Type-2 -> Type-5 path must retain a link remains call-site dependent and requires further source/binary parity work.
- The model stores a raw payload per occupied native record but does not yet reproduce every other legacy packed field or exact allocator side effect.
- No shipping-JAR binary parity or runtime trace is claimed.

## CI status

The previous DEV-003BQ workflow `44745945297` completed successfully, including core JVM tests and Android debug APK assembly/upload.

Workflows triggered by DEV-003BR commits were still in progress when last checked. The current DEV-003BS commits have not yet been validated by a completed workflow in this document.

## Commits

- Raw payload storage: `faad83dc9a105d54599bd6dd8924d7e5f91f6c24`
- ObjectRecordTable tests: `a333b16a6889e4b03fe70bc978b6a40542f6944f`
- Coordinator packed-link integration: `105b1477b395189b0e0102d64b3d8cb168555855`
- Coordinator regression tests: `88385af95713e903f3179984d023309f7eb49d48`

## Next milestone — DEV-003BT: Legacy Record Lifecycle & Type-Reclassification Matrix

1. Audit every source call site that changes type bits 28..30 and classify whether bits 54..56 are retained, rewritten, or irrelevant.
2. Model explicit per-transition policies rather than assuming one universal reclassification rule.
3. Test release/reuse and checkpoint restore against raw payload/generation changes.
4. Verify Type-7's linkless exception across all creation paths, not only the six-argument allocator.
5. Resolve and record current GitHub Actions results for DEV-003BS.
6. Keep gameplay integration blocked until source call-site coverage and shipping-JAR provenance are adequate.

No passing CI for DEV-003BS, APK output, gameplay integration, or original-JAR parity is claimed.
