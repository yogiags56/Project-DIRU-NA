# DIRU-NA-AUDIT-020 — World Seal Menu, Unlock Conditions & World Entry

## Scope and evidence boundary

This audit examines the read-only source candidate kubikaugustyn/DiamondRushSource, pinned at commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. Inspected files: src/main/java/a_Config.java and selected sections of src/main/java/i.java covering world-seal constants, seal navigation, red-gem unlock checks, world-entry state changes, stage progression initialization, and map-screen currency display.

This is static-source analysis only. It does not validate the menu visually or in an original runtime. No assets were copied and no Android build/runtime test was performed.

## 1. Four seal-menu destinations

The source defines four positions:

| Position | Constant | Destination |
|---:|---|---|
| 0 | SEAL_POS_ANGKOR | Angkor Wat |
| 1 | SEAL_POS_BAVARIA | Bavaria |
| 2 | SEAL_POS_SIBERIA | Siberia |
| 3 | SEAL_POS_SHOP | Shop |

Angkor and Shop are always considered unlocked. Bavaria and Siberia require red-gem balances.

## 2. Unlock prices and conditions

The source configuration a_Config.java defines Angkor: 0 red gems; Bavaria: 10 red gems; Siberia: 25 red gems.

The helper isSealPositionUnlocked(position) returns true for Angkor and Shop, or when the 16-bit little-endian red-gem balance at save offsets 6–7 is at least the configured price for the selected world.

The seal menu displays the required price followed by the localized Red diamonds label when a world is locked. When unlocked, or when Shop is selected, it displays the localized Press 5 prompt and destination label.

## 3. Seal navigation graph

The four-position menu uses a direction-to-destination table in a_Config.sealMoveDirections:

| Direction | Angkor | Bavaria | Siberia | Shop |
|---|---:|---:|---:|---:|
| Up | invalid | invalid | Angkor | invalid |
| Right | Bavaria | invalid | Shop | invalid |
| Down | Siberia | Siberia | invalid | invalid |
| Left | invalid | Angkor | invalid | Siberia |

Invalid transitions are represented by -1. This is a constrained graph, not a conventional rectangular four-item list.

Arrow coordinates are defined separately in sealArrowPosOffsets, and the source interpolates the arrow toward the destination coordinates after a valid directional move.

## 4. World-entry behavior

On confirmation:
- Angkor sets crtWorldIndex = 0, retains the current stage index subject to later stage-bound validation, and starts world loading.
- Bavaria first checks the red-gem unlock condition, sets crtWorldIndex = 1, resets stage index to 0, starts world loading, and raises save byte 9 to at least 1.
- Siberia checks the unlock condition, sets crtWorldIndex = 2, resets stage index to 0, starts world loading, and raises save byte 9 to at least 2.
- Shop switches to the shop state rather than loading a world.

The world-entry path releases current player resources, updates world/stage state, and calls the shared world-loading transition. It does not immediately subtract red gems when entering Bavaria or Siberia in the inspected code. The price check is an eligibility threshold, not a purchase deduction in this path.

## 5. Automatic unlock flags and save byte 1

During seal-menu initialization, the source checks whether the red-gem balance meets the configured Bavaria/Siberia thresholds. If the corresponding bits in save byte 1 are not already set, it sets the relevant bit and commits the save.

- Bit 0 (0x01): Bavaria unlock flag.
- Bit 1 (0x02): Siberia unlock flag.

The helper isSealPositionUnlocked itself checks the current red-gem balance against the price, rather than relying solely on those bits. The bits therefore also act as persisted unlock/progression flags, while the price check remains an eligibility calculation.

## 6. World/stage progression and map UI

The world map UI displays several progress/economy values: azInt, a stage/world-map collectible or progression counter shown in the map UI; save offsets 4–5, violet-gem balance; and save offsets 6–7, red-gem balance, shown alongside save byte 0 as a total/progress display.

World loading clamps the selected stage index to the maximum stage available for the selected world using bInt(worldIndex). The source also stores stage completion flags and stage indices through separate per-world save blocks, already covered in earlier save and stage audits.

## 7. Native Android implementation requirements

- Implement the seal selector as a four-node navigation graph, not a generic vertical list.
- Keep destination labels, lock prompts, and currency labels localized.
- Display configured red-gem requirements: 10 for Bavaria and 25 for Siberia.
- Keep eligibility check separate from currency deduction; do not charge the player on world entry unless another source path proves a cost is deducted.
- Persist world unlock flags separately from the live balance and derive final availability consistently.
- Reset stage index to the first stage when entering Bavaria or Siberia, matching inspected behavior.
- Keep Angkor's current-stage retention behavior until the full menu flow confirms whether it is intentional.
- Clamp selected stage index against the destination world's stage count before loading.
- Recreate arrow movement as a graph transition with explicit invalid edges.
- Preserve the 240×320 logical viewport and existing menu coordinate assumptions for the first playable build.

## 8. Open work

- Trace all writers/readers of save byte 1 and determine how unlock flags interact with red-gem balance changes/refunds.
- Trace all per-world stage unlock and completion bit semantics.
- Confirm whether Angkor's selected stage is intentionally retained during seal entry.
- Inspect original UI assets to document exact seal/menu layout and arrow animation.
- Verify localization strings for world names, locked prompts, and currency labels.
- Add deterministic tests for each navigation edge, locked/unlocked conditions, world entry, stage reset/clamp, and persistence.
- Validate menu behavior against original runtime evidence.

## 9. Audit status

Completed at static-source level: four destinations; red-gem requirements; unlock helper; menu lock/unlock prompts; directional navigation graph; world-entry actions; Bavaria/Siberia progression tier updates; save-byte-1 unlock flag writes; stage-index clamp on world loading.

Not completed: full save-byte-1 semantics; full per-world stage progression flags; exact UI asset mapping; runtime menu validation; synthetic tests; licensing clearance.

No Android build, APK, or runtime test was performed for this audit.