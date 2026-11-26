# DEV-003CS-2 — Checkpoint Copy/Restore Source Trace

**Status:** Partial trace completed from existing static audits.  
**Candidate source:** `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`.  
**Purpose:** Record exact known save/restore pairs and distinguish proven snapshot data from categories that still need source-level tracing.

> Evidence is static decompilation analysis, not runtime validation or a comparison with the original shipping JAR bytecode. Source identifiers remain opaque where semantics are not proven.

## 1. Confirmed copy/restore pairs

| Live state | Checkpoint buffer | Save evidence | Restore evidence | Current conclusion |
|---|---|---|---|---|
| Tile metadata grid `eIntArrArr` | `fIntArrArr` | `saveCheckpoint()`, approximately source lines 15190–15270; exact row/element copy described in DEV-003AZ | `restoreCheckpoint()`, approximately lines 15330–15370 | Restore grid and record tables as one coherent transaction |
| Object-record table `aLongArr[15]` | `cLongArr[15]` | Copy all 15 words, lines 15223–15226 (DEV-003AE) | Copy back, line 15353 (DEV-003AE) | Exact fixed capacity 15; opaque packed records |
| Auxiliary table `bLongArr[15]` | `dLongArr[15]` | Copy all 15 words, lines 15227–15230 (DEV-003AE) | Copy back, line 15354 (DEV-003AE) | Exact fixed capacity 15; separate from object records |
| Background layer `bIntArrArr` | Snapshot counterpart not yet independently named in this trace | AUDIT-004 §6.2 lists background layer as part of checkpoint contents | AUDIT-004 §6.3 says stage arrays are restored | Inclusion is established at category level; exact copy target/range remains open |
| Player/object layer `crtStagePlayerLayer` | Snapshot counterpart not yet independently named in this trace | AUDIT-009 §3 describes it among checkpoint state arrays | AUDIT-009 §4 says player/object layer is restored | Inclusion established at category level; exact copy target/range remains open |
| Foreground layer `crtStageForegrondLayer` | Snapshot counterpart not yet independently named in this trace | AUDIT-004 §6.2 / AUDIT-009 §3 | AUDIT-004 §6.3 / AUDIT-009 §4 | Inclusion established at category level; exact copy target/range remains open |
| Per-cell animation/timer array `bByteArrArr` | Exact snapshot buffer not yet independently named in this trace | AUDIT-002 §17.1 explicitly lists `bByteArrArr` in checkpoint contents; AUDIT-009 §3–4 describes per-cell byte timers and stage-array restoration | AUDIT-002 §17.1 says the listed arrays are copied back | Candidate include; exact dimensions and copy target still need direct trace |

## 2. Object and auxiliary table semantics

### 2.1 `aLongArr` — object records

DEV-003BE records:
- 15 slots.
- Packed object type bits 28..30.
- X coordinate bits 31..36.
- Y coordinate bits 37..42.
- Metadata group bits 43..44.
- One-based object IDs; packed accessors index `recordId - 1`.
- The Type-5 traversal stores the allocated object ID in tile metadata.

The exact meaning of every remaining bit is not established. Native schema should initially preserve the packed record as an opaque 64-bit value or use a separately reviewed typed model; it must not guess missing semantics.

### 2.2 `bLongArr` — auxiliary work records

DEV-003BE / DEV-003BL records:
- 15 slots, distinct from `aLongArr`.
- Low four bits are a state/kind field.
- Bits 4..5 encode direction.
- Coordinate fields begin at bits 13 and 20.
- Controller state 4 consumes entries whose low four bits equal 3.
- Coordinate lookup alone does not prove that an entry is active.

DEV-003BL adds an isolated Native model that stores the 15 entries as opaque longs with defensive-copy snapshot behavior. Its current scope is the object/auxiliary subsystem, not the entire gameplay checkpoint.

### 2.3 Atomic relationship

When the feature is active, checkpoint save/restore couples:
1. Tile metadata grid `eIntArrArr` / `fIntArrArr`;
2. Object records `aLongArr` / `cLongArr`;
3. Auxiliary records `bLongArr` / `dLongArr`;
4. Associated controller state.

Restoring tile metadata without the referenced object table, or restoring object records without their tile metadata, can produce inconsistent references. Treat these as one validated atomic state group.

### Byte-state distinction

AUDIT-002 §3 identifies both `bByteArrArr` (auxiliary per-cell byte state) and `cByteArrArr` (animation/timing/state byte layer) in stage runtime data. However, AUDIT-002 §17.1 explicitly lists `bByteArrArr` in checkpoint contents and does **not** explicitly list `cByteArrArr`. Therefore:
- `bByteArrArr` is a checkpoint candidate supported at category level.
- `cByteArrArr` remains runtime state with checkpoint inclusion **unproven**; do not add it to the serialized snapshot until a direct save/restore copy path is located.

## 3. Conditional feature lifecycle

DEV-003AZ records that:
- `arVoid()` initializes fresh 15-entry object and auxiliary tables and their checkpoint arrays when `lBoolean` is active.
- The stage initialization path conditionally creates a fresh tile metadata grid and initializes the feature.
- A first gameplay interaction can activate the feature and initialize the metadata grid.
- Stage teardown nulls live and checkpoint tables together.

Therefore the object/auxiliary section is conditional feature state. A Native snapshot must carry an explicit feature-enabled state and validate that the related tables are all present together. It must not serialize a mixture of active and absent tables.

## 4. Corrections to the preliminary field matrix

- Replace the generic label “integer object-state grid” with **tile metadata grid `eIntArrArr`**, which stores packed references/metadata and is snapshotted to `fIntArrArr`.
- Keep `bIntArrArr` as the separate background/property layer; do not conflate it with `eIntArrArr`.
- Add `aLongArr[15]` and `bLongArr[15]` as distinct conditional feature tables, each with its own checkpoint copy.
- Keep `bByteArrArr` and `cByteArrArr` as distinct per-cell byte-state arrays; do not conflate either with the 15-entry `bLongArr`.
- The source checkpoint is an in-memory respawn snapshot, not durable RMS persistence.

## 5. Remaining trace work

1. Find the exact row/element copy statements for player/object, background, foreground, and per-cell byte arrays, including their paired restore assignments.
2. Confirm allocation and dimensions for each layer and byte array at every stage setup path.
3. Trace the scalar/controller fields copied in save and restore, including feature activation, traversal state, and mode-specific values.
4. Reconcile optional moving-object arrays with the object/auxiliary tables and stage feature flag.
5. Establish which arrays are always present and which exist only when the feature is active.
6. Review DEV-003BL's Native isolated table model against the full gameplay owner contract before reuse.

## 6. CS-2 assessment

**Newly confirmed:** exact 15-entry object and auxiliary table copy pairs; tile metadata grid pairing; atomic relationship between tile metadata and referenced records; conditional feature lifecycle.

**Still open:** exact copy/restore pairs and dimensions for the three stage layers and `bByteArrArr`; checkpoint inclusion of `cByteArrArr` is unproven; full scalar/controller snapshot list; final schema types and compatibility policy.

CS-2 remains in progress. This trace does not authorize a production snapshot provider yet.


## 7. Scalar, controller, and reconstruction-state audit

Cross-checking AUDIT-002 §17.1 and §22, AUDIT-004 §§6.1–6.3, and AUDIT-009 §§3–4 yields this category-level inventory. The audits do not yet provide a complete assignment-by-assignment scalar variable list, so these are categories, not invented source identifiers.

| State category | Capture evidence | Restore evidence | Native treatment |
|---|---|---|---|
| Player tile X/Y | AUDIT-009 §3; AUDIT-004 §6.2 | AUDIT-009 §4 steps 1/4 | Required scalar pair; validate against restored stage bounds |
| Checkpoint index/value (save byte 10) | AUDIT-009 §3 | AUDIT-009 §4 step 2 | Required for checkpoint progression consistency; distinct from durable save serialization |
| Player/gameplay counters and state bytes | AUDIT-002 §22; AUDIT-009 §3 | AUDIT-002 §22; AUDIT-009 §4 step 2 | Include only individually traced fields; preserve opaque values until semantics are proven |
| World/stage progress and inventory-like fields | AUDIT-009 §3; AUDIT-002 §22 | AUDIT-009 §4 step 2 | Establish checkpoint-local versus profile progression ownership before schema inclusion |
| Currency/score-like counters | AUDIT-002 §22 | AUDIT-002 §22; AUDIT-009 §3–4 | Conditional pending exact writer/UI and restore trace; do not conflate score, currency, and profile totals |
| Movement/interpolation state | AUDIT-004 §§2.2, 6.2 | AUDIT-009 §4 step 4 resets movement/state fields | Capture only at safe point after interpolation reaches zero; do not persist mid-transition interpolation |
| Interaction/animation state | AUDIT-004 §6.2 | AUDIT-009 §4 steps 1/3 reset transient fields and set hero animator slot 0 to animation 2 | Prefer deterministic reset/reconstruction for transient fields |
| Camera offsets/target | AUDIT-002 §17.1 notes camera-related state; AUDIT-004 §6.3 and AUDIT-009 §4 step 1 describe camera recentering | Camera recentered from saved player tile | Treat as derived presentation state unless direct saved scalar assignments are found |
| Dirty/update markers | AUDIT-009 §4 step 6 | Rebuilt after stage grids restore | Recompute after restore |
| Object code 48 multi-cell reconstruction | AUDIT-002 §17.1; AUDIT-009 §4 step 8 | Reconstructed after grid restore using mode/background-dependent path | Explicit post-restore phase; validate metadata/table references first |
| Input/held-key state | AUDIT-009 §4 step 1 | Transient input fields reset | Exclude |
| Audio/resource handles | No snapshot capture evidence | Runtime resources reinitialized as needed | Exclude; reacquire through runtime resource systems |

### 7.1 Capture safe point

AUDIT-004 §6.1 and §2.2 establish deferred capture:
1. Detect a new checkpoint tile and set `shouldSaveCheckpoint`.
2. Finish tile movement/interpolation (`jInt == 0` in the audited movement description).
3. Play checkpoint feedback and invoke `saveCheckpoint()`.

Native capture must be requested on the single-writer gameplay owner at a stable tick boundary. A request arriving during movement remains pending; the Android/UI thread must never read mutable game arrays directly.

### 7.2 Restore ordering

The observed behavior supports this high-level order:
1. Reset transient input/interaction/animation state and establish camera offsets from the saved logical player tile.
2. Restore only confirmed checkpoint-local scalar/progression fields.
3. Restore player tile coordinates and reset movement state.
4. Restore stage grids and conditional object/auxiliary tables as coherent groups.
5. Rebuild dirty/update markers.
6. Reconstruct special multi-cell objects (including code 48) after backing metadata and records are valid.
7. Recompute camera presentation from restored logical state.

This is an observed outline, not a final native API. Exact scalar assignment order and the precise field set in step 2 remain open.

### 7.3 Remaining scalar evidence gaps

- Enumerate every scalar copied by `saveCheckpoint()` and assigned by `restoreCheckpoint()` with exact source identifiers and line references.
- Trace progression/inventory/currency candidates to all writers and UI display sites to decide checkpoint-local versus durable profile ownership.
- Confirm the optional `lBoolean` feature flag and associated counters/arrays.
- Locate exact camera scalar copies, if any; until then camera remains recomputable presentation state.
- Confirm whether object code 48 is the only post-restore reconstruction path.
- Preserve the distinction between fields captured and fields reset/recomputed during restore.


## 8. Save-record byte ownership cross-check (AUDIT-002 §§21–22)

A separate save-record audit maps bytes 3–12 to durable progression fields. This is a different persistence domain from the in-memory checkpoint snapshot. The cross-check is useful for ownership, but it does not prove that every save-record byte is copied by `saveCheckpoint()`.

| Save-record byte(s) | Audited source behavior | Checkpoint interpretation |
|---|---|---|
| 3 | Current/selected progression index copied to/from `azInt` | Candidate scalar relationship; exact checkpoint capture/restore assignment still needs direct source statement |
| 4–5 | Little-endian 16-bit `aZInt`, read/written by shop logic | Durable profile/shop state; do not include in temporary stage snapshot without direct copy evidence |
| 6–7 | Little-endian 16-bit `bbInt`, shop increments and price thresholds | Durable profile/shop state; same exclusion rule |
| 8 | Hero progression value initialized to 4, palette offset `value - 4`, passed to damage handling | Gameplay progression value; retry handler explicitly resets bounded hero value from save byte 8 |
| 9 | Progression/unlock tier, values 1, 2, 8 used at story/seal milestones | Durable progression; include in checkpoint only if direct snapshot assignment is verified |
| 10 | Boolean-like gameplay state, initialized 0 and toggled during gameplay | **Not a checkpoint index.** AUDIT-009 says restore copies this byte; exact semantics remain unresolved |
| 11 | Small state byte `rByte`, persisted by `wVoid()`/`xVoid()` | Durable save field; checkpoint inclusion unproven |
| 12 | Small progression counter, incremented/clamped to 3 in story flow | Durable progression candidate; checkpoint inclusion unproven |

### 8.1 Corrections to earlier shorthand

- Replace “checkpoint index/value (save byte 10)” with **“save byte 10 — boolean-like gameplay state restored by checkpoint; semantic name unresolved.”**
- `azInt` has a documented relationship with save byte 3 (selected progression index), while the retry handler also decrements `azInt`. This dual use must be traced through all writers/readers before assigning a semantic label such as lives, world index, or retry count.
- Save byte 8 is the bounded hero progression value restored by the retry handler; it must not be confused with `nByte` or assumed to be a conventional health field.
- Bytes 4–7, 9, 11, and 12 belong to the durable save-record map. Their presence in that map is not proof that they are part of the in-memory checkpoint snapshot.
- The source audit confirms checkpoint restore copies selected progression fields, but the exact variable-by-variable assignment list is still outstanding.

### 8.2 Persistence-domain rule

Maintain two explicit models:
1. **Durable profile save:** record bytes and progression/shop/unlock state persisted through the ordinary save writer.
2. **Temporary in-memory checkpoint:** stage/player state captured for respawn and restored during the current runtime.

A field may participate in both only when the source shows both paths. Never infer checkpoint membership from save-record membership alone.

## 9. Exact source-level capture/restore assignment ledger

Direct source inspected: kubikaugustyn/DiamondRushSource/src/main/java/i.java, pinned revision 215c71d04057d66a1b08419b1a4b1043ae3e0881. Method ranges: saveCheckpoint() lines 15190–15246; restoreCheckpoint() lines 15248–15382. Static source inspection only; not runtime validation or shipping-JAR bytecode comparison.

### 9.1 Unconditional scalar pairs

| Capture (line) | Restore (line) | Evidence label |
|---|---|---|
| jByte = i.iByteArr[10] (15191) | i.iByteArr[10] = jByte (15284) | Save byte 10; boolean-like gameplay state, semantic name unresolved |
| chInt = cfInt (15192) | cfInt = chInt (15286) | Opaque scalar |
| cgInt = ceInt (15193) | ceInt = cgInt (15287) | Opaque scalar |
| afInt = aaInt (15194) | aaInt = afInt (15285) | Opaque scalar |
| bZInt = aCInt (15195) | aCInt = bZInt (15288) | Opaque scalar |
| bXInt = aZInt (15196) | aZInt = bXInt (15290) | Opaque scalar |
| bYInt = bbInt (15197) | bbInt = bYInt (15289) | Opaque scalar |
| bSInt = crtPlayerX (15198) | crtPlayerX = bSInt (15294) | Player logical tile X |
| bTInt = crtPlayerY (15199) | crtPlayerY = bTInt (15295) | Player logical tile Y |
| bUInt = aUInt (15200) | aUInt = bUInt (15296) | Opaque scalar |
| bVInt = aVInt (15201) | aVInt = bVInt (15297) | Opaque scalar |
| bWInt = ayInt (15202) | ayInt = bWInt (15344) | Spawn/original-X-related per stage init audit; retain opaque |
| ajInt = aiInt (15203) | aiInt = ajInt (15298) | Opaque scalar |
| cnInt = cmInt (15206) | cmInt = cnInt (15321) | Static/global scalar |

### 9.2 Mode-dependent scalar pairs (only when lBoolean is true)

| Capture (line) | Restore (line) |
|---|---|
| yByte = xByte (15231) | xByte = yByte (15355) |
| dAInt = dqInt (15232) | dqInt = dAInt (15356) |
| dBInt = drInt (15233) | drInt = dBInt (15357) |
| dCInt = dsInt (15234) | dsInt = dCInt (15358) |
| zByte = vByte (15235) | vByte = zByte (15359) |
| dDInt = dvInt (15236) | dvInt = dDInt (15360) |
| dEInt = dwInt (15237) | dwInt = dEInt (15361) |
| dFInt = dxInt (15238) | dxInt = dFInt (15362) |
| AByte = wByte (15239) | wByte = AByte (15363) |
| dGInt = dyInt (15240) | dyInt = dGInt (15364) |
| dHInt = dzInt (15241) | dzInt = dHInt (15365) |
| amBoolean = alBoolean (15242) | alBoolean = amBoolean (15347) |
| dIInt = dtInt (15243) | dtInt = dIInt (15348) |
| dJInt = duInt (15244) | duInt = dJInt (15349) |

The caInt = amInt and cbInt = alInt pairs are captured unconditionally (15204–15205), but restored only when kByte == 1 (15315–15318). Other kByte branches execute reconstruction/reset behavior rather than restoring a paired value.

### 9.3 Exact array copy pairs

