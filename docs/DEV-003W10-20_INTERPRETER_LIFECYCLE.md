# DEV-003W10.20 — Interpreter lifecycle and session traversal

## CI baseline

W10.19 is green on GitHub Actions run #804 (Core JVM tests and Android debug build), commit `bd3ec60e930cf08fe4e494fc0e4685f9e9624cac`.

## Correctness repair before lifecycle work

The dispatch plan previously described opcode 17 and 18 as falling through label 14. Their operand descriptors identify label 15. The plan is corrected to:
- 16 → 14 → 12
- 17 → 15 → 12
- 18 → 15 → 12

`DemoExecutionContext` now rejects a non-skipped plan whose complete label sequence differs from the descriptor. This prevents a trace-only engine from silently accepting mismatched metadata.

## Session implementation

`DemoExecutionSession` traverses normalized top-level records and children inside one-level opcode-0 groups. It:
- exposes the current record and cursor;
- dry-runs exactly one record per successful step;
- advances tick/cursor only after successful dispatch;
- moves to the next top-level buffer after its records are consumed;
- skips empty groups as containers, not executable records;
- passes the runtime skip flag into the existing dispatch plan;
- resets traversal and runtime counters together.

## Safety boundary

This remains a dry-run lifecycle. It does not mark records complete, modify normalized bytes, mutate stage state, invoke rendering/audio/timing, or implement gameplay effects. If an opcode lacks an audited descriptor/handler, the step throws and leaves the cursor and tick unchanged.

## Tests

Session tests cover top-level ordering, packed-group child ordering, empty groups, unsupported opcode cursor stability, reset, skip propagation, and empty scripts.

## Follow-up

Validate the lifecycle against captured demo fixtures and source control-flow conditions before introducing semantic opcode handlers or gameplay mutation.
