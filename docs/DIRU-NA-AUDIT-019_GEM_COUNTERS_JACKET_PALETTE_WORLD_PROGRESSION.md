# DIRU-NA-AUDIT-019 — Gem Counters, Jacket Palette State & World Progression Byte

## Scope and evidence boundary

This audit examines the read-only source candidate `kubikaugustyn/DiamondRushSource`, pinned at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`. Inspected source: `src/main/java/i.java` and `src/main/java/a_Config.java`, focusing on stage-result aggregation, pickup/entity update cases, shop purchase logic, hero palette loading, world progression writes and reads, and the cheat-page editor.

This is static-source analysis only. It does not identify the actual visible gem/jacket colors because the binary sprite/palette resources have not been decoded. Decompiled field names remain opaque where semantics are not fully established. No runtime test or asset extraction was performed.

## 1. Two independent gem balances

The source configuration explicitly documents:
- `itemPrices` as jacket prices in **violet gems**: 150, 400, 1,000, 3,000.
- `worldPrices` as world-access prices in **red gems**: 0, 10, 25.

The stage-result aggregation writes two separate 16-bit little-endian balances:
- Offsets 4–5: previous value plus `aZInt`.
- Offsets 6–7: previous value plus `bbInt`.

The shop subtracts `itemPrices[nInt]` from offsets 4–5. The world-seal unlock logic compares offsets 6–7 against `worldPrices`. This confirms the two balances must remain independent.

## 2. Pickup/update counter mapping

In the inspected player/entity update path:
- Object code 2 increments `bbInt`; stage-result aggregation later adds `bbInt` to save offsets 6–7, the red-gem world-unlock balance.
- Object code 41 adds `aHInt` to `aZInt`, and reduces the remaining amount `aaInt` by the same quantity. Stage-result aggregation later adds `aZInt` to save offsets 4–5, the violet-gem jacket-shop balance.
- Object codes 4 and 5 increment `aUInt` and `aVInt` respectively; their currency role is not established in this trace.
- Object code 6 increments `azInt` and records a stage coordinate/flag; this is tracked as a stage collectible/progression counter, not either gem balance in the inspected aggregation.

The code-41 branch is quantity-based (`aHInt`) and may represent a multi-unit pickup or remaining amount. The exact visual identity and pickup appearance are not confirmed by the inspected logic alone.

## 3. Jacket ownership and active hero palette

New-save initialization sets save byte 8 to 4. The shop purchase for index `nInt` writes `nInt + 5` to byte 8 after charging the corresponding violet-gem price.

The hero sprite loader calculates:
`crtHeroPalette = saveByte8 - 4`
and uses that value to create the hero images and set the sprite palette index.

Therefore, byte 8 is directly tied to the hero palette selected by the game. The shop's four purchase thresholds are 5, 6, 7, and 8, corresponding to palette indices 1, 2, 3, and 4. The exact colors and whether the game separately tracks “owned” versus “currently equipped” are not established; the inspected implementation appears to use the same byte for both the shop threshold and hero palette choice.

A UI drawing path also uses byte 8 to determine how many jacket indicators to draw. This reinforces that byte 8 is a jacket/palette progression value, not a potion count.

## 4. Save byte 9: world progression evidence

Save byte 9 has direct world/story progression uses:
- When entering the Bavaria world, the source raises byte 9 to at least 1.
- When entering Siberia, it raises byte 9 to at least 2.
- The interaction handler for selected special objects writes values 1, 2, or 8.
- Gameplay action/interaction branches gate certain behaviors on thresholds `>= 1`, `>= 2`, and `>= 8`.

The cheat page copies byte 9 into a local value and displays “blue potion” when that value is at least 4. That debug label is inconsistent with the many direct progression checks/writes and does not prove byte 9 is potion inventory. The best-supported interpretation is a progression/state tier with multiple threshold uses; exact meaning of the value 8 and special-object writes remains open.

## 5. Native Android data model implications

The Native port should model these concepts as distinct typed values:
- `violetGemBalance`: shop currency.
- `redGemBalance`: world unlock currency.
- Stage result counters/targets for each collectible category.
- `jacketPaletteProgress` or explicit unlocked jacket IDs, after exact ownership semantics are confirmed.
- `worldProgressionTier` or explicit progression flags, after all byte-9 call sites are traced.

Do not reproduce the old 1001-byte save buffer as the Native save schema. Do not model byte 9 as potion quantity. Do not merge the two currencies. Do not infer visual palette names/colors from palette indices.

## 6. Open work

- Decode the four hero palette variants in `/o.f` or another legally usable exact package and document their visible colors.
- Trace all writers/readers of save byte 8 to distinguish unlocked jacket set from active jacket selection.
- Trace all writers/readers of save byte 9, especially the values 8 and special-object IDs 24, 26, and 27.
- Identify the visible pickup resource and animation for object code 2 and code 41.
- Trace the complete `aHInt` / `aaInt` quantity logic for code 41.
- Determine the semantic roles of counters `aUInt` and `aVInt`.
- Verify exact pickup mappings and currency names against the original legally usable JAR/runtime.
- Add deterministic tests for both currency balances, stage-result aggregation, jacket purchase/palette progression, and world progression thresholds.

## 7. Audit status

**Completed at static-source level:** separate violet-gem and red-gem balances; code-2 contribution to red-gem balance; code-41 quantity contribution to violet-gem balance; shop and world-price configuration; save-byte-8 connection to jacket purchase progression and hero palette; save-byte-9 use as world/story progression thresholds.

**Not completed:** exact visual pickup mapping; palette colors/names; exact unlocked-versus-equipped jacket semantics; full byte-9 meaning; roles of counters `aUInt`/`aVInt`; potion behavior; synthetic tests; runtime validation; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.