| Live state | Snapshot buffer | Capture lines | Restore lines |
|---|---|---:|---:|
| Optional mByteArr | oByteArr | 15207–15209 | 15322–15324 |
| bIntArrArr | cIntArrArr | 15210–15211 | 15325–15326 |
| bByteArrArr | dByteArrArr | 15210–15212 | 15325–15327 |
| crtStagePlayerLayer | eByteArrArr | 15210–15213 | 15325–15328 |
| crtStageForegrondLayer | dIntArrArr | 15210–15214 | 15325–15329 |
| Conditional eIntArrArr | fIntArrArr | 15216–15222 | 15346–15352 |
| Conditional aLongArr | cLongArr | 15223–15226 | 15353 |
| Conditional bLongArr | dLongArr | 15227–15230 | 15354 |

Grid copies iterate crtStageWidth rows and copy crtStageHeight elements. The 15-long snapshot buffers are allocated when null and copied using the live array length.

Correction: cByteArrArr is not copied by these checkpoint methods. bByteArrArr is copied through dByteArrArr.

### 9.4 Restore-side behavior that is not captured data

- Resets transient/control fields (cEInt, aHInt/aGInt/aFInt, bgInt/bhInt, EBoolean, blInt, tBoolean, axInt, kInt, bjInt) and sets hero animator slot 0 to animation 2 (15249–15259, 15282–15293).
- Computes camera offsets from saved tile coordinates: ciInt = bSInt * 24 - 120; cjInt = bTInt * 24 - 160 + 40 (15257–15258). These are derived, not copied.
- Applies special-cell mutations when qBoolean or rBoolean is active (15264–15280).
- Rebuilds cell update/dirty state by scanning restored player/foreground layers and calling kVoid(k,j) (15331–15339).
- Resets optional animator slot 2 and clears player-layer cell [18][63] if that animator exists (15340–15343).
- Sets oBoolean = true (15345).
- After optional mode arrays restore, scans player-layer cells for object code 48 and dispatches dVoid(48) or aiVoid() based on background bit 0x8 (15367–15380).

### 9.5 CS-2 scalar evidence status

Closed at static-source assignment level: exact unconditional scalar pairs; mode-dependent scalar pairs; kByte == 1 pair; stage-grid and byte-array mappings; conditional object/auxiliary table mappings; restore-side reset/derived/reconstruction behavior.

Still open before CS-2 can close: semantic ownership for opaque scalars; stage dimension/allocation invariants and source null/length preconditions; complete lBoolean activation/lifecycle; world/stage/mode compatibility key; independent comparison against original shipping binary; deterministic runtime round-trip tests.

Do not invent friendly names for opaque decompiler identifiers. Assign Native semantic names only after tracing their writers/readers and mode-specific behavior.


## 10. Scalar semantic ownership trace — source revision 215c71d

This section traces uses outside the checkpoint methods. Labels are intentionally conservative: a variable is assigned a gameplay role only where its writers/readers support that role.

| Field(s) | Additional source evidence | Conservative interpretation | Snapshot/schema consequence |
|---|---|---|---|
| `aCInt` | Checkpoint tile handler reads foreground high bits, requires value >= current `aCInt`, then sets `aCInt = n11 + 1` (12159–12162); restored from `bZInt` at 15288 | Checkpoint progression threshold / next checkpoint marker | Strong semantic evidence; preserve as checkpoint progression scalar, not generic checkpoint ID |
| `aaInt` | Stage setup assigns it from a cell's background value (3793); decremented on collection and compared with zero to remove checkpoint/associated cell (15437–15440) | Remaining count for a stage object/collectible group | Snapshot field is required for consistent stage object state; exact UI label remains unknown |
| `aZInt` | Incremented when qualifying object is collected (15434); used with other stage progress logic; retry path also mutates it (11235) | Collected-object/progress counter candidate | Include as opaque progression counter until all writers and reset rules are mapped |
| `aUInt`, `aVInt` | Incremented in pickup cases 4/5 (11244–11251); checked as positive before interaction with foreground types 9/8 (13184) | Two distinct consumable/key-like inventory counters | Keep distinct typed counters; do not merge |
| `bbInt` | Incremented in pickup case 2 (11254–11257) | Pickup/progression counter; exact item identity unresolved | Preserve separately; do not label currency or score without UI/source proof |
| `cmInt` | Set from foreground cell high bits for a door interaction (12972); bounds-checked against `mByteArr.length` (12974–12976); decremented in `alVoid()` and used to locate matching door cells (14793–14804) | Selected door/lock group index into `mByteArr` | Pair `cmInt` with `mByteArr` snapshot; restore ordering and bounds validation are critical |
| `cfInt`, `ceInt` | `dBoolean()` sets `cfInt` to ±1 based on `ceInt` (9440–9455); update loop advances `ceInt`, stops at 0 or 9, and scans/rebuilds affected cells at phase 5 (10500–10520) | Direction and phase/progress for a staged foreground mechanism/animation | These are active simulation state, not presentation-only; both must be captured together |
| `ayInt` | Read by runtime gate at 12519; copied into `bWInt` and restored at 15344; also written in stage/runtime logic | Runtime gating/position-related state; semantic role not established | Remain opaque; don't call it spawn X until exact writer/reader path is proven |
| `aiInt` | Used as argument to `lVoid(int)` (14462–14476), and updated in other gameplay flow | State/operation code associated with object interaction | Opaque enum/state code; preserve exact integer value |
| `xByte` and `dqInt`...`duInt`, `vByte`, `wByte`, `dvInt`...`dzInt` | Initialized together by `arVoid()` when `lBoolean` is true (15840–15860); consumed by `asVoid()` (15932 onward) and mode-specific movement/reconstruction branches (16512 onward) | Coherent optional mode engine state; fields include mode, phase/counters, coordinates/scan bounds, and selected encoded record | Treat as one conditional sub-snapshot. Do not individually rename fields until packed-record bitfields and all mode branches are traced |
| `chInt`, `cgInt`, `afInt`, `bZInt`, `bXInt`, `bYInt`, `aUInt`, `aVInt`, `bUInt`, `bVInt`, `bWInt`, `ajInt`, `cnInt` | Some have partial use sites, but not enough to establish a stable semantic identity across all stage modes | Opaque scalar pairs except the specific interpretations above | Preserve source-width/type and exact pairing; defer friendly Native names |

### 10.1 Important correction to earlier provisional labels

- `aCInt` is supported as checkpoint threshold/progression state; it is not merely an opaque counter.
- `aaInt` is loaded from stage cell metadata and decremented with object/collectible removal; it must be restored consistently with stage grids.
- `cmInt` is a door/lock group selector tied to `mByteArr`; it is not a general stage ID.
- `cfInt` and `ceInt` form a coupled directional phase mechanism. Resetting one without the other can leave the stage mechanism inconsistent.
- The prior tentative “spawn/original-X” label for `ayInt` is withdrawn; current evidence only supports an opaque runtime gating/position-related role.

### 10.2 Remaining semantic gaps

- Trace all writers/readers and reset points for the remaining opaque unconditional scalar pairs.
- Decode the optional-mode packed record format and map each mode field to stable semantics.
- Determine exact item identities/UI meanings for `aUInt`, `aVInt`, and `bbInt`.
- Establish whether `aZInt` is stage-local, checkpoint-local, or also profile-persistent.
- Verify the relationship between `cmInt` and `mByteArr` under every door/lock mode.


## 11. Optional mode lifecycle and packed-record structure

Direct source: pinned i.java revision 215c71d04057d66a1b08419b1a4b1043ae3e0881.

### 11.1 Activation and teardown

- Constructor defaults set `alBoolean=true`, `dqInt=-1`, `dzInt=-1`, `xByte=3` (lines 691–694).
- Stage teardown/reset nulls the four 15-entry long arrays, sets `alBoolean=false`, initializes the counters/coordinates, and sets `lBoolean=false` (4905–4924).
- On foreground stage trigger code 19, `arVoid()` runs only if `lBoolean` is currently false; then `lBoolean=true`, `alBoolean=false`, the conditional integer grid is allocated if absent, and the mode-specific setup continues (12113–12130).
- `arVoid()` allocates four distinct 15-entry long arrays and initializes mode fields (15840–15860). The four arrays are `aLongArr`, `cLongArr`, `bLongArr`, and `dLongArr`.
- `asVoid()` immediately returns while `lBoolean=false` (15932–15935).
- Restore has a conditional path for the same mode fields and arrays; the stage mode must be established before applying that conditional snapshot.

### 11.2 Packed record evidence

The optional mode uses 15 slots in each of two live record tables, `aLongArr` and `bLongArr`, with checkpoint copies `cLongArr` and `dLongArr`. The source helpers extract and replace bit ranges in each 64-bit slot. Observed bit ranges include:

| Bit offset | Width | Evidence of use |
|---:|---:|---|
| 0 | 4 | Slot kind/state; zero means unused in allocation scan |
| 4 | 2 | Small direction/operation code |
| 6 | 7 | Coordinate or bounded value |
| 13 | 7 | X-like coordinate in several record operations |
| 20 | 7 | Y-like coordinate in several record operations |
| 26 | 2 | Subtype/step selector |
| 28 | 3 | Record lifecycle state; values 1–7 are switched on |
| 31 | 6 | Record coordinate |
| 37 | 6 | Record coordinate |
| 43 | 2 | Axis/cell selector |
| 45 | 2 | Direction/operation selector |
| 47 | 2 | Record behavior mode |
| 49 | 5 | Length/interval-like value |
| 54 | 3 | Encoded linked-record/operation selector |
| 57 | 1 | Boolean flag |

The bit ranges above are directly observed accessor offsets and use sites, not a complete formal schema. Some values have context-dependent meanings and must not yet be assigned user-facing names. Bits 58–63 were not established by the inspected accessors.

The accessor pair for `aLongArr` reads/writes slots by one-based index; the accessor pair for `bLongArr` reads/writes by zero-based index. Keep these APIs distinct in a Native decoder to avoid off-by-one corruption.

### 11.3 Mode lifecycle state machine

The `xByte` switch in `asVoid()` exposes these observed transitions:

| xByte | Observed behavior |
|---:|---|
| 1 | Advances into record-processing state 2 on a subsequent update |
| 2 | Iterates the 15 record slots, advances active records, and handles completion/removal |
| 3 | Idle/ready state; `dBoolean()` permits mode interaction only in this state |
| 4 | Completion/rebuild pass over records; returns to state 2 |
| 5 | Builds/starts a mechanism operation from `dvInt`, `dwInt`, and `dxInt`; creates/updates packed records and enters state 1 |

Additional transitions:
- `lVoid(dvInt,dwInt)` only acts while `lBoolean` is true. It determines direction from adjacent `eIntArrArr` cells, sets `xByte=5`, and records the selected coordinates (16783–16796).
- `mVoid(n,n2)` locates a record for the cell, updates the packed record and enters `xByte=1` (16798–16811).
- During record processing, completion may clear a slot, set `xByte=3`, and set `alBoolean=true` (16218–16245).
- The per-frame stage update uses `alBoolean` as a one-shot work gate; when set, it processes the next `dtInt` entry against the packed table, increments the cursor, and resets `dtInt/duInt` when exhausted (9521–9529).
- `dqInt` and `dzInt` are active-record timers/cursors, `drInt` tracks a processing condition, and `vByte` is a small progress/offset value in the record processing path. Treat these interpretations as provisional until all branches are traced.

### 11.4 Native snapshot contract implications

1. Treat `lBoolean` as the activation discriminator for the entire optional-mode sub-snapshot.
2. When active, capture/restore all four 15-entry long tables and the associated scalar set atomically: `xByte`, `alBoolean`, `dqInt`, `drInt`, `dsInt`, `vByte`, `dvInt`, `dwInt`, `dxInt`, `wByte`, `dyInt`, `dzInt`, `dtInt`, and `duInt`.
3. Preserve the distinct indexing conventions of the two live table accessor families.
4. Validate each packed record before use: known lifecycle/type values, in-range coordinates, valid slot references, and legal field widths. Do not silently normalize unknown values.
5. Preserve stage grid + packed record tables + associated scalar state as one logical transaction; partial restore can leave records referring to absent or changed stage cells.
6. When `lBoolean=false`, do not serialize stale optional-mode arrays as active state. The snapshot should encode the mode as inactive and use the source-defined reset/rebuild path.

### 11.5 Remaining work

- Trace exact meaning of every packed bitfield and linked-record reference.
- Trace all writes to `xByte` and identify transitions caused outside `asVoid()`.
- Confirm whether `cLongArr`/`dLongArr` are always pure snapshot buffers or are read by other runtime paths.
- Determine exact valid coordinate bounds and slot-reference invariants for all stage dimensions.
- Validate this model against runtime captures and the original shipping binary.


### 11.6 Auxiliary-table ownership cross-check

A complete occurrence scan of the pinned source revision found:
- `cLongArr` is only declared, cleared during stage teardown, allocated during checkpoint capture if absent, written by copying `aLongArr`, and read back by checkpoint restore.
- `dLongArr` has the equivalent lifecycle for `bLongArr`.
- No other runtime reader/writer of `cLongArr` or `dLongArr` exists in this source revision. They are checkpoint backup buffers, not active simulation tables.
- `aLongArr` and `bLongArr` are active 15-slot packed-record tables. They are initialized in mode activation and also allocated in global/runtime initialization; they are mutated by record helpers and update logic.
- Do not confuse `aLongArr` (15-entry optional-mode records) with `aLongArrArr` (a separate 12×12 long grid used by unrelated stage/render logic).
- `cLongArr` and `dLongArr` are allocated lazily by capture and are not allocated by `arVoid()`; `arVoid()` allocates the active source arrays and their snapshot peers.

This confirms the active-vs-backup distinction for the two long-table pairs at static-source level.


## 12. Optional mode call-site correction and full transition-writer audit

This section supersedes the activation-order wording in §11.1 where it conflicts with the direct call-site evidence. Pinned source revision remains 215c71d04057d66a1b08419b1a4b1043ae3e0881.

### 12.1 Correct activation/allocation order

There are two call sites for `arVoid()`, and their guards differ:

1. Stage-load switch case 3 calls `arVoid()` only when `lBoolean == true`, then allocates `eIntArrArr` and calls `afVoid()` (4594–4599).
2. Foreground trigger code 19 calls `arVoid()` only when `lBoolean == false`, then sets `lBoolean = true`, clears `alBoolean`, allocates `eIntArrArr` if null, and creates the first record (12114–12130).
3. `arVoid()` itself allocates/reset the four 15-entry long tables only when `lBoolean == true` (15840–15860).

Therefore the trigger-code-19 call occurs while inactive and its call to `arVoid()` is a no-op. Allocation/reset through `arVoid()` is evidenced on the stage-load case-3 path while the optional mode is already active. The previous statement that the trigger-code-19 path itself initializes all optional tables was incorrect and is withdrawn.

This indicates two distinct entry paths: an existing active-mode stage-load reinitialization path, and an inactive-to-active trigger path that sets up the first record without calling the allocating branch of `arVoid()`. Native implementation must preserve the observed distinction until runtime/binary evidence resolves whether it is intentional or a quirk/defect in this source revision.

### 12.2 Complete scalar writer inventory (observed assignments)

| Field | Writer sites / transitions |
|---|---|
| `xByte` | Constructor 3; teardown 3; restore from `yByte`; `arVoid` 3; record completion 4; stage-record transition 2; setup completion 1; `lVoid` starts 5; `mVoid` starts 1 |
| `alBoolean` | Constructor true; teardown false; trigger code 19 false; `arVoid` true; record completion true; per-frame update consumes it by setting false |
| `dqInt` | Constructor/teardown/restore/activation reset -1; increment while active record timer; reset to 0 on interval; reset -1 on completion; set 0 at setup |
| `drInt` | Teardown/activation 0; increments during record traversal; set 1 on one completion path; reset 0 on alternate setup path |
| `dsInt` | Teardown/activation 0; otherwise restored from snapshot; no other direct assignment found in scanned method |
| `vByte` | Teardown/activation 0; reset and computed from displacement ratio during record movement |
| `dvInt`, `dwInt` | Teardown/activation 0; selected cell coordinates set by `lVoid`; cleared to -1 for one branch; restored on a different movement branch |
| `dxInt` | Teardown/activation 0; set by `lVoid` from neighboring stage-grid occupancy; cleared on invalid setup branch |
| `wByte` | Teardown/activation 0; selected active slot set from packed record lookup in `xByte=5` setup |
| `dyInt` | Teardown/activation 0; derived movement/target coordinate set during setup |
| `dzInt` | Constructor/teardown/activation -1; increments during record processing; reset to -1 or 0 on completion/re-entry paths |
| `dtInt` | Teardown/activation 0; per-frame cursor incremented by one-shot `alBoolean` update; reset to 0 when exhausted |
| `duInt` | Teardown/activation 0; incremented by `afVoid()` once per stage player-layer cell code 38; consumed as upper bound by `dtInt` cursor and reset with it |

### 12.3 Additional lifecycle findings

- `afVoid()` scans the complete stage grid and increments `duInt` for each player-layer cell equal to 38 (12611–12622). This establishes `duInt` as a count/bound for queued cell work, not an arbitrary opaque integer.
- Per-frame update at 9521–9529 consumes one queued coordinate per `alBoolean` pulse: it reads record coordinates from the `dtInt`th packed record, calls `mVoid`, increments `dtInt`, and resets both cursor/count after exhaustion.
- `lVoid(x,y)` sets `dxInt` from adjacent `eIntArrArr` occupancy and records selected coordinates; it enters setup state 5 only when a direction is found (16783–16795).
- `mVoid(x,y)` finds a matching packed record, changes its type/state, clears the corresponding player-layer cell, and enters state 1 (16798–16811).
- `xByte=1` advances to 2 on the next update; `xByte=4` rebuilds type-3 records and returns to 2; `xByte=5` creates/links records and enters 1; `xByte=3` is the interaction-ready state used by input/interaction guards.
- The record table uses a 1-based accessor family for `aLongArr` and a 0-based accessor family for `bLongArr`. Do not normalize these APIs without tests.

