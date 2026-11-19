# DIRU-NA-DEV-003AM — bLongArr State Transition & Capacity Audit

**Status:** Static source audit completed; runtime semantics still require tests  
**Parent:** DEV-003AL  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Scope and correction

This stage inventories the byte-indexed `bLongArr[15]` record store, its allocator, direct field mutations, coordinate lookup, and the connection to `aLongArr`. It does not conflate the int-indexed `aLongArr` setter with the byte-indexed `bLongArr` setter.

The overloaded helpers are decisive:
- `aInt(byte, byte, byte)` and `aVoid(byte, byte, byte, byte)` access `bLongArr[slot]`, with zero-based slot IDs.
- `aInt(int, byte, byte)` and `aVoid(int, byte, byte, byte)` access `aLongArr[id - 1]`, with one-based IDs.

## 2. bLongArr type-state write inventory

The source revision contains the following direct writes to the 4-bit type field of bLongArr:

| Line | Write | Meaning |
|---:|---|---|
| 15875 | type = 1 | Four-argument bLongArr allocator creates a record |
| 16144 | type = 3 | Movement transition promotes the linked record |
| 16605 | type = 2 | Coordinate lookup result is marked |
| 16802 | type = 2 | `mVoid` marks an existing coordinate match |

The only direct type-field reads found in the byte-indexed bLongArr path are:
- line 15869: allocator tests type == 0 to find a free slot;
- line 16465: consumer tests type == 3 before processing and clearing.

No direct bLongArr type == 2 consumer was found in the inspected source. Type 2 is therefore a marked/intermediate state in the current source trace, not a separately established consumer queue. Its lifecycle is tied to coordinate lookup and aLongArr linkage, but every eventual clearing path is not yet proven.

## 3. Record allocation and exact capacity semantics

The allocator at lines 15863–15884:
1. scans slots in ascending order, 0 through 14;
2. selects the first slot whose type field (offset 0, width 4) is zero;
3. writes type 1, subtype, movement value, mirrored movement value, and coordinates;
4. returns the zero-based slot;
5. returns -1 if no slot has type zero.

Important implementation details:
- It is first-free, not round-robin.
- It does not clear the whole 64-bit word before writing fields.
- Its free test is only the 4-bit type field.
- Therefore any stale payload bits in a type-zero word would survive fields not explicitly overwritten. The inspected type-3 consumer clears the whole word, but no universal guarantee is inferred for every route to type zero.
- Capacity is 15 records. A slot with type 1, 2, or 3 is not considered free.

## 4. All identified allocator callers and ignored results

| Line | Caller context | Return value handling |
|---:|---|---|
| 12130 | Foreground interaction; immediately followed by `mVoid(n8,n9)` | Return ignored |
| 12618 | Player-layer code-38 scan | Return ignored |
| 16603 | Movement transition before coordinate lookup at 16605 | Return ignored |

The source-level risks differ:
- **12130:** if full, `mVoid` performs its own coordinate lookup. It may find an older matching record, or fail to find one. The newly requested record is not guaranteed to exist.
- **12618:** the allocator result is ignored and `duInt` increments regardless. The source does not locally report allocation failure.
- **16603:** after allocation, a separate coordinate lookup is performed and its result is passed directly to the byte-indexed type setter at line 16605. There is no local `>= 0` guard. On full capacity, this can still match a pre-existing duplicate coordinate or return -1; the source does not locally protect the latter case.

No defensive clamp, slot-0 fallback, or synthesized replacement behavior is part of the original semantics.

## 5. Coordinate lookup and duplicate behavior

The lookup at lines 15917–15930:
- scans slots 0 through 14 in ascending order;
- compares only coordinate A (offset 13, width 7) and coordinate B (offset 20, width 7);
- does not filter by type;
- returns the first match;
- returns -1 if no match exists.

Consequences:
- A type-1, type-2, or type-3 record can all be returned if its coordinates match.
- A duplicate coordinate pair resolves to the lowest-index matching slot, not necessarily the slot most recently allocated.
- An all-zero word has coordinates (0,0), so lookup of (0,0) can match an unused zeroed slot unless callers or state invariants prevent it.
- The allocator's type-zero test and coordinate lookup's type-agnostic scan have different eligibility rules.

These are direct consequences of the helper bodies; whether normal game state avoids problematic duplicates or zero-coordinate matches is not yet established.

