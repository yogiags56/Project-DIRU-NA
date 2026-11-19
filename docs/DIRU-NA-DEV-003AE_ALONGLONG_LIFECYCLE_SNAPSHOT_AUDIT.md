# DIRU-NA-DEV-003AE — aLongArr Lifecycle & Snapshot Audit

**Status:** Lifecycle audit committed; codec remains isolated from gameplay  
**Parent:** DEV-003AD  
**Source revision:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Source file:** `src/main/java/i.java`

## 1. Allocation and initialization

The 15-entry `aLongArr` and `bLongArr` arrays are initialized together in two observed contexts:

- Main/static initialization: both are allocated as `new long[15]` (source lines 17752–17753).
- The `arVoid()` setup path when `lBoolean` is true: `aLongArr`, `cLongArr`, `bLongArr`, and `dLongArr` are all allocated as 15-entry arrays (lines 15840–15845).

The first layout's allocator scans zero-based slots 0–14 and returns -1 when no free type field is found (lines 15863–15883). The second layout's allocator scans one-based IDs 1–15 using the 3-bit field at offset 28 (lines 16719–16747). Its source path does not contain an equivalent explicit full-capacity return before indexing; the isolated codec keeps a safe -1 result rather than reproducing a possible out-of-bounds access.

## 2. Field mapping correction

The first-layout allocator writes the arguments as follows:

| Offset | Width | Source value |
|---:|---:|---|
| 0 | 4 | Constant 1 (record type) |
| 4 | 2 | Fourth argument |
| 6 | 7 | First argument |
| 13 | 7 | Second argument |
| 20 | 7 | Third argument |
| 27 | 7 | First argument again |

The duplicate first-argument write at offsets 6 and 27 is intentional source behavior. The codec and test expectations were corrected to match this mapping.

## 3. Mutation and release paths

- First-layout allocation clears the selected 64-bit word before writing its fields (lines 15874–15880).
- The explicit release helper `fVoid(byte)` clears `aLongArr[id - 1]` to zero (lines 16658–16660).
- The second-layout allocator also clears the selected word before writing either type 1 or type 7 (lines 16719–16746).
- A movement/transition path clears an entry in `bLongArr` when its first-layout type is 3 (lines 16465–16470). This is a separate array and must not be redirected to the aLongArr codec.
- Teardown nulls `aLongArr`, `cLongArr`, `bLongArr`, and `dLongArr` (lines 4906–4911).

## 4. Snapshot and restore contract

When the checkpoint condition `lBoolean` is true:

1. Save copies all 15 words from `aLongArr` into `cLongArr` (lines 15223–15226).
2. Save copies all 15 words from `bLongArr` into `dLongArr` (lines 15227–15230).
3. Restore copies `cLongArr` back to `aLongArr` (line 15353).
4. Restore copies `dLongArr` back to `bLongArr` (line 15354).

The arrays are snapshotted independently. There is no source evidence for combining them or sharing snapshot buffers. The stage tile grid has its own separate snapshot path via `eIntArrArr` and `fIntArrArr`.

The save and restore copies use each live array's length and the 15-element arrays are initialized on the observed paths. The codec does not own or perform snapshot operations; preserving that separation avoids accidentally changing checkpoint timing.

## 5. Layout coexistence remains an open invariant

Both allocator layouts target `aLongArr`, but the source paths are distinct and use incompatible type-field interpretations. The source evidence gathered here establishes each path's field operations, not that both are simultaneously active in one stage/session. Do not instantiate two independent logical stores or migrate the records into separate arrays without further state-machine evidence.

The codec therefore exposes explicit nested `MotionLayout` and `ObjectLayout` views over the same backing array. Callers must select the correct view for the corresponding legacy path.

## 6. Validation status

- Source allocation, release, teardown, and snapshot/restore paths audited.
- Motion argument mapping corrected in codec and documentation.
- Regression tests exist for both layouts and raw field semantics.
- GitHub reports no status checks or workflow runs for the latest commits at audit time; tests are not claimed as executed or passing.
- No runtime/gameplay integration is claimed.

## 7. Next stage — DEV-003AF

Audit the state-machine transitions that select the two aLongArr layouts:
- identify all callers of the four-argument motion allocator;
- identify all callers of the six-argument object allocator;
- trace when each record type is consumed, released, and snapshotted;
- prove whether the two layouts can coexist or are mutually exclusive by mode/state;
- audit the independent `bLongArr` codec and its own lifecycle separately.

Only after this state-machine proof should the codec be integrated into a gameplay-facing record manager.
