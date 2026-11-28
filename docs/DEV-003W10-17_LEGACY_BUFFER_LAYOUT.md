# DEV-003W10.17 — Legacy buffer layout reconstruction

## Source evidence

Pinned reference: `kubikaugustyn/DiamondRushSource`, commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`, `src/main/java/c.java`.

The legacy loader allocates exact-size byte arrays for top-level leaf records. Relevant observed sizes include:
- opcode 5: 7 bytes;
- opcode 9: 8 bytes;
- opcode 25: 8 bytes;
- opcode 26: 10 bytes;
- opcode 27: 8 + text length;
- opcode 2: 11 + text length;
- opcode 4: 16 bytes.

Opcode-0 groups are different. The loader first builds each child as its own temporary array, then packs all children consecutively into one shared array:
- byte 0: opcode 0;
- byte 1: child count;
- count entries of four-byte little-endian child lengths;
- child byte arrays concatenated in source order.

The legacy interpreter passes the shared group array and each child offset into its switch handler. Thus a switch fall-through read can pass the current child's declared length while remaining inside the physical group array. A top-level leaf has no adjacent sibling bytes and such a read reaches the physical array boundary instead.

## Implementation

`DemoLegacyBufferLayout` models these two physical allocation modes explicitly:
- `topLevelLeaf`: one isolated array and one slot;
- `packedGroup`: validates the group table and retains one cloned shared backing array with child slots.

Each slot exposes its logical length, offset, and physical backing length. Its `isWithinLogicalRecord` query distinguishes declared-record bounds from physical-array bounds. The `readBackingU8/U16/U32` methods reproduce the legacy array visibility rule: a read may cross a sibling boundary inside a packed group, but cannot cross the physical container boundary.

The model is read-only. It does not permit writes, execute opcodes, or infer behavior from padding.

## Critical opcode 25/26 observation

Opcode 25's allocated leaf is 8 bytes. The source switch falls through from case 25 into case 26, whose u32 read starts at offset +6 and spans bytes +6..+9. In a top-level leaf this would exceed the 8-byte array. In a packed group it can consume the first two bytes of the following child header if a following child exists. This is an observable legacy memory-layout behavior, not permission to silently pad records.

The implementation and tests capture that distinction. It remains a source-compatibility hazard that must be handled deliberately by the eventual executor and verified against real demo fixtures.

## Tests

Synthetic tests verify exact top-level isolation, packed group offsets and lengths, shared-array spillover across a child boundary, physical end-of-container rejection, and malformed group/header rejection.

## Safety boundary

This milestone does not change `DemoRecordOperandWindow`, which remains strictly record-local and safe by default. The new shared-backing view is separately named and opt-in. Do not route arbitrary opcodes through it; the executor must request the legacy backing view only for a source-verified group execution context.

## Next

Build source-derived operand access descriptors and per-opcode minimum physical read spans. Then validate every descriptor against actual extracted demo records before implementing mutable execution. In particular, determine whether any real group has opcode 25 followed by a sibling record and record the exact observed bytes.