### 12.4 Bitfield caution

The previous partial offset table is not yet a canonical packed-record schema. In particular, record creation writes a 7-bit field at offset 27 (15874–15880), while other paths read/write a 2-bit field at offset 26 (e.g. 15961, 15994, 16155, 16158). These accesses overlap and are likely context-dependent views/packed subfields; they must be modeled as observed accessors, not independent non-overlapping properties. Keep exact offset/width pairs and do not infer a flat struct until the complete write/read graph is reconciled.


## 13. Packed-record slot graph, allocation, and link semantics

Direct evidence: pinned source revision 215c71d04057d66a1b08419b1a4b1043ae3e0881.

### 13.1 Slot allocation and identity

- The 15-slot active table is scanned for the first slot whose low 4-bit kind field at offset 0 is zero (15863–15883; 16719–16730).
- The long-table accessors for `aLongArr` are one-based: slot 1 maps to array index 0. The `bLongArr` accessors are zero-based.
- The primary allocator clears the chosen slot before populating it. It initializes record kind/type and writes lifecycle state, coordinates, cell selector, direction, and optional link.
- A slot is cleared by setting the entire long value to zero (`fVoid(byte)`, 16658–16660). Thus a zero kind field is the free-slot marker.
- The allocation scan can exhaust all 15 slots and returns -1 in the four-argument allocator. The six-argument allocator has no explicit full-table guard after its scan; it can reach slot 16 if no free slot exists. Native must add a safe capacity failure instead of reproducing an out-of-bounds write.

### 13.2 Linked-record reference

- Record field offset 54, width 3, stores a link/parent slot reference (16735–16738).
- The reference is one-based, consistent with the `aLongArr` accessor family. Zero is the no-link sentinel; nonzero references are dereferenced to fetch the linked record's coordinate fields (16090).
- When a record completes a handoff, the target record receives the current record's link reference, changes to lifecycle state 2, and the source record is cleared (16251–16260).
- Another path creates a new child/continuation record using the current record's link field as the new record's link (16280–16290).
- Therefore link validation must check: zero or 1..15, referenced slot is occupied, no self-reference unless a proven path permits it, and the referenced record has a compatible lifecycle/type for the operation. The exact allowed graph topology is still being mapped; do not assume a tree or chain yet.

### 13.3 Record lifecycle field

The 3-bit field at offset 28 is the record lifecycle discriminator used by the main update loop (16064–16077):
- 0: free/unallocated (the allocator checks this in addition to kind field in different contexts).
- 1: newly created/initializing record.
- 2: active processing record.
- 3: completed/cleanup path; the update loop clears it.
- 4–5: intermediate/transition values written in completion and handoff paths.
- 6: transitional state that may be reset/aborted by operation setup.
- 7: terminal/alternate completion path.

These labels describe observed control-flow categories, not stable public enum names. In particular, the allocator's free test uses offset 28 == 0, while the first allocator also tests offset 0 low nibble == 0; both conditions must be understood before treating either as the sole free predicate.

### 13.4 Packed record coordinate and cell references

- Record fields at offsets 31 and 37 (6 bits each) are used as stage-grid coordinates.
- Offset 43 (2 bits) selects one of three cell sublayers/columns in the packed 9-bit cell representation.
- The cell record embeds the 1-based slot index in a 3-bit field at offset 0 of the selected 9-bit cell lane (see `aByte` record allocation and `bByte(x,y)` lookup, 15917–15929, 16731–16742).
- `bByte(x,y)` scans all 15 slots and returns the matching zero-based slot index for a coordinate; this differs from the one-based link reference stored at offset 54. Keep coordinate lookup result and link-reference representation distinct.
- The `mVoid(x,y)` path uses coordinate lookup, marks the corresponding player-layer cell, and enters processing state 1 (16798–16811).

### 13.5 Native validation requirements

1. Decode and encode using exact bit-offset accessors; do not flatten overlapping accessors into assumed independent fields.
2. Distinguish slot-index domains explicitly: `aLongArr` accessor/link reference is 1-based; `bByte(x,y)` result is 0-based.
3. Validate link references before dereference; reject dangling, out-of-range, or incompatible references without mutating live state.
4. Validate each occupied slot's lifecycle/type fields and stage coordinates against actual stage dimensions before any array access.
5. Validate reverse cell-to-slot references: a packed stage cell pointing to a slot must resolve to an occupied record whose coordinates and sublayer agree.
6. Reject a full-table allocation safely; never index slot 16.
7. Restore the stage grids and record table as one candidate state, validate all forward/reverse references, then commit atomically.

### 13.6 Still unresolved

- Exact graph topology and whether a link is always parent/continuation or can be bidirectional.
- Full semantic map for type nibble, lifecycle values 4–7, and overlapping bit ranges.
- Whether all 15 slots are reachable/needed in every mode and the exact stage-bound constraints for 6-bit coordinates.
- Runtime confirmation of source-level edge cases, especially full-table allocation and malformed link handling.


## 14. Lifecycle transition graph refinement (record field offset 28)

This section refines §13.3. The field at offset 28 width 3 is a record lifecycle/control field, distinct from the xByte global mode phase and distinct from the low-nibble record kind at offset 0.

### 14.1 Observed state consumers

The xByte=2 update loop scans slots 1..15 and dispatches on offset28:
- State 0: no update branch; allocator/free checks use zero.
- State 1: movement/transition processing branch.
- State 2: active timed record-processing branch.
- State 3: record is cleared (fVoid(slot) sets the whole long to zero).
- State 5: observed as a newly created auxiliary record in xByte=5 setup; no dedicated state-5 branch exists in the main xByte=2 switch, so it is not yet safe to assign a standalone behavior label.
- State 6 and state 7: movement/transition processing branch, with state 7 distinguished by a boolean flag used by movement/cleanup logic.
- State 4: no writer or dedicated consumer was found in the scanned source revision. Do not label it as an established lifecycle state; retain as an unclassified 3-bit value.

### 14.2 Observed transitions

| From | To | Trigger/evidence |
|---|---|---|
| Free (0) | Initializing (1) | Allocator creates a normal record; writes link, cell back-reference, type, coordinates and selector (16734–16746) |
| Free (0) | Special/terminal (7) | Allocator special case when record type argument is 7; clears cell lane and marks state 7 (16731–16733) |
| 1 | 2 | Global xByte=1 phase advances to xByte=2; record is then processed in the main loop (16451–16453) |
| 2 | 3 | Timed record completes; main loop marks state 3 (16142) |
| 3 | Free (0) | Main loop clears entire slot on next scan (16075–16078) |
| 2 | 5 | Completion/handoff path marks source record state 5 when a linked record exists and current operation reaches terminal branch (16143–16149) |
| 2 | 2 | Handoff initializes linked record as state 2, copies link and coordinate/cell context, clears source slot (16251–16260) |
| Free (0) | 5 | xByte=5 setup creates an auxiliary movement record and changes it to state 5 (16573–16575) |
| 5 | 2 | No direct state-5 case is present in the main switch; transition path is indirect/contextual and remains unresolved |
| 6 | 2 | Obstacle/continuation path resets movement counters and marks current record state 2 (16353–16359) |
| Free (0) | 6 | xByte=5 setup marks a selected record state 6 before abort/reset path (16643–16645) |
| 6/7 | Free (0) | State 7 may clear itself on terminal linked-cell condition; state 6 shares the movement branch but exact terminal cases remain context-dependent (16218–16245) |

The table records only transitions directly visible in the pinned source; it is not a complete formal state machine. Some transitions are controlled by xByte and stage-grid conditions, so they cannot be modeled as unconditional state-to-state edges.

### 14.3 Link handoff semantics

A confirmed handoff path in the state-1/6/7 movement branch:
1. Detects a neighboring stage cell whose packed lane contains another record slot.
2. Reads that record's one-based slot reference.
3. Copies the current record's link reference into the target record's link field.
4. Sets target lifecycle to 2 and updates its mode/direction/coordinate fields.
5. Clears the source record.

This is consistent with a continuation/handoff graph, but does not prove that every link graph is a simple singly linked list. Multiple creation paths and shared cell lanes still require reconciliation.

### 14.4 Important separation of control fields

- xByte: global optional-mode phase (1–5).
- Record offset 28 (3 bits): per-record lifecycle/control.
- Record offset 0 low nibble / 4-bit kind: record kind/type, with zero used by a separate allocator free test and other values used in rebuild/record classification.
- Record offset 47 (2 bits): per-record operation mode (switch cases 0 and 1 observed).

Do not merge these fields into one enum or assume they share numeric semantics.

### 14.5 Native schema constraints

- Preserve raw 64-bit records losslessly until the overlapping accessor graph is fully decoded.
- Validate known state values by context; do not reject state 4 solely because no writer was found, but mark it unclassified and do not execute it as a known transition.
- Keep xByte, offset28, offset0 kind, and offset47 operation mode as separate fields in the analysis model.
- Model transitions with guards and stage-cell conditions, not as a simple enum progression.
- Candidate restore must validate link target occupancy and cell backreferences for every occupied slot before commit.


## 15. Accessor-family separation and packed-bit overlap catalog

