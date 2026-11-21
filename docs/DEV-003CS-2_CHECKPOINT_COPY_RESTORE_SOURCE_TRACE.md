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
| Per-cell byte arrays `bByteArrArr`, `cByteArrArr` | Exact snapshot buffers not yet independently named in this trace | AUDIT-009 describes per-cell byte timers; AUDIT-002 identifies auxiliary per-cell byte state | AUDIT-009 describes timer-array restore | Keep separate; exact per-array copy/restore mapping still open |

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

**Still open:** exact copy/restore pairs for all three stage layers and per-cell byte arrays; full scalar/controller snapshot list; final schema types and compatibility policy.

CS-2 remains in progress. This trace does not authorize a production snapshot provider yet.
