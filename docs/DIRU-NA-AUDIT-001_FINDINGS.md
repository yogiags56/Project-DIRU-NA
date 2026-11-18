# DIRU-NA-AUDIT-001 — Source and Runtime Findings

**Status:** In progress  
**Audit input:** `kubikaugustyn/DiamondRushSource`  
**Reviewed source revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Scope of this update:** gameplay entry loop, key input, resource container access, sprite decoding, map parsing, audio indexing, and RMS persistence.

These are static-source findings. No source was compiled, no binary assets were decoded in this pass, and no Android runtime behavior has been validated.

## 1. Main game loop and input

Source: `src/main/java/i.java`

- Main game class is `i extends Canvas implements Runnable`.
- `run()` performs initial preference loading, then enters a loop while the game is active.
- Each active iteration calls `handleKeyPresses()`, increments frame/tick counters (except in a selected state), requests repaint, and sleeps for the remainder of a nominal 50 ms interval.
- If measured frame work exceeds 65 ms, the loop sets a timing flag and resets its elapsed-time accumulator rather than applying the normal repaint/sleep path.
- `keyPressed(int)` normalizes the incoming key code through `getKeyFromKeyCode` and ORs the result into a shared bitmask.
- `keyReleased(int)` clears the corresponding bit. This is a held-key state model, not a queued per-event input model.
- `handleKeyPresses()` interprets the bitmask according to the current game state.

**Native implication:** implement a platform-independent input bitset/action state and a deterministic update loop. Android touch controls should feed the same action state as physical keys. Do not bind gameplay directly to Android view callbacks.

**Caveat:** the 50 ms sleep suggests a nominal 20 Hz loop target, but it is not proof of a stable 20 FPS simulation; elapsed-time handling and render scheduling need runtime validation.

## 2. Resource container and sprite loading

Source: `src/main/java/i.java`, `src/main/java/f_Sprite.java`, `src/main/java/b_SpriteAnimator.java`

The shared `readChunk(fileName, chunkIndex)` helper shows a common chunk-container layout:

1. First byte gives the number of 8-byte directory entries.
2. The directory is read as `count * 8` bytes.
3. Each entry contains a 32-bit little-endian offset followed by a 32-bit little-endian chunk length.
4. The selected chunk is read from the resource stream at the recorded offset.

The source uses a little-endian `unpackUint32` helper for those directory fields. The helper currently uses `InputStream.skip(offset)` and a single `read(byte[])`; Java stream APIs do not guarantee that one call consumes the full requested byte count. The native resource reader should use bounded reads / read-fully semantics and validate offsets and lengths.

`loadSprite(...)` calls `readChunk(...)`, passes the resulting chunk to `f_Sprite.parseSprite(...)`, creates images for the selected palette, then releases the temporary encoded module data.

The sprite parser exposes these pixel formats:

- 8888
- 4444
- 1555
- 0565

It also exposes indexed/encoded pixel formats:

- I256
- I16
- I4
- I2
- I256RLE
- I127RLE

The parser stores module dimensions, frame-to-module references, frame bounding boxes, animation frame references, per-frame timing/offsets/flags, palette data, and encoded module pixel data. Some parser comments explicitly flag uncertain byte/word field interpretation; these need validation against actual binary samples before a clean-room decoder is finalized.

`b_SpriteAnimator` tracks animation index, current animation frame, elapsed frame time, position, and flip flags. `tick()` advances animation frames based on each frame's duration and loops at the end. `draw()` delegates to frame/module/animation drawing routines.

## 3. Resource call-site map (initial)

The following paths are directly referenced by `i.java` or related audited classes:

| Resource | Observed role | Confidence |
|---|---|---|
| `/o.f` | Hero sprite container; chunk directory and hero sprite data; optional fire animation chunk | High |
| `/cm.f` | Effect animation sprite chunk | High |
| `/ui.f` | Top/bottom UI sprite chunk | High |
| `/demoui.f` | Demo UI texture/palette imports | High |
| `/ms.f` | World-map sprite chunks | High |
| `/mmv.f` | Menu/map visual textures | High |
| `/mm0.f`, `/mm1.f` | Menu/map sprite resources | Medium |
| `/gen0.f` | General sprite/resource group | Medium |
| `/b0.f`, `/b1.f` | Sprite animation resources used in menu/world state | Medium |
| `/spl.f`, `/cr.f` | Referenced sprite/resource groups; detailed role not yet fully traced | Low |
| `/tips.f` | Tips-related resource group | Medium |
| `/map_angkor.out` | Angkor world map data | High |
| `/map_scotland.out` | Scotland/Bavaria map data | High |
| `/map_tibet.out` | Tibet/Siberia map data | High |
| `/w0.bin`, `/w1.bin`, `/w2.bin` | Per-world binary data loaded into save/world initialization path | Medium |
| `/mc` | 256-byte lookup/table data read during game construction | High |
| `/snd.f` | Audio container with 21 sound entries | High |
| `/lang.<locale>`, fallback `/lang.xx` | Localized text tables | High |

