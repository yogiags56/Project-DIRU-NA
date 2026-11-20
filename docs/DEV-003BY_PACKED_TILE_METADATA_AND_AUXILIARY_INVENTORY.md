# DEV-003BY — Packed Tile Metadata & Auxiliary Reader Inventory

## Scope and source pin

Static audit of the packed tile metadata word `eIntArrArr`, object records `aLongArr`, and auxiliary/controller records `bLongArr`.

Source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`
File length at inspected revision: 17,763 lines.

## 1. Packed tile word layout

The generic tile metadata accessor uses a nine-bit group stride: `shift = group * 9 + fieldOffset`. Each tile word therefore contains three groups, occupying bits 0..8, 9..17, and 18..26. Bits 27..31 are outside these three groups.

Within each group, the inspected code uses:
- bits 0..2: three-bit object-record ID (read at `aInt(..., group, 0, 3)` and written by the matching `aVoid` overload);
- bits 3..6: four-bit field;
- bits 7..8: two-bit field.

`cVoid(x,y,group)` clears all nine bits of one selected group, leaving the other two groups unchanged. The generic writer updates only its selected field and preserves the rest of the word. This is now represented by `PackedTileMetadataCodec`.

Important: the middle and upper fields remain semantically opaque in the native codec. The decompiled source contains several consumers with different meanings; their semantics must not be generalized from the bit widths alone.

## 2. eIntArrArr access inventory

All 42 textual occurrences in the pinned source were enumerated. The field declaration is line 488. Lifecycle and access sites:

- 1273, 9139, 10800: read the current player's group field at offset 3, width 4.
- 4597, 12128: allocate the tile metadata grid; 4906: null it during teardown.
- 6592, 6600, 7668, 7710, 8823, 8899, 8990, 12106, 14323, 14387, 14609, 14885, 15412, 15417, 15484, 15499, 16561, 16583, 16785, 16787: collision, traversal, stage-state, or neighboring-tile reads. These are not all object-ID reads; many only test whether a packed tile word is zero/nonzero.
- 15221: checkpoint copies the full grid into `fIntArrArr`.
- 15351: restore copies the saved grid back.
- 15892, 15894: generic packed-field read/modify/write using the group*9 layout.
- 15978: reads a four-bit field at offset 3 from a selected group.
- 16225: reads a two-bit field at offset 7 from a selected group.
- 16239, 16252: reads a four-bit field and a three-bit object ID from the selected group in the Type-1/6/7 controller branch.
- 16397: reads a four-bit field at offset 3.
- 16520, 16523: obtains a row from the metadata grid for traversal.
- 16713, 16715: clears one selected nine-bit group by masking/shifting.
- 16785, 16787: neighbor-cell nonzero checks.

Direct object-ID lookup is specifically evidenced at 16252, where the selected group's bits 0..2 are read and used as the object-record index. The controller then reads the selected `aLongArr` record. Do not treat every `eIntArrArr != 0` check as a record reference.

## 3. aLongArr object-record inventory

All 30 textual occurrences were enumerated. The array is declared at line 489, allocated as 15 entries at 15842 (and again in an initialization path at 17752), and nulled at teardown line 4908. Checkpoint copy and restore are at 15226 and 15353.

Packed object-record field accessors are at 15900 and 15906. Whole-record clear is `fVoid(byte)` at 16659 and 16730. The inspected controller path reads record type/coordinates/group around 16218..16227, copies bits 54..56 from the source record to the target at 16252..16253, writes target Type-2 and related fields, then releases the source.

Allocator and release sites include the six-argument allocator around 16703..16738 and the distinct four-argument allocator around 15860..15890. These must remain separate: they have different failure behavior and field initialization.

## 4. bLongArr auxiliary/controller inventory

All nine textual occurrences were enumerated:
- declaration: 490;
- teardown: 4910;
- checkpoint copy/restore: 15230 and 15354;
- 15-entry allocations: 15844 and 17753;
- packed reader/writer: 15910 and 15914;
- explicit entry consumption/clear: 16469.

State 4 iterates all 15 entries, checks the low four-bit state field, reads direction/coordinates from the same word, and clears the selected `bLongArr` entry before it continues. No bLongArr write appears in the inspected Type-1/6/7 object-record replacement sequence around 16205..16410. That proves payload preservation only for this branch, not for all controller transitions.

## 5. Three-bit link versus 15-entry arrays

The object-record link field at bits 54..56 can encode only values 0..7. The source arrays `aLongArr` and `bLongArr` each have 15 entries. The six-argument allocator searches one-based object IDs 1..15; its non-Type-7 branch writes the supplied three-bit value into bits 54..56. The auxiliary table index and the three-bit value are therefore not automatically interchangeable. The source call sites and the exact semantic meaning of each supplied value must be audited before imposing a one-to-one mapping.

The native implementation currently treats auxiliary slots as zero-based 0..7 for safe link encoding, while preserving a 15-entry opaque auxiliary array. This is a conservative model constraint, not proof that original game logic never addresses entries 8..14 by another mechanism.

## 6. Native implementation and tests

Added `PackedTileMetadataCodec` with:
- generic read/write for fields fully contained in a nine-bit group;
- dedicated three-bit object-ID read/write;
- clear-one-group behavior matching `cVoid`;
- range validation and preservation of unrelated bits.

Added `PackedTileMetadataCodecTest` covering object-ID writes across all three groups, preservation of other fields, clear-one-group behavior, 4-bit and 2-bit fields, invalid group/field/value boundaries, and unsigned shifts on negative Java ints.

These tests are source-level unit coverage. The CI workflow for the codec and tests must complete before claiming pass.

## 7. Checkpoint invariant gaps identified

The current coordinator restore validates every supplied sidecar association against a live record, its packed link, an allocated/ready auxiliary state, and unique slot ownership. However, this one-way validation does not yet prove the reverse invariant for every eligible object record: a live record with a nonzero packed link but no sidecar association may still be accepted. Zero is ambiguous because a freshly initialized record also has zero link bits. A follow-up must define type/state-specific association requirements rather than simply requiring an association for every non-Type-7 record.

The object-table restore validates record generations, record type bits, Type-7 linklessness, and tile links, but it does not yet cross-validate all raw object-record link values against auxiliary entry states. This is a deliberate open audit item.

## 8. Commits

- Packed tile metadata codec: `ee63bd0898b8e855a6fd85a039eed2936a668cb9`
- Codec tests: `25ee94e9fb9daa618b8eb28bdb2139deabf36484`

## 9. Next milestone — DEV-003BZ: Snapshot Cross-Table Invariants & Call-Site Semantics

1. Build a type/state-specific matrix for whether bits 54..56 are meaningful, zero/default, or required to resolve a bLongArr entry.
2. Audit every call site of the six-argument allocator and trace its supplied three-bit link value to the eventual bLongArr reader.
3. Audit the four-argument allocator independently; do not merge its `-1` failure contract with the six-argument allocator's failure path.
4. Add snapshot corruption tests for orphan packed links, missing sidecar associations, duplicate slot ownership, empty/invalid auxiliary states, and Type-7 records.
5. Determine whether slots 8..14 have any reachable association path distinct from bits 54..56.
6. Re-run full core JVM tests and Android debug APK assembly.

No original shipping-JAR bytecode parity, runtime trace, gameplay integration, or full semantic proof is claimed.