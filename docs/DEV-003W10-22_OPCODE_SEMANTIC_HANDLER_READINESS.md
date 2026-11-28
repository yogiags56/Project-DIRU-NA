# DEV-003W10.22 — Opcode semantic handler readiness

## Entry gate
W10.21 multi-record regression validation is accepted only after the exact branch head passes CI. The terminal-state fix is validated by workflows #836 and #837 on commit `350900c7e50a30c3b2715c7ff8f41e7ec49d1950`.

## Purpose
Prepare the first source-backed semantic handler family without replacing the dry-run registry prematurely. The current `DemoOpcodeHandlerRegistry.dryRunRegistry()` is trace-only by design; it must remain the default until semantic behavior has dedicated tests.

## Pinned source evidence
Legacy interpreter source: `kubikaugustyn/DiamondRushSource`, revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`, files `src/main/java/c.java` and `src/main/java/i.java`.

The W10.12 audit records the immediate callback contract:
- `i.aVoid(short x, short y, byte playerLayer, int backgroundValue)` writes player-layer at `[x][y]`, then background at `[x][y]`.
- Opcode 5 supplies signed-short x/y, byte +6 as player-layer, and background value 0.
- Opcode 8 falls through to opcode 9; both supply signed-short x/y, player-layer 0, and signed-short +6 as background value.
- The callback's coordinate convention is `[x][y]`; invalid coordinates must fail before any partial native write.

## First semantic family candidate: stage-cell callback path
Candidate entry opcodes: 5, 8, and 9. These are not independent isolated effects:
- The source switch has fall-through paths (5 → 8 → 9 → 27 → 2; 8 → 9 → 27 → 2; 9 → 27 → 2).
- The implementation must preserve reached-label order and conditional early exits.
- Repeated writes to the same cell are observable and must not be collapsed without source proof.
- The existing `DemoStageMutationAdapter` supplies the narrow bounds-checked write boundary, but it is not yet invoked by the interpreter.

## Preconditions before enabling mutation
1. Preserve the current dry-run engine and registry as a no-mutation baseline.
2. Add a semantic execution context that separates decoded operands, runtime tick, and stage adapter.
3. Prove signed-short coordinate conversion and signed/unsigned byte behavior against the pinned source.
4. Model exact label ordering and source-level conditional return/break behavior, not just the static dispatch list.
5. Add tests that assert the full ordered write trace for entry opcodes 5, 8, and 9.
6. Add invalid-coordinate tests proving zero writes occur before bounds validation.
7. Keep semantic execution opt-in; no Android renderer, camera, audio, or real retail stage wiring.

## First opt-in implementation slice

Implemented in `DemoStageCellCallbackExecutor` with isolated tests in `DemoStageCellCallbackExecutorTest`:
- Opcode 5 reads signed-short x/y and the signed byte at +6, applies the opcode-5 callback (player-layer then background=0), then preserves the audited fall-through callback at label 9 using the signed-short value at +6.
- Opcode 8 executes the shared label-9 callback using signed-short x/y and signed-short +6.
- Opcode 9 executes the same immediate callback directly.
- Each callback routes through `DemoStageMutationAdapter`; its bounds check runs before either cell write.
- The executor is explicitly opt-in and accepts only entry opcodes 5, 8, and 9. It does not replace the dry-run registry or claim to execute downstream opcode-27 completion / opcode-2 dialogue semantics.

The ordered callback trace is asserted in unit tests, including opcode-5 repeated writes, signed-short negative coordinate rejection, and fail-closed behavior for opcode 25. The opcode-25/26 composite sequence remains deferred until a dedicated ordered-effect executor models its foreground, background, completion, callback, and conditional downstream paths.

## Deferred family
Opcodes 25 and 26 remain deferred. Their path includes foreground/background writes followed by fall-through into the 5/8/9 callback path and labels 27/2. They require an ordered composite-effect model and branch-condition tests; implementing them as independent handlers would be incorrect.

## Opcode 25/26 composite implementation slice

Added `DemoStageCompositeCellExecutor` and isolated regression tests:
- Entry opcode 25 performs the foreground write using the source's signed-byte high/low expression, then follows through opcode 26.
- Entry opcode 26 performs the u32 background write, then both entry paths continue through opcode 5, empty label 8, and callback label 9.
- Opcode 27's raw +7 gate is evaluated after the cell callbacks. Nonzero marks label 27 complete and returns before dialogue; zero reaches the opcode-2 boundary.
- All stage writes use `DemoStageMutationAdapter`; signed-short coordinates are validated before the first write.
- The executor reports completion labels without mutating normalized record bytes. Opcode-2 dialogue timing/text behavior remains deferred.

Important physical-layout constraint: the legacy opcode-26 u32 read at +6 is wider than the normalized 8-byte opcode-25/26 leaf. In packed opcode-0 groups, this read can consume bytes from the following sibling record. Tests therefore construct an actual packed group and assert the shared-backing read rather than padding a leaf into an artificial 10-byte record. The operand descriptor explicitly declares this fall-through u32 read for opcode 25.

## Acceptance criteria
- Source label sequence and conditional exits have explicit tests.
- Operand reads are gated by the existing descriptor and physical record layout.
- Stage writes use `DemoStageMutationAdapter` only.
- Unsupported or unverified paths fail closed.
- Core JVM tests and Android debug build pass on the exact commit.
- The trace-only registry remains available for regression comparison.

## Non-goals
This milestone does not claim full gameplay semantics, player movement, tile interpretation, completion timing, rendering, or playable APK parity.
