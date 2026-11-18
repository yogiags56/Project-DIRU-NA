# DIRU-NA-DEV-001A — Native Android Toolchain Decision Record

**Status:** Proposed baseline; implementation gate remains open  
**Date:** 2026-11-18  
**Repository:** `yogiags56/Project-DIRU-NA`

## 1. Purpose and scope

This record establishes a reproducible candidate toolchain for the first Native Android project skeleton and the platform-independent JVM core. It follows AUDIT-044 and uses current official compatibility information checked on 2026-11-18.

This is a decision record, not a build implementation. No Gradle files, wrapper, source code, tests, CI workflow, SDK configuration, or APK are created by this document.

## 2. Recommended initial toolchain

| Component | Proposed baseline | Rationale / status |
|---|---|---|
| Build system | Gradle Wrapper | One pinned build entry point for local and CI use |
| Gradle | **9.6.0** | Minimum required by Android Gradle Plugin 9.4 |
| Android Gradle Plugin | **9.4.0** | Current stable release identified in the official Android release notes |
| Gradle runtime JDK | **JDK 17** | AGP 9.4.0 lists JDK 17 as its minimum/default |
| Core language | **Java 17** | Lowest-friction first implementation; matches the Java source audit and avoids introducing a second language before core contracts stabilize |
| Android source language | **Java 17 initially** | Keep the first app/core implementation single-language |
| Build-script DSL | **Groovy DSL** | Conservative, widely documented Gradle convention; no Kotlin DSL requirement for this project |
| Compile SDK | **API 37** | AGP 9.4.0 supports up to API 37 |
| JVM unit tests | **JUnit 4** | Small, mature dependency for the pure JVM core; handwritten fakes remain the default |
| Android namespace / application ID | **Pending** | Must be selected deliberately; do not infer one from a GitHub username or project codename |
| minSdk | **Pending device-support decision** | Must follow the oldest Android version/device the project intends to support |
| targetSdk | **Pending release/distribution decision** | Must be selected against current Android behavior changes and distribution requirements |
| Build Tools | AGP 9.4.0 default **36.0.0** | Let AGP manage its documented default unless a verified need requires pinning another version |

The proposed version matrix is deliberately narrow. Do not upgrade one component independently: review the AGP/Gradle/JDK compatibility matrix together whenever any of these versions changes.

## 3. Compatibility evidence

### 3.1 Android Gradle Plugin

The official AGP 9.4.0 release notes identify:
- minimum/default Gradle: 9.6.0;
- minimum/default JDK: 17;
- default SDK Build Tools: 36.0.0;
- maximum supported Android API level: 37.

Source: [Android Gradle Plugin 9.4.0 release notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes).

### 3.2 Gradle and Java

Gradle's current compatibility matrix states that Gradle 9.6.0 can run on JDK 17. Gradle 9.x requires a JVM from 17 onward, and Java 17 is a supported runtime.

Source: [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html).

### 3.3 Kotlin is intentionally not selected for the first baseline

The Kotlin Gradle Plugin compatibility table currently lists Kotlin 2.4.20–2.4.21 as fully supported through AGP 9.3.1, not AGP 9.4.0. This record therefore recommends Java-only for the first implementation rather than mixing an unverified Kotlin/AGP pairing into the initial skeleton.

This is not a permanent rejection of Kotlin. Reconsider it only after the official compatibility table explicitly covers the selected AGP version and the project has a concrete reason to adopt Kotlin.

Sources:
- [Kotlin Gradle configuration and compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Android Kotlin / D8 / R8 compatibility](https://developer.android.com/build/kotlin-support).

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
| Namespace and application ID | Must be unique and intentionally owned by the project | Approve exact reverse-domain identifier |
| minSdk | Depends on intended oldest device / Android release | State minimum supported Android version |
| targetSdk | Affects runtime behavior and release compliance | Decide distribution channel and validate current target requirements |
| Resource packaging | Legal provenance of original game assets is unresolved | Complete resource-rights review |
| CI runner image | Must match the chosen JDK and Android SDK installation | Define reproducible runner and SDK package set |
| Native code / NDK | No audited requirement currently justifies it | Add only if a concrete subsystem requires native code |

Do not set `targetSdk` equal to `compileSdk` automatically. They serve different purposes and require separate decisions.

## 6. Implementation sequence after approval

1. Confirm Java 17, Groovy DSL, Gradle 9.6.0, AGP 9.4.0, and compile SDK 37 as the initial baseline.
2. Approve namespace/application ID, minSdk, and targetSdk.
3. Create the single Gradle root and wrapper.
4. Add a plain JVM `core` module and a minimal JUnit 4 test.
5. Add the Android `app` module and verify its configuration independently.
6. Add renderer/input/audio/storage modules only when their interfaces and ownership are sufficiently specified.
7. Add an independent CI workflow that runs core JVM tests and validates the Android app configuration.
8. Only then implement the Great Anaconda fake harness and regression suite GA-001..GA-028.

## 7. Acceptance criteria for DEV-001A

- [x] Current official AGP/Gradle/JDK compatibility checked.
- [x] Current Kotlin/AGP compatibility checked; Kotlin excluded from the first baseline.
- [x] Core language and JVM test framework recommendation recorded.
- [x] Build/runtime architecture constraints carried forward from AUDIT-044.
- [ ] Project owner confirms the proposed toolchain baseline.
- [ ] Namespace/application ID, minSdk, and targetSdk are approved.

**Status: decision record drafted; not yet an implemented or approved build configuration.**

## 8. Next work item

After the remaining project-level choices are approved, proceed to **DIRU-NA-DEV-001B — Gradle Root and Wrapper**. Do not create build files or claim a successful build before that work item is authorized and actually verified.
