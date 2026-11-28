# DIRU-NA DEV-003W3 — Copy-on-write collision stage/state store

## Implemented

`LegacyCollisionStageStateStore` is the first production implementation of `LegacyCollisionMutationTarget`.

- Captures the constructing thread as the sole commit owner.
- Keeps all mutable arrays and scalar state behind one immutable published root.
- Reads use the current volatile root.
- A commit validates every grid and state expected value again, so a stale preflight cannot overwrite a newer root.
- Clones all grid/cache arrays and scalar state before applying any requested writes.
- Publishes exactly one new root reference after all writes have been prepared.
- Rejected validation and wrong-thread commits leave the old root and version unchanged.
- Foreground cache is read from and written to its coordinate-indexed array in the same root.
- `stageSnapshot()` returns a detached immutable `StageGrid`; prior snapshots remain unchanged.

## Transaction publication model

The root reference is the publication boundary. No mutable array is exposed. All writes are applied to private clones; if validation fails, clones are discarded. A successful transaction swaps the volatile root once, making all included values visible together to readers that acquire the new root.

The store revalidates expected values inside `commitAtomically`, not only in the applier's preflight. This closes the gap between preflight reads and commit preparation.

## Tests

`LegacyCollisionStageStateStoreTest` covers:
- One commit containing player-layer, player-local state, and foreground-cache writes.
- Old snapshot immutability after successful publication.
- Stale state rejection with no partial grid write.
- Out-of-bounds cache rejection.
- Probe no-op without root version change.
- Wrong-thread commit rejection.

## Deliberate limits

- This store is not yet wired into `GameSession`.
- The initial `StageGrid` constructor provides player/background/foreground layers. Packed metadata and auxiliary layers currently start at zero; a decoded-stage constructor/import path is still needed before legacy collision transactions can safely mutate real metadata.
- The scalar state map starts at zero and needs a complete initial-state snapshot from GameSession before production integration.
- No evaluator-to-plan mapper or recorded gameplay replay is connected yet.
- The store is owner-thread single-writer; readers can observe immutable published snapshots.

## Next

DEV-003W4: add an explicit initial state import/snapshot model for packed metadata, auxiliary cells, player fields, animation and movement flags; add full-root snapshot access for tests; then map source-derived evaluator decisions to mutation plans. Do not enable collision movement in GameSession until real stage metadata and state are seeded from the decoded stage/session.
