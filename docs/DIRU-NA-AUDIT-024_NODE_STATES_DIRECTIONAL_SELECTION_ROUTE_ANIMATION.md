# DIRU-NA-AUDIT-024 — World-Map Node States, Directional Selection & Route Animation

## Scope and evidence boundary

This audit examines the read-only source candidate kubikaugustyn/DiamondRushSource at pinned commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Inspected src/main/java/i.java around directional input handling, packed-node field accessors, map initialization, route rendering, route advancement, and node-state-dependent rendering.

This is partial static-source analysis. The decompiled source does not provide semantic names for every packed field. This audit records observed comparisons and effects rather than assigning unsupported labels such as locked, completed, secret, or bonus. No asset extraction, original-runtime validation, Android build, or APK test was performed.

## 1. Low three-bit node field (bits 0–2)

The source reads this field in several map paths. Directional selection rejects a candidate neighbor when this field equals 1. The candidate is later selected only when the destination node's same field equals 0. Other values are not selected by this branch.

During map initialization, the field is rewritten to 0 or 1 based on stage flag bit 0x40, with stage index 0 treated specially. This confirms it is a map-node availability/presentation state, but does not prove a complete one-to-one meaning such as unlocked/locked or visited/unvisited.

Observed behavior:
- Value 0: accepted as a directional destination by the inspected selection branch; rendered through one branch of node rendering.
- Value 1: rejected as a directional destination by the inspected selection branch; rendered through a different branch.
- Other values: present in the packed 3-bit field but not fully characterized in this audit.

## 2. Secondary three-bit field (bits 3–5)

The source reads this field while drawing the map and while building route transitions. Observed branches distinguish values 0 and 1; other values follow a default/alternate path.

For value 0, the route renderer chooses one sprite/frame configuration and derives an additional rendering parameter from the neighbor count. For value 1, it selects a different sprite/frame configuration. This establishes a node classification/rendering role, but the exact visual names and gameplay semantics are not proven.

## 3. Directional neighbor selection

Directional input is converted to four internal direction codes. The source iterates the current node's explicit neighbor list, reading each neighbor's 4-bit x/y coordinate pair.

Candidate filtering observed:
- Direction code 1: candidate x must be greater than current x.
- Direction code 4: candidate x must be less than current x.
- Direction code 2: candidate y must be less than current y.
- Direction code 3: candidate y must be greater than current y.
- Candidates whose low three-bit node state equals 1 are skipped.

When multiple candidates exist in the requested direction, the code retains candidate indices and applies a secondary comparison using stage indices and relative coordinates. The full tie-breaking behavior is not completely reconstructed in this pass; it should not be replaced by nearest-Euclidean-distance logic without further proof.

After a candidate is selected, the code checks its low three-bit state again and only assigns target coordinates when the state is 0. The target coordinate is then consumed by the map transition/animation path.

## 4. Route advancement and flag 0x40

The route progression helper examines the current node's neighbor list. When it finds a qualifying neighbor whose stage index is greater than the current node's stage index, it updates global progression index dZInt, records the current node coordinates, writes stage flag bit 0x40 for the destination stage, and starts the route transition.

If the current node has no qualifying multi-neighbor progression path, dZInt is set to the current stage index in the inspected branch. This is further evidence that route progression follows graph edges and conditions, not simply stageIndex + 1.

The source also uses stage flag bit 0x40 when initializing node state. The precise lifecycle (when it is cleared, whether it means discovered, route target, or another progression marker) remains open.

## 5. Route animation is separate from graph state

The map renderer derives screen coordinates from grid positions using the 13-pixel spacing and base offsets previously recorded in AUDIT-023. It constructs route marker positions and interpolates between node coordinates using per-transition counters and direction signs.

Animation state includes current/target node coordinates, interpolation progress, and marker coordinate arrays. These values are presentation state and should not become the persistent source of truth for unlocks or stage progression.

## 6. Secret-stage status

This pass did not establish a complete secret-stage node encoding or unlock condition. The existence of secret-stage title strings is already confirmed by AUDIT-021/022, but the two low packed fields cannot yet be confidently mapped to a secret-stage flag.

Do not infer that a node with a particular low-field value is a secret stage until the node construction and all readers/writers are traced.

## 7. Native Android implementation requirements

- Decode packed nodes into typed fields while retaining the raw packed value for audit/debug comparison.
- Preserve low-field values as enums with an Unknown state until every value is mapped.
- Implement directional selection over explicit neighbor edges and reproduce source directional filtering.
- Do not substitute nearest-distance selection for the source's stage-index/coordinate tie-break logic until fully traced.
- Keep stage flag 0x40 and node presentation state separate in the native data model.
- Commit route progression independently from marker animation.
- Validate all neighbor coordinates against the 12×12 grid before indexing.
- Preserve the 240×320 logical map coordinate system for the first playable build.
- Add tests for each direction, rejected node state, multiple candidates, branch progression, and animation cancellation/re-entry.

## 8. Open work

- Enumerate all writers/readers for bits 0–2 and bits 3–5 and document every observed value.
- Reconstruct the exact multi-candidate directional tie-break algorithm.
- Determine the complete lifecycle of stage flag 0x40, including any clear/reset paths.
- Trace node construction and establish whether secret-stage nodes use a distinct packed field or external metadata.
- Identify sprite frames used by each node-state/class branch from legally usable resources.
- Validate behavior for nodes with more than two neighbors and malformed/duplicate edges.
- Add deterministic directional-selection and graph-progression tests.
- Validate map interaction against an original legally usable JAR/runtime.

## 9. Audit status

Completed at static-source level: observed low-field values 0 and 1 in directional filtering/rendering; secondary-field values 0 and 1 in route/node rendering; directional coordinate filtering; use of explicit neighbor lists; partial multi-candidate selection logic; route advancement to a qualifying higher-index neighbor; stage flag 0x40 write and node-state initialization; route animation state separation.

Not completed: full low-field semantics; full tie-breaking algorithm; complete 0x40 lifecycle; secret-stage node encoding/prerequisites; sprite-frame identity; tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.