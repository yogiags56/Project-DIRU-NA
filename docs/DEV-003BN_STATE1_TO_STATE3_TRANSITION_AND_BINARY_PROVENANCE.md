# DEV-003BN — State-1 to State-3 Transition Preconditions & Binary Provenance

## Scope

This milestone corrects the preliminary DEV-003BM conclusion and consolidates the source-level producer/consumer chain for auxiliary `bLongArr` state 3.

Source under audit:
- Repository: `kubikaugustyn/DiamondRushSource`
- File: `src/main/java/i.java`
- Pinned revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`

This is a static decompiled-source audit. The exact original shipping JAR was not present in either checked repository, so bytecode parity is not claimed.

## 1. Corrected producer chain

The four-argument instance method at lines 15863–15884 allocates a zero-based `bLongArr` slot by scanning indices 0..14 for a zero low-nibble state. For a free slot it writes:
- state 1 at bits 0..3;
- subtype at bits 4..5;
- movement value at bits 6..12;
- coordinate X at bits 13..19;
- coordinate Y at bits 20..26;
- mirrored movement value at bits 27..33.

It returns the zero-based auxiliary slot or -1 when all 15 entries are occupied.

The six-argument object allocator at lines 16719–16747 is separate. For non-Type-7 records, it stores the auxiliary slot argument in the object record's bits 54..56. This creates a compact cross-table link from an `aLongArr` record to a `bLongArr` slot.

## 2. State-3 transition

The movement/controller loop reads the object record's bits 54..56 into `b21` (around line 16090). It then reads the linked auxiliary entry's movement field at bits 6..12.

Within the Type-2 object-record movement branch, the source checks a compound transition condition. If the branch reaches the subcase where the linked movement value `b20 == 0`, it:
1. writes state 3 to the linked auxiliary entry's low nibble using `aVoid(b21, 3, 0, 4)` (line 16144);
2. changes the current object record's type bits to 5 (line 16145);
3. may set controller state 4 if the related completion condition is met.

Therefore the observed auxiliary transition is **state 1 → state 3**, driven by a linked Type-2 movement/object record. The Type-3 consumer later scans all 15 auxiliary slots, reads coordinates and subtype, clears the entire selected word, and dispatches the direction-specific Type-7/traversal behavior (around lines 16455–16512).

State 3 is not produced by the two state-2 coordinate-lookup writers. It is produced by the movement transition above.

## 3. Cross-table link constraints

The auxiliary allocator accepts slots 0..14. The object-record link field at bits 54..56 is only three bits wide and therefore represents 0..7.

The source does not globally restrict auxiliary allocation to slots 0..7. Consequently:
- slots 0..7 are representable in the link field;
- slots 8..14 can be allocated in `bLongArr`, but cannot be represented without truncation in the three-bit link;
- source-level reachability of high slots depends on occupancy/order and the call paths that create linked object records.

This mismatch is a real compatibility/safety concern and must not be hidden by the typed model. The existing isolated native harness currently stores all 15 opaque auxiliary entries, but it does not yet model this exact legacy cross-link encoding.

## 4. Consumer behavior

Controller state 4 scans indices 0..14 and selects only entries whose low nibble equals 3. For each selected entry it reads:
- X at bits 13..19;
- Y at bits 20..26;
- direction/subtype at bits 4..5.

It clears the entire 64-bit auxiliary word before dispatching the branch. Direction values 0, 1 and 2 adjust the coordinate before subsequent traversal; the default branch calls the six-argument object allocator for a Type-7 record using the coordinate lookup result.

The consumer's whole-word clear is distinct from changing only the low-nibble state.

## 5. Binary provenance check

The upstream source repository contains `target/classes/i.class`, but Git history shows that file was introduced in the 2023 initial commit. The decompiled source file was modified later, with the audited revision dated 2025. The upstream repository does not contain a shipping `.jar` in its tracked tree, and the DIRU-NA repository also contains no original game JAR.

Thus the historical `i.class` is not established as the bytecode counterpart of the 2025 source revision. It cannot settle the exact original-JAR behavior. A conclusive bytecode comparison requires the exact shipping JAR or a class set with verified provenance and matching hashes.

## 6. Corrected findings

- The earlier DEV-003BM statement that no state-3 writer was found is withdrawn.
- The state-3 producer is identified in the pinned decompiled source at line 16144.
- The state-3 consumer is identified around lines 16455–16512.
- The state-1 allocator is identified at lines 15863–15884.
- The aLongArr-to-bLongArr link is stored in object bits 54..56 by the non-Type-7 allocator path.
- The 15-slot versus three-bit link mismatch remains unresolved for runtime reachability and must be modeled explicitly before integration.
- Original shipping-JAR bytecode parity remains unverified.

## 7. Implementation decision

No production code or typed auxiliary state machine was changed. The current raw 15-entry snapshot representation remains conservative. Before introducing a typed link, the native model needs an explicit representation for:
- zero-based auxiliary slot identity;
- the legacy three-bit encoded link value;
- invalid/unrepresentable slot outcomes;
- the source's exact state transition and clear ordering.

Do not silently mask slot IDs 8..14 into 0..6.

## 8. Next milestone — DEV-003BO: Cross-Table Link Encoding & Transition Regression Model

1. Add a small pure-Java model of the three-bit object-to-auxiliary link encoding without changing gameplay.
2. Test slot values 0..14, including aliasing/truncation behavior as a separately named legacy operation.
3. Model the state-1 → state-3 transition preconditions and subsequent whole-entry consumption.
4. Add negative tests for missing/stale/unrepresentable auxiliary references.
5. Keep a safe native API distinct from an explicit legacy-compatibility codec.
6. Revisit exact JAR parity when the original binary is supplied.

No Android gameplay integration, runtime trace, or original-JAR parity is claimed by this milestone.