## 6. State transition evidence

### 6.1 Type 1 — allocated record
The four-argument allocator writes type 1 and initializes the packed payload:
- subtype at offset 4, width 2;
- movement value at offset 6, width 7;
- first coordinate at offset 13, width 7;
- second coordinate at offset 20, width 7;
- mirrored movement value at offset 27, width 7.

### 6.2 Type 2 — coordinate-marked record
Two paths set type 2:
- line 16605: lookup after the movement-transition allocator call at line 16603;
- line 16802: guarded lookup in `mVoid`.

The `mVoid` path checks for a nonnegative lookup result before writing type 2, then invokes the six-argument aLongArr allocator with the coordinate lookup result as its final argument (line 16810).

### 6.3 Type 3 — linked movement record
In the movement processing path:
- aLongArr record field offset 54, width 3, is read as a bLongArr slot index at line 16090;
- the corresponding bLongArr movement field at offset 6, width 7, is read;
- when the relevant condition is satisfied, line 16144 changes the linked bLongArr record's type to 3;
- line 16210 updates the linked bLongArr movement field at offset 6, width 7.

### 6.4 Type-3 consumption and release
At lines 16465–16469:
- the consumer scans all 15 bLongArr slots;
- only type-3 records are selected;
- it reads coordinates and subtype;
- it clears the complete 64-bit word before dispatching subtype-specific behavior.

This is the confirmed full-word release path for type-3 records. The source trace has not established an equivalent generic release for type-2 records.

## 7. aLongArr ↔ bLongArr relationship

The stores are distinct:
- bLongArr: 15 packed 64-bit records, addressed by zero-based byte slot.
- aLongArr: separate packed records, addressed by one-based int ID.

The six-argument aLongArr allocator at lines 16719–16747 writes the supplied bLongArr slot reference at aLongArr offset 54, width 3 (line 16736). The movement path later reads that reference (line 16090) and uses it to mutate the corresponding bLongArr record.

This is an index link, not shared memory. The link width is 3 bits, which can represent values 0–7 only under unsigned interpretation; however, bLongArr has 15 slots. This apparent width/capacity mismatch is a critical unresolved point: do not assume all 15 bLongArr slots can be represented by this field. Inspect source casts, packed setter semantics, and actual reachable slot ranges before implementing the link as an unrestricted 0–14 index.

## 8. Critical new finding: link-width/capacity mismatch

The aLongArr field at offset 54 is read with width 3, but the bLongArr allocator can return indices 0–14. Values 8–14 cannot be represented in a 3-bit field without truncation. The setter's exact masking/bit behavior and the caller's value flow must be examined carefully.

Potential explanations include:
- the linked bLongArr slots are intentionally restricted to a subset;
- the slot value is encoded or transformed before storage;
- the apparent field width or call mapping needs deeper overload-level verification;
- the decompiled source has a mismatch or hidden invariant.

No explanation is selected without evidence. This is now a higher-priority blocker than adding gameplay integration.

## 9. Test plan — not yet implemented

The following deterministic tests are required before gameplay integration:
1. First-free allocation chooses slot 0, then next available slot.
2. Full 15-slot allocation returns -1 without mutating any slot.
3. Allocator writes the exact six fields and preserves unrelated bits as the source does.
4. Allocator does not clear unrelated stale payload bits in a type-zero slot.
5. Coordinate lookup returns the first match irrespective of type.
6. Duplicate coordinates resolve to the lowest slot.
7. Zero-coordinate lookup behavior is explicitly covered.
8. Type-3 consumption clears the whole 64-bit word.
9. Type-2 records are not incorrectly treated as free.
10. aLongArr offset-54 encoding is tested for every possible bLongArr slot value 0–14, with expected source semantics (including truncation if confirmed).

## 10. Next stage — DEV-003AN

Prioritize the aLongArr offset-54 reference encoding:
- prove how 3-bit offset 54 is intended to reference a 15-slot bLongArr store;
- trace every write/read of offset 54 in aLongArr;
- identify any slot remapping or reachability invariant;
- verify whether slots 8–14 can be referenced in normal gameplay;
- only then finalize codec and add tests for the link.

After the link-width issue, implement bLongArr lifecycle regression tests and capacity/duplicate-coordinate cases. No runtime integration or binary parity is claimed by DEV-003AM.
