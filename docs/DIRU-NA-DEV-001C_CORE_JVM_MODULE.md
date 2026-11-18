# DIRU-NA-DEV-001C — Platform-independent Core JVM Module

**Status:** Core module and initial test source committed; execution validation pending  
**Date:** 2026-11-18  
**Repository:** `yogiags56/Project-DIRU-NA`

## 1. Purpose

Establish the first platform-independent Java module and its JVM unit-test source set. This stage proves the intended dependency boundary in source/build configuration; it does not claim that Gradle tests have run successfully.

## 2. Build decisions

- Module: `:core`
- Plugin: Gradle `java-library`
- Java toolchain: 17
- Test framework: JUnit 4.13.2
- Android SDK dependency: none in `:core`
- Android plugin dependency: none in `:core`

The root settings now include `:app` and `:core`. The app remains an Android application module; core remains a plain JVM library.

## 3. Initial core contract

Added `LogicalViewport`, an immutable Java record that:
- stores positive logical width and height;
- exposes `INITIAL_PORTRAIT = 240×320`;
- rejects zero or negative dimensions.

This is only a coordinate-space value object. It does not implement device scaling, camera transforms, adaptive aspect ratios, rendering, or gameplay.

## 4. Initial test coverage

Added `LogicalViewportTest` with four test cases:
1. the initial portrait dimensions are 240×320;
2. positive dimensions can be represented;
3. zero width is rejected;
4. negative height is rejected.

Tests use JUnit 4 and contain no Android classes, proprietary resources, mocks, or emulator requirements.

## 5. Architecture boundary

```text
app (Android)
  |
  +----> core (plain Java library)
              |
              +---- Java standard library

core/src/test
  +---- JUnit 4
```

The core module must remain free of Android SDK imports. Future gameplay entities, encounter state machines, animation logic, and deterministic simulation contracts belong here only after their shared interfaces have been audited.

## 6. Validation status

**Source and Gradle configuration committed; tests not yet executed.**

No claim is made that `./gradlew :core:test`, `:core:build`, Android compilation, or APK assembly has passed. A Gradle/JDK execution environment is still required to validate dependency resolution and actual test results.

## 7. Next work item

**DIRU-NA-DEV-001D — Build and Test Validation / CI Baseline.** Run the wrapper under JDK 17, resolve any real configuration or compatibility failures, execute `:core:test`, and only then add an independent CI workflow that records the result.
