# DEV-003W10.14 — Interpreter runtime state foundation

## Purpose

Create a mutable runtime-state layer over immutable normalized script buffers. This milestone deliberately does not execute opcodes or mutate a stage.

## State model

`DemoInterpreterRuntimeState` owns a detached copy of every normalized record. Each `RecordState` stores:
- immutable normalized bytes, exposed only through defensive copies;
- completion state;
- reveal counter;
- scroll position;
- child states for one-level opcode-0 groups.

Group completion is derived: every child must be complete. An empty group is complete, matching the legacy all-children loop semantics observed in the source audit. Group completion cannot be assigned directly.

## Lifecycle

- Initial selection is buffer zero, or -1 for an empty script.
- `advanceTick()` increments the global tick once and never changes selection.
- `selectNextBuffer()` changes selection only when a next buffer exists and resets tick to zero.
- `reset()` clears all buffer/child runtime fields, tick, and skip flag, then selects the first buffer.
- Tick overflow is rejected rather than wrapping silently.

## Group format validation

The runtime constructor validates the normalized group header, child-length table, aggregate payload size, and exact end boundary before constructing child states. Nested groups remain rejected, consistent with the current wire decoder's supported one-level contract.

## Tests

Synthetic JVM tests cover detached normalized bytes, ordinary completion, all-child group completion, empty groups, reset recursion, tick/selection separation, skip reset, empty scripts, and malformed group lengths.

## Explicit non-goals

No opcode dispatch, stage adapter calls, camera update, sprite resolution, renderer mutation, time-gated reveal scheduling, or Android dependencies are introduced. Runtime state is not yet wired into gameplay.

## Next gate

Before opcode execution, add lifecycle edge-case tests and source-backed dispatch descriptors. The executor must consume this state without mutating normalized buffers and must preserve verified fall-through order.