This is a call-site map, not a complete format specification. Roles marked medium/low require call-chain tracing and byte-level inspection.

## 4. World map parser

Source: `i.java`

The map parser `aVoid(String name)` reads:

- a 16-bit little-endian payload length,
- a one-byte record count,
- then a payload containing per-record fields.

For each record it reads five fixed bytes, then a variable number of additional byte pairs. It feeds these values into the game's world-map coordinate/record helper. The exact semantic names and all boundary conditions remain to be reconstructed.

The game selects:
- Angkor: `/map_angkor.out`
- Bavaria: `/map_scotland.out`
- Siberia: `/map_tibet.out`

The `/w0.bin`, `/w1.bin`, and `/w2.bin` streams are also consumed in the initialization/save-building path. Their binary structure is not yet fully documented.

## 5. Save data and preferences

Source: `i.java`, `javax/microedition/rms/RecordStore.java`

Two RMS stores are used by the game:

- `Preferences`: a one-byte value is loaded at startup and written back through a single-record store. The observed byte is initialized to zero when the store is absent.
- `DiamondRush`: game progress is loaded from record ID 1 and written back as a byte array. The initialization path allocates a 1001-byte buffer; the current used length is tracked separately. Writes use `addRecord` when empty and `setRecord(1,...)` otherwise.

The save buffer includes world/stage-related offsets and data assembled from the world map and world binary files. Exact field meanings, migration behavior, and compatibility constraints remain to be mapped.

**Native implication:** do not port the emulator's RMS implementation as-is. Define a versioned native save schema and a migration/import strategy only after the legacy byte layout is completely mapped. Preserve the original record bytes in tests before attempting conversion.

## 6. Audio container

Source: `j_SoundManager.java`

- `/snd.f` is opened as an audio container.
- The first byte is skipped as a file-count field.
- A header of 21 entries × 8 bytes is read.
- Each entry is used to obtain an audio payload length; the sound manager then reads the MIDI data and creates Java ME media players.
- The declared sound IDs cover 15 effects and 6 music tracks (IDs 0–20).

**Native implication:** keep the 21-entry logical sound ID mapping, but replace Java ME `Manager/Player/VolumeControl` with Android-native audio playback after payload extraction and MIDI compatibility review. Audio lifecycle and thread arbitration require a separate design.

## 7. Native implementation classification

| Candidate component | Decision | Reason |
|---|---|---|
| Gameplay rules/state in `i.java` | Recover and rewrite/adapt | Coupled to MIDP Canvas and emulator APIs; large decompiled monolith |
| `f_Sprite` binary decoder | Recover format, implement native decoder | Data format is useful; Java ME Image/Graphics output is not |
| `b_SpriteAnimator` | Reimplement behavior | Animation timing/state can be isolated from rendering |
| `readChunk` resource access | Reimplement safely | Existing stream reads/skip are not robust for partial reads |
| `RecordStore` usage | Replace with native persistence | Android storage lifecycle and versioned schema needed |
| `j_SoundManager` | Reimplement audio lifecycle | Java ME media Player APIs are not Android-native |
| FreeJ2ME desktop launcher/platform | Exclude from final runtime | AWT/desktop/emulator infrastructure is not the intended Android engine |

## 8. Open questions

- Validate the chunk directory count/offset/length against real `.f` files.
- Confirm every sprite pixel encoding and alpha rule with binary fixtures.
- Resolve parser comments about byte versus word counts in frame/animation tables.
- Map the exact semantics of the map records and `w*.bin` data.
- Identify all game-state transitions and collision/update routines inside `i.java`.
- Map save offsets and define a backward-compatible import strategy.
- Verify the actual logical orientation and 240×320 coordinate system before choosing Android viewport dimensions.

## 9. Evidence and limitations

This update is based on static inspection of the named source files at the revision above. It does not establish legal permission to reuse or redistribute decompiled code or game assets. No build, APK, runtime test, or binary resource extraction was performed.
