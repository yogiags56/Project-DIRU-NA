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
| 54 | 3 | One-based linked-slot reference |
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
