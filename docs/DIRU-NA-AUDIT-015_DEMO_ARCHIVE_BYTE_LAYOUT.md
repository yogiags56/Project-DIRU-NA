# DIRU-NA Source Audit — Demo Archive Byte Layout

## Evidence identity

Read-only audit of:
- Repository: `kubikaugustyn/DiamondRushSource`
- Revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`
- Files: `src/main/java/c.java` and `src/main/java/i.java`

No proprietary runtime or binary asset was used in this audit.

## 1. Endianness

The source defines:
- `i.unpackUint16(array, ptr)`: `(array[ptr] & 0xFF) | ((array[ptr + 1] & 0xFF) << 8)`
- `i.unpackUint32(array, ptr)`: the same low-byte-first composition across four bytes.

Therefore all fields decoded through these helpers are little-endian unsigned bit patterns. This is direct source evidence, not an inference from the nested script format.

## 2. demo.f chunk-zero payload

In `c.aVoid(int tag)`, the source obtains chunk index 0 using `i.readChunk("/demo.f", 0)`, wraps the returned bytes in a `ByteArrayInputStream`, and:
1. Reads/discards the first two bytes.
2. Repeatedly reads a 16-bit tag.
3. For a nonmatching tag, continues to the next entry.
4. For the first matching tag, reads a 16-bit record count, then a 32-bit payload length, then exactly that payload into a new byte array.
5. Stops searching after the first matching tag.

The archive entry layout after the two-byte preamble is therefore:
- tag: unsigned 16-bit little-endian;
- record count: unsigned 16-bit little-endian;
- payload length: unsigned 32-bit little-endian;
- payload: declared number of bytes.

The record count is retained as a signed Java `short` in the legacy code but is read through an unsigned helper. Native parsing should preserve the unsigned value and validate it before any allocation or narrowing conversion.

Compatibility rule: for repeated matching tags in chunk zero, the legacy loader selects the first match. Do not silently change this to last-match-wins. This is distinct from the outer demo-slot selection behavior in `i.java`, where each matching slot replaces the selected instance.

## 3. demoSpr.bin

After parsing the selected demo payload's sprite-ID list, the source insertion-sorts requested IDs in ascending order. It then opens `/demoSpr.bin` and:
1. Reads a 16-bit sprite count.
2. Repeatedly reads a 16-bit sprite ID.
3. Reads a 32-bit sprite payload length.
4. Reads that many bytes for the matching requested sprite, or skips that many bytes when the current sprite is not needed and already cached.

The archive record layout is:
- sprite count: unsigned 16-bit little-endian;
- repeated sprite ID: unsigned 16-bit little-endian;
- payload length: unsigned 32-bit little-endian;
- sprite payload: declared number of bytes.

The legacy implementation scans sequentially and assumes the requested IDs can be found in archive order. It does not establish a safe duplicate-ID policy or robust missing-ID handling. Native parsing must index the complete archive with bounds checks, reject duplicate IDs deterministically, and report missing IDs explicitly. This is a safety improvement; it does not claim to reproduce malformed-archive behavior.

## 4. Outer resource chunk wrapper

`i.readChunk(fileName, chunkIndex)` separately reads the resource's chunk directory:
- first byte is used as the number of 8-byte directory entries;
- each entry has a 32-bit little-endian byte offset and 32-bit little-endian chunk length;
- the requested chunk is then read from that offset.

This outer wrapper is not the same structure as the inner tagged `demo.f` chunk-zero payload. The W10.4 parser boundary accepts the already-extracted chunk-zero byte array; parsing the resource directory is a separate concern and must not be conflated with the inner tagged entries.

## 5. Implementation constraints

- Decode archive fields as unsigned little-endian values.
- Validate every header before reading it.
- Validate a declared 32-bit length against remaining bytes before converting to Java array length.
- Preserve first-matching-tag behavior.
- Preserve sprite archive order as evidence while exposing deterministic ID lookup.
- Reject duplicate sprite IDs and report missing requested IDs.
- Never use `InputStream.read(byte[])` or `skip(long)` as proof that the requested number of bytes was consumed; native parser operates on a bounded byte array.
- Keep parser output detached from mutable caller-owned arrays.
- No image decoding, global sprite cache, interpreter execution, stage mutation, or renderer integration in this increment.

## 6. Scope and remaining uncertainty

This audit verifies archive byte order and field layout from the pinned source. It does not prove that every retail resource variant is identical, does not validate real asset contents, and does not resolve shared sprite-cache lifetime. Those remain separate evidence and implementation tasks.
