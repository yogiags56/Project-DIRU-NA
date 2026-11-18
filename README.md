# Project DIRU-NA

**Project DIRU-NA** is an independent Native Android reconstruction project for *Diamond Rush* (2006), focused on recovering and implementing the original game's gameplay logic in a native Android application.

> This repository is a clean successor project. The older `Diamond-Rush-Native-Android` repository is preserved as an archive/reference and is not replaced by this repository.

## Project goals

- Reconstruct the original gameplay behavior from audited, legally usable source material.
- Implement a native Android application rather than embedding FreeJ2ME as an emulator.
- Separate gameplay logic from Android-specific rendering, input, audio, and persistence.
- Preserve the original game's visual proportions and behavior before introducing responsive screen adaptation.
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

The historical project target was landscape 320×240. The candidate decompilation also contains 240×320 drawing/configuration evidence. The correct orientation, logical viewport, and rotation behavior must be resolved during source audit before being locked into the implementation.

The eventual adaptive presentation must preserve aspect ratio without stretching gameplay, and should support common shapes including 1:1, 2:1, 3:2, 4:3, 5:4, 16:9, 18:9, and other device ratios.

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
- [ ] Logical viewport/orientation verified
- [ ] Native Android skeleton implemented
- [ ] First reproducible gameplay milestone

See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/TECHNICAL_AUDIT.md](docs/TECHNICAL_AUDIT.md).

## License and rights

No license is granted by this repository for Gameloft's original game, decompiled source, trademarks, or proprietary assets. Licensing for any third-party code must be reviewed separately before reuse. Until that review is complete, treat all external source material as read-only research input and do not redistribute it.
