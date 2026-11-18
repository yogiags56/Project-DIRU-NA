# Development Isolation and Versioning

## Purpose

Project DIRU-NA is a clean Native Android reconstruction based on a different source candidate. It is not a continuation of the previous codebase or build lineage.

## Repository boundaries

| Repository | Role | Policy |
|---|---|---|
| `yogiags56/Diamond-Rush-Native-Android` | Previous implementation and historical builds | Archive/reference only; do not merge or copy its implementation into this project |
| `kubikaugustyn/DiamondRushSource` | New candidate source | Read-only audit input; do not commit changes upstream |
| `yogiags56/Project-DIRU-NA` | New Native Android project | Sole active development repository for this source lineage |

## Hard separation rules

1. Do not cherry-pick, merge, or copy source files, generated classes, build scripts, Gradle configuration, resources, or binaries from the previous implementation into Project DIRU-NA.
2. Do not reuse previous build outputs, APKs, intermediate directories, caches, or generated artifacts.
3. Do not reuse previous build numbers (including Build 480) or RVC milestone identifiers.
4. Do not make the new project depend on the old project at compile time or runtime.
5. Do not alter the previous archive repository as part of new development.
6. Candidate source is used for analysis only until licensing/provenance review permits any specific reuse.
7. Every new implementation must be created and tested within this repository's own build environment.

## New milestone and build identifiers

The new lineage uses identifiers independent from the old project:

- Source audits: `DIRU-NA-AUDIT-001`, `DIRU-NA-AUDIT-002`, ...
- Architecture and implementation milestones: `DIRU-NA-DEV-001`, `DIRU-NA-DEV-002`, ...
- Development builds: `DIRU-NA-BUILD-001`, `DIRU-NA-BUILD-002`, ...
- Public release versions remain `v1.0`, `v1.1`, `v1.2`, `v2.0-alpha`, and `v2.0` according to the release roadmap.

These counters start from 001 in this repository. They do not inherit or imply completion of any previous milestone/build.

## Build isolation requirements

When the Android project is introduced:

- Use a new Gradle root and project/module names unique to Project DIRU-NA.
- Use a separate application ID/package namespace.
- Keep build outputs under this repository's own `build/` directories.
- Use a separate CI workflow and artifact naming scheme.
- Do not share local Gradle caches as project artifacts; caches may be regenerated independently.
- Record the source revision, toolchain, build command, result, and artifact checksum for every reproducible build.

## Current status

- [x] New repository exists independently.
- [x] Previous repository is designated archive/reference only.
- [x] Candidate source is designated read-only audit input.
- [x] New audit/milestone/build numbering defined.
- [ ] Verify the repository contains no copied implementation or previous build artifacts.
- [ ] Create the independent Android/Gradle skeleton.
- [ ] Establish independent CI and reproducible build evidence.

No Android build or APK is claimed to exist yet.
