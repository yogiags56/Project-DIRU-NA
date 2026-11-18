# DIRU-NA-AUDIT-015 — Object Pushing, Falling Entities & Environmental Interaction

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source: `src/main/java/i.java`, including stage preprocessing for object code 48, movement/collision helper branches for object codes 0–9 and 48, entity update dispatch for codes 46 and 48, and selected foreground hazard update paths.

This is partial static-source reconstruction. Decompiled numeric codes are retained as source identifiers; visual names such as “crate”, “boulder”, “water”, or “spike” are not assigned unless directly supported by the inspected code. No runtime/emulator tests were performed and no assets were copied.

## 1. Multi-cell object code 48

Stage preprocessing identifies player/object code 48 as a two-cell entity:

- The source initializes metadata at the original cell, using background bits to choose an initial flag value.
- It writes a second code-48 cell at `y - 1`.
- The upper cell receives metadata value 8.
- A helper `kVoid(x, y - 1)` is called after the second cell is created.
- The stage's object count and feature flags are updated.

This confirms that code 48 must be represented as a multi-cell object, not two unrelated single-cell entities. The precise visual identity is not established by this code path.

## 2. Pushing behavior for ordinary movable entities

The movement/collision helper contains a branch for object codes 0, 8, 9, and 47 (alongside other cases) that evaluates a destination two tile-steps from the player in the movement direction. If the destination foreground permits passage and the relevant occupied/neighbor conditions are satisfied, the helper moves the entity into the destination cell, clears the old cell, assigns movement progress/state, and updates its packed metadata.

For several object codes, the packed direction bits are changed to 2 or 4 depending on push direction. The moved object receives a per-cell timer/progress value of 18. The exact object semantics vary by code and metadata; do not generalize all listed codes as identical pushable crates.

## 3. Pushing the two-cell code-48 entity

The code-48 collision branch is distinct:

- It checks a metadata bit `0x8` to determine the two-cell orientation/layout.
- It calculates destination coordinates for both occupied cells.
- Both destination cells must be empty.
- An additional support/neighbor condition must be satisfied.
- On success, both cells are moved together, old cells are cleared, metadata is rewritten, and per-cell movement state is initialized.

This is a two-cell push operation with atomic occupancy requirements. Native implementation should validate both destination cells first, then commit both cell changes together. Partial movement of one half would corrupt the map.

## 4. Falling entity code 46

The entity update path for code 46 stores animation/state data in the integer metadata and byte timer arrays. In the inspected branch:

- It derives a subtype/state from the low five bits of the integer metadata.
- If the cell below is empty and the subtype is not 8 or 9, it selects a destination state (8 for selected even subtypes, otherwise 9), moves code 46 down one cell, clears the original cell, and initializes the destination timer.
- For subtype 8 or 9, it decrements a per-cell timer by 6. When the timer becomes negative, it either moves the entity down if the lower cell is empty or changes the entity's stored state to 10 or 11 and resets the timer.
- In another branch, collision with the player can call `hurtHero` using direction/state data.
- Additional neighbor and collision conditions can remove or transform the entity.

This is direct evidence of a timed, stateful falling/moving entity. It is not enough to assign a visual identity or claim a universal gravity rule for all objects.

## 5. Foreground hazard code 3

The inspected foreground code-3 update path uses a per-cell timer:

- While the timer is positive, it is decremented by 5.
- Once the timer reaches zero, the update checks the cell below.
- If the player is below, the hero damage handler is called.
- If another entity is below, the branch may transform, remove, or otherwise interact with that entity depending on its code.
- If the cell is blocked, the foreground metadata is updated and its timer reset.
- If it is not blocked, the foreground and player/object layer cells are changed to represent a new state.

This demonstrates that at least one environmental foreground effect has timed behavior and can interact with both the player and other entities. The visual identity of code 3 is not established in this audit.

## 6. Additional observed entity transitions

Selected update branches show:
- Code 48 may dispatch to separate update helpers based on metadata bit `0x8`.
- Code 46 can move downward, change subtype/timer state, collide with the player, or be removed.
- Code 37 increments a state counter and can be removed after a threshold.
- Other update paths transform or clear codes 19, 43, 45, and 46 when affected by environmental interactions.

These observations reinforce that object behavior is a state machine over multiple arrays, not a single tile-ID switch.

## 7. Native Android implementation requirements

- Model multi-cell entities with stable entity identity and an explicit occupied-cell footprint.
- For pushes, compute and validate the entire destination footprint before mutating the grid.
- Commit multi-cell movement atomically and preserve metadata/timers for every occupied cell.
- Represent falling/moving entities with explicit subtype, timer, direction, and collision state.
- Keep simulation ticks deterministic and separate from rendering frame rate.
- Implement environmental interactions as typed handlers with explicit outcomes: move, block, transform, damage, remove, or trigger.
- Preserve the source's separate player/object, background, foreground, timer, and integer metadata channels.
- Guard all neighbor accesses against map bounds before indexing.
- Do not apply a generic gravity rule to all object IDs; only implement per-code behavior established by the source audit.
- Keep unknown IDs and visual identities unresolved until further source tracing or valid visual evidence.

## 8. Open work

- Trace helper methods `dVoid(byte)`, `aiVoid()`, `eVoid(byte)`, `anVoid()`, and `amVoid()` to complete code-48 and related entity behavior.
- Trace the full update dispatch for every movable/falling entity code.
- Determine the role of the per-cell arrays `bByteArrArr` and `cByteArrArr` for each entity family.
- Trace all foreground code-3 writers/readers and identify its exact visual/gameplay semantics.
- Build a complete pushability/support/collision matrix across entity codes and foreground metadata.
- Add deterministic tests for single-cell pushes, two-cell atomic pushes, falling timers, blocked falls, player collision, and map-edge conditions.
- Validate puzzle outcomes against original runtime evidence.

## 9. Audit status

**Completed at static-source level:** code-48 multi-cell footprint; selected ordinary push branches; code-48 two-cell push conditions; code-46 timed downward movement/state changes; foreground code-3 timed interaction behavior; selected entity transformations.

**Not completed:** full helper semantics, full movable/falling entity catalog, per-cell timer semantics, complete collision/support matrix, synthetic tests, runtime validation, and licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
