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

## Deferred family
Opcodes 25 and 26 remain deferred. Their path includes foreground/background writes followed by fall-through into the 5/8/9 callback path and labels 27/2. They require an ordered composite-effect model and branch-condition tests; implementing them as independent handlers would be incorrect.

## Acceptance criteria
- Source label sequence and conditional exits have explicit tests.
- Operand reads are gated by the existing descriptor and physical record layout.
- Stage writes use `DemoStageMutationAdapter` only.
- Unsupported or unverified paths fail closed.
- Core JVM tests and Android debug build pass on the exact commit.
- The trace-only registry remains available for regression comparison.

## Non-goals
This milestone does not claim full gameplay semantics, player movement, tile interpretation, completion timing, rendering, or playable APK parity.
