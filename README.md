# Project DIRU-NA

**Project DIRU-NA** is an independent Native Android reconstruction project for *Diamond Rush* (2006), focused on recovering and implementing the original game's gameplay logic in a native Android application.

> This repository is a clean successor project. The older `Diamond-Rush-Native-Android` repository is preserved as an archive/reference and is not replaced by this repository.

## Project goals

- Reconstruct the original gameplay behavior from audited, legally usable source material.
- Implement a native Android application rather than embedding FreeJ2ME as an emulator.
- Separate gameplay logic from Android-specific rendering, input, audio, and persistence.
- Establish 240×320 portrait as the initial logical viewport and first-build baseline.
- Add responsive screen adaptation later without stretching sprites or altering gameplay geometry.
- Document every reverse-engineering finding, resource format, compatibility decision, and test result.

## Architecture (planned)

```text
app/          Android application entry point and lifecycle
core/         Platform-independent gameplay, physics, animation, and level logic
renderer/     Native rendering and viewport management
input/        Touch and controller/key input mapping
audio/        Sound effects and music playback
storage/      Save data and persistence
resources/    Audited, legally usable runtime resources
recovery/     Source audit, resource maps, compatibility findings
docs/         Architecture, decisions, milestones, and test records
tools/        Development and verification utilities
```

The structure is a target layout, not a claim that these modules have already been implemented.

## Development principles

1. **Evidence before implementation.** Findings must be traceable to source files, resource data, or reproducible tests.
2. **No emulator dependency in the final runtime.** FreeJ2ME may be studied as reference infrastructure, but the Android game must not rely on it as its runtime.
3. **Preserve original behavior first.** Responsive UI and aspect-ratio enhancements come after the original gameplay baseline is validated.
4. **Keep provenance clear.** Do not copy or redistribute decompiled game code or proprietary assets until their licensing and rights have been reviewed.
5. **No invented progress.** Milestones are marked complete only when supported by code, tests, or documented evidence.

## Screen and gameplay compatibility

**DIRU-NA initial baseline: 240×320 portrait.** The first Native Android implementation and first playable build will retain this logical resolution. This is a deliberate project decision based on the candidate source's repeated 240×320 rendering and camera calculations. It is independent of the archived `Diamond-Rush-Native-Android` project's historical 320×240 target; no build configuration or implementation is inherited from that project.

Adaptive aspect-ratio support is planned for a later milestone. It must preserve the 240×320 logical gameplay coordinate system and sprite/collision proportions while adapting viewport, camera, and UI layout for 1:1, 2:1, 3:2, 4:3, 5:4, 16:9, 18:9, and other device ratios without stretching gameplay.

## Reference repositories

- Previous project archive: [Diamond-Rush-Native-Android](https://github.com/yogiags56/Diamond-Rush-Native-Android)
- Candidate source for read-only audit: [DiamondRushSource](https://github.com/kubikaugustyn/DiamondRushSource)

The candidate source repository is an audit input only. Its presence does not establish permission to redistribute its contents or original game materials.

## Current status

**Stage:** Repository foundation

- [x] Independent repository created
- [x] Project scope and principles documented
- [ ] Source and resource audit completed
- [ ] Original gameplay loop mapped
- [x] Initial logical viewport decision recorded: 240×320 portrait
- [ ] Adaptive viewport behavior implemented and tested
- [ ] Native Android skeleton implemented
- [ ] First reproducible gameplay milestone

See [docs/ROADMAP.md](docs/ROADMAP.md), [docs/DEVELOPMENT_ISOLATION.md](docs/DEVELOPMENT_ISOLATION.md), [docs/DIRU-NA-AUDIT-001_FINDINGS.md](docs/DIRU-NA-AUDIT-001_FINDINGS.md), and [docs/DIRU-NA-AUDIT-006_SPRITE_RESOURCE_MAPPING.md](docs/DIRU-NA-AUDIT-006_SPRITE_RESOURCE_MAPPING.md), and [docs/DIRU-NA-AUDIT-007_ENTITY_ANIMATION_STATE_MAPPING.md](docs/DIRU-NA-AUDIT-007_ENTITY_ANIMATION_STATE_MAPPING.md), and [docs/DIRU-NA-AUDIT-008_HERO_ANIMATION_MOVEMENT_STATE.md](docs/DIRU-NA-AUDIT-008_HERO_ANIMATION_MOVEMENT_STATE.md), and [docs/DIRU-NA-AUDIT-009_DAMAGE_DEATH_RETRY_CHECKPOINT.md](docs/DIRU-NA-AUDIT-009_DAMAGE_DEATH_RETRY_CHECKPOINT.md), and [docs/DIRU-NA-AUDIT-010_GAME_STATE_SCREEN_TRANSITIONS.md](docs/DIRU-NA-AUDIT-010_GAME_STATE_SCREEN_TRANSITIONS.md), and [docs/DIRU-NA-AUDIT-011_MAIN_MENU_WORLD_STAGE_SELECTION.md](docs/DIRU-NA-AUDIT-011_MAIN_MENU_WORLD_STAGE_SELECTION.md), and [docs/DIRU-NA-AUDIT-012_STAGE_LOADING_GAMEPLAY_INITIALIZATION.md](docs/DIRU-NA-AUDIT-012_STAGE_LOADING_GAMEPLAY_INITIALIZATION.md), and [docs/DIRU-NA-AUDIT-013_PLAYER_SPAWN_CAMERA_MOVEMENT.md](docs/DIRU-NA-AUDIT-013_PLAYER_SPAWN_CAMERA_MOVEMENT.md).

## License and rights

No license is granted by this repository for Gameloft's original game, decompiled source, trademarks, or proprietary assets. Licensing for any third-party code must be reviewed separately before reuse. Until that review is complete, treat all external source material as read-only research input and do not redistribute it.
