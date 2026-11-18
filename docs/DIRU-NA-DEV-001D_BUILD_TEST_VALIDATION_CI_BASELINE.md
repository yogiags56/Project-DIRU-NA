# DIRU-NA-DEV-001D — Build and Test Validation / CI Baseline

**Status:** CI workflow committed; hosted validation blocked by an AGP plugin version-resolution error, fix in progress  
**Date:** 2026-11-18  
**Repository:** `yogiags56/Project-DIRU-NA`

## 1. Purpose

Add an independent GitHub Actions workflow to validate the plain JVM core and assemble the Android debug variant. This is a validation pipeline, not evidence of a successful run until GitHub Actions reports a completed passing job.

## 2. Workflow

File: `.github/workflows/android.yml`

Triggers:
- push to `main`;
- pull requests targeting `main`.

Environment:
- Ubuntu runner;
- Temurin JDK 17;
- Android SDK platform 36;
- Android Build Tools 36.0.0;
- Gradle Wrapper pinned to 9.8.2.

Tasks:
1. `./gradlew --no-daemon --stacktrace :core:test`
2. `./gradlew --no-daemon --stacktrace :app:assembleDebug`
3. Upload `app-debug.apk` as a seven-day workflow artifact.

The workflow has read-only repository content permission. It does not publish to a store or release channel.

## 3. Dependency boundary update

The Android app now declares `implementation project(':core')`. Its temporary launcher shell reads the initial logical viewport from `LogicalViewport.INITIAL_PORTRAIT`, proving that the app consumes the core contract rather than duplicating its dimensions.

The `:core` module remains a Java library with JDK 17 toolchain and JUnit 4. It must not acquire Android SDK or Android plugin dependencies.

## 4. Test refinement

The positive-dimension test now verifies the width and height values of a non-default 320×240 viewport rather than comparing two identical constructor expressions.

Four deterministic tests remain:
- initial 240×320 portrait dimensions;
- acceptance and storage of other positive dimensions;
- zero-width rejection;
- negative-height rejection.

## 5. Validation status

**Workflow committed; hosted run not yet observed.**

Do not mark validation complete until the workflow has:
- completed successfully on the commit;
- passed `:core:test`;
- passed `:app:assembleDebug`;
- uploaded the expected debug APK artifact.

A successful debug build is not a release-ready game. The launcher remains a temporary shell; gameplay, rendering, input, audio, persistence, and resource integration are not implemented.

## 6. Next work item

After the first successful CI run, proceed to **DIRU-NA-DEV-002 — Core Game Loop and Platform Ports**. Before that, fix any failures reported by CI and record the exact run URL, commit, test count, and APK artifact.