Pinned evidence source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java`, revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 15.1 Three distinct accessor families

The decompiled source overloads methods named `aInt` and `aVoid`. They do not all access the same packed object:

1. **64-bit active record table**
   - Read: `aInt(int slot1Based, byte offset, byte width)` reads `aLongArr[slot - 1]`.
   - Write: `aVoid(int slot1Based, int value, byte offset, byte width)` updates `aLongArr[slot - 1]`.
   - Slot numbering is 1..15.

2. **64-bit secondary table**
   - Read/write overloads taking a byte slot access `bLongArr[slot0Based]`.
   - Slot numbering is 0..14.
   - This table is not interchangeable with `aLongArr`; its use and snapshot ownership must remain separate.

3. **Packed 32-bit stage-cell lanes**
   - Read: `aInt(int packedCell, byte lane, byte offsetWithinLane, byte width)`.
   - Write: `aVoid(int x, int y, byte lane, int value, byte offsetWithinLane, byte width)`.
   - Each cell is treated as three 9-bit lanes (27 used bits).
   - The lane index is 0..2; offsets such as 0, 3, and 7 are relative to the selected 9-bit lane, not absolute offsets in a 64-bit record.
   - Cell lane offset 0 width 3 stores a slot reference; offset 3 width 4 stores a cell type/state; offset 7 width 2 stores a lane-local selector/flag.

This overload distinction is critical: e.g. `aInt(..., 3, 4)` can mean a record-field read only if it uses the 64-bit-record overload; in the four-argument cell overload it is a cell-lane field.

### 15.2 Observed 64-bit accessors, separated by backing table

These are distinct 64-bit arrays and their offset namespaces must not be combined.

**Primary table `aLongArr` (one-based slot API)**

| Offset | Width | Evidence / observed use |
|---:|---:|---|
| 0 | 7 | Read as a record classification/value during active processing |
| 6 | 7 | Written during primary allocation; no complete semantic reader map yet |
| 7 | 7 | Read as auxiliary value/coordinate in active processing |
| 14 | 6 | Active record coordinate |
| 20 | 6 | Active record coordinate |
| 26 | 2 | Cell selector/subfield |
| 27 | 7 | Written during allocation; overlaps 26:2 and 28:3 |
| 28 | 3 | Per-record lifecycle/control |
| 31 | 6 | Record coordinate |
| 37 | 6 | Record coordinate |
| 43 | 2 | Cell lane selector |
| 45 | 2 | Direction/operation |
| 47 | 2 | Per-record operation mode |
| 49 | 5 | Interval/length |
| 54 | 3 | Secondary-table slot reference used by primary movement records; observed width limits decoded index to 0–7 |
| 57 | 1 | Boolean flag |

**Secondary table `bLongArr` (zero-based slot API)**

| Offset | Width | Evidence / observed use |
|---:|---:|---|
| 0 | 4 | Free/kind scan and rebuild classification |
| 4 | 2 | Operation/type argument used during rebuild |
| 13 | 7 | Coordinate used by lookup/rebuild |
| 20 | 7 | Coordinate used by lookup/rebuild |

The two tables may encode related objects, but this audit does not establish that they share a single schema. Preserve them as separate raw-word arrays until ownership and cross-table relationships are proven.

**Packed stage-cell lanes (not 64-bit record offsets)**

| Lane-relative offset | Width | Observed use |
|---:|---:|---|
| 0 | 3 | Slot reference stored in a cell lane |
| 3 | 4 | Cell type/state |
| 7 | 2 | Lane-local selector/flag |

These offsets are relative to one selected 9-bit lane inside a packed 32-bit stage cell.

### 15.3 Overlap groups that block a flat struct

Within `aLongArr`, the following ranges intersect:
- `6:7` and `7:7`.
- `26:2`, `27:7`, and `28:3`.

Within `bLongArr`, the observed ranges `0:4`, `4:2`, `13:7`, and `20:7` are adjacent or separate; do not combine them with primary-table fields simply because their numeric offsets match.

The stage-cell lane fields `0:3`, `3:4`, and `7:2` occupy a different coordinate system. They are adjacent within a 9-bit lane and must never be added to the record overlap graph.

A correct implementation should first preserve the raw 64-bit word and expose named, context-specific accessor functions. It should not serialize a guessed Java/Kotlin struct and reconstruct the original bits from assumed independent fields.

### 15.4 Snapshot/decoder consequences

- Preserve the complete raw `aLongArr` and `bLongArr` words without normalization.
- Decode a field only through a named accessor that records its source table, slot index base, bit offset, width, and call context.
- Keep stage-cell 9-bit lane accessors in a separate helper/API from record accessors.
- Add golden test vectors for each observed overload and verify write-then-read masks only the intended bits for that overload.
- Before any restore commit, validate slot references and coordinates using the matching accessor family.
- Do not use the current provisional field labels as production domain-model property names.

### 15.5 Remaining source questions

- Complete use/ownership map and cross-table relationship for `bLongArr` versus `aLongArr` in all paths.
- Whether any writes rely on sign extension or negative values before masking.
- Exact semantics of record offsets 0:7, 3:4, 6:7, 7:7, 13:7, 14:6, 20:7, 20:6, 26:2, and 27:7.
- Golden raw-word fixtures from original runtime captures are not yet available.


## 16. Dual-table checkpoint ownership and xByte=4 rebuild workspace

Pinned source revision: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 16.1 Allocation and ownership evidence

- `aLongArr` and `bLongArr` are separately allocated as independent `long[15]` arrays during global initialization (17752–17753).
- Both arrays are re-created independently when optional mode initializes (15840–15845).
- The one-based primary accessor reads/writes `aLongArr[slot - 1]` (15899–15906).
- The zero-based secondary accessor reads/writes `bLongArr[slot]` (15909–15914).
- The source does not show a direct whole-array conversion or copy from one table into the other during normal allocation. They must be treated as distinct persisted state, not aliases.

### 16.2 Secondary-table use during rebuild

The xByte=4 branch scans primary-table entries whose low 4-bit field at offset 0 equals 3. For each such primary slot:
1. It reads the primary record's coordinates and 2-bit operation/type field.
2. It clears the corresponding zero-based `bLongArr[slot0Based]` word.
3. It derives a directional path/continuation from the operation field and neighboring stage coordinates.
4. The helper chain may create or update records and cell references as the rebuild proceeds.
5. After scanning, the phase advances to xByte=2.

This establishes `bLongArr` as a distinct per-slot working/rebuild table associated by slot index with the primary table during this phase. It is not safe to call it merely a temporary cache: it is part of the game state and is explicitly included in checkpoint capture/restore.

### 16.3 Explicit checkpoint capture and restore

When optional mode is active (`lBoolean`):
- Capture allocates backup arrays `cLongArr[15]` and `dLongArr[15]` if absent.
- It copies all 15 primary words: `aLongArr → cLongArr`.
- It copies all 15 secondary words: `bLongArr → dLongArr`.
- Restore copies `cLongArr → aLongArr` and `dLongArr → bLongArr`.
- The same capture/restore boundary also copies the packed stage-cell grid `eIntArrArr ↔ fIntArrArr` and the associated optional-mode scalar fields.

Therefore a checkpoint that stores only `aLongArr` is incomplete. The paired `bLongArr` contents, packed stage-cell grid, phase byte, and associated scalar fields form one coupled optional-mode snapshot.

### 16.4 Native persistence implications

- Persist primary and secondary raw arrays independently, each with exactly 15 64-bit words for this observed source revision.
- Preserve the 1-based versus 0-based accessor conventions at API boundaries; normalize only in a dedicated, tested adapter if necessary.
- Capture/restore the arrays atomically with stage-cell lanes and associated scalar phase state.
- Do not reconstruct `bLongArr` from `aLongArr`; source evidence shows independent allocation and explicit independent checkpoint copies.
- Add tests proving mutation of one table does not alias or mutate the other, and round-trip tests preserving all 30 raw words.
- The source uses a 15-entry backup copy sized from the source array length; native schema should record the revision-specific capacity and reject incompatible dimensions instead of silently truncating.

### 16.5 Remaining questions

- Complete semantic decode of each secondary-table bit range and the exact helper writes during xByte=4.
- Whether all 15 secondary slots are meaningful for every optional-mode phase or some are dormant.
- Cross-validation rules between primary records, secondary workspace entries, and stage-cell references.
- Runtime captures/golden vectors for both tables are still unavailable.


## 17. Secondary-table record schema and traversal evidence

Pinned source revision: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 17.1 Confirmed secondary-table allocator

The four-byte-argument `aByte(byte, byte, byte, byte)` helper (15863–15884) allocates the first free `bLongArr` slot by scanning offset 0 width 4 for zero. It initializes these fields:

| Offset | Width | Stored value | Evidence |
|---:|---:|---|---|
| 0 | 4 | 1 | allocator initialization |
| 4 | 2 | fourth argument | allocator initialization |
| 6 | 7 | first argument | allocator initialization |
| 13 | 7 | second argument | allocator initialization |
| 20 | 7 | third argument | allocator initialization |
| 27 | 7 | first argument again | allocator initialization |

These are six bitfield writes in the secondary table's 64-bit word. The 7-bit ranges 6:7, 13:7, 20:7, and 27:7 are adjacent, not overlapping. Offset 0:4 and 4:2 are also adjacent. Do not reuse the primary-table offset-28 lifecycle schema for this table.

### 17.2 Coordinate lookup and use

- `bByte(x,y)` scans all 15 secondary slots and compares coordinates read from offsets 13:7 and 20:7; returns the zero-based slot or -1 (15917–15930).
- In the optional-mode path setup (around 16603–16606), the four-argument allocator is called, then `bByte` locates the matching coordinate and sets that secondary slot's offset 0:4 to 2.
- When `alBoolean` is set, the update path reads successive secondary entries using `dtInt`, fetches coordinates at 13:7 and 20:7, and passes them to `mVoid`; then increments `dtInt` until `duInt` (9521–9528).
- This is consistent with an ordered coordinate/path traversal used by optional-mode presentation/update. Exact meaning of the 2-bit field at offset 4 and the offset-6/27 values remains contextual and must not be assigned guessed names.

### 17.3 Relation to xByte=4 rebuild

The xByte=4 branch (16455–16510) scans primary records, clears the same-index secondary word for qualifying primary records, and rebuilds movement/continuation behavior through primary-record and stage-cell helpers. Separately, the four-argument secondary allocator is invoked in the xByte=5 setup path. These are distinct mechanisms; do not state that xByte=4 directly populates the secondary table with the primary path nodes.

### 17.4 Corrected interpretation

The secondary table is not merely a passive scratch buffer. It stores independently allocated 64-bit coordinate/path entries, supports zero-based coordinate lookup, is traversed sequentially by an update/presentation path, and is explicitly checkpointed/restored. Its exact lifecycle and flag semantics still need complete call-site analysis.

### 17.5 Native schema/test implications

- Define a separate `SecondaryPathRecord` raw-word codec with capacity 15 and zero-based slot API.
- Preserve all 64 bits, including presently unexplained offset-6 and offset-27 values.
- Add test vectors for allocator field writes, free-slot scan, coordinate lookup, offset-0 state transition 1→2, and ordered traversal.
- Validate coordinate bounds against actual stage dimensions before using coordinates; do not silently clamp.
- Keep secondary table state atomic with primary records, stage-cell grid, and optional-mode scalars.


## 18. Primary/secondary path handoff and capacity preconditions

Pinned source revision: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 18.1 Secondary allocator and marker behavior

The four-argument `aByte(byte x, byte y, byte z, byte mode)` allocator (15863–15884) scans `bLongArr[0..14]` for the first slot whose offset 0 width 4 is zero. On success it initializes the secondary fields documented in §17 and returns the zero-based slot. If no free slot exists, it returns -1.

In the xByte=5 movement setup path (16512–16606), after stage traversal it calls this allocator with the computed path parameters, then independently looks up the coordinate with `bByte(x,y)` and sets the located secondary marker to 2. The allocator return value is not consumed at that call site. The code therefore relies on the subsequent coordinate lookup to find a valid slot; this precondition must be tested rather than assumed safe under full-table conditions.

### 18.2 Primary movement record creation

The six-argument `aByte(x,y,cellType, direction, lane, secondarySlot)` allocator (16719–16748) creates a record in `aLongArr`. It scans primary slots 1..15 for lifecycle offset 28 width 3 equal to zero, initializes a 64-bit word, then writes:
- lifecycle offset 28 to 1 (or 7 for its special cellType=7 path),
- offset 54 width 3 from the supplied secondary-slot argument,
- stage-cell lane reference at the supplied coordinate/lane,
- cell type at lane-relative offset 3 width 4,
- primary coordinates at offsets 31:6 and 37:6,
- lane at 43:2 and direction at 45:2.

In xByte=5 traversal, this allocator is called with cellType 8, direction -2 (normalized to 2), lane 2, and secondary-slot argument 0; its returned primary slot is then moved to lifecycle state 5 and receives further coordinate/length data. This is a primary movement record associated with the secondary path.

### 18.3 Secondary-slot pointer width correction

Primary offset 54 is 3 bits, not a full 4-bit slot index. During active primary-record processing, source reads:
- `b21 = primary.offset54` (0..7),
- `b20 = bLongArr[b21].offset6:7`.

It then updates the secondary record's offset 6:7 as path progress changes. The allocator initially writes the same first argument to secondary offsets 6:7 and 27:7; the observed update path mutates offset 6:7 while offset 27:7 is not shown being updated in this path.

Therefore:
- Offset 54 is an observed zero-based pointer into the first eight addressable secondary slots, not a one-based pointer to all 15 slots.
- Do not widen or reinterpret it in the native format.
- Secondary capacity 15 and primary pointer width 3 are not equivalent. The reason slots 8..14 are not reachable through this field remains unresolved.
- Offset 6:7 behaves as a mutable progress/extent value in this path; offset 27:7 preserves the initially written value in the observed flow. These are provisional behavioral labels, not final domain names.

### 18.4 Cross-table handoff during secondary path update

The per-frame `alBoolean` path calls `mVoid(x,y)` using coordinates from secondary slots in order (9521–9528). `mVoid`:
1. Looks up the secondary slot for the coordinate.
2. If found, changes its offset 0:4 marker to 2.
3. Clears the corresponding `crtStagePlayerLayer` cell.
4. Switches to xByte=1.
5. Creates a primary record at the next coordinate through the six-argument allocator, passing the located secondary slot as offset-54 input.

During active primary processing, the primary record's offset-54 value is used to access and update the linked secondary record's offset-6 progress value. This is the concrete cross-table relationship; it is not a whole-record alias or copy.

### 18.5 Capacity hazards observed in source

- Secondary allocator has an explicit full-table return of -1.
- At the xByte=5 call site, its return is ignored; the following coordinate lookup must succeed or the subsequent marker write can index an invalid slot. This is a source-level precondition/hazard, not proof that normal gameplay reaches the failure.
- Primary allocator's scan exits when the 1-based candidate exceeds 15, but the following write indexes `aLongArr[candidate - 1]`. If invoked with all 15 primary slots occupied, candidate becomes 16 and the write is out of range. The inspected helper has no explicit full-table failure return.
- Native implementation must never reproduce these unsafe failure paths. It should return typed allocation failure and leave both tables and the stage grid unchanged.

### 18.6 Required tests

- Secondary allocator first-free selection and full-table result -1.
- xByte=5 path setup when the secondary table is full or coordinate lookup misses.
- Primary allocator first-free selection and full-table safe failure.
- Offset-54 round-trip for 0..7; reject or explicitly preserve unsupported pointer values without remapping.
- Cross-table progress update: primary offset54 selects secondary slot, and only secondary offset6:7 changes.
- Preserve secondary offset27:7 independently through checkpoint capture/restore.
- Atomic rollback when either table allocation or stage-cell update fails.


## 19. Stage allocation, dimension, and restore precondition audit — 2026-11-25

Pinned candidate source: `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`. This is static-source evidence only, not runtime or shipping-binary validation.

### 19.1 Stage dimensions and live layer allocation

The stage loader reads a four-byte dimension header as two unsigned 16-bit values: `width = unpackUint16(header, 0)` and `height = unpackUint16(header, 2)` (source lines 3418–3424). For the selected stage it assigns these to `crtStageWidth` and `crtStageHeight`.

The following live grids are allocated as `[crtStageWidth][crtStageHeight]`:
- `crtStagePlayerLayer`: byte, line 3433.
- `cByteArrArr`: byte, line 3434.
- `crtStageForegrondLayer`: int, line 3435.
- `bByteArrArr`: byte, line 3447.
- `bIntArrArr`: int, line 3448.

The loader reads each layer into a flat byte buffer of `width * height` bytes (line 3438), then assigns cells using `x < width`, `y < height` and flat index `x + y * width` (lines 3440–3443, 3450–3453, 3457–3461). Preserve this mapping; do not transpose based on assumptions.

### 19.2 Checkpoint-buffer allocation and copy extents

The checkpoint-buffer initialization path allocates:
- `cIntArrArr`: int [width][height] (4584).
- `dByteArrArr`: byte [width][height] (4585).
- `eByteArrArr`: byte [width][height] (4586).
- `dIntArrArr`: int [width][height] (4587).
- `oByteArr`: byte [mByteArr.length] when `mByteArr != null` (4588–4589).

Capture copies exactly `crtStageWidth` rows and `crtStageHeight` elements per row for the four stage grids (15210–15214). Restore uses the same extents (15325–15329). The optional `mByteArr` copy uses `mByteArr.length` (15207–15208, 15322–15323).

The conditional metadata grid `eIntArrArr` is allocated as [width][height] on the active-mode stage-load case-3 path (4594–4599), and its checkpoint buffer `fIntArrArr` is lazily allocated with the same dimensions before copying (15216–15222). The separate trigger-code-19 path can allocate `eIntArrArr` if null (source lines 12114–12130); keep that path distinct from stage-load reinitialization.

### 19.3 Restore-only fixed-coordinate preconditions

The inspected restore method contains fixed-coordinate writes/reads not guarded by dimension checks at the access sites:
- `dIntArrArr[37][7]` and `dIntArrArr[39][5]` on the `qBoolean` branch (15264–15280).
- `dIntArrArr[46][7]` and `dIntArrArr[50][7]` on the `rBoolean` branch (15271–15280).
- `crtStagePlayerLayer[18][63]` when animator slot 2 exists (15340–15343).

The branch-specific minimum extents are therefore:
- q-branch: width >= 40 and height >= 8.
- r-branch: width >= 51 and height >= 8.
- animator-slot-2 branch: width >= 19 and height >= 64.

These are conditional memory-safety preconditions, not universal minimum dimensions for every stage. Native restore must validate the active branch's required coordinates before touching candidate arrays.

### 19.4 Snapshot classification and invariants

- `cByteArrArr` is allocated at stage load but is not copied by the inspected `saveCheckpoint()` or `restoreCheckpoint()`; keep it excluded unless new direct evidence changes this.
- The four unconditional stage-grid pairs remain `bIntArrArr ↔ cIntArrArr`, `bByteArrArr ↔ dByteArrArr`, `crtStagePlayerLayer ↔ eByteArrArr`, and `crtStageForegrondLayer ↔ dIntArrArr`.
- All four pairs require exact dimension equality before capture or restore. A mismatch must fail before any live array is mutated.
- `mByteArr ↔ oByteArr` requires equal lengths and a coherent `cmInt` index into the associated door/lock data.
- `eIntArrArr ↔ fIntArrArr` exists only under the active optional-mode discriminator and must be validated with both packed-record tables and their scalar controller state.
- The source reads dimensions from a 16-bit header, but this audit has not established accepted min/max values or a canonical compatibility key. Native code must impose explicit safe bounds only when supported by verified format constraints.

### 19.5 Remaining work

1. Trace every stage setup branch and verify allocation/nullability guarantees before checkpoint capture/restore.
2. Inventory actual stage-resource dimensions and reconcile them with fixed-coordinate restore branches.
3. Trace remaining opaque scalar writers/reset points and classify stage-local versus durable-profile ownership.
4. Define compatibility-key fields only after stage identity, world identity, mode discriminator, schema revision, and dimension rules have direct evidence.
5. Add deterministic Native tests for dimension mismatch, missing buffers, fixed-coordinate preconditions, and mode-conditional failures when a real gameplay state owner exists.


## 20. Complete world-resource stage dimension inventory — 2026-11-25

Pinned candidate source: `kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881`. Binary resources were fetched as Base64 and decoded:
- `w0.bin`: Git blob `03b9c41145e60cffba3d04b60f40e443b0390064`, 47,686 bytes.
- `w1.bin`: Git blob `55a39efea6b3b70002b64fcbe5855e250b2b6165`, 46,956 bytes.
- `w2.bin`: Git blob `0a73caac7325af181433e8b2cf78de720f14c8c3`, 56,887 bytes.

The loader (source lines 3407–3444) reads and ignores the first byte, then reads a one-byte stage count. Each stage record is a four-byte header containing little-endian unsigned 16-bit width and height, followed by three raw layers of exactly `width * height` bytes each. The skip path uses `width * height * 3`. Parsing all records consumed each resource exactly, with no truncated payload and no trailing bytes.

World index is the array index in `worldFiles` (line 17759): 0 = Angkor/`w0.bin`, 1 = Scotland/`w1.bin`, 2 = Tibet/`w2.bin`; map names are mapped at source lines 1941–1952.

| World | Resource | Count | Dimensions by zero-based stage index |
|---|---|---:|---|
| Angkor (0) | `w0.bin` | 14 | 0: 26×21; 1: 27×24; 2: 27×26; 3: 40×23; 4: 51×24; 5: 30×75; 6: 26×45; 7: 44×29; 8: 35×14; 9: 50×30; 10: 50×31; 11: 46×31; 12: 46×31; 13: 68×11 |
| Scotland (1) | `w1.bin` | 13 | 0: 45×24; 1: 40×24; 2: 33×30; 3: 43×30; 4: 45×33; 5: 43×33; 6: 36×43; 7: 53×27; 8: 34×32; 9: 35×25; 10: 37×24; 11: 60×20; 12: 23×60 |
| Tibet (2) | `w2.bin` | 14 | 0: 60×26; 1: 39×35; 2: 35×42; 3: 38×38; 4: 46×27; 5: 49×28; 6: 55×30; 7: 51×20; 8: 51×37; 9: 104×17; 10: 35×25; 11: 45×28; 12: 35×32; 13: 35×26 |

Total: 41 stages. Candidate-set width range is 23–104; height range is 11–75. This inventory covers these three pinned candidate resources, not every retail/localization variant.

Applying §19 fixed-coordinate predicates to the dimensions only:
- q-branch (width >= 40, height >= 8): 23 dimension-eligible stages.
- r-branch (width >= 51, height >= 8): 9 dimension-eligible stages.
- animator-slot-2 (width >= 19, height >= 64): exactly Angkor world 0 stage 5 (30×75).

Eligibility is only a memory-bounds fact; it does not prove the corresponding q/r flags or animator slot are active on those stages.

Native implications: preserve per-stage dimensions, source layer order and `x + y * width` mapping; use checked multiplication for allocation sizes; validate positive dimensions, resource bounds and explicit safe memory limits before allocation; validate active fixed-coordinate branches before any state mutation. The observed 16-bit header range is not itself a safe allocation policy. World identity and stage index are distinct compatibility dimensions. This inventory does not settle profile ownership or snapshot compatibility semantics.

Remaining: trace all stage-load paths and malformed-stream behavior; identify q/r and animator activation sites; continue opaque scalar ownership and world/stage/profile separation; define compatibility identity from direct evidence; defer Native provider/tests until the real gameplay state owner exists.


## 21. Fixed-branch activation and stage-loader failure-path audit — 2026-11-25

### 21.1 q/r restore branch activation is gameplay-state driven

The q/r flags are initialized false in the game-state constructor (source lines 631–632) and reset false at stage setup (3381–3382). In the gameplay update path, only while `kByte == 2`, the code checks the player's current foreground cell (source lines 9561–9572):
- If the low byte of `crtStageForegrondLayer[playerX][playerY]` is zero and the high-byte code is 13, set `qBoolean = true`.
- Under the same low-byte condition, high-byte code 16 sets `rBoolean = true`.

The restore path consumes these flags (15264–15280), resets the corresponding flag, and writes fixed coordinates:
- q: sets `bmInt = 15`, clears `dIntArrArr[37][7]` and `dIntArrArr[39][5]`.
- r: sets `bmInt = 17`, clears `dIntArrArr[46][7]` and `dIntArrArr[50][7]`.

Therefore dimension eligibility in §20 cannot be used to infer activation. The flags are triggered by a runtime foreground-cell encoding under a particular gameplay mode. Native restore must validate both the exact active flag and its coordinate bounds before mutation. The mapping from foreground high-byte values 13/16 to human-facing stage mechanics remains unproven.

### 21.2 Animator slot 2

The restore path checks `i.aClassbArr[2] != null`, resets that animation to frame 0, and writes `crtStagePlayerLayer[18][63] = -1` (source lines 15340–15343). The animator slot is populated at source line 4992. The condition is object-presence driven, not shown to be a universal stage property. The 30×75 Angkor stage is the only resource dimensionally capable of satisfying this access among the three pinned files, but actual slot-2 creation/restore activation must be established by tracing the exact assignment context and runtime behavior.

### 21.3 Stage-loader malformed-input and missing-stage behavior

The stage loader (3408–3444) uses `InputStream.read()`, `read(header)`, and `skip(width * height * 3)` without checking the returned byte counts. The outer `while (shouldExit == 0)` has no explicit EOF/failure exit. The source itself comments that the loop freezes when a non-existent stage is requested (line 3410). When the requested stage index is beyond the available stage count, the loader can continue its outer loop after EOF rather than returning a typed missing-stage error.

Consequences for Native:
- Never port the source's unbounded retry loop.
- Require exact reads for the one-byte header, four-byte stage header, and each layer payload; a short read is a parse failure.
- Validate stage index against the parsed stage count before loading.
- Validate positive width/height, checked multiplication, and remaining resource bytes before allocating or advancing.
- Treat missing world resource, EOF, truncated header, truncated layer, invalid dimensions, and out-of-range stage index as distinct typed failures.
- Stage parsing must be candidate-first: failure must not replace the currently active stage or partially mutate gameplay state.
- Do not trust `skip()` to consume the requested count; use a checked cursor or loop until the exact payload length is consumed.

This is a Native safety requirement derived from observed source weaknesses, not a claim that the original game intentionally supports malformed resources.

### 21.4 Remaining

1. Inspect full context around animator slot-2 assignment at source line 4992 and all creation/destruction paths.
2. Trace which stage foreground records can produce high-byte 13 and 16 and whether they occur in pinned stage resources.
3. Keep q/r activation independent of dimension eligibility.
4. Define Native typed stage-load failures and transactional commit semantics after the real stage/game owner exists.


## 22. Animator slot-2 asset lifecycle and restore activation — 2026-11-25

Pinned candidate source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 22.1 Creation path

The sole direct assignment found for `i.aClassbArr[2]` is in `dVoid(int chunkI)` at source lines 4973–5000:
- Chunk 0 opens `/mm0.f`, reads a chunk count, and loads its size table into `tmpBytes`.
- Chunk 1 parses sprite chunk 2 (comment identifies “chunk 2, palette 0, all animations”), constructs `b_SpriteAnimator`, assigns slot 2, then selects animation 0.
- Chunk 2 first assigns texture 27 from sprite chunk 3, then falls through into chunk 1 and also constructs/assigns slot 2 from sprite chunk 2; it then falls through into chunk 0 and constructs slot 1 with animation 2.
- In the `kByte == 1` loader dispatch (source lines 4630–4637), `dVoid(n57)` is called for chunk indices; after chunk index 2 the shared input stream is closed.

This establishes slot 2 as an asset-loader product, not a stage-specific animator constructed by the restore method. The chunk-2 fallthrough reassigns slot 2 after chunk 1; source does not explicitly dispose the prior slot-2 object before replacement. Treat that as observed source behavior, not a lifecycle contract to copy blindly.

### 22.2 Destruction / availability

The global cleanup path iterates `aClassbArr[0..5]`, frees each non-null animator sprite, and sets each slot to null (source lines 4876–4884). This includes slot 2. The inspected cleanup block also releases stage arrays and optional-mode buffers; it is a broad resource teardown, not evidence of an ordinary per-stage slot-2 destroy event.

The restore branch only checks whether slot 2 is non-null, calls `setAnimation(0)`, and writes `crtStagePlayerLayer[18][63] = -1` (15340–15343). Thus the fixed coordinate is coupled to asset availability at restore time. The source does not establish a one-to-one relationship between slot-2 presence and a particular stage ID. Dimension inventory says only Angkor 0:5 is tall enough in the pinned candidate resources, but no runtime activation is inferred.

### 22.3 Native boundary

- Model the animator asset as an optional resource with explicit load state and ownership; do not encode its presence as a stage ID.
- Candidate restore must know whether the corresponding animator resource will be present before commit. If the restore branch needs the [18][63] write, validate the player-layer dimensions first.
- Avoid reproducing chunk-index fallthrough as implicit behavior; represent asset chunks and their dependencies explicitly and test replacement/disposal.
- Keep asset lifecycle separate from persisted gameplay payload unless direct evidence shows the animator's internal frame/state is serialized. The checkpoint restore explicitly resets slot 2 to animation 0 rather than restoring an animator snapshot.
- No direct slot-2 assignment to null was found outside the general six-slot cleanup loop in the inspected source revision.

### 22.4 q/r foreground metadata: unresolved boundary

The q/r trigger reads the signed int foreground cell as a composite value: low byte must be zero and the high-byte code is 13 or 16. The stage loader initially sign-extends one raw foreground byte into an int, so these composite values are not established by the raw stage foreground byte alone. Other gameplay paths mutate foreground cells and encode additional values, but this pass has not established the complete writer chain that produces high-byte 13/16. Do not claim these values are literal stage-file tile IDs or label their mechanics yet.

Next trace must enumerate every writer to `crtStageForegrondLayer[x][y]`, classify raw-load versus runtime mutation, and identify the exact producer chain for the composite q/r values. Retain codes 13/16 as opaque trigger metadata pending that proof.


## 23. q/r composite foreground trigger producer — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 23.1 Exact producer chain

The stage loader order is player layer, background layer, then foreground layer (source lines 3437–3462). The world resource stores each as one byte per cell. During stage initialization, switch case 0 of the foreground byte normalizer writes:

```java
crtStageForegrondLayer[x][y] = (background << 8 | (foreground & 0xFF));
```

(source lines 3637–3640). When the raw foreground byte is 0, this produces a composite foreground integer whose low byte is 0 and whose high bits carry the signed background-layer value. Therefore the q/r trigger's low-byte-zero plus high-byte-13/16 condition is produced by foreground tile byte 0 paired with background-layer value 13 or 16, not by a standalone foreground tile ID 13 or 16.

This explains the observed trigger check at lines 9566–9572:
- background value 13 -> q flag.
- background value 16 -> r flag.
Both are conditional on gameplay mode `kByte == 2` and player position.

### 23.2 Candidate resource scan

All three pinned world resources were decoded using the loader's exact layer order and scanned for cells where raw foreground byte = 0 and raw background byte = 13 or 16.

| World | Resource | Candidate cells |
|---|---|---:|
| Angkor (0) | `w0.bin` | 3 |
| Scotland (1) | `w1.bin` | 0 |
| Tibet (2) | `w2.bin` | 0 |

Angkor world 0, stage 13 (68×11) contains:
- (x=0, y=0): background 16, player-layer byte 85.
- (x=37, y=7): background 13, player-layer byte 255.
- (x=46, y=7): background 16, player-layer byte 255.

No matching raw layer pairs were found in the other 40 stages of this candidate resource set.

These are resource-level trigger candidates. The source scan does not prove the player can reach each coordinate, that the game uses the relevant mode on every route, or that the q/r flags are activated in an actual runtime session. Player-layer byte 85 at (0,0) is not interpreted here.

### 23.3 Other foreground writes and separation

The stage normalizer also transforms several raw foreground codes into composite values: examples include codes 31/19, 17, 2, 8/9, 7, 1/26, 0, 4, 3, and 6 (source lines 3510–3670). Runtime handlers later mutate high-byte metadata for doors, moving objects, and animation state. Do not collapse these into one universal tile schema: low-byte foreground identity, high-byte per-cell metadata, and upper flag bits have different consumers.

The full assignment inventory in this pass confirms several mutation families (stage normalization, door/lock handling, object update, interaction cleanup, animation flag updates, and checkpoint restore), but it is not yet a formal exhaustive write-set proof because some aliases write through local row arrays. Future audit should include those alias writes and any helper methods receiving row references.

### 23.4 Native implications

- Decode the three raw layers separately before applying normalization.
- Preserve signed-byte semantics exactly when the original Java byte is widened to int; explicitly test byte values >= 128.
- Represent foreground low-byte identity and high-byte metadata separately in Native typed code, while retaining a lossless raw representation for compatibility.
- q/r trigger detection should consume normalized composite state (or an equivalent explicit pair), not infer from foreground tile ID alone.
- Add golden tests for raw foreground 0 with background 13/16, ordinary background values, signed byte boundaries, and the three Angkor stage-13 candidate cells.
- Keep actual trigger activation as runtime validation, not a static-resource assertion.


## 24. Foreground mutation write-set audit — direct and row-alias writes — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`. This section records direct assignments and confirmed row-alias writes to the global foreground grid. It is a source-level inventory by mutation family; it is not yet a proof that every helper receiving a row reference has been exhaustively enumerated.

