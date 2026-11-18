# DIRU-NA-DEV-001A — Native Android Toolchain Decision Record

**Status:** Initial toolchain and Android debug build validated by GitHub Actions  
**Date:** 2026-11-18  
**Repository:** `yogiags56/Project-DIRU-NA`

## 1. Purpose and scope

This record establishes a reproducible candidate toolchain for the first Native Android project skeleton and the platform-independent JVM core. It follows AUDIT-044 and uses current official compatibility information checked on 2026-11-18.

This is the project toolchain decision record. The initial Gradle root and launcher shell are now recorded in DEV-001B; build execution validation remains pending.

## 2. Recommended initial toolchain

| Component | Proposed baseline | Rationale / status |
|---|---|---|
| Build system | Gradle Wrapper | One pinned build entry point for local and CI use |
| Gradle | **9.8.2** | Newer than AGP 9.5.1's minimum Gradle 9.6.0 |
| Android Gradle Plugin | **9.5.1** | Stable patch release in the AGP 9.5 line |
| Gradle runtime JDK | **JDK 17** | AGP 9.5.1 lists JDK 17 as its minimum/default |
| Core language | **Java 17** | Lowest-friction first implementation; matches the Java source audit and avoids introducing a second language before core contracts stabilize |
| Android source language | **Java 17 initially** | Keep the first app/core implementation single-language |
| Build-script DSL | **Groovy DSL** | Conservative, widely documented Gradle convention; no Kotlin DSL requirement for this project |
| Compile SDK | **API 36** | AGP 9.5 supports up to API 37; API 36 is the installed stable SDK used by CI |
| JVM unit tests | **JUnit 4** | Small, mature dependency for the pure JVM core; handwritten fakes remain the default |
| Application ID / namespace | `MeongDev.DIRUNA.GloftDIRU` | Exact owner-provided identifier; preserve capitalization |
| minSdk | **23 (Android 6.0)** | Approved by project owner |
| targetSdk | **36 (Android 16)** | Meets Google Play's published minimum for new apps/updates from 2026-08-31 |
| Build Tools | AGP 9.5.1 default **36.0.0** | Let AGP manage its documented default unless a verified need requires pinning another version |

The approved initial version matrix is deliberately narrow. Do not upgrade one component independently: review the AGP/Gradle/JDK compatibility matrix together whenever any of these versions changes.

## 3. Compatibility evidence

### 3.1 Android Gradle Plugin

The official AGP 9.5.0 release notes (including the 9.5.1 patch line) identify:
- minimum/default Gradle: 9.6.0;
- minimum/default JDK: 17;
- default SDK Build Tools: 36.0.0;
- maximum supported Android API level: 37 (API 36 is selected for the current stable SDK package).

Source: [Android Gradle Plugin 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes).

### 3.2 Gradle and Java

AGP 9.5.1's documented runtime baseline is JDK 17. Gradle 9.x requires a JVM from 17 onward; use JDK 17 for the pinned build runtime.

Source: [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html).

### 3.3 Kotlin is intentionally not selected for the first baseline

Kotlin is not needed for the first implementation. Keep the first build Java-only to avoid introducing an additional plugin compatibility axis before the core architecture stabilizes. Revisit only when a concrete requirement exists and the official Kotlin/AGP compatibility table confirms the chosen pairing.

This is not a permanent rejection of Kotlin. Reconsider it only after the official compatibility table explicitly covers the selected AGP version and the project has a concrete reason to adopt Kotlin.

Source for future review: [Kotlin Gradle configuration and compatibility](https://kotlinlang.org/docs/gradle-configure-project.html).

### 3.4 JUnit choice

Use JUnit 4 for the first JVM core test baseline. The Great Anaconda harness already uses deterministic handwritten fakes and does not need a mocking library. Keep tests independent of Android instrumentation and the Android SDK.

JUnit 5 may be reconsidered later if a demonstrated project-wide requirement justifies its additional Gradle test-platform configuration. Do not add both frameworks.

## 4. Architecture constraints carried into the build

The build must preserve AUDIT-044's dependency direction:

```text
app ------> renderer
 |          input
 |          audio
 |          storage
 +--------> core

renderer --+
input -----+
audio -----+----> core
storage ---+

core ------> Java standard library only
core JVM tests -> core + handwritten fakes + JUnit 4
```

Rules:
1. The `core` module must be a plain JVM library and must not apply the Android plugin.
2. The Great Anaconda state machine and its tests must run without an emulator or Android SDK classes on the test classpath.
3. Android lifecycle, graphics, audio backends, and persistence adapters remain outside `core`.
4. Keep the original 240×320 portrait logical gameplay coordinate system. Toolchain choices must not alter gameplay geometry.
5. Preserve the two distinct slot-4 animator tick call sites identified by the audits; do not consolidate them during implementation.
6. Test fixtures remain synthetic. No proprietary resource payloads are added to test source sets.

## 5. Decisions intentionally left open

| Item | Why it remains open | Gate before implementation |
|---|---|---|
| Application name | Fixed by project owner | **Diamond Rush** (launcher/display name) |
| Application ID / namespace | Provided by project owner | `MeongDev.DIRUNA.GloftDIRU` (preserve exact spelling and capitalization) |
| minSdk | Depends on intended oldest device / Android release | State minimum supported Android version |
| targetSdk | Recommended baseline | API 36 / Android 16; review behavior changes before release |
| Resource packaging | Legal provenance of original game assets is unresolved | Complete resource-rights review |
| CI runner image | Must match the chosen JDK and Android SDK installation | Define reproducible runner and SDK package set |
| Native code / NDK | No audited requirement currently justifies it | Add only if a concrete subsystem requires native code |

Do not set `targetSdk` equal to `compileSdk` automatically. They serve different purposes and require separate decisions.

## 6. Application identity

The launcher/display name is **Diamond Rush**. The project owner supplied `MeongDev.DIRUNA.GloftDIRU`; preserve its exact spelling and capitalization as the application ID and namespace. Verify the identifier during Gradle configuration and do not silently normalize it.

## 7. Implementation sequence after approval

1. Use Java 17, Groovy DSL, Gradle 9.8.2, AGP 9.5.1, compile SDK 37, minSdk 23, and targetSdk 36 as the initial baseline.
2. Preserve the approved application name and identifier.
3. Create the single Gradle root and wrapper.
4. Add a plain JVM `core` module and a minimal JUnit 4 test.
5. Add the Android `app` module and verify its configuration independently.
6. Add renderer/input/audio/storage modules only when their interfaces and ownership are sufficiently specified.
7. Add an independent CI workflow that runs core JVM tests and validates the Android app configuration.
8. Only then implement the Great Anaconda fake harness and regression suite GA-001..GA-028.

## 8. Acceptance criteria for DEV-001A

- [x] Current official AGP/Gradle/JDK compatibility checked.
- [x] Current Kotlin/AGP compatibility checked; Kotlin excluded from the first baseline.
- [x] Core language and JVM test framework recommendation recorded.
- [x] Build/runtime architecture constraints carried forward from AUDIT-044.
- [x] Project owner approved Android 6.0 / API 23 as minSdk.
- [x] Application name and identifier recorded.
- [x] targetSdk 36 selected as the initial Google Play-compatible baseline; recheck before release.

**Status: toolchain and initial Android debug build validated by GitHub Actions run #5.**

## 9. Next work item

Proceed to **DIRU-NA-DEV-001B — Gradle Root and Wrapper** using the approved SDK levels and pinned toolchain. Verify the Gradle configuration and Android package identifier before marking the build baseline complete.
