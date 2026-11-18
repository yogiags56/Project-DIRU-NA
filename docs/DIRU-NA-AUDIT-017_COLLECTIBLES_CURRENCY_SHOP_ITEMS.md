# DIRU-NA-AUDIT-017 — Collectibles, Currency, Shop & Item Progression

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source: `src/main/java/i.java`, especially stage-result aggregation, pickup/entity update paths, shop selection/purchase routines, world unlock checks, save-byte reads/writes, and the debug/cheat page.

This is partial static-source analysis. The code is decompiled and uses opaque identifiers. This document preserves exact fields and visible UI text rather than assigning unsupported semantic names. No runtime validation or binary asset extraction was performed.

## 1. Stage result counters and collectible categories

The stage-result preparation state (global state 35) aggregates multiple counters and compares them against stage targets:

- `aZInt` is compared with `aYInt`; the result screen displays the pair with text resource `texts[19]`.
- `bbInt` is compared with `baInt`; the result screen displays the pair with `texts[48]`.
- `bcInt` and `bdInt` are checked for zero and correspond to additional completion flags.
- When a counter matches its target and the relevant stage flag is not already set, the game sets a completion bit in the per-stage flag and increments `azInt`.
- The result state then persists the updated stage flags and progression values.

This proves the game tracks multiple independent stage-result categories. The exact item names represented by every counter should be confirmed by inspecting the corresponding UI strings and all increment sites; this audit does not infer that every category is a diamond type.

## 2. Pickup/update examples

The entity/pickup update routine `aqVoid()` contains several item-specific branches:

- Player/object code 1: when the player overlaps the pickup, the source increments `aZInt`, removes the pickup cell, updates a remaining-count field `aaInt`, and refreshes nearby tile state.
- Code 2: increments `bbInt` when processed through its pickup branch.
- Code 4: increments `aUInt`.
- Code 5: increments `aVInt`.
- Code 6: increments `azInt`, marks the current player coordinate in auxiliary stage arrays, and records the coordinate through a helper.
- Code 7: calls a helper with byte value 127.
- Other cases trigger distinct state, sound, animation, or progression effects.

These are source-code IDs and counter effects. Their visual identities are not fully proven here. In particular, do not rename code 1 or code 2 as red/silver diamonds solely from counter names.

## 3. Currency and world unlock

World unlock uses the unsigned 16-bit value stored at save offsets 6–7:

`unpackUint16(i.iByteArr, 6)`

The world seal UI explicitly displays the text resource `texts[48]` with the configured threshold from `a_Config.worldPrices`. The source comment identifies this text as “Red diamonds”. The unlock helper compares the saved value against the price for the selected seal; Angkor and Shop are always available.

During stage-result aggregation, `bbInt` is added to the saved 16-bit value at offsets 6–7. This is strong evidence that `bbInt` contributes to the world-unlock currency displayed as “Red diamonds”. The result screen also displays `bbInt/baInt` with `texts[48]`.

The save field at offsets 4–5 is handled separately: shop purchases subtract `a_Config.itemPrices[nInt]` from this value, and the resulting value is displayed with `texts[19]`. The source code therefore distinguishes the shop currency balance from the world-unlock currency balance. Their exact visible item names should be verified from the relevant language resource strings before assigning English names in the Native UI.

## 4. Shop and purchasable item progression

The shop uses a four-entry selection index `nInt` (0–3), wrapping at either end.

The availability helper `fInt(n)` returns:
- 0 if save byte 8 is at least `n + 5` (the item is already owned/unlocked according to this check);
- -1 if the saved balance at offsets 4–5 is below `a_Config.itemPrices[n]`;
- 1 if the item can be purchased.

On confirmation, when the helper returns 1:
- the configured price is subtracted from the balance at offsets 4–5;
- the balance is written back as little-endian bytes;
- save byte 8 is set to `n + 5`;
- the save is committed through `uVoid()`;
- the shop displays a purchase confirmation.

This shows a four-entry, progression-gated shop where byte 8 is used as an item ownership/progression threshold. The exact four item names and effects are not established in this inspected path.

## 5. Potion / save byte 9 ambiguity

The stage/resource loader has a water-breathing-potion resource flag, and the cheat/debug page displays a “blue potion” annotation when its local `cAInt` value is at least 4. However, `cAInt` is loaded from save byte 9, while other inspected code also uses save byte 9 as a progression/unlock tier (values 1, 2, and 8).

This is an unresolved semantic collision in the decompiled field interpretation. It may reflect a multi-purpose state byte, a debug-only convention, or a field whose meaning changes by game mode/state. Do not model save byte 9 as a simple potion quantity without tracing every writer/reader and confirming the UI/inventory behavior.

The water-breathing resource flag only proves conditional resource loading. It does not prove pickup, consumption, duration, or protection mechanics.

## 6. Native Android implementation requirements

- Use separate typed fields for each independently tracked stage-result counter and target.
- Keep world-unlock currency and shop currency separate; the source stores them at different save offsets and uses different text resources.
- Model stage completion categories as explicit bit flags with per-stage persistence.
- Represent the four shop entries as data-driven definitions with price, ownership threshold, and effect only after their identities are verified.
- Do not reuse legacy byte offsets as the native save schema; map them into a versioned typed model.
- Preserve unknown counters and save-byte semantics in the audit mapping until all call sites are traced.
- Keep pickup effects in gameplay update logic and commit counter changes exactly once per pickup.
- Do not infer the water-breathing potion's effect or inventory count from conditional sprite loading.
- Add tests for currency accumulation, result targets, stage completion flags, shop affordability, already-owned items, and purchase persistence.

## 7. Open work

- Inspect language string resources for `texts[19]`, `texts[48]`, and the shop item labels.
- Trace every increment/decrement/write of `aZInt`, `bbInt`, `aUInt`, `aVInt`, `bcInt`, and `bdInt`.
- Trace all writers/readers of save bytes 8 and 9, including debug/cheat-only paths.
- Identify the four shop items and their actual gameplay effects.
- Trace item pickup code IDs to their sprite slots and resource chunks.
- Trace water-breathing potion pickup, ownership, consumption, duration, and effect.
- Verify whether save byte 9 has state-dependent or mode-dependent semantics.
- Add deterministic tests for pickup counters, result aggregation, shop purchase, and save persistence.
- Validate all item names and effects against a legally usable original runtime and exact JAR variant.

## 8. Audit status

**Completed at static-source level:** multiple stage-result counters and target comparisons; selected pickup counter effects; red-diamond world-unlock balance at save offsets 6–7; separate shop balance at offsets 4–5; four-entry shop selection and purchase persistence; ambiguity surrounding save byte 9.

**Not completed:** exact labels for all collectible categories; full pickup ID-to-visual mapping; exact shop item identities/effects; potion inventory/consumption semantics; complete save-byte-9 semantics; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
