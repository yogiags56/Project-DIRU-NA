# Project DIRU-NA Roadmap

This roadmap records intended work. A task is not complete until its evidence is recorded.

## RVC-046 — Candidate source audit

- [ ] Inventory candidate source tree and dependencies
- [ ] Map resource-loading call sites
- [ ] Identify binary resource formats (`.f`, `.bin`, map data)
- [ ] Map game loop, input, collision, animation, and level flow
- [ ] Map persistence/save behavior (including RMS usage)
- [ ] Audit MIDP, Nokia, Siemens, Mascot Micro3D, and M3G APIs actually used
- [ ] Resolve 320×240 versus 240×320 orientation/viewport discrepancy
- [ ] Classify components as retain, adapt, or rewrite
- [ ] Record licensing/provenance risks
- [ ] Reproduce the desktop build only if dependencies and rights permit

## RVC-047 — Gameplay and resource specification

- [ ] Write a resource catalog with offsets, dimensions, encoding, and call sites
- [ ] Document sprite and animation decoding
- [ ] Document level/map format and loading
- [ ] Document player state, movement, collision, and interaction rules
- [ ] Document audio event mapping
- [ ] Define test fixtures from legally usable material

## RVC-048 — Native Android foundation

- [ ] Establish Android Gradle project and supported SDK/toolchain
- [ ] Add a platform-independent core module
- [ ] Add application lifecycle and landscape presentation shell
- [ ] Add renderer abstraction and logical viewport
- [ ] Add input abstraction
- [ ] Add audio and persistence interfaces
- [ ] Add automated tests and CI checks

## RVC-049 — First gameplay reconstruction

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
