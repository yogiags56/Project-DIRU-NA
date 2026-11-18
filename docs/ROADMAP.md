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