### 24.1 Storage lifecycle and raw-to-normalized initialization

- Stage allocation creates `int[width][height]`; raw foreground bytes are initially widened into the grid (3435–3460).
- Stage normalizer transforms raw codes and metadata in several switch paths (3505–3701). Observed writes include:
  - composite `background << 8 | lowByte` variants;
  - special sentinels `-1`, `15`, `27`, `33`;
  - normalized tile values `2`, `3`, `6`, `17`, `19`, `31`.
- Stage teardown nulls the grid (3429; global resource cleanup 4888).
- Checkpoint capture/restore copies the foreground grid row-by-row through `dIntArrArr` (capture 15214; restore 15329).

Do not treat normalizer writes as gameplay mutation. They construct the runtime representation from the raw resource layers.

### 24.2 Confirmed runtime mutation families

| Source area | Mutation | Ownership interpretation |
|---|---|---|
| 1244–1245 | Set current foreground cell to 32 when its low byte is negative | Player/current-cell interaction |
| 1289 onward, 1494 onward | Conditional tile checks and interaction paths | Mostly reads; don't classify reads as writes |
| 3510–3701 | Bulk per-cell normalization | Stage initialization |
| 3812–3834 | Write 15, -1, 27 | Stage setup / special-cell normalization |
| 4024–4054 | Door/foreground operation; clear adjacent cell and rewrite current composite | Door/lock interaction |
| 4156 | Write composite `(-193 << 8) | 7` | Special stage/player interaction; high byte intentionally signed |
| 9484–9499 | Clear selected cells to -1 | Gameplay interaction cleanup |
| 9566–9572 | q/r trigger reads composite value | Read-only trigger check, not a writer |
| 9592 | Clear fixed cell [11][50] | Gameplay event path |
| 10533–10547 | Row alias writes 15 or 16, then clears player-layer cell | Player/object interaction |
| 11049–11067 | Door metadata high-byte changes; row-cell assignment | Door animation/state |
| 11672 | Rewrite composite high-byte metadata | Moving/animated foreground object |
| 12062–12063 | Clear contiguous matching cells to -1 | Bulk interaction cleanup |
| 12137 | Clear cell to -1 | Object/foreground update |
| 12627–12630 | Row aliases mask low byte then OR 14 | `iVoid(x,y)` helper; preserves upper bits |
| 12637–12658 | Recursive row update toggles high-byte bit 0x200 for connected 8/9 cells | Connected mechanism state |
| 12850–12927 | Multiple row-alias assignments for types 34/35/37/36/32 and -1 | Animated object update |
| 12962–12964 | Row alias writes animation frame in high byte | Animation lifecycle |
| 13024 | Clear current cell to -1 | Door interaction completion |
| 13125, 13134, 13141, 13156, 13163 | Rewrite or clear mechanism/door metadata | Door/foreground mechanism state |
| 13227–13235 | Row alias updates upper 4-bit animation counter | Animation update |
| 13888 | Write 32 | Gameplay object interaction |
| 14326, 14372, 14395 | Write 35, 34, 32 | Object movement/interaction |
| 14812, 14819 | Set/clear high-byte 0x10 marker or reset high byte | Door/lock mechanism |
| 15120 | Set top-nibble flag bit 0x10000000 while preserving low 28 bits | Foreground animation/event flag |
| 15128, 15133, 15147 | Rewrite low byte and/or increment high-byte animation state | Foreground animation helper |
| 15329 | Restore full row from checkpoint backup | Checkpoint restore, not gameplay update |
| 15456 | Write 32 | Interaction cleanup |
| 15693 | Reads foreground to derive player-layer flags; not a foreground write | Explicitly excluded |

### 24.3 Row-array alias confirmation

Several apparent non-global writes are actual foreground mutations because the local array is a direct row reference:
- `array = crtStageForegrondLayer[n]`; then `array[n2] &= 0xFFFFFF00` and `array2[n2] |= 0xE` in `iVoid` (12627–12630).
- `array2 = crtStageForegrondLayer[n41]`; `array2[n43] = n44` (10533–10547).
- `array2/array3/array4/array7 = crtStageForegrondLayer[row]`; indexed assignments in the animation update (12850–12964).
- `array8 = crtStageForegrondLayer[row]`; indexed assignment for the top-nibble animation counter (13223–13235).

These alias writes must be included in any static write-set analysis. A text search only for assignments containing the global field name is insufficient.

### 24.4 Lossless state model implications

The foreground cell is a packed signed 32-bit runtime value with multiple independently consumed regions:
- low 8 bits: tile/object code;
- bits 8–27: signed/unsigned contextual metadata, including high-byte fields and mechanism flags;
- bits 28–31: animation/event nibble (explicit top-nibble mask);
- some paths use Java signed right shift `>>`, some mask `& 0xFF`, and some cast to byte before switch. These operations are not interchangeable.

Examples:
- `(-193 << 8) | 7` creates a negative composite value while retaining low-byte tile 7.
- `(value & 0xFFFFFFF) | 0x10000000` sets bit 28 while preserving lower 28 bits.
- `(byte)value` dispatches by signed low byte; `value & 0xFF` dispatches unsigned low byte.

Native must preserve each original operation's signedness and mask exactly. Do not prematurely replace the packed int with a narrow enum or separate fields unless a lossless round-trip representation is retained.

### 24.5 Audit limitations and next actions

The writer list above is based on global-field references and inspected row aliases. It is not yet exhaustive for helper methods that receive `int[]` row references, reflection/indirect access, or writes through aliases created outside the inspected contexts. Next:
1. Build a helper-call graph for methods accepting or capturing foreground row arrays.
2. Trace every call to mutation helpers and identify whether it changes foreground, player layer, or metadata grids.
3. Verify the exact semantics of top-nibble flags and high-byte signed metadata.
4. Derive deterministic golden vectors from the raw stage normalizer and runtime mutation helpers.
5. Keep CS-2 open until writer/reader ownership and snapshot compatibility are reconciled.


## 25. Foreground helper call graph and ownership boundaries — 2026-11-26

Pinned candidate source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 25.1 Confirmed helper contracts

**`iVoid(x,y)` (12625–12631)** writes the selected foreground cell through a row alias:
`cell = (cell & 0xFFFFFF00) | 0x0E`, and sets `cLong |= 0x400000`. It preserves bits 8–31 while replacing the low byte with 14. Its call sites include stage/interaction transitions and update paths (2535, 2978, 3770, 3785, 3938, 3954, 3963, 13329, 14206, 14287, 15376). Not every call-site semantic label has been established; the state mutation itself is exact.

**`aVoid(x,y,dx,dy,mode)` (12637–12659)** recursively walks connected neighboring cells whose unsigned low byte is 8 or 9. It toggles bit 0x200 in the signed-shifted upper value according to mode, then continues along the same direction. Door helpers call it in pairs:
- `hVoid(doorI)` opens/decrements a door and invokes the helper left/right with mode 1 (11035–11055).
- `doorHeadClose(x,y)` closes/resets the door and invokes the helper left/right with mode 0 (11059–11069).
This is a connected-mechanism metadata mutation, not a generic tile replacement. The recursion and its boundary condition are part of behavior; Native should use an iterative bounded traversal to avoid stack exhaustion while preserving visitation/order semantics.

**`nVoid(tile)` (15124–15151)** rewrites the current cell's low byte while retaining its shifted upper value. In specific animation/player conditions it sets upper value 0x100 or increments the upper value and marks the cell dirty. This helper combines tile identity and animation progression; keep raw packed value available.

**`jVoid(x,y)` (15119–15122)** sets top-nibble bit 28 via `(cell & 0x0FFFFFFF) | 0x10000000` and then calls `alVoid()`. The corresponding update path later increments/clears the nibble based on animation frame count (13215–13236). This is a flag-driven animation lifecycle.

**`aiVoid()` (14206 onward)** moves/reconstructs player-layer code 48 and updates player/metadata layers; it does not write the foreground grid in the inspected body. It is called from restore when player-layer code 48 has the relevant metadata flag. Keep it in the restore reconstruction set, not foreground mutation inventory.

**`kVoid(x,y)`** is called after many cell updates, but its body (12665 onward) selects/updates a cell renderer/animator object and does not itself mutate foreground values in the inspected portion. Treat it as visual invalidation/update side effect, not a foreground writer.

### 25.2 Alias and layer ownership distinctions

The same update loop mutates multiple distinct grids:
- `crtStageForegrondLayer`: packed signed int cells.
- `crtStagePlayerLayer`: byte-valued player/object occupancy.
- `bIntArrArr`: packed stage-cell metadata and movement flags.
- `bByteArrArr` / `cByteArrArr`: timing/dirty/update counters and auxiliary state.

A helper invocation may update more than one grid; do not infer that all side effects belong in the foreground payload. For example, the object animation path can move player-layer values and update `bIntArrArr` while also replacing foreground codes 35/37/34/-1. These arrays have distinct ownership and must be snapshotted as their own evidenced fields.

### 25.3 Snapshot and Native design consequences

- Preserve the exact 32-bit packed foreground cell through capture/restore.
- Preserve signed Java operations; a logical shift or unsigned byte conversion is not a safe global substitution.
- Treat dirty/render invalidation (`kVoid`) as derived runtime state unless direct evidence proves it is persisted.
- Reconstruct code-48 moving objects after base grid restore, as the original restore does; do not serialize renderer handles.
- Replace recursive connected-cell traversal with bounded iterative traversal, guarding dimensions and cycles while matching source's directional walk.
- Test door open/close connected-cell bit 0x200 behavior, low-byte-preserving upper metadata, top-nibble animation flag lifecycle, and `nVoid` low-byte replacement.
- Static call graph is partial: methods with obfuscated names and overloaded `aVoid` signatures require descriptor-aware mapping. The listed contracts are grounded in the inspected methods and call sites, not a claim that every project helper has been semantically decoded.


## 26. Cross-layer snapshot ownership audit — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 26.1 Allocation and source-layer initialization

