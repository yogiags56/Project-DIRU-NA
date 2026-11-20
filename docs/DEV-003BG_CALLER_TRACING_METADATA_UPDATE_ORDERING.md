# DEV-003BG — Caller Tracing & Metadata Update Ordering

## Scope and provenance

Static caller tracing against `kubikaugustyn/DiamondRushSource/src/main/java/i.java`, pinned to `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This milestone refines the previous DEV-003BF matrix. It distinguishes direct record-ID readers from generic metadata-classification reads and confirms the only direct caller found for the coordinate/group validation helper.

## 1. Important correction: the generic validation helper is a rendering helper

The four-argument helper at lines 16752–16777 has this signature:

`aInt(tileX, tileY, packedTileMetadata, groupIndex)`

It extracts the record ID from the selected tile group, reads that record's stored X/Y/group/type, and compares the identity fields. Its only direct call site found in the full candidate source is the tile-rendering loop at line 7690:

`aInt(n70, n71, n72, b8)`

Therefore the helper is not a general-purpose validator called throughout controller updates. It is specifically used by rendering to derive the visual frame/offset for a linked record.

On identity mismatch, the helper has a special cleanup branch for metadata classification 6: it calls `cVoid(tileX, tileY, groupIndex)`, clearing that nine-bit tile metadata group and marking the tile for redraw. It does not free the referenced object record. For other classifications, the helper returns a value derived from the metadata classification without performing that cleanup.

This narrows DEV-003BF's earlier broad wording: the coordinate/group validation helper's confirmed call-site is rendering, not a shared validation gate for all metadata readers.

## 2. Rendering path

At lines 7668–7700, the renderer:
1. Reads a visible tile's packed metadata.
2. Iterates the three metadata groups.
3. Checks whether the group's three-bit record ID is nonzero.
4. Reads the group's two-bit auxiliary visual field.
5. Calls the validation/render helper with tile X/Y, packed metadata, and group index.
6. Uses the helper's returned classification/visual value to choose sprite frame and clipping.

The rendering loop therefore performs a nonzero-ID guard, then calls the helper. The helper validates stored record coordinates and group for its visual result. A mismatch can cause a metadata unlink only for classification 6. This is a rendering-time side effect, not proof that all stale references are globally repaired.

The renderer's other metadata checks (for example classification 8 in the tile above) are classification reads, not object-record dereferences.

## 3. Controller update path

The controller update method `asVoid()` is invoked from the main update sequence at line 10616. Within `asVoid()`, controller state 2 and its related branches read the current object record's stored coordinates and group, inspect the corresponding tile metadata group, and may:
- update tile group classification;
- move a record's group association;
- change record type bits;
- allocate a new record through the allocator;
- clear a whole object record through `fVoid()` in specific controller branches.

The state-4 branch separately consumes auxiliary `bLongArr` entries. It is not a Type-5 record cleanup loop.

The controller code does not call the four-argument rendering validation helper. It has its own direct checks and mutations. Consequently, native code should not assume that every controller mutation is automatically preceded by the rendering helper's coordinate/group validation.

## 4. Update and render ordering

The main update sequence at line 10616 invokes `asVoid()` after several other update helpers, then continues with further update logic. The renderer is a separate path that reads `eIntArrArr` during drawing.

The inspected source establishes that controller updates and rendering both access the metadata grid, and that rendering may clear a mismatched Type-6 metadata group. It does not, by itself, prove the exact thread/frame scheduling relationship between update and paint callbacks on every device. No runtime trace was performed. Treat update-before-render as the observed order within this update method only, not as a universal concurrency guarantee.

## 5. Reference lifetime implications

1. A tile metadata reference can be stale relative to the object record's stored X/Y/group; the renderer contains an explicit identity comparison.
2. The only proven generic identity helper is used by rendering, and its mismatch cleanup is conditional on metadata classification 6.
3. That cleanup clears one tile metadata group only. It does not free the object record or prove that other tile groups no longer reference it.
4. Controller update logic performs separate checks and can mutate record and tile metadata state without calling the rendering helper.
5. Because Type-5 records may be referenced by multiple metadata groups, unlinking one group is insufficient evidence to free a record.
6. No complete all-groups reference-counting or reverse-index mechanism has been proven in the inspected source.

## 6. Safe native design requirements

- Keep tile-group unlink and object-record release as separate operations.
- Use a validated record lookup for all mutating native operations: validate ID range, nonzero slot, record type, stored coordinates, and group identity.
- Keep a distinct rendering lookup if exact legacy behavior (including Type-6 stale-group cleanup) is required; document that it may mutate tile metadata.
- Do not free an object record merely because one tile group was cleared.
- Before record release, either prove all metadata references are unlinked or introduce a native-owned reverse-reference/counting scheme whose updates are atomic with every link/unlink path.
- Do not infer original scheduling/thread safety from decompiled method order alone.

## 7. Limits and next milestone

No original shipping JAR bytecode comparison, device/runtime trace, native test, or Android build was performed. The complete set of metadata writers and all indirect helper behavior still needs validation.

### Next: DEV-003BH — Native Object Handle & Reference Ownership Contract

Specify the native record handle, validated lookup, link/unlink, reclassification, relocation, and release APIs. Include the rule for shared tile references and checkpoint snapshots. Then create isolated tests for zero/out-of-range IDs, stale coordinate/group identity, Type-6 render cleanup, multiple tile references, and safe full-table allocation failure.
