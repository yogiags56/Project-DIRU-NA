# DEV-003W10.3 — Demo script execution and resource lifecycle

## Source identity and evidence boundary
Pinned decompiled source:
- `kubikaugustyn/DiamondRushSource/src/main/java/c.java`
- `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- revision `215c71d04057d66a1b08419b1a4b1043ae3e0881`

Static source audit only. No original binary assets or runtime were executed. This document records observed control flow and explicitly separates update, execution, render and cleanup.

## 1. Correct method/overload map

The obfuscated `c` class is a demo/script interpreter. Method names alone are ambiguous; signatures and call sites are required.

| Method | Observed role |
|---|---|
| `aVoid(int tag)` | Load and parse one tagged demo.f chunk; populate this instance's script buffers and shared sprite cache |
| `aVoid()` | Time-gated interpreter update: returns if no active buffer or less than 1000 ms elapsed; processes active record(s) |
| `bVoid()` | Execute one interpreter tick/frame over active buffer, then increment `bInt` |
| `aBoolean()` | Completion predicate: active record's done byte, or all children in a nested group done |
| `bByteArr()` | Select next script buffer; resets `bInt`; returns null at end |
| `aByteArr()` | Reset all script records and interpreter/UI state, then select first buffer |
| `aVoid(Graphics)` | Render current demo records and optional overlay; may mutate overlay drawing state |
| `aVoid(byte[],int)` (private) | Apply per-record time/position/text update rules |
| `bVoid(byte[],int)` (private) | Reset record/nested-group runtime state |
| `cVoid(byte[],int)` (private) | Execute record behavior for the current tick |

The class has a static tick interval `c.aLong = 1000L`. The public no-argument `aVoid()` compares wall-clock milliseconds against `bLong`; it is not equivalent to the per-frame `bVoid()` executor.

## 2. Runtime call flow in the game

Observed from `i.java`:
- Resource/demo loading constructs slot instances and calls `aVoid(tag)`.
- Selecting a demo tag scans the parallel `pByteArr` array; each match replaces the selected instance, so last-match-wins remains the rule.
- Selection invokes `c.aVoid()` (reset all records and select first buffer).
- In the gameplay update path, when the active interpreter reports completion and has no next buffer, the game clears the active pointer. If it remains active, the game calls `c.bVoid()` once in that update pass.
- A separate input path can call `c.aVoid()` (reset/restart). Another input path sets the public `aBoolean` field true, which causes render to return immediately.
- The inspected update path does not show a call to `c.aVoid(Graphics)`; rendering call sites must be traced separately before wiring a renderer.

Important: do not assume public time-gated `aVoid()` is called every frame. The observed gameplay update invokes `bVoid()` for per-update execution. Keep these operations distinct in the native API.

## 3. Buffer and nested group representation

Each script buffer is a byte array:
- Ordinary record: byte 0 is record type; byte 1 is completion flag; remaining bytes are record-specific fields.
- Nested group: byte 0 = 0; byte 1 = child count; following entries are 32-bit little-endian child record lengths; payloads follow contiguously.
- The group executor iterates each child using the length table and calls the corresponding private record operation.
- The group completion predicate is true only when every child completion byte is 1.
- Reset traverses nested children and resets each child record; it also resets parent interpreter UI/overlay state.

The loader's nested-record parser uses a child counter and nested buffer stack-like state. The encoded child lengths are authoritative for advancing between children. Native code must bounds-check every length and ensure the child count and total lengths fit the parent payload.

## 4. Tick and execution semantics

- `bVoid()` executes all ordinary records in the current buffer (or all children of the current nested group), then increments `bInt` once.
- Record handlers mutate their own completion byte at offset +1.
- Record types 16,17,18 have a parity gate on even/odd `bInt`; their remaining-count and toggle fields are updated conditionally.
- Types 14/15 set overlay state and complete themselves, then deliberately fall through into type 12 position handling.
- Type 11 updates sprite/frame overlay identifiers and deliberately falls through into type 10 behavior.
- Type 10 updates shared game/UI state and calls the time-gated interpreter update method.
- Types 25 and 26 mutate stage foreground/background runtime grids and complete themselves, then deliberately fall through into later handlers.
- Types 5,8,9,27 have additional deliberate fall-through/guard behavior; preserve source ordering exactly.
- Types 1,4,6,13 terminate when the interpreter tick exceeds a stored duration/threshold; types 1 and 13 resolve 10000 sentinel coordinates from player or overlay positions.
- Type 2 updates a text/dialogue position/scroll field and may complete based on mode and threshold.
- The record executor's `bInt` is an interpreter frame/tick counter, not elapsed milliseconds.

Do not rewrite these into independent switch arms. Record fall-through is part of the bytecode semantics.

## 5. Update versus render

- `aVoid(Graphics)` returns immediately if public `aBoolean` is true.
- For active record types 25/26, it may skip the top/bottom black bars and Skip label when no more buffers remain; otherwise it draws UI bars and Skip text.
- It traverses nested group children and calls the private record renderer for each.
- Optional overlay rendering draws a black panel, an animation frame from shared sprite cache, a frame from sprite slot 0, and an optional control/skip icon from sprite slot 1.
- Some overlay state fields are adjusted during render based on a global boolean; rendering is therefore not purely observational. Preserve this mutation or isolate it behind a compatibility layer until proven unnecessary.
- The private update method can mutate record payload fields, while render methods may also inspect/modify per-record state. Do not merge update and render into one native pass.

## 6. Resource parsing and shared sprite cache

- demo.f chunk 0 is read as a sequence of tagged, length-delimited chunks.
- For a matching tag, the loader reads record count and payload byte length, then parses the payload.
- The payload begins with a sprite count and 16-bit sprite IDs.
- IDs are insertion-sorted ascending before demoSpr.bin lookup.
- demoSpr.bin starts with a 16-bit sprite count, followed by repeated 16-bit ID, 32-bit payload length, and sprite payload.
- Shared static `c.aClassfArr` is allocated to the sprite count on first load. Missing entries are parsed and have images created; already cached sprites are skipped when the file entry is not the current requested ID.
- The source scans sprite records sequentially until each requested ID is found. It assumes valid IDs/order and a well-formed resource; malformed files can cause overread or indefinite scan.
- Native parser must use checked arithmetic, bounded reads, explicit missing-ID errors, duplicate-ID policy, and deterministic cache ownership.

## 7. Cleanup and ownership

- Per-stage `aClasscArr` entries are individually passed to cleanup and nulled during stage teardown.
- The cleanup routine traverses each instance's sprite references and frees script-owned resources; shared static sprite-cache lifetime crosses individual interpreter instances and is not equivalent to per-instance ownership.
- Stage teardown later frees selected global sprite resources and nulls shared caches.
- The exact ownership/refcount semantics for sprites referenced by multiple demo slots remain unresolved. Do not free a shared sprite when disposing one interpreter unless ownership is proven exclusive.

## 8. Native API boundary proposal (not implemented)

Keep separate interfaces:
- `DemoChunkReader`: bounded chunk iteration and tag selection.
- `DemoSpriteArchiveReader`: bounded sprite archive index and payload retrieval.
- `DemoSpriteCache`: shared cache with explicit ownership/lifetime.
- `DemoRecordDecoder`: typed record decode preserving original raw bytes and little-endian semantics.
- `DemoScriptBuffer`: ordinary/nested buffer structure with validated offsets.
- `DemoScriptInterpreter`: reset, select-next-buffer, execute-one-tick, completion query.
- `DemoScriptRenderer`: separate render traversal, with documented legacy render-side mutations.

The first implementation slice should be parser-only and must not touch gameplay stage state. A later interpreter slice can run against fake stage-state interfaces.

## 9. Deterministic fixture requirements

- Chunk archive with multiple tags, skipped unknown tags, duplicate tags, zero-size payload, and truncated header/body.
- demo.f payload with zero and multiple sprite IDs; ascending/descending/duplicate IDs.
- demoSpr.bin with valid IDs, missing ID, duplicate ID, out-of-order entries, truncated sprite body and invalid declared lengths.
- Every fixed/variable record type, including text lengths 0, 1, maximum valid and truncated.
- Nested group with zero, one, many children, invalid child count, child length overflow, and total-length mismatch.
- Completion predicate for ordinary and nested records.
- One interpreter tick increments counter exactly once and preserves per-record fall-through.
- Reset clears all record completion/overlay fields and restarts first buffer.
- Render traversal is tested independently from update; capture any legacy render-side mutation explicitly.
- Shared sprite-cache test: two script instances reference one sprite; disposing one must not invalidate the other.

## 10. Next milestone

DEV-003W10.4 — Demo archive parser contract and synthetic binary fixtures. Implement only bounded readers and parser tests, after deciding malformed-input policy. No interpreter-to-gameplay integration until parser fixtures pass and shared sprite ownership is resolved.
