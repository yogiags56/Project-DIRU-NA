# DIRU-NA-DEV-001B — Gradle Root and Wrapper

**Status:** Initial build skeleton committed; execution validation pending  
**Date:** 2026-11-18  
**Repository:** `yogiags56/Project-DIRU-NA`

## 1. Purpose

Create the independent Gradle root, pin a reproducible Gradle Wrapper, and add the smallest Android launcher shell for the Native Android project. This does not implement gameplay, the core engine, Great Anaconda, rendering, input, audio, or persistence.

## 2. Pinned build configuration

- Gradle Wrapper: 9.8.2
- Android Gradle Plugin: 9.6.0
- Gradle runtime JDK: 17
- Java source/target compatibility: 17
- Compile SDK: 37
- minSdk: 23 (Android 6.0)
- targetSdk: 36 (Android 16)
- Application label: Diamond Rush
- Application ID and namespace: `MeongDev.DIRUNA.GloftDIRU`

AGP 9.6.0 requires Gradle 9.8.2, uses JDK 17, and supports up to API 37. The Gradle distribution SHA-256 is pinned in `gradle-wrapper.properties`. Official checksums are published by Gradle.

## 3. Files introduced

- `settings.gradle` — root project and app module inclusion.
- `build.gradle` — pinned Android application plugin declaration.
- `gradle.properties` — conservative Gradle runtime settings.
- `gradle/wrapper/gradle-wrapper.properties` — pinned Gradle distribution and checksum.
- `gradle/wrapper/gradle-wrapper.jar` — official Gradle 9.8.2 wrapper JAR.
- `gradlew`, `gradlew.bat` — official Gradle 9.8.2 launch scripts.
- `app/build.gradle` — Android app configuration.
- `app/src/main/AndroidManifest.xml` — launcher activity declaration.
- `app/src/main/java/MeongDev/DIRUNA/GloftDIRU/MainActivity.java` — temporary non-gameplay launcher shell.

## 4. Deliberate scope boundaries

- The launcher only displays a foundation/status message and the 240×320 logical viewport baseline.
- No game code, proprietary assets, emulator, or external game runtime is included.
- No `core` module or Great Anaconda test harness is introduced in this stage.
- No CI workflow or APK is claimed.
- The exact owner-provided package identifier is preserved; Gradle configuration must verify whether it is accepted without normalization.

## 5. Validation status

**Committed, not yet executed.**

The repository now contains the wrapper and Android app configuration. A successful `./gradlew tasks`, `./gradlew :app:assembleDebug`, or device launch has not yet been observed in this work item. SDK 37 and JDK 17 must be installed in the execution environment before Android compilation can be verified.

## 6. Next work item

**DIRU-NA-DEV-001C — Platform-independent Core JVM Module.** Add a plain Java library module, pin Java 17 toolchain behavior, add JUnit 4, and prove the core unit-test task runs independently of Android instrumentation.
