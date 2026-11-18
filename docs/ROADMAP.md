# Project DIRU-NA Roadmap

This roadmap belongs exclusively to the new source lineage. Previous RVC milestones and build numbers are not carried forward. See [Development Isolation](DEVELOPMENT_ISOLATION.md).

## DIRU-NA-AUDIT-001 — Candidate source audit

- [x] Inventory candidate source tree and dependencies
- [x] Map resource-loading call sites (initial catalog recorded; further binary validation remains open)
- [ ] Identify binary resource formats (`.f`, `.bin`, map data)
- [x] Inventory all language resource filenames (14 locale resources + `lang.xx` fallback; see [LANGUAGE_RESOURCE_CATALOG.md](LANGUAGE_RESOURCE_CATALOG.md))
- [ ] Map game loop, input, collision, animation, and level flow
- [ ] Map persistence/save behavior (including RMS usage)
- [ ] Audit MIDP, Nokia, Siemens, Mascot Micro3D, and M3G APIs actually used
- [x] Set DIRU-NA initial logical viewport baseline to 240×320 portrait (project decision; separate from the archived project's 320×240 target)
- [x] Classify components as retain, adapt, or rewrite (initial classification; see DIRU-NA-AUDIT-001 findings)
- [x] Record licensing/provenance risks (rights remain unresolved; see DIRU-NA-AUDIT-001 findings)
- [ ] Reproduce the desktop build only if dependencies and rights permit

## DIRU-NA-AUDIT-002 — Gameplay and resource specification

**Working document:** [DIRU-NA-AUDIT-002 Gameplay and Resource Specification](DIRU-NA-AUDIT-002_GAMEPLAY_RESOURCE_SPEC.md)

- [ ] Write a complete resource catalog with offsets, dimensions, encoding, and call sites (initial resource call-site catalog exists; binary validation remains open)
- [x] Document sprite and animation decoding at static-source level (binary fixtures and decoder tests remain open)
- [x] Document stage-package outer structure and three tile layers at static-source level (field dictionary and binary validation remain open)
- [ ] Document player state, movement, collision, and interaction rules (spawn marker, direction states, action gating, checkpoint snapshot/restore arrays, partial occupancy predicates, and moving-object code 48 behavior recorded; full state/physics map and semantic tile dictionary remain open)
- [x] Document audio event IDs and priority mapping (container header/payload validation remains open)
- [ ] Define test fixtures from legally usable material
- [x] Record partial entity/foreground initialization effects and generated sprite-chunk slot mappings (semantic entity identities remain unresolved)
- [x] Record logical viewport evidence (candidate source repeatedly uses 240×320; original Nokia orientation still requires confirmation)

### Language-resource scope decision

Language payload changes are deferred until a Native Android APK is verified playable. Keep the existing candidate language inventory as documentation only. Do not import files from the previous Android build. After playable status, audit original Nokia Diamond Rush language resources read-only, subject to provenance and licensing review, then design language selection.

## DIRU-NA-DEV-001 — Native Android foundation

- [ ] Establish Android Gradle project and supported SDK/toolchain
- [ ] Add a platform-independent core module
- [ ] Add application lifecycle and 240×320 portrait presentation shell
- [ ] Add renderer abstraction and logical viewport
- [ ] Add input abstraction
- [ ] Add audio and persistence interfaces
- [ ] Add automated tests and CI checks

## DIRU-NA-DEV-002 — First gameplay reconstruction

- [ ] Implement verified game-state model
- [ ] Implement movement and collision behavior
- [ ] Implement animation/state transitions
- [ ] Load a legally usable test level/resource set
- [ ] Validate behavior against documented reference observations

## Later release direction

- **v1.0:** first playable Native Android release with basic touchscreen controls, retaining the 240×320 portrait logical viewport.
- **v1.1:** About screen, appropriate game attribution, and “Porting by MeongDev” credit, subject to rights review.
- **v1.2:** language selection.
- **v2.0-alpha:** touchscreen UI adaptation while preserving the original visual identity.
- **v2.0:** adaptive aspect ratios and full-screen presentation without stretching gameplay, while preserving the 240×320 logical coordinate system and adapting the viewport/layout around it.

**DIRU-NA baseline decision:** the first implementation and first playable build retain a 240×320 portrait logical resolution. This is an explicit decision for this independent project, informed by the candidate source's repeated 240×320 drawing/camera constants. It does not inherit the archived Native Android project's 320×240 target or build lineage. Future adaptive aspect-ratio support is a later enhancement: preserve the 240×320 gameplay coordinate system, maintain sprite/collision proportions, and adapt viewport/camera/UI for 1:1, 2:1, 3:2, 4:3, 5:4, 16:9, 18:9, and other screen ratios without stretching gameplay.

## DIRU-NA-AUDIT-003 — Nokia MIDlet identity and platform metadata

**Working document:** [DIRU-NA-AUDIT-003 Nokia MIDlet Identity](DIRU-NA-AUDIT-003_NOKIA_MIDLET_IDENTITY.md)

- [x] Inspect candidate resource manifest and compiled-output manifest
- [x] Confirm MIDlet name, vendor, version, profile/configuration, and Nokia-specific attributes
- [x] Inspect MIDlet entry point and Nokia API compatibility namespaces
- [x] Record evidence-based classification as a Nokia-identified J2ME MIDlet package, version 1.1.7
- [ ] Obtain independently sourced original JAR/JAD and compare hashes/manifest for provenance authentication
- [ ] Trace actual Nokia-specific API call sites and determine whether any are required by gameplay
- [ ] Confirm original handset model, physical display characteristics, and distribution channel (not established by manifest alone)























## DIRU-NA-AUDIT-027 — Stage ID, title mapping and secret-route correlation

**Working document:** [DIRU-NA-AUDIT-027 Stage Title & Secret Route Correlation](DIRU-NA-AUDIT-027_STAGE_TITLE_SECRET_ROUTE_CORRELATION.md)

- [x] Trace generated normal, secret, and intro title ranges
- [x] Extract per-world stage-title mapping arrays
- [x] Cross-reference all 40 map nodes with generated localized title numbers
- [x] Confirm secondary field value 1 maps to Secret stage titles in all three maps
- [x] Record the unresolved extra Tibet/Siberia mapping entry at stage index 14
- [ ] Trace exact secret-stage unlock prerequisites and stage flag lifecycle
- [ ] Reconcile map/title arrays with /w0.bin, /w1.bin, /w2.bin stage streams
- [ ] Verify localized text indices 66, 62, and 31 against language resources
- [ ] Validate secret-stage availability and navigation against original runtime


## DIRU-NA-AUDIT-026 — Binary world-map extraction and topology validation

**Working document:** [DIRU-NA-AUDIT-026 Binary World Map Extraction & Topology](DIRU-NA-AUDIT-026_BINARY_WORLD_MAP_EXTRACTION_TOPOLOGY.md)

- [x] Decode all three map resources from the pinned read-only source candidate
- [x] Confirm exact payload lengths and record counts
- [x] Reconstruct per-world node coordinates, stage IDs, and neighbor lists
- [x] Validate unique node coordinates and 12×12 grid bounds
- [x] Validate neighbor references, reciprocal edges, and graph connectivity
- [x] Correct AUDIT-025 record layout: five fixed bytes per node, not six
- [ ] Determine semantic meaning of secondary field values, especially value 1
- [ ] Correlate stage IDs with title mappings and actual stage streams
- [ ] Complete directional tie-break analysis using real neighbor ordering
- [ ] Identify node sprite frames and validate visual classes
- [ ] Add automated parser and topology fixtures/tests
- [ ] Validate map navigation against original runtime evidence


## DIRU-NA-AUDIT-025 — World-map resource records and node construction

**Working document:** [DIRU-NA-AUDIT-025 World Map Resource & Node Construction](DIRU-NA-AUDIT-025_WORLD_MAP_RESOURCE_NODE_CONSTRUCTION.md)

- [x] Identify three world-map resource paths
- [x] Trace binary envelope: 16-bit little-endian payload length and 1-byte record count
- [x] Reconstruct node record field order and widths
- [x] Trace record fields into packed-long node bits
- [x] Confirm 12×12 grid allocation and direct X/Y indexing
- [x] Separate map topology from stage gameplay resources and save records
- [ ] Decode each binary map resource and publish node/edge inventories
- [ ] Validate coordinates, neighbor counts, graph references, duplicates, and connectivity
- [ ] Determine full semantics of packed classification fields
- [ ] Identify secret-stage nodes and prerequisites
- [ ] Add robust map parser tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-024 — World-map node states, directional selection and route animation

**Working document:** [DIRU-NA-AUDIT-024 Node States, Directional Selection & Route Animation](DIRU-NA-AUDIT-024_NODE_STATES_DIRECTIONAL_SELECTION_ROUTE_ANIMATION.md)

- [x] Record observed values and uses of packed node field bits 0–2
- [x] Record observed values and uses of secondary field bits 3–5
- [x] Trace directional candidate filtering over explicit neighbor coordinates
- [x] Record partial multiple-candidate selection behavior
- [x] Trace qualifying higher-index neighbor progression and flag 0x40 writes
- [x] Separate route marker animation state from persistent graph state
- [ ] Complete semantics for every low-field value
- [ ] Reconstruct exact directional tie-break algorithm
- [ ] Trace full lifecycle of stage flag 0x40
- [ ] Establish secret-stage node encoding and prerequisites
- [ ] Identify node sprite frames from legally usable resources
- [ ] Add deterministic direction, graph, and progression tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-023 — Packed world-map format and route graph reconstruction

**Working document:** [DIRU-NA-AUDIT-023 Packed World Map & Route Graph](DIRU-NA-AUDIT-023_PACKED_WORLD_MAP_ROUTE_GRAPH.md)

- [x] Confirm 12×12 packed-long node grid and empty-cell convention
- [x] Identify stage-index field at bits 6–10
- [x] Identify neighbor-count field at bits 11–13
- [x] Trace repeated 4-bit neighbor x/y coordinate pairs from bit 14
- [x] Trace graph-based neighbor traversal and route advancement
- [x] Record partial low-field state uses and 240×320 map coordinate constants
- [ ] Determine complete semantics of low fields 0–2 and 3–5
- [ ] Validate maximum neighbor count and 64-bit packing boundary
- [ ] Complete directional neighbor selection and tie-breaking analysis
- [ ] Decode secret-stage node encoding and prerequisites
- [ ] Identify source/provenance of generated 12×12 node array
- [ ] Add packed-node parser and graph-validation tests
- [ ] Validate topology and route rendering against original runtime


## DIRU-NA-AUDIT-022 — Stage result flags, route advancement and secret-stage evidence

**Working document:** [DIRU-NA-AUDIT-022 Stage Result Flags, Route & Secret Stages](DIRU-NA-AUDIT-022_STAGE_RESULT_FLAGS_ROUTE_SECRET_STAGES.md)

- [x] Trace stage result flag snapshot and one-time award checks
- [x] Record result-condition bits 0x04, 0x08, 0x10, and 0x20
- [x] Trace bit 0x02 use in result commit and stage/map state
- [x] Trace bit 0x40 writes during progression advancement
- [x] Distinguish coordinate collectible persistence from result flags
- [x] Confirm secret-stage title generation
- [ ] Enumerate all readers/writers of stage flag bits
- [ ] Decode packed node state and relate it to stage flags
- [ ] Trace all progression-index transitions and branching paths
- [ ] Establish exact secret-stage prerequisites
- [ ] Add deterministic progression and persistence tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-021 — World map, stage selection and stage unlock conditions

**Working document:** [DIRU-NA-AUDIT-021 World Map, Stage Selection & Unlocks](DIRU-NA-AUDIT-021_WORLD_MAP_STAGE_SELECTION_UNLOCKS.md)

- [x] Record per-world stage-title mapping entries and indices
- [x] Trace regular, secret, and intro-stage title generation
- [x] Identify 12×12 packed world-map node grid
- [x] Trace selected node title and current/target counter display
- [x] Record per-world save offsets and stage-record structure
- [x] Trace coordinate-pair collection persistence
- [x] Record observed stage-result flag writes and world-specific stage-index clamping
- [ ] Decode complete packed world-map node schema
- [ ] Trace all stage flag bit semantics and secret-stage prerequisites
- [ ] Reconcile stage-title indices with actual world stage counts
- [ ] Extract map node coordinates, routes, and marker animation
- [ ] Add deterministic map, unlock, and persistence tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-020 — World seal menu, unlock conditions and world entry

**Working document:** [DIRU-NA-AUDIT-020 World Seal, Unlock & Stage Progression](DIRU-NA-AUDIT-020_WORLD_SEAL_UNLOCK_STAGE_PROGRESSION.md)

- [x] Record four seal-menu destinations
- [x] Confirm red-gem requirements: Angkor 0, Bavaria 10, Siberia 25
- [x] Trace unlock eligibility helper and locked/unlocked prompts
- [x] Reconstruct directional navigation graph
- [x] Trace world-entry state transitions and stage reset behavior
- [x] Record save-byte-1 Bavaria/Siberia unlock flag writes
- [x] Confirm world entry does not deduct red gems in inspected path
- [ ] Complete save-byte-1 and per-world stage progression semantics
- [ ] Confirm Angkor selected-stage retention behavior
- [ ] Map exact seal menu assets and localization strings
- [ ] Add deterministic menu/unlock/world-entry tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-019 — Gem counters, jacket palette state and world progression

**Working document:** [DIRU-NA-AUDIT-019 Gem Counters, Jacket Palette & World Progression](DIRU-NA-AUDIT-019_GEM_COUNTERS_JACKET_PALETTE_WORLD_PROGRESSION.md)

- [x] Confirm separate violet-gem and red-gem balances
- [x] Trace code-2 counter into red-gem balance
- [x] Trace code-41 quantity counter into violet-gem balance
- [x] Trace shop and world-price configuration
- [x] Trace save-byte-8 into jacket progression and hero palette
- [x] Trace save-byte-9 world/story progression thresholds
- [ ] Decode and identify visible jacket palette variants
- [ ] Distinguish jacket ownership from equipped selection
- [ ] Complete save-byte-9 state mapping and special-object semantics
- [ ] Identify exact pickup visuals and remaining collectible counters
- [ ] Add deterministic economy, jacket and progression tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-018 — Shop jackets, hero palette and progression-item state

**Working document:** [DIRU-NA-AUDIT-018 Shop Jackets, Hero Palette & Progression](DIRU-NA-AUDIT-018_SHOP_JACKETS_HERO_PALETTE_PROGRESSION.md)

- [x] Confirm four shop entries are documented as jackets
- [x] Record configured violet-gem prices: 150, 400, 1,000, 3,000
- [x] Trace ownership threshold written to save byte 8
- [x] Trace save byte 8 into hero palette selection
- [x] Distinguish jacket currency from red-gem world-unlock currency
- [x] Record save-byte-9 ambiguity and progression writes
- [ ] Identify visible colors/names of all four jacket palettes
- [ ] Complete save-byte-8 and save-byte-9 reader/writer trace
- [ ] Trace potion pickup, consumption, duration, and effect
- [ ] Map violet-gem pickup IDs and counter increments
- [ ] Add deterministic shop, palette, and persistence tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-017 — Collectibles, currency, shop and item progression

**Working document:** [DIRU-NA-AUDIT-017 Collectibles, Currency, Shop & Items](DIRU-NA-AUDIT-017_COLLECTIBLES_CURRENCY_SHOP_ITEMS.md)

- [x] Trace stage-result counters and target comparisons
- [x] Record selected pickup-to-counter effects
- [x] Identify red-diamond world-unlock balance at save offsets 6–7
- [x] Distinguish shop balance at save offsets 4–5
- [x] Trace four-entry shop selection, affordability, ownership check, and purchase persistence
- [x] Record unresolved save-byte-9 / blue-potion interpretation
- [ ] Verify collectible category labels from language resources
- [ ] Trace all counter writers/readers and pickup visual mappings
- [ ] Identify all shop items and gameplay effects
- [ ] Trace potion pickup, inventory, consumption, and effect
- [ ] Add deterministic inventory, currency, shop, and persistence tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-016 — Water-related tiles, potion resource flags and environmental damage

**Working document:** [DIRU-NA-AUDIT-016 Water, Potion & Environmental Hazards](DIRU-NA-AUDIT-016_WATER_POTION_ENVIRONMENTAL_HAZARDS.md)

- [x] Trace foreground code-8/code-9 preprocessing distinction
- [x] Trace water-related stage feature flags
- [x] Trace conditional chunk-28 loading and texture slots 24/25
- [x] Record confirmed environmental damage call examples
- [x] Document unsupported assumptions about swimming, oxygen, and drowning
- [ ] Trace all feature-flag and texture-slot references
- [ ] Trace potion pickup, inventory, consumption, and gameplay effect
- [ ] Trace player movement/collision on foreground codes 8 and 9
- [ ] Determine whether breathing/underwater timers exist
- [ ] Validate water tile and potion semantics against original runtime evidence


## DIRU-NA-AUDIT-015 — Object pushing, falling entities and environmental interaction

**Working document:** [DIRU-NA-AUDIT-015 Object Pushing, Falling & Environment](DIRU-NA-AUDIT-015_OBJECT_PUSHING_FALLING_ENVIRONMENT.md)

- [x] Trace code-48 multi-cell object initialization
- [x] Record selected ordinary object push branches
- [x] Trace two-cell code-48 push destination and occupancy checks
- [x] Record code-46 timed downward movement and state transitions
- [x] Record foreground code-3 timed interaction with player/entities
- [x] Document native atomic multi-cell movement requirements
- [ ] Trace code-48 helper methods and complete lifecycle
- [ ] Complete movable/falling entity update catalog
- [ ] Map per-cell timer arrays and metadata semantics
- [ ] Build complete pushability/support/collision matrix
- [ ] Add deterministic object-physics tests and runtime validation


## DIRU-NA-AUDIT-014 — Player movement, collision and tile interaction

**Working document:** [DIRU-NA-AUDIT-014 Player Movement, Collision & Tile Interaction](DIRU-NA-AUDIT-014_PLAYER_MOVEMENT_COLLISION_TILE_INTERACTION.md)

- [x] Record directional input mapping and committed movement direction
- [x] Document separation of tile coordinates and sub-tile movement progress
- [x] Record movement eligibility gates and direction-vector use
- [x] Document examples of metadata-dependent collision behavior
- [x] Trace checkpoint confirm action and selected context-sensitive interactions
- [x] Record camera synchronization with interpolated player position
- [ ] Complete movement helper and all movement-progress transitions
- [ ] Build full collision and interaction matrix
- [ ] Trace pushing, climbing, swimming, doors, ladders, and hazards
- [ ] Add deterministic movement/collision/action tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-013 — Player spawn, camera system and movement initialization

**Working document:** [DIRU-NA-AUDIT-013 Player Spawn, Camera & Movement](DIRU-NA-AUDIT-013_PLAYER_SPAWN_CAMERA_MOVEMENT.md)

- [x] Trace player-start marker 79 and initial logical coordinates
- [x] Record the 24-pixel tile coordinate model
- [x] Trace normal camera-follow offsets, thresholds, and bounds
- [x] Trace scripted camera transition interpolation
- [x] Identify shared keyboard input bitmask
- [ ] Trace all uses of the spawn marker's retained X coordinate
- [ ] Complete movement interpolation and key-to-action mapping
- [ ] Trace camera update cadence and all scripted camera entry points
- [ ] Define and test small-stage camera bounds
- [ ] Add deterministic spawn, camera, and input-equivalence tests
- [ ] Validate against original runtime evidence


## DIRU-NA-AUDIT-012 — Stage loading, gameplay initialization and resource lifecycle

**Working document:** [DIRU-NA-AUDIT-012 Stage Loading & Gameplay Initialization](DIRU-NA-AUDIT-012_STAGE_LOADING_GAMEPLAY_INITIALIZATION.md)

- [x] Trace incremental initialization steps 7–10
- [x] Record world stage-file selection and binary stage header
- [x] Document extraction of player/object, background, and foreground layers
- [x] Record selected tile preprocessing transformations
- [x] Record selected resource loading and world-map cleanup paths
- [x] Identify parser robustness risks in single read/skip calls and unchecked dimensions
- [ ] Complete all initialization steps and conditional branches
- [ ] Complete tile preprocessing and spawn/camera ordering
- [ ] Map resource load masks, ownership, and all unload paths
- [ ] Add synthetic parser and lifecycle tests
- [ ] Validate stage entry against original runtime evidence


## DIRU-NA-AUDIT-011 — Main menu, world selection and stage selection

**Working document:** [DIRU-NA-AUDIT-011 Main Menu, World & Stage Selection](DIRU-NA-AUDIT-011_MAIN_MENU_WORLD_STAGE_SELECTION.md)

- [x] Trace title/menu input routine and confirm/cancel behavior
- [x] Identify three world indices and corresponding map resource filenames
- [x] Trace seal/world unlock threshold checks
- [x] Trace transition into world-map preparation states 15 and 21
- [x] Record map-node and stage-progress reconciliation fields
- [ ] Complete menu layout/options and cursor movement
- [ ] Trace exact map-node selection and stage-launch route
- [ ] Resolve world seal names versus map resource names
- [ ] Decode stage flag bits and progression helper semantics
- [ ] Add deterministic selection/unlock tests and runtime validation


## DIRU-NA-AUDIT-010 — Game state machine and screen transitions

**Working document:** [DIRU-NA-AUDIT-010 Game State & Screen Transitions](DIRU-NA-AUDIT-010_GAME_STATE_SCREEN_TRANSITIONS.md)

- [x] Identify the global game-flow state field and initialization
- [x] Record partial state roles supported by inspected update/input/paint paths
- [x] Trace the incremental world-map/resource preparation state
- [x] Trace credits screen loading, scrolling, and return path
- [x] Record selected input and lifecycle state routing
- [ ] Enumerate all state writes and map every state value
- [ ] Complete title, world-selection, and stage-selection navigation
- [ ] Trace terminal game-over presentation and retry transitions
- [ ] Complete pause/resume and lifecycle behavior
- [ ] Build deterministic state-transition tests and validate against runtime evidence


## DIRU-NA-AUDIT-009 — Damage, death, retry and checkpoint reconstruction

**Working document:** [DIRU-NA-AUDIT-009 Damage, Death, Retry & Checkpoint](DIRU-NA-AUDIT-009_DAMAGE_DEATH_RETRY_CHECKPOINT.md)

- [x] Inspect damage-handler gates, state-bit updates, and animation requests
- [x] Record observed damage argument patterns without assigning unsupported meanings
- [x] Trace checkpoint snapshot contents and in-memory persistence boundary
- [x] Trace checkpoint activation and restore sequence
- [x] Trace retry-count decrement and exhausted-retry state branch
- [ ] Trace all writers/readers for damage, retry, and counter fields
- [ ] Complete death animation and global state-transition graph
- [ ] Identify exact checkpoint-save trigger conditions
- [ ] Build deterministic damage/checkpoint/retry tests
- [ ] Validate behavior against original runtime evidence


## DIRU-NA-AUDIT-008 — Hero animation and movement state reconstruction

**Working document:** [DIRU-NA-AUDIT-008 Hero Animation & Movement State](DIRU-NA-AUDIT-008_HERO_ANIMATION_MOVEMENT_STATE.md)

- [x] Trace hero sprite resource, chunk, and palette selection
- [x] Inspect central hero animation selector and environment-dependent overrides
- [x] Record special auxiliary animator path
- [x] Separate hero animator timeline from nearby entity update logic
- [ ] Trace all callers of the hero animation selector and assign state names only where proven
- [ ] Complete hero movement and scripted-state mapping
- [ ] Trace damage, death, retry, and checkpoint animation transitions
- [ ] Complete hero draw/tick lifecycle and offset behavior
- [ ] Create deterministic hero state-transition fixtures
- [ ] Validate animation behavior against original runtime evidence


## DIRU-NA-AUDIT-007 — Entity animation and state mapping

**Working document:** [DIRU-NA-AUDIT-007 Entity Animation & State Mapping](DIRU-NA-AUDIT-007_ENTITY_ANIMATION_STATE_MAPPING.md)

- [x] Record multi-pass stage rendering structure at static-source level
- [x] Map selected foreground codes to sprite slots and animation/frame selection
- [x] Map selected object-layer codes to sprite slots and animation/frame selection
- [x] Record examples of per-cell animation progression and completion side effects
- [ ] Complete hero animation state mapping
- [ ] Complete enemy AI and animation mapping
- [ ] Complete object/entity codes 0–79 state and animation mapping
- [ ] Trace all per-cell state arrays and reset/restore behavior
- [ ] Create deterministic synthetic animation/state tests
- [ ] Validate render ordering and behavior against original runtime evidence


## DIRU-NA-AUDIT-006 — Sprite resource mapping and animation reconstruction

**Working document:** [DIRU-NA-AUDIT-006 Sprite Resource Mapping](DIRU-NA-AUDIT-006_SPRITE_RESOURCE_MAPPING.md)

- [x] Document sprite parser sections, palette formats, and encoded module formats at static-source level
- [x] Document module-to-frame composition and animation timing behavior
- [x] Map selected generated-resource chunks to runtime sprite slots and texture arrays
- [x] Map selected `cm.f`, UI, hero, and other directly loaded sprite resources
- [ ] Complete conditional load-mask, palette, world, and resource-lifetime mapping
- [ ] Identify visual meaning of sprite slots and animation indices using legally usable fixtures
- [ ] Validate decoder formats, offsets, flips, frame bounds, and animation timing with synthetic fixtures
- [ ] Trace hero/enemy/object state-to-animation transitions
- [ ] Confirm unresolved chunk boundaries and compare with original handset/runtime evidence


## DIRU-NA-AUDIT-004 — Gameplay state machine and movement reconstruction

**Working document:** [DIRU-NA-AUDIT-004 Gameplay State & Movement](DIRU-NA-AUDIT-004_GAMEPLAY_STATE_MOVEMENT.md)

- [x] Document main loop timing and input bitmask capture at static-source level
- [x] Document four-direction input mapping and 24-unit tile coordinate model
- [x] Trace movement interpolation and animation selection paths
- [x] Record partial foreground/object-layer collision and push behavior
- [x] Trace damage handler, manual respawn, retry transition, and checkpoint snapshot boundary
- [x] Record camera-follow coordinate relationship
- [ ] Complete direction vector and animation-index dictionary
- [ ] Trace status/retry counters to UI and save/load call sites before assigning semantic labels
- [ ] Complete tile-code dictionary using legally usable visual/resource fixtures
- [ ] Trace all damage sources, scripted exits, and special movement states
- [ ] Create deterministic movement/collision/checkpoint test fixtures
- [ ] Validate gameplay timing and behavior against original handset/runtime evidence


## DIRU-NA-AUDIT-005 — Tile and entity dictionary reconstruction

**Working document:** [DIRU-NA-AUDIT-005 Tile & Entity Dictionary](DIRU-NA-AUDIT-005_TILE_ENTITY_DICTIONARY.md)

- [x] Confirm three world map resources and stage header/layer layout
- [x] Document player/object, background, and foreground layer roles
- [x] Record partial foreground code catalog from initialization, collision, and rendering branches
- [x] Record partial object/entity code catalog and confirmed player-start/checkpoint/door/multi-cell behaviors
- [x] Document sprite parser structure and animation composition model
- [x] Record partial sprite-slot to file/chunk/palette mapping (see AUDIT-006; visual identities and conditional paths remain open)
- [ ] Validate all image decoder formats and malformed-input behavior
- [ ] Correlate tile/entity codes with visual sprite frames using permitted fixtures
- [ ] Trace dynamic entity updates and remaining puzzle/hazard/collectible behaviors
- [ ] Build synthetic map and sprite parser fixtures
- [ ] Validate against original Nokia package/runtime evidence

