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
- [ ] Resolve 320×240 versus 240×320 orientation/viewport discrepancy
- [x] Classify components as retain, adapt, or rewrite (initial classification; see DIRU-NA-AUDIT-001 findings)
- [x] Record licensing/provenance risks (rights remain unresolved; see DIRU-NA-AUDIT-001 findings)
- [ ] Reproduce the desktop build only if dependencies and rights permit

## DIRU-NA-AUDIT-002 — Gameplay and resource specification

- [ ] Write a resource catalog with offsets, dimensions, encoding, and call sites
- [ ] Document sprite and animation decoding
- [ ] Document level/map format and loading
- [ ] Document player state, movement, collision, and interaction rules
- [ ] Document audio event mapping
- [ ] Define test fixtures from legally usable material

## DIRU-NA-DEV-001 — Native Android foundation

- [ ] Establish Android Gradle project and supported SDK/toolchain
- [ ] Add a platform-independent core module
- [ ] Add application lifecycle and landscape presentation shell
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

- **v1.0:** first playable Native Android release with basic touchscreen controls.
- **v1.1:** About screen, appropriate game attribution, and “Porting by MeongDev” credit, subject to rights review.
- **v1.2:** language selection.
- **v2.0-alpha:** touchscreen UI adaptation while preserving the original visual identity.
- **v2.0:** adaptive aspect ratios and full-screen presentation without stretching gameplay.

Responsive aspect-ratio work must preserve the logical game coordinate system and must not distort sprites or collision geometry.
