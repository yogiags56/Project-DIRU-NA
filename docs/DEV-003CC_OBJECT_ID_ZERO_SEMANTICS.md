# DEV-003CC — Object-ID Zero Semantics & Safe Legacy Compatibility

## Pinned source and evidence boundary

Source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

The Project-DIRU-NA repository contains no original shipping Diamond Rush JAR/class files. Findings here are static observations from the pinned decompiled Java source, not bytecode parity or runtime proof.

## 1. Raw field zero and object reference are distinct concepts

The tile metadata object-ID field is three bits wide. Its raw values are 0..7. The raw field reader at the packed metadata helper returns zero as an ordinary numeric value; it does not itself assign semantic meaning to zero.

The generic object-record accessor around lines 15903–15908 converts a one-based object ID to an array index by decrementing it before indexing `aLongArr`. It has no visible zero guard in that helper. Therefore passing raw tile ID 0 to this accessor would address index -1 and fail.

Likewise, the object-record packed field accessor reads `aLongArr[n - 1]`. Callers must establish a valid one-based ID before using it.

## 2. Source paths that clear or inspect tile metadata

`cVoid(x,y,group)` around lines 16712–16717 clears all nine bits of the selected metadata group. This sets the object-ID subfield to zero as part of clearing the whole group; it does not prove that every zero-valued group was produced by this clear operation.

The generic metadata writer around lines 15890–15898 updates only the selected field, preserving the other bits in the metadata word. It masks the supplied value to the field width rather than validating it. For the three-bit object-ID field, values above 7 are truncated.

At the Type-1/6/7 replacement path around lines 16222–16253, the code reads a target group object ID and uses it in the object-record accessor. The inspected snippet does not show a local explicit `id == 0` guard immediately before the accessor. The full control-flow conditions and runtime reachability of that path need original bytecode or a runtime trace before concluding whether zero can reach the `n - 1` access.

At the Type-5 traversal creation path around lines 16573–16577, the allocator's returned one-based ID is written to the three-bit tile object-ID field. This remains the previously identified 15-slot-versus-3-bit representability hazard.

## 3. Native API change

`PackedTileMetadataCodec` now exposes two distinct operations:

- `readObjectId(rawWord, group)`: returns the raw three-bit numeric value, including zero. Use this for forensic inspection and exact bit preservation.
- `readObjectReference(rawWord, group)`: returns `NO_OBJECT_REFERENCE` for zero, `OK` for IDs 1..7, and preserves invalid-group errors. Callers should use this semantic reader before resolving a tile object.

This does not claim the original game always treats zero as empty in every context. It establishes a safe native lookup contract: zero is never passed to a one-based object-array accessor.

## 4. Regression tests

Three tests were added:

1. Raw ID zero remains readable as zero, while the safe reference reader returns `NO_OBJECT_REFERENCE`.
2. IDs 1..7 remain valid object references.
3. Invalid group errors are preserved by the safe reader.

Existing tests continue to reject IDs 8..15 for the three-bit field and verify group-local writes and clears.

## 5. Conclusions

Established:
- The raw object-ID field can contain zero.
- Clearing a nine-bit tile group writes zero to the object-ID subfield.
- The one-based object-record accessor decrements the supplied ID and has no local zero guard in the inspected helper.
- The native lookup API now prevents raw zero from becoming an array index.

Not established:
- That every zero field in the original game means an empty tile.
- That the Type-1/6/7 replacement path can actually reach its object-record accessor with ID zero.
- That original shipping JAR bytecode exactly matches the decompiled source.

No gameplay integration, original-JAR parity, runtime trace, or Android APK validation is claimed.

## 6. Commits

- Zero-aware native reader: `0e068560e9057c6b2a7cd66664ad071929776cfb`
- Zero and valid-ID tests: `7664566f5b68b0cbfd10ff4f9c4d5df08ec236e2`

## 7. Next milestone — DEV-003CD: Guarded Tile Lookup & Call-Site Contract

1. Enumerate all native tile lookup call sites and migrate semantic lookups to `readObjectReference`.
2. Ensure no code path passes raw ID zero into one-based object-array access.
3. Trace all source callers around the Type-1/6/7 replacement lookup and identify upstream conditions that constrain zero.
4. Add integration tests for empty tile groups, stale links, and valid IDs 1..7.
5. Preserve raw read/write APIs for serialization and forensic compatibility.
6. Run the core JVM suite and Android debug APK assembly; record exact outcomes before declaring validation complete.