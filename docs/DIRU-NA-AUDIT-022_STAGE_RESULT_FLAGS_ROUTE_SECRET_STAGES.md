# DIRU-NA-AUDIT-022 — Stage Result Flags, Route Advancement & Secret-Stage Evidence

## Scope and evidence boundary

This audit examines the read-only source candidate kubikaugustyn/DiamondRushSource at pinned commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Inspected src/main/java/i.java around result-state processing, per-stage flag accessors, save commits, world-map node advancement, route marker animation, and stage-result UI.

This is static-source analysis only. Numeric flags are documented as observed; they are not assigned descriptive names unless the inspected code supports that meaning. No asset extraction, original-runtime validation, Android build, or APK test was performed.

## 1. Stage record flag byte

The per-stage record accessor reads a flag byte at the stage record's +2 offset. The result routine snapshots this byte into tByte and uses it to decide which result conditions have already been awarded.

Observed writes in the result routine:

| Bit | Write condition observed | Effect in result flow |
|---:|---|---|
| 0x02 | Written at the start of result processing | Marks a result/visit state; full semantics remain unresolved |
| 0x04 | aZInt equals aYInt | Sets the corresponding completion indicator and increments azInt once |
| 0x08 | bbInt equals baInt | Sets the corresponding completion indicator and increments azInt once |
| 0x10 | bcInt equals 0 | Sets the corresponding completion indicator and increments azInt once |
| 0x20 | bdInt equals 0 | Sets the corresponding completion indicator and increments azInt once |

The four condition bits are checked against the snapshot tByte before writing, preventing the same result award from being counted repeatedly for a stage. The result UI shows separate rows/counters for these conditions.

## 2. Stage progression index and map-node state

The global dZInt is used as a current progression/next-stage index in the world-map flow. On stage completion, the map routine inspects packed node metadata and may advance dZInt to a later stage index. When it does so, it writes bit 0x40 into that stage's flag byte and sets an internal transition marker.

Another path writes bit 0x40 when a stage node is selected/marked as part of progression. The map renderer and route logic inspect this bit while deciding node appearance/availability. The exact distinction between 'visited', 'unlocked', and 'current route target' is not completely established from the inspected code; retain the raw bit name until all writers/readers are mapped.

## 3. Bit 0x02 and stage-route animation

The stage loader reads bit 0x02 for the current stage into PBoolean. The world-map route transition also checks bit 0x02 on the next stage in combination with dZInt when deciding whether to change route-animation state.

This confirms bit 0x02 participates in stage/map state, but the inspected excerpts do not establish a complete user-facing meaning. Do not equate it automatically with 'completed' or 'unlocked'.

## 4. Completion commit and persistent data

The result commit path:
- Marks the current stage with bit 0x02.
- Persists world/stage progression state.
- Writes the stage's completion/collected counter from the current run into its stage record.
- Updates global counters and currency balances through the separate result aggregation path.
- Commits the save record.

Coordinate-pair collection is persisted separately in the stage record: when a matching coordinate is found, the pair is cleared and the record's collected counter increments. This is distinct from the four result-condition flags.

## 5. Secret-stage evidence and limitations

The stage-title generator creates regular labels for title positions 1–11 and secret-stage labels for later title positions, plus a separate intro-stage label. The world map maps stage indices through per-world title-index arrays.

The inspected code confirms the existence of secret-stage title presentation and special node/route metadata, but it does not fully prove the prerequisites for unlocking every secret stage. It is not safe to infer that all secret stages unlock from collecting all four result indicators or from a single flag bit.

## 6. Native Android implementation requirements

- Preserve the raw stage-result flags as separate typed fields until each bit's full semantics are proven.
- Track each of the four result conditions independently; award each at most once per stage.
- Keep coordinate-based collectible persistence separate from result-condition flags.
- Model current progression index and map-node availability as separate concepts.
- Implement route animation as presentation driven by progression state, not as the source of truth.
- Keep secret-stage prerequisites data-driven and unimplemented until verified.
- Commit stage completion, currency, and coordinate collection atomically in the versioned native save model.
- Do not copy the legacy bit layout directly into the native schema.

## 7. Open work

- Enumerate every reader and writer of stage flag bits 0x02, 0x04, 0x08, 0x10, 0x20, and 0x40.
- Decode packed world-map node fields and relate them to stage flag bytes.
- Trace all dZInt transitions, including branching and secret-stage paths.
- Establish exact prerequisites for every secret stage in each world.
- Determine whether result bit 0x02 is visit, completion, route, or another state.
- Verify when bit 0x40 is written and cleared, and whether it means unlocked, discovered, or current progression.
- Add deterministic tests for one-time awards, stage completion commit, route advancement, and secret-stage prerequisites.
- Validate the full flow against an original legally usable JAR/runtime.

## 8. Audit status

Completed at static-source level: result snapshot behavior; four independent result-condition bits and their award checks; bit 0x02 writes during result commit and stage-load/map checks; bit 0x40 writes during progression advancement; coordinate collectible persistence; secret-stage title generation.

Not completed: full semantic labels for all flag bits; complete route-node schema; all dZInt transitions; secret-stage unlock prerequisites; tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.