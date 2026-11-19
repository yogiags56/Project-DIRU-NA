# DIRU-NA-DEV-003AU — aLongArr Write/Clear, Reset, and Snapshot Inventory

**Status:** Source-wide inventory of the audited aLongArr storage completed; capacity semantics documented, no production behavior changed
**Parent:** DEV-003AT
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Storage lifecycle boundaries

The 15-entry `aLongArr` is created in two observed places:
- `arVoid()` at 15840–15850 when the stage path is initialized: creates `aLongArr`, `cLongArr`, `bLongArr`, and `dLongArr` as new 15-word arrays.
- global/static setup at 17752: creates `aLongArr` and `bLongArr` as new 15-word arrays.

The teardown path at 4908–4911 nulls `aLongArr`, `cLongArr`, `bLongArr`, and `dLongArr`. This is object lifecycle teardown, not an in-level per-record release.

## 2. Snapshot / restore semantics

When the relevant `lBoolean` path is active, the state-capture code at 15216–15235 copies all 15 words of `aLongArr` into `cLongArr` and all 15 words of `bLongArr` into `dLongArr`.

The restore path at 15346–15360 copies `cLongArr` back to `aLongArr` and `dLongArr` back to `bLongArr`. This is a whole-table snapshot/rollback, not a record-type transition. A type-5 record can therefore be restored if it was present in the captured snapshot; it is not inherently released by restore.

## 3. Inventory of aLongArr writes and clears

| Source location | Operation | Capacity consequence |
|---|---|---|
| 15906 | Packed field setter | Mutates one field; does not itself release a record |
| 16075–16078 | Type-3 movement consumer calls `fVoid(id)` | Whole-word clear; makes the record eligible for first-free allocation |
| 16142 | Writes object type 3 | Pending cleanup marker, not yet free |
| 16145 | Writes object type 5 | Occupied; allocator only treats type 0 as free |
| 16253–16259 | Rewrites offset 54, type 2, movement fields, and bit 57 | Active transition; not a release |
| 16356–16372 | Writes type 2 and related movement fields, with one dynamic field write | Occupied; no whole-word clear |
| 16573–16575 | Creates/updates type-5 object and coordinates | Occupied; no clear at this call site |
| 16643 | Writes type 6 | Occupied; no clear at this call site |
| 16658–16660 | `fVoid(id)` assigns `aLongArr[id-1] = 0L` | Explicit whole-word release |
| 16730 | Allocator clears selected word before constructing a new record | Reinitializes the selected free slot |
| 16733–16746 | Allocator writes type 7 or type 1 and fields | Creates occupied record |

The type-field write inventory (offset 28, width 3) in the audited source includes types 1, 2, 3, 5, 6, and 7. Type 0 is observed through whole-word clearing, not as a separately written type value.

## 4. Capacity accounting rules

For a live level state, the conservative allocator-compatible count is:
- **Free:** type field equals 0.
- **Occupied:** every nonzero type, including 1, 2, 3, 5, 6, and 7.

Important distinction: type 3 is not free merely because it has been marked 3. It becomes free only after the type-3 consumer executes `fVoid(id)` and zeroes the entire 64-bit word. Type 5 has no release path demonstrated in the audited movement switch. The known stage reset replaces the entire array, and snapshot restore can reinstate an earlier population; neither is an ordinary per-record release during uninterrupted gameplay.

The six-argument allocator's scan at 16721–16730 stops when it sees type 0. It does not skip type 5, type 6, or type 7. Thus all nonzero types consume one of the 15 slots until a whole-word clear or table reinitialization occurs.

## 5. Full-table boundary remains a real source hazard candidate

The allocator loop checks `b5 > 15 || type(b6) == 0`, but the following code unconditionally indexes `aLongArr[b6 - 1]`. If all 15 records are nonzero, the loop can exit with `b6 == 16` and then index 15. No visible full-capacity guard appears between the loop and the array access in this method.

This is a strong source-level out-of-bounds candidate, but legal gameplay reachability and shipping-bytecode parity remain unverified. Do not silently change it to return -1 in a compatibility port until the target binary or runtime behavior is checked.

## 6. Offset-54 implications

The non-type-7 allocator writes its supplied `b4` value to offset 54, width 3, without masking the incoming value. The relocation caller supplies the bLongArr coordinate lookup result. Therefore a returned index 8–14 can spill into bit 57 under the source's long-shift setter semantics. The allocator occupancy rules and type-5 persistence affect how quickly the 15-slot bLongArr/aLongArr populations can evolve, but they do not prove a real level produces a high index.

## 7. Validation and limitations

- Source inventory is based on the pinned decompiled Java revision above.
- Existing field-level tests cover type-5 occupancy and type-3 whole-word clearing; no new runtime behavior is asserted here.
- No local Gradle execution or successful CI run is claimed.
- No shipping JAR bytecode comparison or live level trace was available.
- No production code was modified.

## 8. Next stage — DEV-003AV

1. Trace all aLongArr type-5 creation contexts (movement completion and collision fallback) to determine whether the record is intentionally persistent, consumed by another subsystem, or only cleared by reset.
2. Inspect the full `aLongArr` reader inventory, not only writers, to find any type-5-specific consumer.
3. Build a conservative 15-slot capacity timeline for mixed types 1/2/3/5/6/7, where only explicit word clears return capacity.
4. Analyze the full-table allocator exit with source-language byte/array semantics and identify callers that may reach it.
5. Keep the offset-54 correction blocked pending a target-JAR or live-runtime trace.