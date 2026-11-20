# DEV-003BX — Auxiliary Payload Preservation & Association Invariant Audit

## Scope

This milestone audits whether the source replacement branch mutates the auxiliary bLongArr payload and tightens coordinator tests around preserving the auxiliary word. It also corrects one DEV-003BW regression-test expectation that had incorrectly expected the source record to be reclassified rather than released.

## Pinned source

- Repository: kubikaugustyn/DiamondRushSource
- File: src/main/java/i.java
- Revision: 215c71d04057d66a1b08419b1a4b1043ae3e0881

## Source evidence

### Auxiliary array lifecycle

The source declares two independent 15-entry arrays: aLongArr for object records and bLongArr for auxiliary/controller entries. Stage initialization allocates both separately (around lines 15840–15844). Checkpoint capture copies aLongArr to cLongArr and bLongArr to dLongArr (around lines 15226–15230); restore copies both back independently (around lines 15353–15354).

The packed accessors are separate: aInt/aVoid access aLongArr around lines 15900–15906, while another aInt/aVoid pair accesses bLongArr around lines 15910–15914.

### State-4 consumption

Controller state 4 scans all 15 bLongArr entries. For entries whose low four state bits equal 3, it reads direction and coordinates, clears the entire selected bLongArr word, computes a next coordinate, and may invoke the seven-argument object allocator in its Type-7 mode (around lines 16455–16515).

This is an auxiliary-entry consume path. It is not evidence that every object-record replacement consumes the corresponding auxiliary word.

### Type-1 / Type-6 / Type-7 replacement branch

In the inspected controller branch (around lines 16200–16300), the source resolves the target tile coordinate/group from the source object's packed fields, reads the target tile's object ID, copies source object-record bits 54..56 to the target object record, writes target type bits as Type-2 and updates target coordinate/direction fields, then calls whole-record release on the old source object.

No write to bLongArr occurs in this replacement sequence. The auxiliary word is therefore preserved at the array level by this specific path; it is not consumed or rewritten as part of the record transfer. This conclusion is scoped to the inspected branch, not all game transitions.

## Native regression coverage

Added a test that performs coordinator replacement with the source auxiliary word in both state 1 (allocated) and state 3 (ready). It asserts the exact 64-bit auxiliary payload is unchanged by replacement and that the sidecar association moves to the target handle. This verifies preservation rather than accidental reconstruction.

Also corrected the independent-association rejection test: a rejected replacement must leave the source record at its original Type-1, not Type-2.

## CI correction

The first DEV-003BW CI run exposed the incorrect test expectation at CrossTableLifecycleCoordinatorTest.java:283. The source implementation correctly leaves the source record untouched when target association validation rejects the operation. The test now asserts Type-1 remains unchanged. The new preservation test is included in the same correction commit.

Previous DEV-003BW run: coordinator implementation workflow succeeded; test and documentation workflows failed because of that test assertion. A new workflow is triggered by the correction commit. Its final result must be checked before claiming DEV-003BX validation.

## Known boundaries

- The source is decompiled Java, not the original shipping JAR; no bytecode parity or runtime trace is claimed.
- The source inspection proves no bLongArr write in this specific replacement branch; it does not prove every replacement-like path preserves the auxiliary array.
- The native coordinator still represents the association map as a sidecar. It is not a byte-exact implementation of the original packed tile metadata array.
- Auxiliary slots 0..14 exist, while the object link field is only three bits wide. Safe native attachment remains limited to representable slots 0..7; no assumption is made that slots 8..14 are unreachable in every game path.
- No gameplay integration is performed.

## Commits

- Corrected rejected-replacement assertion and added state-1/state-3 payload-preservation test: 244977f970edb6d34556e2bf36c05288b11ad7b2

## Next milestone — DEV-003BY: Packed Tile Metadata & Full Auxiliary Reader Inventory

1. Audit every read/write of tile metadata array eIntArrArr that stores object IDs, including coordinate/group transforms.
2. Trace all call paths that read object link bits 54..56 and their corresponding bLongArr consumers.
3. Determine whether auxiliary slots 8..14 can be referenced through any alternate path or whether their purpose differs.
4. Verify coordinator snapshot invariants for dangling records, duplicate slot ownership, invalid state words, and object links with no association.
5. Compare the native model against original JAR bytecode or runtime traces if available.
6. Only then decide whether the sidecar can be replaced with a source-faithful representation.

No passing CI, APK artifact, shipping-JAR parity, runtime parity, or gameplay integration is claimed in this document.