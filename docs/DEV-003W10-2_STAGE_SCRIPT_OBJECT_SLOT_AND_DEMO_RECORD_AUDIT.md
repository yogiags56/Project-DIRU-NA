# DEV-003W10.2 — Stage script-object slots and demo.f record audit

## Source identity
Pinned decompiled source:
- `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- `kubikaugustyn/DiamondRushSource/src/main/java/c.java`
- Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

Static source audit only. No retail runtime, stage binary fixture, or asset bundle was executed in this milestone.

## 1. Critical correction: overloaded helper identity

The stage-preprocessing call `iVoid(x,y)` is **not** the object allocator. The exact overload is:

`private void iVoid(int x, int y)`

It:
- ORs feature bit `0x400000` into `cLong`;
- clears the packed foreground cell's low byte;
- writes foreground low-byte code `0x0E` (14), preserving the remaining packed bits.

Therefore prior W10.1 wording that described this call as invoking an object initializer was inaccurate. The call sites for player codes 40, 42, 6, 24 and 27 must not be interpreted as allocating a `c` script/demo object merely because the method name contains `iVoid`.

The no-argument `iVoid()` is a different overload used for seal-arrow/UI animation. Name-based matching without signature is unsafe.

## 2. Two-stage script-slot construction

The stage preprocessing pass builds two parallel arrays:
- `aClasscArr = new c[n11]`: one script/demo runtime slot per counted stage entry.
- `pByteArr = new byte[n11]`: parallel byte tag associated with each slot.

The second stage-grid scan populates `pByteArr`:
- Foreground low-byte codes 0 and 30 append the foreground packed high value as the tag.
- Player-layer code 26 appends tag 25.
- Player-layer code 42 appends tag 11.
- Player-layer code 24 appends tag 22.
- Player-layer code 27 appends tag 23.
- Player-layer code 40 appends tag 24.

The scan is Y-major then X-minor (different from the first preprocessing mutation pass, which is X-major then Y-minor). Preserve this distinct ordering.

Other runtime arrays are allocated in the same post-scan phase:
- `dByteArr = new byte[n8 * 2]`
- `kByteArr = new byte[(n9 + 1) * 2]`
- group countdown/text arrays based on the group bit mask.
- moving-object auxiliary data may be allocated separately based on `awInt`.

Do not assume every entry in `aClasscArr` is populated by the stage scan; the arrays are sized by counts, and the script/demo instances are materialized by the later resource-loading sequence.

## 3. demo.f chunk lookup and c.aVoid(tag)

The `c` class is a script/demo record interpreter, not a generic physics entity class.

Its `aVoid(int tag)` method:
1. Opens the configured `demo.f` resource stream.
2. Iterates chunk records by 16-bit tag.
3. For a matching tag, reads a 16-bit record count and 32-bit payload length.
4. Reads the selected payload.
5. Reads a list of sprite IDs, sorts them, and loads missing sprite records from the configured `demoSpr.bin` resource into shared `c.aClassfArr`.
6. Parses the selected demo payload into bytecode-like record buffers held by `aByteArrArr`.
7. Supports nested record groups (record type 0) with per-child length entries.

If a chunk tag does not match, the reader skips its length-delimited payload. Exceptions are swallowed by this method; a native port should report structured diagnostics rather than silently treating a malformed record as a valid empty script.

The script object stores:
- `aByteArrArr`: parsed script record buffers;
- `aByteArr`: currently selected/active buffer;
- `aInt`: cursor into available buffers;
- `bInt`: script tick/frame counter;
- `bLong`: last execution timestamp;
- animation/UI state fields and a 16-element short scratch array.

## 4. Record-type parser shape

The `c.aVoid(tag)` parser materializes records from demo.f payloads. Confirmed record IDs and normalized buffer sizes/fields include:

| Record ID | Materialized shape |
|---|---|
| 18 | 8 bytes; one byte argument, 24-bit color, active flag |
| 16 / 17 | 6 bytes; 16-bit argument plus byte argument |
| 11 / 12 | 6 bytes; two 16-bit values |
| 13 | 12 bytes; three 16-bit values plus two 10000 sentinel coordinates |
| 4 | 16 bytes; seven 16-bit values |
| 14 / 15 | 2 bytes |
| 7 | 2 bytes |
| 1 | 12 bytes; two tile coordinates converted to pixels, duration, two 10000 sentinels |
| 6 | 6 bytes; 32-bit value |
| 26 | 10 bytes; two 16-bit coordinates plus 32-bit value |
| 25 | 8 bytes; two 16-bit coordinates plus two byte values |
| 5 | 7 bytes; two 16-bit coordinates plus byte value |
| 27 | variable `length + 8`; 16-bit string length and copied text bytes |
| 2 | variable `length + 11`; byte argument, 16-bit values and copied text bytes |
| 9 | 8 bytes; three 16-bit values |
| 10 | 3 bytes; one byte argument |
| 0 | nested group marker; child count followed by length-prefixed child records |

These are parsed script/demo records, not the stage foreground tile IDs. Do not merge the two namespaces.

## 5. Runtime slot loading and selection

In the resource-loading sequence, when the selected demo resource index is in the 0..15 range:
- If index is within `aClasscArr.length`, construct `new c(this)` in that slot and call `aVoid(pByteArr[index])`.
- At index 15, if the array contains more than 16 entries, remaining slots from 16 onward are also constructed and loaded using their corresponding `pByteArr` tags.
- This path is driven by resource/demo loading state, not directly by player-layer case 40/42/24/27.

At runtime, `kVoid(int tag)`:
- resets local script-selection counters;
- scans all `pByteArr` entries;
- assigns `aClassc` whenever a tag matches, so if duplicate tags exist the last matching slot wins;
- invokes `aClassc.aVoid()` to reset/select the script's active buffer.

The `c.aVoid()` no-argument method resets all script buffers and returns the first buffer via `bByteArr()`. This is distinct from `c.aVoid(int tag)`, which parses a tagged resource chunk.

The script object also has:
- `aVoid(Graphics)` for rendering script records and associated overlays;
- `aVoid()` for selecting/resetting the first script buffer;
- `bVoid()` for advancing/ticking the current buffer;
- `aBoolean()` for checking completion state;
- `aByteArr()` for reset-and-select behavior;
- `bByteArr()` for selecting the next buffer.

## 6. Ownership and lifecycle conclusions

- `aClasscArr` and `pByteArr` are parallel stage-scoped slot/tag tables.
- Each slot is a script/demo interpreter instance loaded from a tag in demo.f; it is not a generic per-cell entity object.
- `c.aClassfArr` is a shared static sprite cache populated from demoSpr.bin.
- Script byte buffers are owned by each `c` instance; sprite objects are shared.
- Stage teardown explicitly iterates `aClasscArr`, frees each non-null script instance and its sprite resources, then nulls the array. Full ownership of shared sprite cache and cross-slot references still needs a separate reference-count/lifetime audit.
- The stage grid's cell-to-tag mapping is indirect through `pByteArr`; runtime tag lookup is by byte equality, not by direct cell coordinates.
- Duplicate tags are possible from this source structure and need fixtures; do not silently replace the linear scan with a map unless last-match-wins behavior is preserved.

## 7. Deterministic test plan

1. Assert first-pass and second-pass scan orders independently.
2. Verify each source case that appends to pByteArr and the exact tag written.
3. Verify array lengths equal the corresponding preprocessing counters.
4. Parse each fixed-size demo record type and assert field offsets, signedness, and normalized lengths.
5. Parse variable text records 2 and 27 with zero, short, and maximum valid lengths.
6. Parse nested record type 0 with zero, one, and multiple children; verify child length boundaries.
7. Resolve a tag with one slot and with duplicate slots; assert last-match-wins.
8. Verify tag not found behavior is surfaced safely; original method can leave active script null and later dereference it.
9. Verify no resource chunk match and truncated payload produce diagnostics, not silently accepted state.
10. Verify teardown frees each non-null script object once and does not double-free shared sprite resources.

## 8. Scope and next step

W10.2 is static source audit/documentation only. No native script interpreter, resource parser, sprite loader, or stage runtime is implemented.

Next milestone: DEV-003W10.3 — demo.f/demoSpr.bin resource format and script execution lifecycle. Trace tick/update ordering, record execution/termination, nested buffer selection, rendering-vs-update behavior, and resource cache ownership. Then define a bounded native parser contract and binary fixtures before implementation.