At stage load, all four runtime grids use dimensions `[crtStageWidth][crtStageHeight]` (3427–3449):
- `crtStagePlayerLayer`: raw player layer, byte grid.
- `bIntArrArr`: raw background layer widened from byte to int, then normalized and mutated as packed stage metadata.
- `bByteArrArr`: newly allocated byte grid; initialized to zero during stage normalization.
- `cByteArrArr`: newly allocated byte grid; initialized to zero during stage normalization.

The three resource layers are player, background, foreground. `bByteArrArr` and `cByteArrArr` are not raw resource layers; they are runtime auxiliary grids.

### 26.2 `bIntArrArr`: persisted gameplay metadata

The grid is initialized/rewritten during stage normalization according to player/background codes (e.g. player 48, 41, 11, 43, 30, 37, 10, 7, and connected mechanism types; lines 3716–3975). Runtime consumers decode multiple packed fields: low 3 bits, flags 0x8/0x10, 5-bit and 7-bit subfields, offsets 8/10/12/13, 22+, sign bit, and others. Runtime update paths mutate it in concert with player occupancy and timers.

Checkpoint capture copies every row into `cIntArrArr` (15211); restore copies back (15326). Therefore this grid is explicitly part of the in-memory checkpoint payload. Do not interpret it as static background pixels or as a single enum. It is packed gameplay/object state and needs lossless 32-bit preservation until all bitfields are decoded.

### 26.3 `crtStagePlayerLayer`: persisted occupancy/object state

The byte grid is loaded from the player resource layer, then normalized (e.g. spawn/player markers, moving objects, special codes) and heavily mutated during gameplay. It stores occupancy/object codes, including negative empty values and codes such as 48 used for reconstructable moving objects.

Checkpoint capture copies it into `eByteArrArr` (15213); restore copies back (15328). It is explicitly persisted in the temporary checkpoint. Values must preserve signed Java byte semantics; do not convert to unsigned byte without a separate exact representation.

### 26.4 `bByteArrArr`: persisted runtime counters/timing

This grid is initialized to zero (3503) and later stores movement progress, animation/timing counters, and per-object update values. Consumers include movement/render calculations (e.g. 6924–6926, 7121, 7151–7180, 7252–7262, 8878–8893); writers include object movement/animation paths (e.g. 10728–10731, 14753–14786, 14941–15043). Its values are not merely visual dirty markers: they affect movement positions and state transitions.

Checkpoint capture copies it into `dByteArrArr` (15212); restore copies back (15327). Thus it belongs in the temporary gameplay checkpoint even though many uses look timing-related. Preserve raw signed bytes and exact wrap/clamp behavior; semantic subfields remain partially unresolved.

### 26.5 `cByteArrArr`: derived dirty/update scheduling grid

This grid is initialized to zero (3504), then receives values such as 24, 30, 48, and 127 from stage setup and gameplay mutations. The update path uses it to mark cells/regions for refresh (including 24/30/48 assignments in movement, animation, and mechanism paths); renderer/update scheduling consumes these markers. Crucially, it is not copied in checkpoint capture or restore: the four base grids copied are `bIntArrArr`, `bByteArrArr`, `crtStagePlayerLayer`, and foreground. The restore path subsequently calls `kVoid` for relevant player/foreground cells to rebuild visual/update state.

This is strong evidence that `cByteArrArr` is derived invalidation/scheduling state rather than checkpoint payload. Do not serialize it as an authoritative gameplay field unless further evidence contradicts this. On Native restore, rebuild invalidation/dirty state from restored authoritative grids and force appropriate renderer refresh.

### 26.6 Snapshot domain matrix

| Grid | Raw resource layer? | Checkpoint copied? | Current classification |
|---|---|---|---|
| `bIntArrArr` | Background-derived | Yes | Authoritative packed gameplay metadata |
| `crtStagePlayerLayer` | Player-derived | Yes | Authoritative occupancy/object codes |
| `bByteArrArr` | No | Yes | Authoritative runtime counters/progress |
| `cByteArrArr` | No | No | Derived dirty/update scheduling |

Foreground is separately checkpointed as packed int grid. Optional mode adds `eIntArrArr` and the two active 15-word long tables with their backups.

### 26.7 Remaining uncertainty

- Full field-level bitfield semantics of `bIntArrArr` are not yet canonical; accessors overlap.
- `bByteArrArr` contains multiple context-dependent values; do not assign one universal meaning such as “animation timer”.
- `cByteArrArr` exact scheduler cadence and all marker consumers need additional tracing, but its exclusion from snapshot plus post-restore invalidation supports derived-state classification.
- Confirm snapshot copy dimensions and lifecycle invariants for every allocated grid; candidate-first restore must reject mismatched dimensions before any mutation.


## 27. Auxiliary scheduler semantics and checkpoint reconstruction — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 27.1 Correction: `cByteArrArr` is not merely a renderer dirty grid

The prior §26 classification as derived/non-snapshotted state remains correct, but the phrase “dirty/update scheduling” needs precision. `cByteArrArr` is an active transient object-update scheduler, not just a renderer invalidation bitmap.

The bounded update scan in `ahVoid()` (12803–12845) visits a player-centered region (approximately ±8 cells, clipped to the interior stage bounds). A cell is eligible when `cByteArrArr[x][y] > 0` and it is not the currently excluded cell. The scheduler decrements the byte by 6 and dispatches behavior based on the signed low byte of the foreground cell. Depending on the foreground code, the handler may advance animation metadata, move player/object occupancy, update packed background metadata, decrement `bByteArrArr`, clear cells, or schedule another pass.

Observed scheduler values:
- 24: commonly schedules a short update window; the scan decrements by 6 per eligible pass.
- 48: schedules a longer update window and is also written by `kVoid(x,y)` to a 3×3 neighborhood (15174–15187).
- 30: used by selected interactions/updates; exact cadence must be tested because decrement-by-6 produces a different sequence.
- 127: assigned during stage normalization at specific cells; its full lifecycle and signed-byte behavior need separate tracing.

Thus the byte is not simply “dirty yes/no”; its magnitude controls how many eligible update passes remain. Since values are signed Java bytes, 127 remains positive, while values can become negative during decrement and then fail the `> 0` eligibility test.

### 27.2 Restore rebuild evidence

The original restore does not copy `cByteArrArr`. After restoring the four authoritative grids, it scans interior cells and calls `kVoid(x,y)` for cells with player-layer code in 0..79 or foreground low byte in 0..79 (15331–15338). `kVoid` writes 48 to the surrounding 3×3 scheduler region. This is direct evidence that the scheduler is deliberately rebuilt after restore rather than restored byte-for-byte.

This rebuild is not a proof that every transient timing phase is bit-identical to pre-checkpoint execution. It establishes the original game's own restore contract: rebuild eligible update work from restored authoritative stage state. Native should reproduce this contract and separately test whether gameplay-visible outcomes remain deterministic.

### 27.3 `bByteArrArr` refinement: context-dependent progress values

`bByteArrArr` is authoritative checkpoint data, but it has no single global semantic. Examples:
- Value 18 is assigned when movement/object processing is initialized, then some handlers decrement it by 6 until completion (e.g. object update paths around 12880–12940 and `dVoid`, 14253–14300).
- Some animation paths read it as frame/progress input, including multiplication by direction vectors (8874–8879), animation frame selection (7252–7263), and bounded visual displacement (7377–7395).
- Other paths increment it, reset it to zero, or use threshold checks; values such as 10, 12, 18 and 24 occur in distinct object families.
- Movement/object collision and state transitions read it together with `bIntArrArr` and player occupancy.

Therefore preserve its raw signed byte and exact arithmetic. A future Native model may expose typed views for individual object families, but the snapshot codec must retain the original byte values.

### 27.4 `bIntArrArr` accessor overlap: known contextual slices only

Observed readers include:
- low 3 bits (`& 0x7`) for movement/direction/type dispatch;
- bit 3 (`0x8`) as a mode/phase flag in multiple object paths;
- bits 3–5 (`0x38`) for animation/direction selection in specific render/update paths;
- bits 5–12 (`0x1FE0`) for an animation/progress subfield in foreground/player code 46;
- bits 10–12 (`0x1C00`) for a staged interaction counter in one input path;
- bits 13–20 (`0x1FE000`) for a separate progress subfield;
- bits 22–27 (`0xFC00000`) for an object/action discriminator;
- top byte / sign bit for additional metadata and flags.

These masks overlap. They are evidence of context-specific views, not a canonical bitfield struct. Preserve all 32 bits and keep per-consumer decoding descriptors separate.

### 27.5 Cross-grid invariants for snapshot/restore

The observed update logic couples grids:
- player/object occupancy changes are paired with packed `bIntArrArr` movement/type metadata;
- `bByteArrArr` progress values are initialized/advanced alongside those transitions;
- `cByteArrArr` schedules subsequent processing around changed cells;
- foreground packed cells are changed in the same transaction for many object/door animations.

Native mutation APIs should therefore perform multi-grid changes as one game-owner-thread transaction. Snapshot capture must occur at a defined quiescent/tick boundary or under a consistent immutable snapshot mechanism. Restore should validate all dimensions and cross-grid references before committing authoritative grids, then rebuild `cByteArrArr` scheduling and renderer invalidation. Do not claim exact mid-tick replay until a tick-boundary contract is tested.

### 27.6 Remaining work

1. Trace every writer/reader of scheduler values 24/30/48/127 and derive exact update cadence per object family.
2. Audit bounds of the 3×3 `kVoid` neighborhood and update scan for all valid player positions.
3. Decode `bByteArrArr` values by object family with golden vectors.
4. Build descriptor-aware `bIntArrArr` accessor map and identify true overlapping unions.
5. Test restore reconstruction against a captured gameplay state over multiple ticks; keep physical/runtime validation distinct from static source evidence.


## 28. Scheduler writer inventory and cadence evidence — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 28.1 Writer families

A full textual occurrence scan found direct `cByteArrArr` assignments at the following source regions. This is a writer inventory, not yet a complete semantic proof of every caller or stage-specific branch.

| Source region | Value | Context established |
|---|---:|---|
| 3504 | 0 | Runtime grid initialization |
| 3657 | 127 | Background-layer normalizer case 3; special stage cell |
| 3741 | 48 | Player-layer code 47 normalization |
| 3748, 3755, 3760 | 24 | Player-layer codes 46, 45, 44 initialization |
| 3876 | 48 | Player-layer code 11 initialization |
| 3973, 3980 | 48 | Player-layer codes 0/1 initialization |
| 4049 | 24 | Foreground code 7 special neighboring-cell setup |
| 789 | 24 | Player current-cell interaction path |
| 8650, 9895–9913 | 24 | World/stage-specific event and scripted object placement |
| 10470, 10731 | 24/30 | Interaction and directional player movement setup |
| 11051, 11091 | 24 | Door/lock state transitions |
| 12367–12369, 14283–14285, 14755, 15179–15187 | 48 | Multi-cell object/mechanism scheduling; includes 3×3 `kVoid` |
| 12861–15679 and 16716 | 24/30 | Object update, animation, door, mechanism, packed optional-mode record mutations |
| 13128 | 0 | Explicit scheduler cancellation/reset in an object transition |
| 15895–15896 | 24 | Optional packed-cell mutation schedules the edited cell and its adjacent cell |

The source also creates local row aliases for reads; direct assignments are visible above. The inventory does not infer semantics solely from the numeric value.

### 28.2 Value 127 is not proven to be a 127-tick timer

The only directly established initialization site is normalizer case 3 (3657), where the scheduler byte is set to 127. Because `ahVoid()` subtracts 6 on each eligible update, a positive 127 would follow the arithmetic sequence 127, 121, 115, ... until it reaches 1 and then -5, if it remains eligible and no handler overwrites it. However, the cell's foreground low byte and handler may change the value or other grids during that sequence. Do not encode 127 as a fixed timer duration or assume the cell survives all passes.

### 28.3 Value 24/30/48 are scheduling requests, not universal durations

- 24 is written by many interaction and mutation helpers to schedule an affected cell.
- 30 is explicitly written in movement/interaction paths (e.g. 10731, 15035, 15603); it is not interchangeable with 24.
- 48 is written for selected initialized objects and multi-cell neighborhoods. `kVoid` writes 48 to all nine positions around a coordinate without local bounds checks.
- The scheduler scan is player-centered, clipped to x/y interior range [1, width-2] and [1, height-2], with nominal radius 8. It excludes one selected cell when `biInt` matches its coordinate.
- A cell's counter is decremented by 6 before the foreground-code switch dispatch. The handler can then overwrite the counter, clear it to zero, mutate other grids, or move the scheduled work.

Therefore, a simplistic formula “value / 6 = exact number of updates” is not generally valid.

### 28.4 Scheduler and authoritative-grid coupling

Examples from the source:
- Player movement creates occupancy code 32, sets `bByteArrArr=18`, writes packed `bIntArrArr=4|direction`, and schedules `cByteArrArr=30` (10724–10732).
- Optional object movement can set target metadata, scheduler 30, and progress byte 18 together (15023–15037).
- Door/lock interactions schedule 24 while modifying foreground and connected mechanism metadata.
- Optional packed-cell accessors schedule the edited cell and, for one setter, its adjacent cell (15890–15897).
- Scheduler 48 may be written to a 3×3 region, but bounds safety relies on call-site coordinate constraints not yet exhaustively proven.

These examples reinforce that scheduler updates are transient side effects of authoritative multi-grid transitions.

### 28.5 Native implementation requirements and open risks

1. Keep scheduler state out of the durable snapshot schema, matching source capture/restore.
2. Rebuild scheduler from restored authoritative state using the original restore scan and neighborhood semantics.
3. Do not convert values 24/30/48/127 to a single enum or timer duration until per-handler behavior is characterized.
4. Implement bounds-safe neighborhood scheduling, but first prove or reproduce the source's valid call-site coordinate constraints; do not silently clip in a way that changes intended semantics.
5. Golden tests should include: 24 and 30 overwritten before/after decrement; 48 neighborhood scheduling; 127 signed-byte countdown; explicit zero cancellation; skipped current cell; edge coordinates; and handlers that reschedule themselves.
6. Mid-tick snapshot/replay equivalence remains unproven; capture must be on a defined game-owner tick boundary or use an immutable consistent view.


## 29. Scheduler dispatch by foreground code and neighborhood caller audit — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 29.1 Confirmed `ahVoid()` dispatch cases in inspected source range

The scheduler scan is row-major over y descending from clipped `playerY+8` to `playerY-8`, then x ascending from clipped `playerX-8` to `playerX+8`. It skips the selected coordinate when `biInt != 0` and the cell equals `(bgInt,bhInt)`. For each eligible positive scheduler byte, it subtracts 6 before dispatching on the foreground low byte.

The inspected handler cases include:

| Foreground low byte | Observed scheduler/update behavior |
|---:|---|
| 36 | Advances high-byte animation frame; reschedules 24 until frame reaches 16, then clears cell to -1 |
| 35/37 | Reschedules current cell 24; performs horizontal connected-object movement, may initialize/decrement `bByteArrArr`, move player occupancy and packed metadata |
| 32 | Advances animation frame; clears at sprite frame-count boundary; otherwise reschedules 24 |
| 26 | Door/lock interaction; resolves group and target cell, closes/clears the activated cell |
| 6 | Door state chooses open or close path based on occupancy and progress byte |
| 33/14 | Dispatches `nVoid` for tile/animation mutation |
| 2 | Checks adjacent player occupancy code 30 and may invoke `bVoid` transition |
| 3 | Advances high-byte phase conditionally; reschedules 24 until phase reaches 6, then explicitly sets scheduler to 0 |
| 30/0 | Conditional player interaction; may consume/remove the foreground cell |
| 7 | Advances packed substate every third global tick; connected metadata mutation and reschedule 24 |
| 8/9 | Runs paired mechanism/pickup interaction logic when player/counters/animation conditions match |

This is not the complete switch inventory: source range beyond the excerpt contains further dispatch and player-layer handling. Do not treat the table as an exhaustive enum or infer unlisted cases are inert.

### 29.2 Scheduler handler may alter multiple authoritative grids

Examples:
- Foreground 35/37 can move a neighboring player-layer object, set its `bByteArrArr` progress to 18, update `bIntArrArr` low bits, clear the source occupancy and call transition helpers.
- Foreground 32 advances animation in the foreground packed high byte and reschedules itself.
- Foreground 3 reschedules while advancing phase, then cancels its scheduler explicitly at phase >= 6.
- Foreground 7 modifies high-byte substate, connected foreground metadata, and may schedule again.
- Player-layer code 54 increments a packed `bIntArrArr` counter, changes nearby occupancy, schedules affected cells, and invokes `kVoid`.

Consequently, scheduler decrement is only the entry action. The handler can overwrite the decremented value, schedule adjacent cells, or cancel work.

### 29.3 `kVoid(x,y)` call-site audit

The static scan found 41 textual occurrences including the method declaration and the distinct no-argument instance method. Relevant two-coordinate calls span stage normalization, player/input transitions, object updates, restore reconstruction, and optional packed-cell updates.

The helper writes the full 3×3 neighborhood `[x-1..x+1] × [y-1..y+1]` with scheduler value 48 and performs no local bounds checks.

- Restore caller (15336) iterates interior cells x=1..width-2 and y=1..height-2, so the 3×3 write is safe for a valid rectangular grid.
- Scheduler scan itself clips to interior cells, but this alone does not prove every other caller supplies interior coordinates.
- Several callers operate on neighboring or offset coordinates (e.g. 10552, 11791, 12370, 15516); each needs call-path proof before Native can rely on the same precondition.
- Stage normalizer caller (3737) is safe only if the decoded stage coordinate itself is interior; this must be proven from stage format/normalization rules.

