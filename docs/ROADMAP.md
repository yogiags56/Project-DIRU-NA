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

