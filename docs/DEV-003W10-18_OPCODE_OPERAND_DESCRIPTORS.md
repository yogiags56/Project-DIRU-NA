# DEV-003W10.18 — Opcode operand access descriptors

## Source baseline

Descriptors are transcribed from the pinned legacy interpreter source:
`kubikaugustyn/DiamondRushSource/src/main/java/c.java` at commit
`215c71d04057d66a1b08419b1a4b1043ae3e0881`.

The table captures explicit byte/U16/U32 reads visible in the switch labels and the ordered fall-through labels. It is intentionally a partial audited catalogue, not a claim that every opcode has been described.

## Implemented entries

Audited entries: 1, 2, 4, 5, 6, 8, 9, 11, 16, 17, 18, 25, 26, 27.

Each descriptor exposes:
- entry opcode;
- ordered switch labels reached by fall-through;
- read label, byte offset, width/kind, and purpose;
- maximum end-exclusive physical read span.

Unknown opcodes return null. No guessed default descriptor is provided.

## Fall-through paths represented

- 11 → 10 → 13
- 16 → 14 → 12
- 17 → 15 → 12
- 18 → 15 → 12
- 25 → 26 → 5 → 8 → 9 → 27
- 26 → 5 → 8 → 9 → 27
- 5 → 8 → 9 → 27
- 8 → 9 → 27
- 9 → 27

These are switch-label sequences. Conditional returns, breaks, and runtime state may terminate a path earlier. The descriptor therefore represents the union of source-visible reads, not a guarantee that all reads occur on every invocation.

## Safety and interpretation

Offsets are relative to the current record start. `minimumBackingSpan` is the maximum end-exclusive offset among recorded reads. It is a physical backing-array requirement, not a logical child-record length requirement. A packed group may satisfy a read beyond a child's logical length while a top-level leaf cannot. The executor must combine the descriptor with `DemoLegacyBufferLayout.RecordSlot` and select the correct physical layout.

Opcode 16/17 descriptors include the union of dynamic branch offsets; their exact branch conditions remain part of interpreter execution semantics. Opcode 2 has variable text-dependent payload; this descriptor records only fixed reads visible in the inspected switch region and is not a complete serialized-record schema.

## Tests

Tests cover audited/unknown opcode lookup, source-order fall-through labels, maximum spans, opcode-25's 10-byte fall-through requirement, and descriptor immutability.

## Non-goals

No opcode execution, mutation, stage writes, graphics calls, timing behavior, text decoding, or automatic fallback for unknown opcodes. Do not infer complete operand schemas from these descriptors alone.
