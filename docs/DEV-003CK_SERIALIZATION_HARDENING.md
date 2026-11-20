# DEV-003CK — Serialization Hardening

## Validation baseline

DEV-003CJ's three workflows completed successfully:

- Codec implementation `5838fc62`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44818646645
- Codec tests `274328f5`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44818680647
- Serialization contract docs `b3c68a96`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44818718141

## Hardening changes

### Exact size preflight

The encoder now computes the fixed-width payload size before allocating the byte buffer. The size formula includes:

- 20 bytes for magic, format version, outer schema, inner schema, and record count;
- 32 bytes per object record (six 32-bit fields and one 64-bit raw payload);
- 64 bytes for the fixed generation array including its length;
- 20 bytes per tile link;
- 124 bytes for the fixed 15-entry auxiliary 64-bit array including its length;
- 12 bytes per association;
- 20 bytes per packed metadata entry.

The encoder rejects an estimated payload over 16 MiB before serialization begins.

### Opaque data preservation

Tests exercise all 15 auxiliary entries with varied bit patterns, including zero, all-one, high-bit, alternating-bit, and mixed values. A separately constructed object snapshot verifies preservation of non-type opaque payload bits alongside the required Type-2 type field and tile link.

### Malformed-input coverage

New tests cover truncation at each header boundary, negative and excessive record counts, invalid record-type ordinals, unknown outer schema, and unknown inner object schema. Existing tests continue to cover invalid magic, unsupported binary format version, oversized input, duplicate/trailing/truncated payload cases, deterministic output, and empty-world round-trip.

## Commits

- Size preflight: `1d0053afffe27b3f796a477d1297f6078fecb59c`
- Malformed-payload and opaque-bit tests: `e4d57c3e562ecff83b20f3976fa7dab76a7d8b9f`
- Exact encoded-size correction (32 bytes/record; 20 bytes/link and metadata): `80932a2455905909f570bb601eebd51e8120b1ea`

## CI

DEV-003CK validation is in progress. Do not mark complete until the latest code and test commits have passed core JVM tests and Android debug APK assembly:

- Size preflight workflow: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829692527
- Hardening tests workflow: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829720492
- Exact size correction workflow: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829745999

## Limits

This remains a DIRU-NA native checkpoint format. It is not a parser or compatibility claim for original Diamond Rush save files. No checksum was added: current corruption detection is structural and semantic; a checksum should be introduced only with an explicit integrity/threat model and a versioned format decision.

## Next milestone — DEV-003CL: Codec Fuzz & Atomic Restore Validation

1. Exercise systematic byte mutations across header, count fields, record fields, tile keys, associations, and packed words.
2. Verify every rejected decode leaves an existing destination coordinator untouched.
3. Test duplicate tile keys and duplicate association keys using byte-level crafted payloads.
4. Confirm deterministic encoding across equivalent snapshots assembled with different map insertion orders.
5. Evaluate whether a checksum or authenticated envelope is warranted, then version the format rather than silently changing v1.