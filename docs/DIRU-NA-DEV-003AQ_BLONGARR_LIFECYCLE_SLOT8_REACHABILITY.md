# DIRU-NA-DEV-003AQ — bLongArr Lifecycle Model & Slot-8 Reachability Fixture

**Status:** Deterministic lifecycle fixtures added; real-level reachability remains unproven  
**Parent:** DEV-003AP  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`

## 1. Lifecycle model established from source

The audited source exposes three allocation call sites for the 15-slot bLongArr table:

| Source line | Trigger | Inputs written |
|---:|---|---|
| 12130 | Foreground-layer value 19 interaction | tile value, player-layer coordinates, subtype 0 |
| 12618 | Full player-layer scan for value 38 | tile value, player-layer coordinates, subtype 0 |
| 16603 | Movement transition | movement-derived tile/coordinates and subtype |
 
The shared allocator (15863–15884) chooses the first slot whose four-bit type field is zero. It scans all 15 entries and returns the zero-based slot, or -1 when full. It writes type 1, subtype, tile value twice, and the two coordinate fields. It does not clear the whole word before those field writes.

The coordinate lookup (15917–15930) scans slots 0–14 in ascending order, compares only coordinate fields, does not inspect type, and returns the first match. This means:
- a matching inactive or stale-coordinate entry can shadow a later active entry;
- duplicate coordinates resolve to the lowest slot;
- the lookup itself does not enforce an eight-slot reference limit.

The type-3 consumer (16465–16469) clears a matching bLongArr word to zero after reading its coordinates and subtype. This makes that slot eligible for first-free reuse.

## 2. New deterministic lifecycle fixtures

Added:
`core/src/test/java/MeongDev/DIRUNA/GloftDIRU/core/collision/LegacyBLongArrLifecycleTest.java`

The tests cover:
1. Nine sequential allocations occupy slots 0 through 8; coordinate lookup can return slot 8.
2. A type-3-marked record that is cleared becomes eligible for first-free reuse.
3. Duplicate coordinate pairs resolve to the earliest matching slot, even if a later slot also contains a live-looking record.

These are controlled data-structure fixtures against the existing codec/allocator helpers. They demonstrate that the modeled 15-slot allocator and lookup permit index 8. They do **not** establish that the real game reaches the offset-54 relocation call with slot 8–14 during ordinary gameplay.

## 3. Critical distinction: deterministic reachability vs. game reachability

The new fixture proves this abstract sequence:
- allocate nine unique records;
- the ninth allocation receives index 8;
- querying its unique coordinate returns index 8.

It does not yet model the full stage event order, exact coordinate production, movement transitions, or all slot-release paths. The line-16810 relocation path specifically follows a successful coordinate lookup from `mVoid`; proving its high-slot reachability requires a level fixture or a captured runtime state in which that coordinate lookup returns 8–14.

## 4. Reuse and lookup hazards

The source-level behavior creates two independent hazards worth retaining in future tests:
- **first-free reuse:** a cleared lower-numbered slot can be reused before higher slots, changing which slot a coordinate lookup returns;
- **coordinate shadowing:** because lookup ignores type, an earlier zeroed/stale coordinate pair can match before a later active record. In particular, zero-initialized records all decode to coordinate (0,0), so a lookup for (0,0) can resolve to slot 0 even if that record has type 0.

Do not change lookup to filter by type without checking original binary behavior; current source explicitly compares coordinates only.

## 5. Validation and limitations

- Lifecycle test source added to the repository.
- No local Gradle test execution is claimed.
- No CI status check was reported for the documentation commit.
- No target JAR bytecode comparison or live gameplay trace was available.
- No production code was changed.

## 6. Next stage — DEV-003AR

1. Build a faithful event-level simulator for the three allocator call sites and type-3 release consumer, preserving their order and exact field writes.
2. Model the `mVoid` sequence: coordinate lookup, type-2 mark, player-layer mutation, second lookup, and six-argument aLongArr allocation.
3. Add explicit fixtures for zero-coordinate aliasing and lower-slot shadowing.
4. Identify an original level/map fixture or runtime instrumentation route to validate whether slot 8–14 can reach the actual relocation path.
5. Keep any production change blocked pending binary/runtime evidence.