Do not claim all call sites are safe yet. Native should centralize a bounds-checked scheduler-neighborhood operation and surface invalid call-site coordinates in tests/logs while the source preconditions are being established. Once proven, valid-game behavior must match the exact nine-cell write.

### 29.4 Native golden tests derived from confirmed handlers

- Foreground 36: frame progression, 16 boundary, clear-to-`-1`.
- Foreground 32: sprite frame-count boundary and reschedule behavior.
- Foreground 3: phase progression gated by global tick parity and scheduler cancellation at phase 6.
- Foreground 7: every-third-tick progression and connected-cell metadata update.
- Foreground 35/37: left/right object transfer with player occupancy, metadata, and progress counter updated atomically.
- Foreground 6: open/close decision using player occupancy and progress threshold 12.
- Foreground 26: door group lookup, invalid group handling, and target-cell resolution.
- All handlers: scheduler byte decremented by 6 before dispatch, then overwritten/rescheduled/cancelled according to handler.
- `kVoid`: interior coordinates, all four edges, corners, and invalid coordinates; verify no partial grid mutation on failure.

Static source analysis only. No Native gameplay code or executable tests added.


## 30. Player-layer dispatch inside scheduler scan — 2026-11-26

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`, `ahVoid()` lines 13238–14196.

### 30.1 Important execution order

For each cell selected by the scheduler scan:
1. Test `cByteArrArr[x][y] > 0` and selected-cell exclusion.
2. Decrement scheduler byte by 6.
3. Dispatch foreground low-byte handler.
4. Continue into a separate player-layer switch for the same coordinate, unless an earlier labelled break skips the remainder.
5. Some player-layer handlers mutate adjacent cells, schedule them, and update other grids.

Thus player-layer dispatch is not an independent later pass. It is coupled to the same eligible scheduler visit and may run after foreground dispatch has already modified the cell. Native must preserve this ordering and the source's labelled early-exit semantics.

### 30.2 Player-layer cases and evidenced roles

| Player-layer code | Observed behavior |
|---:|---|
| 54 | Multi-stage spreading/activation: increments packed counter in `bIntArrArr`, then at midpoint processes a 3×3 neighborhood; can clear or convert codes 16/19/43/49, convert code 8 to 54, and trigger damage. |
| 50 | Damage check when player overlaps, subject to timer threshold and invulnerability flag. |
| 49 / 43 / 19 | Dispatch to `eVoid(code)` shared lifecycle helper. |
| 48 | If metadata flag 0x8 absent, dispatch `dVoid(48)`; otherwise reconstruct moving object through `aiVoid()`. |
| 46 | Complex moving/animated object. Uses low 5 metadata bits as state and bits 5–12 as progress; may fall/move to cell below, decrement progress by 6 for state 8/9, change direction/state, collide/damage, or clear/rebuild. |
| 45 | Animation/hazard family. Uses low 4 metadata bits, bits 10–12 selector, bits 13–20 frame/progress; uses `bByteArrArr` as context-dependent timer and can move/replace occupancy after animation threshold. |
| 44 | Calls `akVoid()`. |
| 40 | Calls `lVoid(40)`. |
| 36 | Checks metadata and cell above; may activate global mechanism or damage player. |
| 28 | Checks a metadata-derived target cell, clears incompatible occupancy and updates it; schedules current cell. |
| 16 | Paired/linked horizontal or vertical object behavior; shares metadata and timer state between neighboring cells, may damage player or clear both cells. |
| 14 | Directional moving object. Uses metadata direction/phase and `bByteArrArr`; moves to adjacent cell, resets source, schedules destination, or updates direction/phase. |
| 10 | Converts to foreground animation 32 when metadata counter is positive, clears player occupancy, invokes `lVoid`, and schedules cell. |
| 21 | Directional moving mechanism with paired-cell/link behavior; updates occupancy, direction metadata, and progress/timer values. |
| 32 / 11 | Calls `anVoid()` / `amVoid()`. |
| 37 / 30 | Counter-driven multi-cell propagation/activation; schedules affected neighbors and eventually clears occupancy. |
| 24 / 27 / 26 | Calls `mVoid(code)`. |
| 42 / 2 / 53 / 51 / 52 / 5 / 4 / 6 / 7 / 41 | Calls `lVoid(code)`. |
| 47 | Calls `aqVoid()` and `ajVoid()`. |
| 1 / 0 / 9 / 8 | Calls `aqVoid()`, with code 9 also updating paired global counters for a specific packed metadata signature. |
| 23 / 22 | Schedules current cell and performs horizontal player collision checks across a span derived from `n5`. |

This is a source-level behavior inventory, not final human-readable object naming. Shared helpers `eVoid`, `dVoid`, `lVoid`, `mVoid`, `aqVoid`, `akVoid`, `anVoid`, and `amVoid` still require independent descriptor-aware tracing.

### 30.3 Important multi-grid examples

- Code 54 writes occupancy, metadata, and scheduler across a 3×3 region; its midpoint branch can clear/convert neighboring cells and apply damage.
- Code 46 can move occupancy one cell downward, copy direction metadata, initialize destination progress to 18, clear source occupancy, and call `kVoid`.
- Code 45 increments a packed progress field and updates `bByteArrArr`; at its threshold it relocates the active state into a target cell and clears the source.
- Code 14 can move occupancy one cell down, copy metadata and progress, or decrement the current timer by 6 while waiting.
- Code 21 maintains a directional pair/chain of code-21 cells, transferring metadata and progress between predecessor/current cells.
- Code 16 shares metadata across adjacent cells and may mirror timer state between them.
- Code 37/30 propagate counters to neighboring code-30 cells and schedule those cells.

These handlers reinforce that the scheduler visit is a multi-grid transaction. Snapshot capture cannot occur in the middle of a handler or between its coupled writes.

### 30.4 Bounds and neighborhood implications

The scheduler scan itself uses interior coordinates. Several player-layer handlers access neighbors at x±1/y±1 and some derive a second target from direction arrays. This gives a strong reason to preserve stage border/wall invariants, but does not by itself prove every computed target is valid. The stage format/normalization and helper preconditions must be traced before translating direct array indexing into Native.

### 30.5 Native test requirements

- Preserve foreground dispatch before player-layer dispatch for the same cell.
- Reproduce early exits/label breaks so player-layer processing is skipped in the same cases.
- Add per-code golden vectors for 54, 46, 45, 14, 21, 16, 37, 30, and shared-helper dispatch codes.
- Test coupled updates atomically across foreground, player occupancy, metadata, progress, and scheduler.
- Test neighbor boundaries and directional target calculations for all four edges and corners.
- Define capture only at end-of-tick/handler boundary; a mid-handler snapshot is invalid.


## 31. Shared gameplay helper signatures and observed contracts — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`. The method names are obfuscated and overloaded; this section identifies exact signatures and records only behavior visible in the inspected body.

### 31.1 `eVoid(byte)` — moving player-layer object transition

This is distinct from `eVoid()` (no args; lifecycle/input helper) and `eVoid(int)` (RMS/save-record and map loading logic). The byte overload at 14632–14787 reads current occupancy, packed metadata, and signed progress. It chooses/updates direction and movement phase, may consult stored target coordinates in packed metadata, and calls `kVoid` when initializing movement. On a movement transition it clears source occupancy and writes target occupancy, scheduler=48, progress byte, and packed metadata together. It can also invoke hero damage when movement/collision conditions are met. Preserve raw packed bits and the helper's direction-selection branch order; no single semantic label for all caller codes is established.

### 31.2 `dVoid(byte)` — directional transfer/continuation at current cell

The inspected overload at 14253–14301 operates on current `bEInt/bFInt`. It handles a special foreground-6 case via `hVoid`, then either:
- when progress is exhausted, updates a low-three-bit direction/phase flag, transfers occupancy to the cell below/rightward in the inspected coordinate convention (target `y+1`), initializes target progress to 18 and metadata, clears an earlier occupancy cell, and schedules a 3-cell vertical neighborhood plus `kVoid`; or
- if transfer is blocked, optionally damages the player and clears low direction bits; or
- while progress remains positive, subtracts 6.
It finally schedules current and previous-y cells at 24. The byte parameter is not read in this inspected method body; do not infer it changes the transition.

### 31.3 `lVoid(int)` — player-contact action dispatcher

The overload at 14462–14564 requires the player to occupy the current cell and rejects while `jInt>0). It also rejects selected foreground codes 14/33 with high byte 255. Its integer argument selects an action branch (codes 40, 42, 41, 4, 5, 2, 53, 51, 52, 6, 7), which sets action/animation selectors, updates player-layer state and sometimes recurses into `lVoid(7)` or `lVoid(41)`. This is not the same as `lVoid(int,int)), which selects a neighboring optional-mode cell for packed-record processing.

### 31.4 `mVoid(int)` — player-contact progression/exit action

The overload at 14830–14873 requires the player at the current cell and one of two player animation IDs (40 or 48). It clears the player-layer cell, then maps argument 24/27/26 to distinct durable byte-9 values and action selector fields. The default branch calls world/stage load `aVoid(world,stage,x,y)`. It is distinct from `mVoid(int,int)), which looks up a secondary packed record by coordinates and may allocate/transition a new record.

### 31.5 `akVoid()` — phase-driven foreground mechanism

At 14341–14460 it first schedules current cell at 24, then dispatches from packed metadata bits 3–5. Branches include:
- mode 0: checks player/scroll/world timing, scans upward/downward player-layer occupancy, and may switch metadata phase and set progress to 10;
- mode 1: decrements progress by 1 and, at exhaustion, changes foreground to 34, updates packed metadata, and clears progress;
- mode 3: decrements progress by 5 while positive; when exhausted, examines the cell below, may damage player or transform/clear certain player-layer codes, and may reschedule.
This helper is not interchangeable with the no-argument `amVoid()`.

### 31.6 `amVoid()` — direction/phase-driven player-layer movement

At 14875 onward it reads metadata bits 8–11 as a substate, handles terminal/clearing states, and otherwise derives movement direction from low-three-bit direction and bit-4 polarity. It tests three candidate cells using `fBoolean`; if a target is available and progress permits, it transfers occupancy and metadata, sets target progress=18, clears source, and may call scheduling helpers. The method continues beyond the inspected excerpt; treat this as a partial behavior description pending full body audit.

### 31.7 `anVoid()` — adjacent-cell propagation/transfer controller

At 15016 onward it schedules the current cell at 24 and, when progress is zero, examines a neighboring cell selected by the low metadata bit. A positive packed counter can be decremented while target progress=18 and scheduler=30 are written; several occupancy codes trigger sound/action-state changes and update packed direction/phase. This method continues beyond the inspected excerpt; exact completion and all caller effects remain open.

### 31.8 `aqVoid()` — coupled interaction/movement/objective handler

At 15406 onward it reads progress, packed metadata, player-layer code, optional packed-stage-cell state, player position, and collision state. It may increment objective/pickup counters, decrement the stage remaining-object count, clear occupancy, invoke `bVoid`, move an object into an adjacent cell, update packed direction/progress fields, and schedule source/target cells. It is a multi-grid transaction and its remaining body must be audited before assigning a final semantic label.

### 31.9 Signature collision guard

Keep these distinct:
- `eVoid()`, `eVoid(int)`, `eVoid(byte)`
- `dVoid(int)`, `dVoid(int,int)`, `dVoid(byte)`, `dVoid(boolean)`
- `lVoid()`, `lVoid(int)`, `lVoid(int,int)`
- `mVoid()`, `mVoid(int)`, `mVoid(int,int)`
- `amVoid()` and `akVoid()` are separate methods with different data/dispatch paths.

Native ports should use descriptive names based on proven role and typed parameters, never copy obfuscated overload names as if they were a single function.

### 31.10 Remaining helper audit

Complete full method bodies and caller inventories for `amVoid()`, `anVoid()`, and `aqVoid()`; inspect all callers of each signature; trace remaining `dVoid(int,int)`, `lVoid(int,int)`, and `mVoid(int,int)` separately; build transaction-level golden vectors. No runtime implementation is justified by this source audit alone.


## 32. Completion of selected helper bodies: `amVoid()`, `anVoid()`, `aqVoid()` — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 32.1 `amVoid()` — direction/substate transfer controller (full body inspected)

- Reads substate from metadata bits 8–11 (`0xF00 >> 8`). If nonzero, states >=4 clear player occupancy; otherwise a global phase condition can advance substate by 1.
- If substate is zero but optional packed stage cell is present, initializes substate to 1.
- Otherwise, while progress byte <=4, reads direction low bits 0–2 and polarity bit 4, derives three candidate neighbor offsets, and checks them with `fBoolean`.
- On a valid candidate and zero current progress, copies metadata/occupancy to target, sets target progress 18, clears source occupancy. Alternate branch can rotate direction bits and transfer to a different candidate.
- If no candidate is available, updates direction bits in place.
- Calls `kVoid(current)`; damages player on overlap; finally subtracts 5 from positive progress.
- Direction mapping is encoded in the switch for direction values 1–4 and polarity; preserve exact mapping rather than normalizing to compass labels before golden vectors exist.

### 32.2 `anVoid()` — directional neighbor controller (full body inspected)

- Always schedules current cell 24.
- If current progress is zero, selects neighbor at x±1 based on metadata bit 0 and reads neighbor player-layer/foreground state.
- If packed counter (metadata >> 1) is positive and target is empty/eligible, decrements counter, schedules target 30, initializes target progress 18, and temporarily treats target as code 32 for subsequent branch logic.
- A family of eligible occupancy codes selects movement/interaction state, updates player-relative exclusion coordinates and direction bits; invalid/blocked targets can trigger a scan clearing up to three code-32 cells.
- In all cases it writes selected occupancy or progress back and calls `kVoid(current)`.
- When current progress is nonzero, subtracts 6 from it and still schedules neighborhood.
- Preserve the distinction between current scheduler=24 and target scheduler=30.

### 32.3 `aqVoid()` — packed object interaction/movement controller (full body inspected through method end)

- Derives side/target column from low metadata direction and optional-mode packed-cell presence/type.
- A specific code-1 contact path with valid collision increments `aZInt`, clears current occupancy, decrements `aaInt`, updates terminal/objective occupancy when count reaches zero, and invokes adjacent/current cell updates.
- Other branches can convert target foreground 33 to 32, move player position and apply damage; clear incompatible occupancy; move current object to target and transfer metadata/progress; or mutate direction, packed phase, progress and scheduler.
- Progress handling includes decrement by 6, a special decrement by global phase parity for a particular state/foreground-6 combination, and threshold transitions at 0/12.
- Late in the method, metadata high flags 0x20000000 and 0x40000000 are reconciled against current progress and optional-mode presence, invoking `bVoid`/`cVoid` on edge transitions. It then rewrites the high status flags and a derived top-bit state based on local occupancy, neighboring availability, foreground, and excluded coordinates. A separate 3-bit field at bits 6–8 is advanced cyclically over values 0–4.
- Do not reduce this to a simple pickup handler: it is a composite object interaction/transfer controller with objective-count effects and derived occupancy flags.

### 32.4 Snapshot consequences

These three helpers write occupancy, metadata, progress, foreground, scheduler, objective counters, and global player/action state. They are multi-grid transactions. A checkpoint must be captured only after the full handler and its nested helper calls return at a stable game-owner tick boundary. Restore must rebuild transient scheduler/derived flags using the original reconstruction order and validate objective counter consistency; do not serialize derived transient flags as independent truth without evidence.


## 33. Exact caller inventory for selected helper families — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 33.1 Scheduler-dispatched helpers

Within the player-layer switch in `ahVoid()`:
- `anVoid()` is dispatched for player-layer code 32.
- `amVoid()` is dispatched for player-layer code 11.
- `aqVoid()` is dispatched for codes 47, 1, 0, 9, and 8. Code 47 additionally calls `ajVoid()` after `aqVoid()`; code 9 conditionally updates `auInt/avInt` before calling it.
- `lVoid(int)` is dispatched for codes 42, 2, 53, 51, 52, 5, 4, 6, 7, and 41; recursive calls for 7 and 41 also occur inside that overload.
- `mVoid(int)` is dispatched for codes 24, 27, and 26.
- `eVoid(byte)` is dispatched for codes 43 and 19.
These are dispatch entry points, not the complete transitive call graph.

### 33.2 `dVoid(int,int)` callers

Observed direct call sites:
- Stage/global transition loop when `ceInt == 5`: foreground code 15/16 invokes `dVoid(x,y)`, then converts player-layer occupancy to 34/35.
- `bBoolean(int,int)`: scans two fixed rows and clears eligible player-layer code 0 cells; after helper call it schedules the cell, clears occupancy, and calls `bVoid`.
- `eBoolean()`: similar cleanup over a dynamic row interval.
The helper itself has a switch on player-layer code; cases 0,19,43,45,46 clear foreground and invoke `jVoid`; code 48 also clears a paired cell and a related `eByteArr` slot. Fall-through behavior is intentional in the source and must be represented exactly.

### 33.3 `lVoid(int,int)` callers

Observed direct call sites:
- Foreground/player-layer transition code 10: after converting foreground to 32 and clearing player occupancy, invokes `lVoid(x,y)`.
- Player-layer code 37 after its counter reaches at least 8: invokes `lVoid(x,y)`, clears occupancy, schedules the cell, increments counter, and reschedules.
- Stage/global transition loop when `ceInt == 5`: for player-layer code 34/35, writes foreground 15/16, clears occupancy, then invokes `lVoid(x,y)`.
The overload only acts when optional mode `lBoolean` is true. It examines adjacent `eIntArrArr` cells in priority order (x-1, x+1, y-1), sets `xByte=5`, sound 13, and stores `dvInt/dwInt` when a neighbor is selected.

### 33.4 `mVoid(int,int)` callers

Observed direct call sites:
- Foreground code 19 activation: after optional mode initialization, first packed record creation, and `eIntArrArr` allocation, invokes `mVoid(x,y)`.
- Optional-mode queue in `UVoid()`: when `alBoolean` pulses and `dtInt < duInt`, decodes x/y from secondary table record offsets 13 and 20 and calls `mVoid(x,y)`, then increments `dtInt`.
The helper performs coordinate lookup through `bByte(x,y)`; if found, allocates/updates a secondary packed record and may clear occupancy, set `xByte=1`, and call `aByte` for the next linked/neighbor record. The caller context distinguishes initial activation from queued continuation.

### 33.5 Caller completeness and snapshot implications

For `amVoid()`, `anVoid()`, and `aqVoid()`, the inspected source has only the scheduler dispatch call sites listed above plus method declarations. Their invocation therefore occurs within the per-cell scheduler visit. The two-coordinate helpers have separate stage-transition, cleanup, and optional-mode callers and must not be merged into the scheduler helper contracts.

All caller contexts show that helper side effects can be followed by additional writes in the caller (e.g. occupancy clear, scheduler set, counter increment, or a second helper). A transaction boundary must include the entire caller branch, not merely the callee return. Snapshot safety is therefore at the outer game-owner tick/dispatch boundary after caller continuation has completed.


## 34. Transitive helper effects and side-effect classification — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 34.1 `ajVoid()`
The scheduler code-47 branch invokes `aqVoid()` then `ajVoid()`. The inspected helper at 14321 onward performs a follow-up foreground/occupancy transition and scheduling update. It is caller continuation, not part of `aqVoid` itself. Snapshot boundary must include both calls and the remaining scheduler branch.

### 34.2 `jVoid(int,int)` — neighborhood scheduler invalidation
The two-coordinate overload at 15119 is a neighborhood scheduling helper. It writes scheduler/dirty timing values for a bounded neighborhood around a cell; callers include movement, transfer, interaction, and object-state helpers. It is derived transient scheduling state, not proof of authoritative world progression. Capture/restore policy must be reconciled with the original checkpoint's explicit grid copies and restore-time rebuild behavior.

### 34.3 `bVoid(int,int,int)` — neighborhood state mutation
The three-argument overload at 12061 recursively visits left/right/up neighbors around the supplied coordinate, using the byte argument as the operation selector. It is not equivalent to `bVoid(int,int)` nor the other overloads. It mutates neighboring stage/object state and is used during grid reconstruction and interaction paths. Keep the operation selector and recursive order in any Native port; validate bounds before each neighbor access.

### 34.4 `cVoid(int,int,int)` — coordinate/operation dispatcher
The three-integer overload at 11009 is a separate coordinate operation dispatcher. It is called with operation values including 1, 3, 5, and -1 from gameplay and restore/reconstruction paths. Do not conflate it with `cVoid(int,int)` or the static `cVoid(int,int,byte)` packed-bit helper. Its semantic labels remain provisional until each operation branch and transitive callee is traced.

### 34.5 Overload collision warning
`bVoid` has distinct int, boolean, byte, two-int, and three-argument forms plus unrelated graphics/static forms. `cVoid` has int, boolean, byte, two-int, three-int, and static packed-bit forms. `jVoid()` and `jVoid(int,int)` are distinct. Any Native contract must identify descriptor/argument types and caller context, not name only.

### 34.6 Snapshot classification
- Authoritative candidates: stage grids, player occupancy, foreground/object metadata, objective counters, packed optional-mode records, progression values whose capture/restore is directly evidenced.
- Derived/transient candidates: scheduler timing grid, dirty/rebuild markers, input/interaction flags, animation phases explicitly reset or reconstructed, camera values recomputed from player coordinates.
- Conditional state: optional-mode packed tables/grid/scalars guarded by `lBoolean`.
- Unresolved: semantics and ownership of several opaque scalar fields and operation selectors passed to `bVoid(...,...,byte)` and `cVoid(...,...,...)`.
Do not finalize schema or persist transient grids until every direct capture/restore and restore-rebuild path has been reconciled.

### 34.7 Remaining audit
Trace every branch of `ajVoid()`, `bVoid(int,int,int)`, and `cVoid(int,int,int)`; map their nested calls to authoritative vs derived writes; prove neighbor bounds and recursion order; then build caller-complete transaction vectors. Static inspection does not establish runtime equivalence.


## 35. Correction: exact behavior of `cVoid(int,int,int)`, `bVoid(int,int,byte)`, and `jVoid(int,int)) — 2026-11-27

