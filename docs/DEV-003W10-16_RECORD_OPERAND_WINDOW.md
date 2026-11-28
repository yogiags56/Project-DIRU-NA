# DEV-003W10.16 — Record-local operand window

## Goal

Provide a detached, read-only, bounds-checked operand accessor for one normalized leaf record. This is the safety boundary required before any opcode executor reads operands.

## API

`DemoRecordOperandWindow` provides:
- unsigned u8, little-endian u16, signed i16;
- unsigned little-endian u24 and u32 represented as Java long;
- defensive byte-range copies;
- opcode and record-length metadata.

All offsets are absolute offsets into the normalized record, including its two-byte header. Every read validates the entire range before accessing memory. Negative, overflowing, and out-of-record ranges throw `OperandBoundsException`.

The window clones its input and never exposes the backing bytes. It can be constructed from runtime leaf state or directly from normalized bytes. Group records and malformed headers are rejected.

## Important compatibility boundary

This is intentionally a record-local window. It does not make adjacent records visible, pad short records, or infer a legacy shared-buffer layout. The previously identified opcode 5 → 8 → 9 fall-through hazard therefore remains blocked: the opcode-5 normalized record is seven bytes, while a later switch label may attempt to read at offset +6 as a two-byte value. This accessor will reject that read instead of borrowing bytes from a following record.

Do not wire stage mutation or opcode execution to this class until the exact legacy allocation and buffer-sharing behavior is reconstructed from source and fixtures. If the original runtime used a fixed-size shared record slot, model that explicitly in a separately named bounded slot abstraction with source-backed stride and initialization rules.

## Tests

Synthetic tests cover little-endian unsigned/signed reads, u24/u32 values, detached input and output copies, record-end overread rejection, negative/overflowing ranges, and malformed/group headers.

## Non-goals

No opcode dispatch, execution, skip handling, stage writes, camera updates, renderer calls, or Android dependencies.
