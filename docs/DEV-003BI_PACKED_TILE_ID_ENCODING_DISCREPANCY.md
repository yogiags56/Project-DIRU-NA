# DEV-003BI — Packed Tile-ID Encoding Discrepancy Investigation

## Scope and provenance

Static investigation of `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at pinned revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This milestone narrows the 15-slot versus three-bit discrepancy using the actual Type-5 creation call site and the allocator implementation. It is not a shipping-JAR bytecode comparison or runtime reproduction.

## 1. Confirmed source-level data path

The six-argument object allocator around line 16719:
- scans record IDs from 1 through 15;
- considers a slot free when the record type field at bits 28..30 is zero;
- clears the chosen slot;
- for modes other than 7, initializes a record and writes its ID into tile metadata using a three-bit field;
- returns the one-based slot ID as a byte.

The tile metadata writer stores only three bits. Its effective representable range is 0..7.

## 2. Type-5 path is a concrete affected caller

In controller state 5 around lines 16505–16590:
1. The controller calls the six-argument allocator with mode 8.
2. The returned slot ID is assigned to `aByte`.
3. The code changes the new record's type field to 5.
4. It writes the returned ID into tile metadata for each tile traversed by the Type-5 branch, through a three-bit write at group offset 0.
5. The same record ID is therefore intended to link multiple tile groups along that branch.

This is not merely a theoretical mismatch between unrelated structures. The Type-5 path visibly uses the 15-slot allocator and then writes its returned ID into a three-bit metadata field.

## 3. Exact encoding consequences

| Allocated object slot | Three-bit metadata result | Consequence |
|---:|---:|---|
| 1–7 | 1–7 | Representable without truncation |
| 8 | 0 | Encodes as empty/no record |
| 9 | 1 | Aliases slot 1 |
| 10 | 2 | Aliases slot 2 |
| 11 | 3 | Aliases slot 3 |
| 12 | 4 | Aliases slot 4 |
| 13 | 5 | Aliases slot 5 |
| 14 | 6 | Aliases slot 6 |
| 15 | 7 | Aliases slot 7 |

The metadata reader extracts the three-bit value and uses the one-based object accessor, which indexes `aLongArr[id - 1]`. A zero value must be guarded before that accessor; otherwise it would index `-1`. An aliased value 1..7 resolves to a different lower slot, not the slot 8..15 that was allocated.

The source therefore contains a real representational hazard on this call path if allocator IDs 8..15 are reachable. The Type-5 branch may produce missing links or links to unrelated lower-numbered records. This is a static consequence of the visible field widths and writes; whether shipped levels reach the condition remains unproven.

## 4. Mode-7 distinction

The same allocator has a special mode-7 branch:
- clears the tile metadata group passed to it;
- sets the newly allocated record type to 7;
- does not write the allocated slot ID into that tile's three-bit record-ID field in that branch.

That special path must not be used to dismiss the Type-5 issue: Type-5 creation uses mode 8, which follows the branch that writes the returned ID into the three-bit metadata field.

## 5. Native compatibility decision

The native implementation must not silently reproduce truncation or aliasing.

Required behavior:
- Keep the legacy 15-slot record table as a separate capacity constraint.
- Treat the legacy tile metadata ID as a distinct three-bit field.
- Until the original binary proves a different interpretation, only object slots 1..7 may be linked through this field.
- If Type-5 allocation selects slot 8..15, return a safe, explicit failure before publishing any metadata link; roll back the allocation atomically.
- Do not remap high slots to low slots, because that would create incorrect aliasing.
- Do not reduce the entire object table to seven slots without proving that other object/controller paths do not need slots 8..15.
- Keep an investigation flag open: perhaps shipped content never reaches a high Type-5 slot, or the decompiled source lost an important detail. Neither possibility is established.

## 6. Required follow-up evidence

1. Inspect the original shipping JAR bytecode for the allocator, the three-bit tile metadata writer, the Type-5 state-5 call site, and the metadata reader.
2. Trace all allocator modes and determine whether modes other than 8 also publish slot IDs into three-bit metadata.
3. Instrument or emulate the original JAR with controlled slot occupancy to observe allocation and metadata results for slots 7, 8, 9, and 15.
4. Inspect representative shipped stages and determine whether the Type-5 allocator can encounter seven occupied lower slots before it runs.
5. Record whether the original binary crashes, aliases, avoids the state, or applies an unobserved indirection.

## 7. Next milestone: DEV-003BJ — Isolated Native Contract Test Harness

Build a small, independent test harness for the safe native contract, without gameplay integration. Tests must cover:
- table capacity 15 versus tile-link capacity 7;
- slot 7 link success;
- slot 8..15 explicit unrepresentable-link failure and atomic rollback;
- zero-ID no-link behavior;
- stale coordinate/group and generation rejection;
- shared tile references and unlink/release ownership;
- full-table allocation failure without out-of-bounds writes.

The harness should document that it tests the proposed native safety contract, not original-game parity.