This section supersedes the provisional descriptions of these overloads in §34. Their exact bodies were inspected at the pinned source revision.

### 35.1 `cVoid(int,int,int)) is an 8-slot render-animation queue, not a coordinate operation dispatcher

At 11009–11016 it:
- Writes x/y as bytes into `aByteArr[aeInt*2]` and `aByteArr[aeInt*2+1]`.
- Writes animation/type selector into `cByteArr[aeInt]`.
- Resets per-slot frame counter `bByteArr[aeInt]` to zero.
- Advances write index `aeInt = (aeInt + 1) & 7`.

Render/update paths consume entries from `adInt` toward `aeInt`, increment each slot's frame counter, and advance the read index modulo 8 when the selected animation reaches its frame count. Stage-load case 4 resets both indices. This is transient presentation state, not authoritative gameplay coordinate state. Queue capacity is eight; producer/consumer behavior and possible overwrite when full require exact ring-buffer semantics if Native rendering needs parity. Do not serialize this queue as gameplay snapshot payload unless a separate presentation-resume requirement is explicitly defined.

### 35.2 `bVoid(int,int,byte)) is a recursive vertical/side flood-clear helper

At 12061–12071 it clears foreground cells whose low byte equals the supplied selector. For each match it:
1. Writes foreground = -1.
2. Recursively checks left and right at the same y.
3. Recursively checks the cell above (y-1).
4. Advances y downward in a loop and repeats at the same x.

It does not include explicit bounds checks in this body. Callers and stage border invariants must guarantee safe coordinates; Native must validate before recursion and preserve the exact traversal order. This overload mutates the foreground stage grid and is authoritative, unlike the render queue above. It is not equivalent to other `bVoid` overloads.

### 35.3 `jVoid(int,int)) sets a foreground metadata high bit

At 15119–15122 it preserves the foreground cell's other bits while OR-ing `0x10000000`, then invokes `alVoid()`. It is not a scheduler invalidation helper. The high bit is part of the foreground packed state and therefore may be authoritative if the cell is included in checkpoint copy. `alVoid()` performs door/lock-group progression: it decrements `mByteArr[cmInt]`; when it reaches zero it scans interior stage cells for matching foreground code 17 and group metadata, updates door-top/neighbor cells and schedules the changed door cell. It may play sound 8. The helper's full ownership and the relation of the high bit to door/lock state need separate caller analysis.

### 35.4 Correction to §34 and snapshot classification

The earlier provisional labels “jVoid neighborhood scheduler invalidation” and “cVoid coordinate operation dispatcher” were incorrect. Replace them with the contracts above. `cVoid(int,int,int)` is derived transient animation queue; `jVoid(int,int)` changes packed foreground metadata and invokes lock/door progression; `bVoid(int,int,byte)` clears matching foreground cells recursively. These distinctions materially change snapshot inclusion decisions.


## 36. Door-group counter ownership and checkpoint evidence — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 36.1 Stage-load initialization

At stage initialization, the loader computes the required `mByteArr`/`lByteArr` length from the highest set bit in a stage-derived mask (`n10`), then allocates arrays of that length when nonzero. During the foreground-grid scan, each foreground code 17 cell with group ID `n43 != -1` initializes:
- `mByteArr[n43]` from `hashtable2[groupId]`, default 0.
- `lByteArr[n43]` from `hashtable3[groupId]`, default 57.

The source establishes that `mByteArr` is per-stage group counter state associated with foreground code 17 and its high-byte group ID. Do not rename it to “door count” without qualification: it is a group counter consumed by the door/lock progression paths.

### 36.2 Selection and decrement

Foreground code 26 interaction selects `cmInt` from the foreground high byte. It validates `0 <= cmInt < mByteArr.length`; otherwise it resets `cmInt=-1`. On valid selection it scans interior foreground code 17 cells with matching high-byte group ID and records an eligible door/head coordinate. This provides direct evidence that code-26 interaction selects a group ID shared with code-17 cells.

`alVoid()` returns early for selected global modes when `aqInt>0). Otherwise, if `cmInt>=0`, it decrements `mByteArr[cmInt]`. When the counter becomes zero, it scans interior foreground cells for code 17 with matching group ID and applies the observed door-top/neighbor transformations; some neighboring foreground codes 14/33 are cleared, and a code-7 cell can be marked with metadata and scheduled at 24. Sound 8 is played on the zero transition.

Other `alVoid()` callers are:
- Player-layer code 7 animation/state branch after `apInt>80`, setting `aoInt=8`.
- Player-layer code 36 when a neighboring player-layer code 11 is present and the corresponding `bIntArrArr` cell changes from 0 to 1.
- `jVoid(x,y)` after setting foreground metadata bit `0x10000000`.

These callers show the group decrement is a shared progression effect; its relationship to each caller's visible mechanism must remain caller-contextual.

### 36.3 Snapshot copy/restore is explicit

The temporary checkpoint capture stores `cnInt=cmInt` and copies `mByteArr` into `oByteArr` when the array exists. Restore sets `cmInt=cnInt` and copies `oByteArr` back into `mByteArr`. Thus both the selected group index and group-counter array are direct checkpoint payload, not inferred additions.

The foreground grid is also copied directly, preserving code-17 group IDs and packed metadata including bit `0x10000000`. The capture path allocates `oByteArr` when the live array exists. Stage initialization resets `cmInt=-1` and nulls `mByteArr/lByteArr` before loading new stage data.

### 36.4 Safe Native invariants

- If `mByteArr` is absent, `cmInt` must not be dereferenced.
- If `cmInt >= 0`, require `cmInt < mByteArr.length`.
- Every code-17 foreground group ID used as an array index must be validated against the allocated group-array length.
- Counter decrement must not silently underflow; exact source behavior for malformed zero/negative counter inputs needs a golden vector.
- When a group counter reaches zero, all matching code-17 cells and their dependent neighboring cells must be updated as one outer game-owner transaction.
- Capture/restore must keep `cmInt`, `mByteArr`, foreground grid, and any dependent progression state coherent.
- `lByteArr` is stage-loaded from hashtable3 and used for text lookup; it is not copied by the temporary checkpoint in the inspected capture method. Its omission and reconstruction/lifetime must be reconciled before claiming complete stage-state persistence.

### 36.5 Remaining

Trace all writers of hashtable2/3 and the stage binary fields feeding them; inspect complete code-17/code-26/code-36/code-7 caller contexts; determine whether `lByteArr` is immutable stage metadata or mutable state; and build golden vectors for multiple groups, zero transition, selected-group restore, and malformed IDs.


## 37. Door-group initialization correction: counters are derived from stage occupancy — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`. This section refines §36: the two Hashtables are constructed locally during stage initialization; they are not external persistent tables.

### 37.1 `hashtable2` count derivation

During the initial stage-grid scan, for each foreground code 17 cell:
- Read its background-layer value as `background`.
- Inspect player-layer cell immediately above it (`y-1`).
- If that player cell is one of 19, 36, 43, 45, 46, or 49, increment `hashtable2[background]` and clear the foreground code-17 cell to -1.
- Otherwise retain/rewrite the foreground cell as `(background << 8) | 17`.
- For nonnegative background values, set the corresponding bit in a 32-bit group mask.

Later, array length is derived from the highest set bit of that mask plus one. `mByteArr[group]` is initialized from `hashtable2[group]`, defaulting to zero. Thus the initial group counter is computed from a stage-layout condition, not read as a precomputed count from a standalone binary counter field.

### 37.2 `hashtable3` text selector derivation

For the same qualifying foreground code-17 cells, selector is assigned based on the player-layer code above and game mode:
- Player code 36: set selector 58 only if no selector has yet been recorded for that group; later code-36 matches do not overwrite it.
- Other qualifying player codes: set selector 56, then override with 51 for `kByte==4`, 52 for `kByte==5`, or 53 for `kByte==3`.
- During array population, `lByteArr[group]` is copied from `hashtable3[group]`, default 57.

The selector is read by the door-dialog path as `texts[lByteArr[cmInt]]`. Search of the pinned class finds no post-initialization writes to `lByteArr`; its only assignment is stage-load population and it is nulled at stage teardown. It is therefore stage-derived lookup metadata, not a mutable counter. Since it is rebuilt from the same stage and mode, exclusion from the temporary gameplay checkpoint is supported, provided restore never crosses stage/mode identity.

### 37.3 `cmInt` lifecycle and counter mutation

- Stage initialization sets `cmInt=-1`.
- Foreground code 26 interaction selects `cmInt = foreground >> 8`, then validates it against `mByteArr.length`; invalid values reset to -1.
- `alVoid()` returns early in modes 3/4/5 while `aqInt>0`; otherwise, for nonnegative `cmInt`, it decrements the selected byte counter directly.
- No clamp is present at the decrement site. Valid caller/state invariants must prevent underflow; malformed/duplicate calls could produce a negative byte and must be covered by a golden vector or rejected safely in Native.
- When the counter equals zero after decrement, matching foreground code-17 cells are scanned and the door-top/neighbor transition is applied.
- Checkpoint capture/restore explicitly copies `cmInt` and `mByteArr`; `lByteArr` is not copied.

### 37.4 Revised source-of-truth model

The group identity originates in the background-layer cell value used when foreground code 17 is processed. The source proves derivation from the already-decoded stage grids, but this audit has not yet located the exact binary serialization field/record that originally populated that background layer. Do not claim a direct binary counter field. The next audit must trace the stage decoder that populates background layer and prove group-ID domain/bounds, including the 32-bit mask limitation.


## 38. Stage binary layer decoder and background group-ID provenance — 2026-11-27

Pinned source: `i.java@215c71d04057d66a1b08419b1a4b1043ae3e0881`.

### 38.1 Resource and stage record layout

Stage resources are selected from `/w0.bin`, `/w1.bin`, and `/w2.bin` by world index. The loader reads one leading byte, then repeatedly reads a one-byte stage count. Each stage record begins with a 4-byte header:
- width = unsigned little-endian 16-bit at header offset 0
- height = unsigned little-endian 16-bit at header offset 2

The stage payload then contains exactly three row-major byte layers, each `width * height` bytes:
1. player layer
2. background layer
3. foreground layer

For the selected stage, each layer is read into a flat byte buffer and mapped to `grid[x][y] = buffer[x + y * width]`. For nonselected stages, the loader calls `skip(width * height * 3)`. The selected stage allocates:
- player layer: byte[width][height]
- `cByteArrArr`: byte[width][height]
- foreground layer: int[width][height]
- background `bByteArrArr`: byte[width][height]
- background `bIntArrArr`: int[width][height]

The background byte payload is assigned to int cells by Java signed-byte widening, not unsigned conversion. This is significant: values >= 0x80 become negative int values. The group-count builder only sets a mask bit for background values >= 0, so negative signed values are excluded from group indexing.

### 38.2 Width/height decoding and resource parser caveats

`unpackUint16` is the little-endian unsigned operation `(array[ptr] & 0xFF) | ((array[ptr+1] & 0xFF) << 8)`. Header dimensions are therefore unsigned 16-bit little-endian.

The loader uses InputStream.read(byte[]) without checking the returned byte count and uses skip(long) without checking the actual skipped count. This is a source robustness defect for truncated/short reads or streams whose skip advances fewer bytes than requested. Native parser must use read-fully semantics, validate positive bounded dimensions and multiplication overflow before allocation, and implement exact checked seeking. Do not reproduce silent partial-layer decoding or assume skip always consumes all requested bytes.

The stage count and leading resource byte are read as signed Java bytes but consumed in integer comparisons/loop bounds. Their accepted range and resource-level header semantics still require validation against actual bundled resources.

### 38.3 Background group-ID domain

The background layer originates directly from the second byte layer in the stage record. Values are sign-extended to int. For code-17 group processing:
- only `background >= 0` contributes to `n10 |= 1 << background`;
- the Hashtable key is the signed-widened background value;
- array length is derived by scanning the highest set bit in the 32-bit mask;
- Java int shift distance is masked to five bits, so `1 << background` aliases for values >=32. No proof yet that valid nonnegative group IDs are restricted to 0..31.

This creates a critical unresolved format invariant. Before treating mask-derived length as safe, inspect actual world resources or all stage-authoring evidence to prove background group IDs are within 0..31 (and whether 31 is valid under signed bit handling). Native should not silently inherit Java shift aliasing: validate group IDs against an explicit supported domain, preserving source behavior only when proven.

### 38.4 Checkpoint and schema implications

The original checkpoint copies `bIntArrArr` (int background grid) but not `bByteArrArr` (byte auxiliary grid). The group ID used by door logic is derived from `bIntArrArr`; however the original stage resource stores a byte and Java sign extension changes values 0x80..0xFF. Snapshot schema must preserve the decoded int semantics, not reinterpret as unsigned byte.

### 38.5 Next evidence required

1. Inspect bundled `w0.bin/w1.bin/w2.bin` bytes and enumerate every width, height, layer byte value, group ID and stage record boundary.
2. Prove maximum dimensions and safe `width*height` bound.
3. Prove group ID domain and resolve 32-bit shift aliasing.
4. Verify resource header and stage-count byte semantics.
5. Build parser tests for truncated header, short layer reads, short skip, overflow, negative/signed group values, and every real stage.
6. Keep CS-2 open; static source format is now identified, but resource-level validation is not complete.
