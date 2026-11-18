# DIRU-NA-AUDIT-044 — Great Anaconda Test Harness Implementation Readiness & Core Project Dependency Map

## 1. Purpose

This audit determines whether Project DIRU-NA currently has enough project-level structure to implement the Great Anaconda JVM test harness in its intended location. It also defines the minimum dependency and module map required before test code is introduced.

This is a repository-readiness audit, not a code implementation. No Gradle files, source classes, test classes, workflow files, or Android artifacts are created by this document.

## 2. Repository inspection

Repository inspected: `yogiags56/Project-DIRU-NA`.

### 2.1 Existing project declarations

The repository README describes the following as the **planned** target layout:

```text
app/
core/
renderer/
input/
audio/
storage/
resources/
recovery/
docs/
tools/
```

The README explicitly states that this is a target layout, not a claim that the modules have already been implemented.

The Development Isolation document also marks these items incomplete:
- independent Android/Gradle skeleton;
- independent CI;
- reproducible build evidence.

### 2.2 Build and source-file probes

The following conventional project paths were checked through the repository file interface and returned not found (HTTP 404):

- `settings.gradle`
- `settings.gradle.kts`
- `build.gradle`
- `build.gradle.kts`
- `gradlew`
- `gradle/libs.versions.toml`
- `app/build.gradle`
- `core/build.gradle`
- `pom.xml`
- `package.json`
- `Makefile`
- `.github/workflows/android.yml`

Repository code search for `gradle`, `AndroidManifest.xml`, `GreatAnacondaEncounter`, `src/test`, and `workflow` returned no indexed matches.

These probes support the conclusion that no project build/test skeleton is currently available at the expected conventional paths. They do not prove that every possible nonstandard file path is absent; a complete tree listing was not available in this inspection.

### 2.3 Readiness verdict

**Not ready for test-code implementation in the intended project structure.**

Reason: there is no verified Gradle root, module declaration, source package namespace, language choice, test framework, or CI convention to attach the harness to.

Do not create a standalone Gradle project inside `tools/` or a temporary repository root just to begin GA tests. That would create a second build convention and risk moving or rewriting tests later.

## 3. Decisions that can be made now

The following architecture decisions are sufficiently independent of the eventual language/build versions:

1. Great Anaconda logic belongs in the platform-independent core module.
2. Tests belong in that same module's JVM unit-test source set, not Android instrumentation tests.
3. Tests use handwritten fakes; a mocking framework is not required.
4. Test inputs are synthetic and immutable; each test receives a fresh mutable fake world.
5. Animator operations and stage-grid writes must be recorded in order.
6. The encounter core must not import Android classes, graphics APIs, Android lifecycle types, or audio backends.
7. Resource decoding and production resource payloads are outside the unit-test fixture layer.
8. The Native project uses its own build files, package namespace, CI, and output directories, independent of the archived project.

## 4. Proposed module and dependency map

Target structure after DIRU-NA-DEV-001 establishes the build:

```text
Project-DIRU-NA/
  settings.gradle[.kts]
  build.gradle[.kts]
  gradle/
    wrapper/
    libs.versions.toml              # optional, if version catalog is selected
  app/
    build.gradle[.kts]
    src/main/
      AndroidManifest.xml
      ...
  core/
    build.gradle[.kts]
    src/main/
      java|kotlin/<new-package>/core/
        encounter/greatanaconda/
    src/test/
      java|kotlin/<new-package>/core/
        encounter/greatanaconda/
  renderer/
  input/
  audio/
  storage/
  docs/
  tools/
  .github/workflows/
```

The exact language directory and package name remain pending until the project language and namespace are approved.

### 4.1 Dependency direction

```text
app  ---> renderer
 |       input
 |       audio
 |       storage
 |       core
 |
renderer ---> core (read-only render-facing state/API)
input    ---> core (commands/input abstraction)
audio    ---> core (event interface or audio port)
storage  ---> core (snapshot/save interfaces)
core     ---> Java/Kotlin standard library only

core JVM tests ---> core production API + test fakes + chosen unit-test framework
```

Rules:
- `core` must not depend on `app`, `renderer`, `input`, `audio`, or `storage`.
- The Great Anaconda unit tests must not require Android SDK classes or an emulator.
- The renderer may consume immutable encounter/render state but must not own or mutate the encounter state machine.
- Audio is represented as an event/port; tests capture sound IDs rather than playing sounds.
- Storage serializes an explicit snapshot; it must not serialize Android objects or live animator implementations.

### 4.2 Minimum Great Anaconda production types

Create only after the core module/package conventions are established:

- `GreatAnacondaEncounter`
- `GreatAnacondaState`
- `GreatAnacondaStateData`
- `GreatAnacondaStagePort`
- `GreatAnacondaAnimatorPort`
- `GreatAnacondaSnapshot`
- `GreatAnacondaResourceBinding`
- `GreatAnacondaUpdateCoordinator` (or a narrowly scoped shared coordinator adapter)
- `GreatAnacondaEvent`
- `GreatAnacondaTraceSink`

Avoid creating a second generalized game-engine abstraction just for this boss. Reuse shared interfaces only after they exist and their responsibilities are understood.

### 4.3 Minimum test types

- `GreatAnacondaEncounterTest`
- `FakeGreatAnacondaStage`
- `FakeGreatAnacondaAnimator`
- `RecordingGreatAnacondaTrace`
- `GreatAnacondaFixtures`
- `GreatAnacondaScenarioVectors`

