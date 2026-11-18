# DIRU-NA-AUDIT-016 — Water-Related Tiles, Potion Resource Flags & Environmental Damage

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source ranges in `src/main/java/i.java` include stage foreground preprocessing for codes 8 and 9, resource-chunk filtering and texture assignment, and selected gameplay/entity update branches that invoke `hurtHero`.

This is partial static-source analysis only. The source uses decompiler names and numeric IDs. The inspected code supports water-related resource handling and damage paths, but it does not establish a complete swimming/breathing simulation or the user-facing effect of every potion. No runtime/emulator validation was performed and no assets were copied.

## 1. Foreground codes 8 and 9

During stage preprocessing, foreground code 8 sets `ahBoolean = true` and then falls through to the shared handling for code 9.

The shared code-8/code-9 branch:
- sets `crtShouldLoadWaterBreathingPotion = true` only when the foreground code is not 8 (therefore for code 9);
- sets feature bit `0x10000000` in `cLong`;
- counts the background metadata value in a per-value table;
- packs the background value into the upper bits of the foreground cell.

The evidence supports these distinctions:
- Code 8 activates the `ahBoolean` feature flag.
- Code 9 requests the water-breathing-potion resource.
- Both participate in a shared environmental-feature path.

It does not prove that code 8 means ordinary water and code 9 means deep water, nor does it prove the exact gameplay behavior of either tile.

## 2. Conditional potion resource loading

The resource loader filters generated sprite chunks based on stage feature flags:

- Chunk 28 is image-created only when `crtShouldLoadWaterBreathingPotion` is true.
- Chunk 24 is image-created only when `crtShouldLoadFanPot` is true.

For chunk 28, the loader maps the sprite to slot 45. For chunk 2, if `crtShouldLoadWaterBreathingPotion` is true, it assigns `sprite.images[0]` to `textures[24]`. If `ahBoolean` is true, it creates an additional image and assigns `sprite.images[1]` to `textures[25]`.

This is direct evidence that the stage feature flags control conditional loading/creation of water-related graphics resources. It does not independently prove when or how the potion is consumed by the player.

## 3. Environmental damage paths

The inspected gameplay code contains multiple calls to `hurtHero` from environmental/entity update branches. Examples include:
- A code-46 entity path that can damage the player when a timed downward movement or collision condition is met.
- A foreground code-3 timed update path that damages the player if the cell below is occupied by the hero.
- Several other entity branches that invoke damage with different response parameters.

These calls show that environmental hazards can damage the hero. They do not establish that all water-related tiles cause damage, or that the water-breathing potion grants universal immunity.

## 4. What remains unverified about swimming and breathing

The inspected evidence does not yet establish:
- A player oxygen/breath meter or countdown.
- A drowning timer.
- A complete swimming movement mode.
- The exact effect, duration, or consumption rule for the water-breathing potion.
- Whether code 8 and code 9 represent different water depths, regions, or trigger types.
- The full visual mapping of the corresponding resources.

These must remain open questions. The Native implementation should not invent an oxygen system or drowning mechanic from the resource name alone.

## 5. Native Android implementation requirements

- Preserve foreground code 8 and code 9 as distinct source tile IDs in the audit mapping.
- Model stage feature flags separately from tile identity.
- Load optional water-related resources only when the parsed stage requires them.
- Keep resource availability separate from gameplay effect; do not infer potion behavior from a sprite being loaded.
- Route environmental damage through the same deterministic damage system as other hazards.
- Keep water/potion behavior behind a typed environment capability interface until source behavior is fully traced.
- Do not introduce an oxygen meter, drowning timer, or swimming mode without source evidence.
- Add tests for conditional resource loading and each confirmed damage trigger.
- Validate the exact visual and gameplay semantics against a legally usable original runtime before finalizing the tile names.

## 6. Open work

- Trace all reads/writes of `ahBoolean` and `crtShouldLoadWaterBreathingPotion`.
- Trace every use of `textures[24]`, `textures[25]`, and sprite slot 45.
- Find the exact potion inventory, pickup, consumption, and effect code paths.
- Trace player collision and movement while standing on foreground codes 8 and 9.
- Determine whether any player state/timer tracks underwater duration or breathing.
- Identify visual resource chunks from a legally usable package and confirm the exact tile/potion appearance.
- Add deterministic tests for stage feature flags and optional resource loading.
- Validate water-related gameplay against original runtime evidence.

## 7. Audit status

**Completed at static-source level:** distinct preprocessing effects of foreground codes 8 and 9; conditional resource chunk 28 and texture assignment behavior; examples of environmental damage calls.

**Not completed:** exact water tile semantics; potion pickup/consumption/effect; player swimming/breathing behavior; oxygen/drowning evidence; visual resource mapping; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
