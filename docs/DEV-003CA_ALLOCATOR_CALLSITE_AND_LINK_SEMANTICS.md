# DEV-003CA — Allocator Call-Site & Link Semantics Matrix

## Pinned source

Repository: `kubikaugustyn/DiamondRushSource`
File: `src/main/java/i.java`
Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

## DEV-003BZ CI prerequisite

All three DEV-003BZ workflows completed successfully:
- reverse validation implementation: success;
- reverse invariant tests: success;
- documentation: success.

## Critical distinction: two overloaded allocator families

The decompiled source contains two distinct allocation methods with similar obfuscated names. They allocate different arrays and must not be conflated.

### A. Four-argument allocator — auxiliary `bLongArr`

Method around line 15863: `aByte(byte b, byte b2, byte b3, byte b4)`.

Observed behavior:
- scans `bLongArr` indices 0..14;
- chooses the first entry whose low four-bit state field is zero;
- writes state 1 into bits 0..3;
- writes `b4` into bits 4..5;
- writes `b` into bits 6..12 and 27..33;
- writes `b2` into bits 13..19;
- writes `b3` into bits 20..26;
- returns the zero-based auxiliary index 0..14, or -1 when full.

Observed caller around line 16603 passes `(n25, n24, n26, n23)`, then looks up the matching entry by coordinates through `bByte(n24,n26)` and changes its state field to 2. This is an auxiliary/controller allocation, not an `aLongArr` object-record allocation.

### B. Six-argument allocator — object `aLongArr`

Method around line 16719: `aByte(int x, int y, byte initialType, byte direction, byte group, byte auxiliaryLink)`.

Observed behavior:
- scans one-based object IDs 1..15;
- checks the three type bits at offsets 28..30 for a free slot;
- clears the selected 64-bit object record;
- writes initial type bits, the three-bit auxiliary link at bits 54..56, tile metadata object ID, coordinates, group, and direction;
- returns the one-based object ID 1..15, or follows the source's unsafe exhausted-index path documented in DEV-003BA.

The only direct six-argument invocation found in the pinned source is around line 16573, in the traversal-created Type-5 path. It passes `(n31, n30, 8, -2, 2, 0)`, then explicitly overwrites type bits 28..30 with value 5 and writes the returned object ID into the tile metadata's three-bit object-ID field.

## Semantics matrix

| Path | Array | Index domain | Link/payload behavior |
|---|---|---:|---|
| Four-argument allocator | `bLongArr` | 0..14 | Creates auxiliary entry in state 1; returns -1 on full |
| Four-argument allocator caller | `bLongArr` | returned 0..14 | Finds entry by coordinate and changes state to 2 |
| Six-argument allocator | `aLongArr` | 1..15 | Stores caller-supplied 3-bit link; returns one-based object ID |
| Type-5 traversal call | `aLongArr` | 1..15 | Supplies link value 0, then reclassifies record bits to Type-5 |
| Controller state 4 | `bLongArr` | 0..14 | Consumes entries with state 3 and clears selected whole word |
| Type-1/6 replacement branch | `aLongArr` | object IDs from tile metadata | Copies source bits 54..56 to target; inspected branch does not write `bLongArr` |

## Important unresolved representation hazard

The six-argument object allocator returns IDs 1..15, but the tile metadata write at the Type-5 call site uses a three-bit field (bits 0..2 of a nine-bit group). A three-bit field can encode only 0..7. In Java bitfield insertion, writing IDs 8..15 into a three-bit field retains only their low three bits; ID 8 therefore aliases to 0, ID 9 to 1, and so on.

This is a source-level representability hazard, not proof of a shipping-game defect. Possible explanations include a reachable-object limit, a different effective allocator invariant, a decompiler/source mismatch, or another lifecycle restriction not yet established. Do not change the native model to mimic silent aliasing until original JAR bytecode or a runtime trace confirms the behavior.

The link argument at the sole inspected Type-5 six-argument call is explicitly zero. That call does not prove how nonzero link values are supplied in other object-record creation paths or whether zero is a sentinel versus auxiliary slot 0.

## Native-model implications

- Keep the 15-entry auxiliary table separate from the three-bit object-record link field.
- Keep the 15-entry object table separate from the three-bit tile metadata object-ID field.
- Continue rejecting unsafe slot aliasing in native APIs.
- Do not infer that object IDs, auxiliary indices, and encoded link values are the same namespace.
- Existing codecs intentionally expose safe encoding and explicit legacy truncation as separate operations; the native path must continue using safe encoding.

## Evidence boundary

This is a complete call-site audit of the allocator invocations found in the pinned decompiled `i.java` file, not a proof about original shipping-JAR bytecode. The six-argument allocator's full-capacity path, the practical maximum live object count, and the semantics of zero link values remain unresolved. No runtime trace, APK build, or gameplay integration is claimed here.

## Next milestone — DEV-003CB: Object-ID Reachability & Original-JAR Evidence

1. Trace every read of the tile metadata three-bit object ID and every corresponding `aLongArr[id - 1]` access.
2. Determine whether object IDs 8..15 are ever produced and then consumed, or whether game logic enforces an effective maximum of seven.
3. Inspect original JAR bytecode for the allocator and packed metadata writer if the original artifact is available.
4. Add adversarial native tests for object IDs 7, 8, 9, and 15, ensuring the safe model rejects unrepresentable IDs without aliasing.
5. Revisit zero-link semantics only after all source call sites are classified.
6. Run the full core JVM suite and Android debug APK assembly after any implementation change.

No source-code production behavior was changed in this milestone; it is an evidence and contract clarification.