# DEV-003BZ — Snapshot Cross-Table Invariants

## Scope

This milestone strengthens checkpoint restore validation across object records, packed auxiliary links, auxiliary entry words, and the coordinator sidecar association map.

## Invariant model

1. Every sidecar association must point to a live object handle.
2. The object's semantic packed link must decode to the same auxiliary slot as the sidecar.
3. The referenced auxiliary entry must be in state 1 (allocated) or state 3 (ready).
4. One auxiliary slot may have only one sidecar owner.
5. Type-7 records remain linkless.
6. Reverse validation now checks every non-Type-7 record whose raw packed link is nonzero: it must have a matching sidecar association for that exact slot.
7. A zero packed link is deliberately not treated as proof of missing association. Slot 0 is representable and encodes as zero, while freshly allocated/unassociated records also have zero link bits. Type/state-specific semantics are not yet sufficiently established to disambiguate these cases.

## Implementation

`CrossTableLifecycleCoordinator.restore()` continues to restore into a temporary ObjectRecordTable candidate first, validates all supplied sidecar associations, and only then mutates the live coordinator. A reverse pass now iterates the object snapshot and rejects any non-Type-7 record with a nonzero packed link that has no matching association or has a different sidecar slot.

This closes the previous one-way-validation gap for nonzero links while preserving valid default records and slot-0 ambiguity.

## Regression tests

Added four tests:
- reject a nonzero packed link with no sidecar association;
- reject swapped/mismatched sidecar ownership even when the sidecar slots are individually unique;
- allow an unassociated record whose raw link field is zero;
- reject an association whose auxiliary entry has been cleared to state 0.

Existing restore tests continue to cover stale handles, invalid slots, duplicate association slots, invalid auxiliary states, Type-7 linklessness, and atomic failure behavior. CI is the authority for execution status.

## Known boundaries

- Zero cannot currently distinguish an unassociated default record from an association to auxiliary slot 0. A future type/state/call-site audit must resolve this without inventing semantics.
- This model validates the coordinator sidecar and three-bit link field; it does not yet reproduce all original packed tile metadata mutations or every auxiliary reader.
- No claim is made that the native association map is byte-exact with the original JAR.
- No gameplay integration or runtime parity is claimed.

## Commits

- Reverse packed-link snapshot validation: `e90099dfcc0d7203a6131ab35d018e153c2b11a8`
- Reverse invariant regression tests: `8b40685627612d4c64459905851574470e8c6113`

## Next milestone — DEV-003CA: Allocator Call-Site & Link Semantics Matrix

1. Enumerate every call to the six-argument allocator and record its link argument, record type, and associated auxiliary lifecycle.
2. Audit the four-argument allocator independently, including its -1 failure contract.
3. Trace every read of bits 54..56 to the corresponding bLongArr consumer.
4. Establish whether zero means no association, slot 0, or a type/state-dependent sentinel at each call site.
5. Add table-driven tests only for semantics supported by pinned source evidence.
6. Run core JVM tests and Android debug APK assembly before marking the milestone validated.

No original shipping-JAR bytecode parity, runtime trace, APK artifact, or gameplay integration is claimed in this document.