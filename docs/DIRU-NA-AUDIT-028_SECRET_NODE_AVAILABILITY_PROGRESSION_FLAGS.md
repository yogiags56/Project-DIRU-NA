# DIRU-NA-AUDIT-028 — Secret-Node Availability, Progression Flags & Stage-Stream Bounds

## Scope and evidence boundary

This audit examines the pinned read-only candidate kubikaugustyn/DiamondRushSource at commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Relevant excerpts in src/main/java/i.java include world-map initialization, node state assignment, stage loading from /w0.bin-/w2.bin, stage-result processing, route advancement, and save initialization. This audit extends AUDIT-027; it does not claim original-runtime verification.

## 1. Secret-node classification is not the same as availability

AUDIT-027 established that the map resource's secondary field value 1 maps to generated Secret Stage titles in all three world maps. The map initialization path separately derives the low three-bit node state from the stage record flag byte:

- If stage flag bit 0x40 is set, node state is set to 0.
- Stage index 0 is also forced to node state 0.
- Otherwise node state is set to 1.

The directional-selection path skips candidate nodes whose low three-bit state equals 1, and accepts a destination when that field equals 0. Therefore the source distinguishes a node's classification (secondary field) from its current selectable/progression state (low field). A Secret Stage node can be classified as secret while still being unavailable.

## 2. Flag 0x40 lifecycle observed

Observed code paths:

1. World-map initialization reads each node's stage index and checks the corresponding stage save flag byte for bit 0x40 to derive node state.
2. Route advancement (zVoid) examines the current node's neighbor list. When it finds a qualifying neighbor whose secondary field equals 1 and whose stage index is greater than the current node's stage index, it sets global progression index dZInt to that neighbor's stage index, writes flag 0x40 to that stage record, records the current node coordinates, and starts a route transition.
3. The result-processing block contains an older/decompiled commented-out progression fragment that also mentions 0x40. It is commented and must not be treated as an active independent writer.

This establishes a concrete route to at least some secret-class nodes: a qualifying higher-index neighbor with secondary field 1 can be marked with 0x40 and become state 0 on the next map initialization. It does not prove every secret node is unlocked by the same rule or that all branches are traversable under identical conditions.

## 3. Stage flag 0x02 is a separate state

The stage loader initializes PBoolean from stage flag bit 0x02. Result processing writes bit 0x02 when committing the stage result. The world-map route animation checks bit 0x02 on the next numeric stage in one condition, alongside the progression index dZInt.

These uses show 0x02 is separate from 0x40 and participates in stage/map state. Its exact user-facing semantic label remains unresolved; do not collapse it into the secret-node unlock flag.

## 4. Result conditions do not directly prove secret unlock

Result flags 0x04, 0x08, 0x10, and 0x20 track four separate result conditions and are awarded once per stage. The route helper zVoid separately inspects map neighbors and uses secondary field 1 plus stage-index ordering to select a progression target.

Therefore, the available code does not establish that collecting all four result indicators is a universal prerequisite for secret-stage availability. The route selection condition observed is based on graph neighbor metadata and progression ordering; more source/runtime evidence is needed to determine any additional gameplay prerequisites.

## 5. Stage-stream resource bounds

The game uses /w0.bin, /w1.bin, and /w2.bin for the three worlds. The stage loader first reads a leading byte and then repeatedly reads a world-stage count byte followed by stage records with 4-byte width/height headers and three width×height layer arrays. It scans until the requested stage index is reached.

During save initialization, the source reads the world-stage count and records a per-world maximum stage index used to clamp the selected stage. This stage stream is a separate structure from map_*.out and from gIntArrArr title mappings.

The earlier AUDIT-027 anomaly remains: the Tibet/Siberia title mapping has an entry at stage index 14, while map_tibet.out contains node IDs 0–13. This audit has not independently decoded the full /w2.bin stream, so it cannot determine whether stage 14 exists as a stage resource without a corresponding map node, is reserved, or is a stale mapping.

## 6. World progression interpretation

Current source-supported model:
- Map resource secondary field: node class; value 1 maps to Secret Stage title strings in the three decoded maps.
- Stage save flag bit 0x40: progression/availability marker consulted when building map node state and written during qualifying route advancement.
- Packed node low field bits 0–2: runtime selectable/presentation state derived from 0x40 and intro-stage special handling.
- Stage save flag bit 0x02: independent stage/map state written during result commit and read by loader/map animation.
- Result flags 0x04/0x08/0x10/0x20: independent result-condition awards.
- dZInt: global map progression target/index, advanced through graph neighbor inspection rather than assumed numeric sequence.

## 7. Native Android implementation requirements

- Keep node class, node availability, stage completion, result awards, and current route target as separate fields.
- Model secret nodes as classified nodes that may remain locked/unavailable until native progression rules grant access.
- Implement 0x40 behavior as a native progression state only after the remaining writer/reader paths are audited; do not reuse the literal legacy bit layout in the new save format.
- Do not assume all four result-condition awards are prerequisites for every secret node.
- Parse stage-stream counts and dimensions with bounded reads and overflow-safe width×height calculations.
- Reconcile map node IDs, title mappings, and actual stage stream indices per world before defining final stage catalogs.
- Preserve the Scotland/Bavaria and Tibet/Siberia resource/UI naming distinction until the exact package variant is verified.

## 8. Open work

- Decode /w0.bin, /w1.bin, and /w2.bin to establish exact stage counts, dimensions, and stage-index availability.
- Resolve the Tibet/Siberia stage-index-14 title mapping anomaly.
- Enumerate every active writer and reader of stage flag 0x40 and identify any clear/reset paths.
- Trace all conditions controlling zVoid route advancement, including the meaning of its current-stage/neighbor-count guard.
- Trace all active readers/writers of stage flag 0x02.
- Validate whether all secondary-field-1 nodes are reachable and determine the exact route prerequisites for each.
- Add tests for secret-node classification versus availability, progression flag persistence, stage-stream bounds, and missing map nodes.
- Validate against an original legally usable JAR/runtime.

## 9. Audit status

Completed at static-source level: distinction between secret classification and availability; map-state derivation from flag 0x40; active zVoid route path that marks qualifying higher-index secondary-field-1 neighbors; separate observed use of flag 0x02; stage-stream loading structure; separation of map, title mapping, stage stream, and save progression.

Not completed: full stage-stream binary inventory; exact all-secret-node prerequisites; complete 0x40 and 0x02 lifecycle; Tibet stage-index-14 resolution; tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed.