# DEV-003W10.19 — Opcode execution engine foundation (dry-run)

## Scope

This milestone adds a descriptor-gated operand reader, execution context,
handler contract, audited handler registry, and dry-run dispatcher. It does
not implement gameplay behavior.

## Components

- `DemoOperandAccessReader`: allows only offset/kind pairs present in the
  entry opcode's `DemoOpcodeOperandDescriptor`; delegates physical bounds
  to `DemoLegacyBufferLayout.RecordSlot`.
- `DemoExecutionContext`: immutable record/descriptor/dispatch inputs with
  an append-only switch-label trace.
- `DemoOpcodeHandler`: callback contract for a reached switch label.
- `DemoOpcodeHandlerRegistry`: registers only opcodes with both an operand
  descriptor and a verified dispatch plan.
- `DemoOpcodeDryRunEngine`: traverses the existing `DemoOpcodeDispatchPlan`
  and returns a read-only trace. It does not call the operand reader, runtime
  mutation adapter, rendering, audio, or timing code.

## Memory semantics

Logical record bounds remain separately observable from physical backing
bounds. A declared U32 read for opcode 25 at +6 can cross the child boundary
inside a packed group, while the physical group array remains the hard limit.
The same read against an isolated top-level leaf fails at its physical end.

## Safety boundaries

The registry's default handler only records switch labels. No handler mutates
stage data, record bytes, completion state, or interpreter counters. Unknown
or descriptor-less opcodes are rejected rather than assigned guessed behavior.
Skip behavior is delegated to the already-audited dispatch plan allow-list.

## Tests

Tests cover declared read gating, undeclared read rejection, packed-group
cross-child access, opcode-25 dry-run label order, skip allow-list behavior,
unsupported opcode rejection, and immutable traces.

## Follow-up

Before gameplay execution, replace trace-only handlers one opcode family at a
time with source-derived semantics and tests against real demo fixtures.
