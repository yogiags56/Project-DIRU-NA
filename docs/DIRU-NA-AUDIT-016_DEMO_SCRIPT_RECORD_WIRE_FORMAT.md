# DIRU-NA-AUDIT-016 — Demo script record wire-format audit

## Evidence identity and limits

Pinned decompiled source:
- Repository: `kubikaugustyn/DiamondRushSource`
- Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`
- File: `src/main/java/c.java`
- Relevant loader: `aVoid(int tag)`, script payload parsing block after sprite-ID list.

This is a static source audit only. No retail binary, original asset, or runtime was executed. The audit distinguishes input wire bytes from the normalized internal byte arrays built by the legacy loader.

## 1. Top-level script buffer framing

After the selected demo payload's two-byte sprite count and its 16-bit sprite-ID list, the loader reads a 16-bit little-endian record/buffer count and allocates a top-level array of that many script buffers.

The source casts this value to Java `short`. For a safe native compatibility contract, accept only 0..32767; reject values with the high bit set rather than allowing a negative count or allocation size.

Each top-level buffer is a sequence of compact wire records. The parser increments a byte cursor as it consumes the opcode-specific operands. The source does not store an explicit byte length around each top-level buffer; the record count and opcode grammar determine the boundary.

## 2. Observed opcode wire grammar

All 16-bit operands below are little-endian. Unsigned byte operands are read with `& 0xFF`. Opcode itself is one byte.

| Opcode | Wire operands after opcode | Total wire bytes | Legacy normalized record |
|---:|---|---:|---|
| 0 | child count: u8 | 2 | group marker; children collected and packed into offset-table representation |
| 1 | x:u16, y:u16, duration:u16 | 7 | 12-byte record; x/y multiplied by 24; two coordinate fields initialized to sentinel 10000 |
| 2 | mode:u8, value:u16, textLength:u16, text bytes | 6 + textLength | variable record, base 11 bytes plus text |
| 4 | seven u16 operands | 15 | 16-byte record |
| 5 | x:u16, y:u16, flag:u8 | 6 | 7-byte record |
| 6 | value:u32 | 5 | 6-byte record |
| 7 | no operands | 1 | 2-byte record |
| 9 | three u16 operands | 7 | 8-byte record |
| 10 | value:u8 | 2 | 3-byte record |
| 11 | two u16 operands | 5 | 6-byte record |
| 12 | two u16 operands | 5 | 6-byte record |
| 13 | three u16 operands | 7 | 12-byte record; two extra coordinate fields initialized to sentinel 10000 |
| 14 | no operands | 1 | 2-byte record |
| 15 | no operands | 1 | 2-byte record |
| 16 | value:u16, flag:u8 | 4 | 6-byte record |
| 17 | value:u16, flag:u8 | 4 | 6-byte record |
| 18 | value:u8, value:u24 little-endian | 5 | 8-byte record |

The table describes wire sizes, not semantic behavior. The legacy normalized record layouts include runtime fields and completion bytes that are not present in the wire stream.

### Additional variable-length forms

- Opcode 2: a one-byte mode, two 16-bit values (the second is text length), then exactly that many text bytes.
- Opcode 18: one byte followed by a three-byte little-endian integer. Do not widen this to a four-byte wire operand.
- Opcode 0: one-byte child count, followed by that many child records parsed using the same opcode grammar in the active child-collection context.

## 3. Nested group packing

When opcode 0 is encountered, the loader allocates a temporary child-record array of the declared one-byte count. Child records are normalized using the same opcode switch. Once the child count is exhausted, the loader builds a packed group buffer:

- byte 0: group type 0;
- byte 1: child count;
- next `childCount * 4` bytes: little-endian 32-bit child-record lengths;
- remaining bytes: normalized child records concatenated in child order.

The group is then inserted as one entry in the parent top-level buffer array.

The inspected implementation uses a single temporary child-array/count state rather than an obvious general-purpose recursive stack. Therefore arbitrary nested-inside-nested groups are **not established** by this source excerpt. W10.7 must not claim recursive nesting support; the decoder should initially support the verified group form and reject deeper group nesting unless further call/data-flow evidence proves it valid.

The child lengths describe normalized record buffers, not original wire-record lengths. Native parsing must validate the offset table and packed payload separately.

## 4. Unsupported and malformed input behavior

The legacy switch has no explicit default error branch. An unknown opcode can leave the operand cursor unchanged after the opcode, causing subsequent operand bytes to be interpreted as opcodes. This is not a safe compatibility behavior to reproduce.

Native decoder policy:
- reject unknown opcodes with a structured error and exact byte offset;
- reject truncated fixed operands;
- validate variable-length operands before slicing or allocation;
- validate opcode 0 child count and all child record boundaries;
- reject incomplete group assembly and any trailing bytes not consumed by the declared top-level record count;
- cap top-level record count at 32767, consistent with the legacy signed-short count;
- preserve raw wire bytes and normalized fields separately for auditability.

## 5. Source ambiguities requiring explicit treatment

1. The loader's local variables are heavily obfuscated; wire sizes above are derived from cursor increments, not names.
2. Opcode 0's temporary child state does not demonstrate arbitrary recursive groups.
3. The legacy code catches broad exceptions and suppresses them, so malformed-input behavior is not a reliable compatibility target.
4. Opcode semantic execution, update fall-through, render-time mutation, and reset behavior are outside this wire-format audit.
5. The source normalizes some operands (e.g. opcode 1 coordinates multiplied by 24) and initializes runtime sentinel fields. A parser should retain wire values; normalization belongs in a later typed-decoding layer.

## 6. Required synthetic fixture matrix for DEV-003W10.8

- Every observed fixed-size opcode, minimum and exact-length operands.
- Opcode 18 with 24-bit values 0, 1, and 0xFFFFFF.
- Opcode 2 and any other variable-length record with lengths 0, 1, configured maximum, truncated by one byte, and length larger than remaining input.
- Opcode 0 with child counts 0, 1, and multiple children.
- Group offset-table lengths exactly matching and exceeding packed payload.
- Unknown opcode, truncated opcode byte, truncated operand at every operand boundary.
- Top-level count 0, 1, multiple, and high-bit-set invalid count.
- Trailing bytes after declared record count.
- One-level group accepted; nested-inside-nested behavior remains rejected pending evidence.
- Defensive-copy tests proving source byte arrays cannot mutate decoded records.

## 7. Next

DEV-003W10.8 — implement a bounded wire-record decoder that produces immutable typed wire records and a validated one-level group representation. Do not execute records, mutate stage state, create sprites, or integrate with the interpreter in this increment.


## 8. Additional opcodes found during decoder implementation

A second full pass over the complete opcode switch identified three additional forms that were absent from the initial summary:

| Opcode | Wire operands | Total wire bytes | Normalized legacy record |
|---:|---|---:|---|
| 25 | two u16, then two u8 | 7 | 8 bytes |
| 26 | two u16, then one u32 | 9 | 10 bytes |
| 27 | length:u16, then exactly length opaque bytes | 3 + length | 8 + length bytes |

Opcode 27's length is an unsigned 16-bit wire value, but the legacy implementation casts it to signed Java `short` before allocating and copying. The safe parser keeps it unsigned and bounds-checks against remaining input; any later compatibility-normalization layer must decide explicitly how to handle values above 32767. Opcode 2 similarly carries a one-byte mode, a u16 value, a u16 data length, then that many bytes.

The decoder supports opcodes 25, 26, and 27 as wire records. Their semantic interpretation remains unknown and is deliberately not inferred.

## 9. DEV-003W10.8 implementation boundary

`DemoScriptRecordDecoder` parses the leading u16 top-level logical-record count, then exactly that many records. It supports the observed opcode forms 0, 1, 2, 4–7, 9–18, and 25–27. Opcode 0 is represented as an immutable one-level group with parsed child records. Nested opcode-0 groups, unknown opcodes, truncated operands, invalid signed-short top-level counts, and trailing bytes fail closed.

The decoder retains each record's original wire bytes, typed integer operands, opaque variable data, and byte offset. It does not construct the legacy normalized runtime byte arrays, execute opcodes, allocate sprites, mutate stages, or render.
