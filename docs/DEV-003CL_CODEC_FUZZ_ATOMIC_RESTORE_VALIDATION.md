# DEV-003CL — Codec Fuzz & Atomic Restore Validation

## DEV-003CK validation

All five DEV-003CK code/test workflows completed successfully, including the final opaque-bit test correction:

- `1d0053af`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829692527
- `e4d57c3e`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829720492
- `80932a24`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829745999
- `bca23385`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829797515
- `d04df45e`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44829836272

Each workflow includes the core JVM test stage and Android debug APK assembly.

## Implementation

Added `CrossTableCheckpointBinaryCodec.decodeInto(byte[], CrossTableLifecycleCoordinator)`.

Behavior:

- A null destination returns `INVALID_SNAPSHOT` without mutation.
- Binary decode and full checkpoint integrity validation complete before destination restore is attempted.
- Malformed bytes throw `IOException` before the destination is touched.
- A semantically invalid but structurally decodable checkpoint returns `INVALID_SNAPSHOT` through the checkpoint validator.
- A valid checkpoint restores through the coordinator's candidate-first restore implementation.

This convenience operation does not change the version-1 wire format.

## Regression coverage

New tests cover:

- malformed decode against a populated destination, verifying the live record, type, tile lookup and auxiliary association remain intact;
- successful decode-into replacement, verifying the destination is replaced only after validation;
- deterministic output when packed metadata map insertion order changes;
- deterministic output when tile-link map insertion order changes;
- a deterministic one-byte mutation sweep across the encoded payload, requiring that malformed mutations be rejected without unchecked runtime exceptions;
- duplicate tile keys encoded at the binary layer;
- duplicate association handles encoded at the binary layer.

Existing tests continue to cover empty-world and full round trips, opaque 64-bit auxiliary payloads, object raw payload bits, truncation, trailing bytes, invalid magic/version, invalid counts, invalid record ordinals, and unknown schemas.

## Commits

- `decodeInto` API: `eff3e2ced1f29f24c4c31187aa03c4eda5660052`
- atomicity/determinism/mutation tests: `a993f595160773f94d2af14ebf5563b68eedf464`
- duplicate binary-key tests: `0d0db147f9312f51b5c14878b2cb0068627a08e0`

## CI

At documentation time, workflows for `eff3e2ce` and `a993f595` were still running. The final duplicate-key test workflow was queued:

- `eff3e2ce`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44830312551
- `a993f595`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44830337606
- `0d0db147`: https://github.com/yogiags56/Project-DIRU-NA/actions/runs/44830367444

Do not mark DEV-003CL complete until these workflows pass core JVM tests and Android debug APK assembly.

## Scope limits

The mutation sweep is deterministic test fuzzing, not a proof of exhaustive parser safety. It mutates one byte at a time with a fixed XOR mask. No randomized long-running fuzz campaign, external persistence integration, or original Diamond Rush save-format compatibility is claimed.

## Next milestone — DEV-003CM: Persistence Boundary & File I/O Contract

1. Define a small persistence boundary that writes and reads complete byte arrays without coupling the core model to Android APIs.
2. Specify write-to-temp, flush/sync, and atomic-replace expectations for a future Android file adapter.
3. Add file round-trip tests using temporary files in the JVM test environment.
4. Test truncated files and failed writes without overwriting the last known-good checkpoint.
5. Keep Android storage integration and original-game save migration as separate future tasks.