# DEV-003CJ — Deterministic Checkpoint Binary Serialization

## Scope

Added a native DIRU-NA binary container codec for the current `CrossTableCheckpoint` model. This format is explicitly separate from the original Diamond Rush save format; no claim of byte-level compatibility with the shipping JAR is made.

## Binary contract — format version 1

All integer fields use Java `DataOutputStream` big-endian encoding.

Field order:

1. Four-byte magic `DIRU` (`0x44495255`).
2. Four-byte binary format version (`1`).
3. Four-byte outer checkpoint schema version.
4. Four-byte inner object-table snapshot schema version.
5. Record count, then records sorted by one-based slot ID. Each record contains slot ID, generation, record-type ordinal, anchor X, anchor Y, anchor group, and raw legacy payload.
6. Generation-array length and its fixed 15 integer values.
7. Tile-link count and entries sorted by X, Y, group; each entry contains those three coordinates and one-based object slot ID.
8. Auxiliary-entry array length and its fixed 15 64-bit words.
9. Association count and entries sorted by object-handle value; each entry contains a 64-bit handle and auxiliary slot.
10. Packed-metadata count and entries sorted by X, Y, group; each entry contains the tile key and original 32-bit metadata word.

## Decoder validation

- Rejects null, empty, or greater-than-16-MiB input.
- Rejects wrong magic and unsupported binary format version.
- Bounds record, tile-link, association, metadata, and fixed-array counts before allocating collections.
- Rejects duplicate tile keys, duplicate association handles, and duplicate packed-metadata keys.
- Rejects invalid tile coordinates/groups and invalid record type ordinals.
- Rejects truncated input and any trailing bytes.
- Reconstructs the immutable model snapshots, then validates the full checkpoint through `restoreInto` before returning it.

## Encoder validation

The encoder first validates the complete checkpoint using `restoreInto` against an isolated coordinator. It then sorts all unordered collections to provide deterministic bytes for the same logical snapshot. It preserves raw 32-bit tile metadata words and raw 64-bit object/auxiliary payloads without reinterpretation.

## Regression coverage

`CrossTableCheckpointBinaryCodecTest` covers:

- deterministic repeated encoding;
- round-trip restoration of records, links, packed metadata, and auxiliary associations;
- empty-world round-trip;
- truncated payload rejection;
- trailing-byte rejection;
- unknown binary version rejection;
- invalid magic and oversized-input rejection.

## Commits

- Binary codec: `5838fc62f114ee573d3a0417973989c844669239`
- Codec regression tests: `274328f5b4efcda078f898081fbd6f527cce2382`

CI workflows for these commits must be checked before declaring the milestone validated. The codec is a native model persistence layer, not a production gameplay integration or original-JAR save parser.

## Next milestone — DEV-003CK: Serialization Hardening & Compatibility

1. Verify round-trip preservation of nontrivial opaque payload bits and all auxiliary array entries.
2. Test malformed counts, duplicate keys, invalid enum ordinals, unknown outer and inner schema versions, and truncated data at multiple offsets.
3. Consider an integrity checksum only if the persistence threat model and format requirements justify it.
4. Document forward/backward compatibility and migration behavior for format version changes.
5. Keep original-game save compatibility blocked on primary binary-format evidence.