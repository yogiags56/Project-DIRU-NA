# DIRU-NA-AUDIT-018 — Shop Jackets, Hero Palette & Progression-Item State

## Scope and evidence boundary

This audit examines the read-only candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected files: `src/main/java/a_Config.java` and selected sections of `src/main/java/i.java` covering shop prices, purchase logic, hero sprite palette creation, world progression, and a pickup handler.

This is partial static-source analysis. The source is decompiled; numeric IDs and opaque fields are retained where semantic identity is uncertain. No runtime test or resource-image extraction was performed.

## 1. Shop inventory: four jackets

The candidate's `a_Config.java` explicitly documents `itemPrices` as **the prices for the individual jackets in the shop in violet gems**.

| Shop index | Price | Ownership threshold written to save byte 8 |
|---:|---:|---:|
| 0 | 150 | 5 |
| 1 | 400 | 6 |
| 2 | 1,000 | 7 |
| 3 | 3,000 | 8 |

The ownership threshold is calculated as `nInt + 4 + 1`. The shop's availability helper considers an item already owned when save byte 8 is greater than or equal to that threshold. If not owned, it compares the separate shop balance at save offsets 4–5 against the configured price.

The purchase routine subtracts the price from save offsets 4–5, writes the little-endian balance back, stores the ownership threshold in byte 8, and calls the save routine.

## 2. Jacket effect: hero palette selection

The hero sprite loader reads save byte 8 and sets:

`crtHeroPalette = i.iByteArr[8] - 4`

It then calls `hero.createImages(crtHeroPalette, 0, -1, -1)` and assigns the same palette index to the hero sprite.

This directly connects save byte 8 to the hero sprite palette. The shop's jacket purchases therefore change the palette used to create the hero images. The inspected code supports a visual/palette customization effect; it does not show a change to movement speed, health, damage, or other gameplay statistics.

The exact color/name of each jacket palette remains unverified without decoding the relevant palette data or observing a valid original runtime.

## 3. Shop and currency labels

The candidate configuration documents:
- `itemPrices`: jacket prices in violet gems.
- `worldPrices`: access prices for different worlds in red gems.

The source's world seal UI uses the text resource identified in a source comment as “Red diamonds”. The shop result screen uses a different text resource for the balance after purchase. This supports separate balances for jacket purchases and world access.

Do not merge these two currencies in the Native save model.

## 4. Save byte 9 is not established as potion inventory

The source writes values 1, 2, and 8 to save byte 9 in world-progression/pickup paths. It also reads byte 9 into a debug-page variable whose debug label conditionally says “blue potion” when that local value is at least 4.

These observations conflict with treating byte 9 as a simple potion count. The inspected code does not establish a potion quantity, duration, consumption rule, or inventory slot. The water-breathing resource flag from AUDIT-016 is still only evidence of conditional resource loading.

Keep byte 9 semantically unresolved until all reads/writes and state transitions are traced.

## 5. Other progression pickup handler

The inspected handler `mVoid(int)` reacts to selected values 24, 27, and 26 when the hero overlaps the relevant tile and has one of two animation states. It removes the tile and writes save byte 9 as 1, 2, or 8 respectively, while updating additional local state fields and an interaction identifier.

This confirms that these pickup/interaction branches update progression state, but the code inspected here does not establish that they are potion pickups. Do not label them as potion types without tracing their call sites and corresponding resource mapping.

## 6. Native Android implementation requirements

- Represent the four shop entries as jacket/palette unlocks, not consumable inventory items.
- Preserve the four configured prices: 150, 400, 1,000, and 3,000 violet gems.
- Keep violet-gem shop balance separate from red-diamond world-unlock balance.
- Store jacket ownership and selected jacket as typed native fields; do not reuse the old byte layout.
- Map the selected jacket to a verified hero palette resource. Do not assume palette color names before inspecting the binary palette.
- Do not attach gameplay-stat bonuses to jackets; none are supported by the inspected source.
- Keep save byte 9 unresolved in the legacy audit mapping; do not implement it as potion quantity.
- Implement potion inventory/effects only after tracing pickup, consumption, duration, and effect code paths.
- Add tests for shop prices, affordability, ownership, palette selection, currency separation, and persistence.

## 7. Open work

- Decode or inspect the four hero palette variants from `/o.f` and identify their visible colors.
- Trace all readers/writers of save byte 8 and verify whether it stores current jacket selection or highest unlocked jacket, or both.
- Trace all readers/writers of save byte 9 and distinguish world progression from debug-page behavior.
- Trace call sites for `mVoid(24)`, `mVoid(27)`, and `mVoid(26)`.
- Identify violet-gem pickup IDs and the exact increment counter.
- Trace water-breathing potion pickup, ownership, consumption, duration, and effect.
- Validate shop and palette behavior against an original legally usable JAR/runtime.
- Add deterministic tests after semantics are complete.

## 8. Audit status

**Completed at static-source level:** four shop items are jackets; configured prices; ownership threshold calculation; save offsets 4–5 used as shop currency; save byte 8 drives hero palette selection; jacket purchases persist ownership/progression; world prices are a separate red-gem currency.

**Not completed:** visible jacket palette names/colors; complete byte-8 semantics; byte-9 semantics; potion inventory/effects; all collectible-to-currency mappings; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
