# DEV-003CS-2A — Exact Checkpoint Copy/Restore Assignment Ledger

**Status:** Static-source assignment trace completed for the pinned `saveCheckpoint()` and `restoreCheckpoint()` bodies; semantic classification remains partial.  
**Evidence:** `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.  
**Native branch:** `dev-003cr-snapshot-provider-boundary`.

> This is decompiled-source evidence, not a retail JAR bytecode comparison or runtime test. Opaque identifiers are preserved exactly. The ledger describes the temporary in-memory respawn checkpoint, not RMS/profile persistence.

## 1. Scalar copy ledger

| Checkpoint destination written by save | Live source copied | Restore assignment | Notes |
|---|---|---|---|
| `jByte` | `i.iByteArr[10]` | `i.iByteArr[10] = jByte` | Stage/checkpoint byte |
| `chInt` | `cfInt` | `cfInt = chInt` | Preserve opaque |
| `cgInt` | `ceInt` | `ceInt = cgInt` | Preserve opaque |
| `afInt` | `aaInt` | `aaInt = afInt` | Remaining-object count candidate |
| `bZInt` | `aCInt` | `aCInt = bZInt` | Checkpoint progression threshold |
| `bXInt` | `aZInt` | `aZInt = bXInt` | Preserve opaque/profile ownership unresolved |
| `bYInt` | `bbInt` | `bbInt = bYInt` | Pickup/progression candidate; ownership unresolved |
| `bSInt` | `crtPlayerX` | `crtPlayerX = bSInt` | Player logical tile X |
| `bTInt` | `crtPlayerY` | `crtPlayerY = bTInt` | Player logical tile Y |
| `bUInt` | `aUInt` | `aUInt = bUInt` | Distinct pickup counter candidate |
| `bVInt` | `aVInt` | `aVInt = bVInt` | Distinct pickup counter candidate |
| `bWInt` | `ayInt` | `ayInt = bWInt` | Door/timer-like state; opaque |
| `ajInt` | `aiInt` | `aiInt = ajInt` | Preserve opaque |
| `caInt` | `amInt` | only restored in `kByte == 1` branch as `amInt = caInt` | Mode-specific |
| `cbInt` | `alInt` | only restored in `kByte == 1` branch as `alInt = cbInt` | Mode-specific |
| `i.cnInt` | `i.cmInt` | `i.cmInt = i.cnInt` | Global group index pair |

The two mode-specific scalars above are copied on every save call but explicitly assigned back only in the `kByte == 1` restore branch. Do not generalize their restore semantics to every mode.

## 2. Always-copied grid/layer arrays

The save method iterates `x = 0 .. crtStageWidth-1` and copies exactly `crtStageHeight` elements per row:

| Live array | Checkpoint array | Restore direction |
|---|---|---|
| `i.bIntArrArr` | `i.cIntArrArr` | `cIntArrArr -> bIntArrArr` |
| `i.bByteArrArr` | `i.dByteArrArr` | `dByteArrArr -> bByteArrArr` |
| `i.crtStagePlayerLayer` | `i.eByteArrArr` | `eByteArrArr -> crtStagePlayerLayer` |
| `i.crtStageForegrondLayer` | `i.dIntArrArr` | `dIntArrArr -> crtStageForegrondLayer` |

Important corrections:
- `bIntArrArr` is the background/property integer layer, not the separate object-reference metadata grid `eIntArrArr`.
- `bByteArrArr` is explicitly copied. The source does **not** copy `cByteArrArr` in these loops; its checkpoint inclusion remains unproven.
- Foreground is an `int[][]`, so upper metadata bits must be preserved exactly; truncating it to a byte is incompatible.
- The source copies row contents, not row references.

## 3. Conditional object/auxiliary feature state

All following copies occur only when `lBoolean` is true:

| Live state | Checkpoint state | Restore behavior |
|---|---|---|
| `i.eIntArrArr` | `i.fIntArrArr` | Copy checkpoint grid back to live grid |
| `i.aLongArr` | `i.cLongArr` | Copy 15-entry object table back |
| `i.bLongArr` | `i.dLongArr` | Copy 15-entry auxiliary table back |
| `xByte` | `yByte` | `xByte = yByte` |
| `dqInt` | `dAInt` | `dqInt = dAInt` |
| `drInt` | `dBInt` | `drInt = dBInt` |
| `dsInt` | `dCInt` | `dsInt = dCInt` |
| `vByte` | `zByte` | `vByte = zByte` |
| `dvInt` | `dDInt` | `dvInt = dDInt` |
| `dwInt` | `dEInt` | `dwInt = dEInt` |
| `dxInt` | `dFInt` | `dxInt = dFInt` |
| `wByte` | `AByte` | `wByte = AByte` |
| `dyInt` | `dGInt` | `dyInt = dGInt` |
| `dzInt` | `dHInt` | `dzInt = dHInt` |
| `alBoolean` | `i.amBoolean` | `alBoolean = i.amBoolean` |
| `dtInt` | `dIInt` | `dtInt = dIInt` |
| `duInt` | `dJInt` | `duInt = dJInt` |

The live `mByteArr` has a separate conditional copy into `oByteArr` when non-null; restore copies back only when the live array is non-null. Preserve the null/length precondition. This is not the same feature gate as `lBoolean`.

## 4. Restore side effects and ordering observed

The restore method does more than copy saved values:

1. Resets several transient controller/interaction fields, sets a restore flag, and recenters camera coordinates from saved player tile X/Y using 24-pixel tile arithmetic.
2. Handles pending `qBoolean` / `rBoolean` branches with hard-coded stage-grid mutations and mode transitions.
3. Restores checkpoint byte, selected counters, player coordinates, and mode-dependent values; resets player animation to index 2 and movement state to `kInt = 2`.
4. Restores `cmInt` and optional `mByteArr`.
5. Restores the four always-copied grids/layers.
6. Rebuilds dirty/update markers by scanning interior cells and calling `kVoid(x,y)` when player-layer or foreground low-byte values are in the observed 0..79 range.
7. Performs optional animator reset/marker cleanup.
8. Restores `ayInt`, clears/sets selected transient state.
9. If `lBoolean`, restores the metadata grid, both 15-long tables, and all associated conditional scalar/boolean state as one group.
10. If `i.eByteArr != null`, scans the restored player layer for code 48 and reconstructs moving-object runtime state through `dVoid((byte)48)` or `aiVoid()`, selected by the cell's background/property bit 0x8.

This ordering is evidence for a multi-phase restore transaction. The exact transitive writes of helper calls `kVoid`, `yVoid`, `dVoid`, and `aiVoid` still need dedicated source traces before a Native restore implementation can reproduce them.

## 5. Native schema decisions

- Preserve the four copied grid/layer types and dimensions exactly; retain foreground as 32-bit cells.
- Include `bByteArrArr` as checkpoint state; keep `cByteArrArr` excluded until a direct save/restore path is found.
- Treat `mByteArr` as a separate optional array with explicit presence and exact length validation.
- Treat the `lBoolean`-gated metadata/object/auxiliary/scalar group as all-present or all-absent; validate the 15-entry table capacities.
- Preserve opaque scalar values until each writer, reader, mode scope, and ownership domain is traced.
- Model restore as detached candidate construction followed by validation and one commit; derived caches, dirty markers, animation, camera, and code-48 runtime spans are rebuilt/reset in explicit post-copy phases.
- Never serialize platform resources, sprite handles, animator objects, or input-held state.

## 6. Open evidence gaps

- Exact allocation/initialization paths and dimensions for every copied array on every stage/mode setup path.
- Full scalar writer/reader and mode-ownership traces for opaque values.
- Exact `mByteArr` producer/consumer and nullability lifecycle.
- Complete semantics of the `qBoolean` / `rBoolean` hard-coded restore branches.
- Full transitive side-effect traces of `kVoid`, `yVoid`, `dVoid`, and `aiVoid`.
- Runtime/retail-JAR vectors for capture/restore and conditional feature on/off paths.
- Independent tests proving invalid candidates never mutate live state.

**Exit:** The direct copy/restore assignment inventory for the pinned method bodies is now recorded. CS-2 is not complete: semantics, invariants, helper side effects, and runtime compatibility remain open. No production snapshot schema or provider is authorized by this document.