The test source set should not contain proprietary resource binaries. Synthetic metadata belongs in source-controlled test fixtures with explicit labels.

## 5. Build and test dependency choices

The following are recommendations, not current repository settings.

### 5.1 Build system

Use a single Gradle root for the Native Android project when DEV-001 begins. Use the Gradle wrapper and pin the Gradle/Android Gradle Plugin/JDK versions together in a reproducible toolchain decision record.

Do not select exact versions in this audit because no SDK/JDK/Gradle compatibility matrix has been approved or verified yet.

### 5.2 Language

Java or Kotlin can both support the architecture. The current repository does not establish a language convention.

Choose one language for the first core implementation and keep test code in the same language unless a documented reason requires otherwise. Do not mix Java and Kotlin solely to mirror the decompiled Java candidate.

### 5.3 Unit-test framework

Choose one JVM unit-test framework for the core module once the language and Gradle conventions are established.

Recommended minimal policy:
- one test framework only;
- no mocking library initially;
- no Android instrumentation dependency for encounter logic;
- deterministic tests with handwritten fakes;
- test reports generated by Gradle and retained in CI.

JUnit 4 is a conservative option if Android/Gradle integration simplicity is prioritized. JUnit 5 is also viable for a JVM core module but requires deliberate Gradle test-platform configuration. The final choice is pending the project toolchain decision.

### 5.4 Avoid unnecessary dependencies

The Great Anaconda harness does not need:
- coroutine libraries;
- dependency injection frameworks;
- mocking frameworks;
- JSON serialization libraries;
- game-engine libraries;
- AndroidX;
- image/audio decoders.

Add dependencies only when a concrete module requirement exists and record why they are needed.

## 6. Required project decisions before implementation

| Decision | Current status | Required output |
|---|---|---|
| Root Gradle DSL (Groovy/Kotlin) | Open | Build convention |
| JDK/toolchain version | Open | Reproducible JDK target |
| Android Gradle Plugin/SDK | Open | Compatible Android build matrix |
| Core language (Java/Kotlin) | Open | Source/test language |
| Application ID and package namespace | Open | Unique Native namespace |
| Module inclusion strategy | Open | Initial settings.gradle |
| JVM test framework | Open | Unit-test dependency and task config |
| CI workflow | Open | Independent build/test pipeline |
| Resource licensing gate | Open | Allowed runtime resource inputs |
| Shared engine port contracts | Partial design only | Interfaces validated against core architecture |

## 7. Implementation order

The safe order is:

1. **DEV-001A — Project toolchain decision:** choose JDK, Gradle, Android Gradle Plugin, compile/target SDK, language, and namespace.
2. **DEV-001B — Gradle root and wrapper:** create the root build and settings; verify Gradle configuration.
3. **DEV-001C — Core JVM module:** add a platform-independent core module and one trivial JVM test to prove the test task works.
4. **DEV-001D — CI baseline:** run core tests and Android configuration checks in an independent workflow.
5. **DEV-001E — Shared core contracts:** define stage, animator, event, snapshot, and update coordinator interfaces based on broader game-loop findings.
6. **DEV-002A — Great Anaconda fake harness:** implement the fake stage, fake animator, trace sink, and fixture builders.
7. **DEV-002B — Pure encounter model:** port state transitions incrementally against the deterministic vectors.
8. **DEV-002C — Regression suite:** implement GA-001..GA-028 and the GA-X extensions.
9. Integrate resource/render/audio adapters only after their legal provenance and technical contracts are established.

Do not label DEV-001 complete until the project builds reproducibly and the CI baseline passes. Do not label Great Anaconda implementation complete until its regression suite passes and authorized runtime comparison has been performed.

## 8. Risks and controls

| Risk | Control |
|---|---|
| Premature standalone test project | Wait for the official Gradle root and core module |
| Android dependency leaks into core | Enforce module dependency direction and JVM-only core tests |
| Wrong language/package assumption | Decide and record before creating source files |
| Tests validate invented behavior | Keep every scenario linked to AUDIT-042 source anchors |
| Synthetic fixtures mistaken for original assets | Label every fixture synthetic; keep proprietary payloads out |
| Animator double tick accidentally deduplicated | Separate shared and encounter call-site labels in fake animator |
| State-5 render gate incorrectly moved into update | Keep render handshake as a separate API/event |
| Checkpoint test over-asserts | Assert only source-proven restored fields |
| Build versions drift | Pin wrapper/toolchain and record CI environment |

## 9. Current status

**Verified:**
- README describes module structure as planned, not implemented.
- Development Isolation marks Gradle skeleton and CI as incomplete.
- Expected Gradle root/module files were not found at conventional paths.
- Code search returned no indexed matches for build/test skeleton identifiers.
- Great Anaconda architecture and test data are documented through AUDIT-043.

**Not present/verified:**
- Gradle root/wrapper;
- Android app module;
- core JVM module;
- source package namespace;
- unit-test framework;
- test classes;
- CI workflow;
- APK or Android runtime.

This audit does not create any of those artifacts.

## 10. Completion and next step

AUDIT-044 is complete as a readiness assessment and dependency map.

Recommended next work item: **DIRU-NA-DEV-001A — Native Android Toolchain Decision Record**. This should compare supported JDK, Gradle, Android Gradle Plugin, SDK, language, and test-framework combinations before the first build files are committed.

Great Anaconda test implementation remains blocked by the missing project-level skeleton. The next productive action is to establish that skeleton correctly, not to write tests into an unapproved directory.
