# DIRU-NA-DEV-003AL — bLongArr Allocator, Coordinate Provenance & State Invariant Correction

**Status:** Static source trace completed; prior coordinate-provenance conclusion corrected  
**Parent:** DEV-003AK  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Critical correction

The earlier DEV-003AJ/AK conclusion that no producer for bLongArr coordinates or subtype had been found was incorrect. It overlooked the four-argument instance allocator at lines 15863–15884:

`private byte aByte(byte x, byte y, byte movementValue, byte subtype)`

This method uses the byte-indexed `aInt(byte,...)` and `aVoid(byte,...)` overloads, so it allocates and writes **bLongArr**, not aLongArr.

This method is the missing producer for the record fields read by the type-3 consumer. DEV-003AL supersedes the unresolved-coordinate conclusion in DEV-003AJ and DEV-003AK. The historical 2023 classfile provenance mismatch remains valid: it is still not a bytecode parity proof for the 2025 source.

## 2. Exact four-argument bLongArr allocator

Source lines 15863–15884:
1. Scan zero-based slots 0–14.
2. Read each slot's low 4-bit type at offset 0.
3. Select the first slot whose type is zero.
4. Write type 1 at offset 0, width 4.
5. Write subtype at offset 4, width 2.
6. Write movement value at offset 6, width 7.
7. Write the same movement value at offset 27, width 7.
8. Write first coordinate at offset 13, width 7.
9. Write second coordinate at offset 20, width 7.
10. Return the selected zero-based slot; return -1 if all 15 slots are occupied.

The backing store is cleared by the allocator before fields are written only if the source method's selected-slot path explicitly reaches that clear? The inspected body does not clear the selected word; it tests type zero then writes fields using the field setter. Do not add a whole-word clear to the reconstructed behavior.

## 3. Confirmed allocator call sites

| Source line | Call context | Result |
|---:|---|---|
| 12130 | Foreground interaction, case 19 | Creates a bLongArr record using the packed tile value and tile coordinates, then calls `mVoid` |
| 12618 | `afVoid` scan of player-layer code 38 | Creates a bLongArr record for each matching cell and increments `duInt` |
| 16603 | Movement transition after computing `n25,n24,n26,n23` | Creates a bLongArr record immediately before coordinate lookup and type transition |

These are calls to the four-argument instance allocator, not the six-argument aLongArr object allocator at lines 16719–16747.

## 4. Record lifecycle and cross-store link

The source establishes a cross-reference between the two separate 15-slot stores:

- The six-argument aLongArr allocator at lines 16719–16747 stores its final argument in aLongArr offset 54, width 3.
- Movement code reads that aLongArr field at line 16090 to recover a bLongArr slot index (`b21`).
- It then reads the linked bLongArr movement field at offset 6, width 7.
- In the relevant transition branch, line 16144 changes the linked bLongArr record's type to 3.
- Line 16210 updates the linked bLongArr field at offset 6, width 7.

Thus, aLongArr offset 54 acts as a compact link to a bLongArr slot in these paths. The arrays remain separate; the link is an index, not shared storage.

The four-argument allocator initializes coordinates and subtype. The six-argument aLongArr allocator stores the bLongArr slot index in the aLongArr record, allowing later movement processing to find and mutate the corresponding bLongArr record.

## 5. Type-3 consumer field map — now source-supported

The type-3 consumer at lines 16458–16510 reads fields that the four-argument allocator writes:

| Field | Offset | Width | Producer |
|---|---:|---:|---|
| Type | 0 | 4 | Allocator writes 1; transition changes to 3 |
| Subtype | 4 | 2 | Allocator argument |
| Movement value | 6 | 7 | Allocator argument; updated at line 16210 |
| Coordinate A | 13 | 7 | Allocator first coordinate |
| Coordinate B | 20 | 7 | Allocator second coordinate |
| Mirrored movement value | 27 | 7 | Allocator writes same movement argument |

This confirms the previously observed consumer reads at offsets 0, 4, 13, and 20. The earlier claim that offsets 4, 13, and 20 had no producer is withdrawn.

## 6. Sentinel handling: refined interpretation

The coordinate lookup `bByte(x,y)` remains a first-match scan over 15 slots and returns -1 when no coordinates match.

At line 16603, the code calls the allocator but discards its returned slot, then at line 16605 looks up the new record by coordinates and writes type 2. This means:
- If allocation succeeds and the new coordinate pair is unique under the first-match scan, the lookup can find the new record.
- If the array is full, the allocator returns -1; because the return is ignored, the following lookup may still find an older matching coordinate record or return -1.
- If duplicate coordinates exist, first-match lookup can select an earlier record rather than the newly allocated one.
- The source does not locally guard the line-16605 setter against -1.

The `mVoid` path at lines 16798–16810 is different: it checks lookup success before setting type 2 and proceeding. This remains an important difference in caller preconditions.

Do not “fix” either path by clamping -1 or changing first-match semantics without a separate compatibility decision backed by runtime evidence.

## 7. Corrected interpretation of type transitions

The source now supports this partial lifecycle:
- New bLongArr record: type 1.
- Movement/segment processing can mark a linked record type 3.
- Type-3 consumer reads the packed coordinates/subtype and clears the entire word before dispatch.
- Separate coordinate lookup callers can change a matching record to type 2.

The complete set of type-2 consumers and all possible transition branches still requires a dedicated typed state-transition inventory. Do not assume type 2 is consumed by the same loop as type 3.

## 8. Validation and remaining limitations

Confirmed from static source:
- bLongArr is a separate `long[15]` store.
- Four-argument allocator initializes the coordinate and subtype fields.
- The allocator returns a zero-based slot or -1.
- Three direct allocator call sites were found in the current source file.
- aLongArr offset 54 stores a bLongArr slot reference in the relevant object/transition paths.
- Type-3 consumer field offsets now have a matching producer.

Not yet proven:
- Runtime uniqueness of coordinate pairs.
- The full type-2 consumer lifecycle.
- Whether ignored allocator return at line 16603 can be reached with a full bLongArr in normal gameplay.
- Exact binary parity against the original game JAR; the checked-in classfile is older than the source revision.
- Gameplay/runtime behavior in the Native Android implementation.

No runtime code was changed in DEV-003AL.

## 9. Next stage — DEV-003AM

Perform a complete bLongArr state-transition and capacity audit:
1. Enumerate every type value written or tested in bLongArr.
2. Trace all allocator call sites and the lifecycle of the returned slot.
3. Prove when aLongArr offset 54 is assigned, retained, and cleared.
4. Analyze duplicate-coordinate behavior and lookup ordering.
5. Analyze full-capacity behavior at each allocator caller, especially line 16603.
6. Trace type-2 consumers and distinguish them from type-3 consumption.
7. Add deterministic tests for allocator layout, field updates, lookup, full capacity, duplicate coordinates, and whole-word consumption only after the expected semantics are fully documented.

No gameplay integration or binary parity is claimed.
