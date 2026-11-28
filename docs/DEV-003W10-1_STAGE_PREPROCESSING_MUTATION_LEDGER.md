# DEV-003W10.1 — Stage preprocessing mutation ledger (source pass 1)

## Source identity and evidence boundary

- Candidate: `kubikaugustyn/DiamondRushSource/src/main/java/i.java`
- Pinned revision: `215c71d04057d66a1b08419b1a4b1043ae3e0881`
- Scope: stage initialization step 9, including the complete nested tile scan and post-scan allocations visible in the inspected method.
- Evidence type: static decompiled-source audit. No candidate runtime was executed and no original binary stage was modified or redistributed.
- This is a mutation ledger, not a Native implementation. The original method contains Java switch fall-through; every native translation must preserve it explicitly rather than treating each case as an isolated branch.

## 1. Pass order and input channels

The pass iterates X outermost and Y innermost:

`for x = 0..width-1; for y = 0..height-1`

At each cell it reads the authored/transient channels:
- `foreground = crtStageForegrondLayer[x][y]` (packed int; low byte is tile code, upper bits carry metadata)
- `background = bIntArrArr[x][y]` (int)
- `player = crtStagePlayerLayer[x][y]` (signed Java byte)

At the start of each cell:
- `bByteArrArr[x][y] = 0`
- `cByteArrArr[x][y] = 0`

Foreground preprocessing occurs before player/object-layer preprocessing for the same cell. Player-layer cases may then mutate foreground, background runtime, and either byte-state grid. Any port that performs separate whole-grid passes must prove equivalence to this interleaved order.

## 2. Foreground mutation ledger

The following table records the first foreground switch. All codes are read from `foreground & 0xFF`; the packed int must not be flattened into a tile enum.

| Authored low-byte code | Observed effects |
|---|---|
| 31 | Set feature bit `cLong |= 0x40000000`; rewrite foreground as `background << 8 | 31`; fall through to code 19 packing. |
| 19 | Rewrite foreground as `background << 8 | 19`. |
| 17 | Inspect foreground and player cells above. If above foreground code is 14/33, set its packed high value to `0xFF00 | code`. If above player code is 19/36/43/45/46/49, increment hashtable2[background], clear this foreground cell, and update hashtable3[background] (36 selects 58 only if absent; others select 56 or mode-specific 51/52/53). Otherwise rewrite current foreground as `background << 8 | 0x11`. For nonnegative background, set bit `1 << background` in group mask n10. |
| 14 / 33 | If `fBoolean()` is false, normalize foreground to 33. Set feature bit 22 or 33 according to resulting low byte. If stage-specific predicate `aBoolean(world,stage,x,y)` is true: if `fBoolean()` true, set player=41, background runtime=10, add 10 to aYInt; otherwise set player=-1 and OR 0x100 into foreground. |
| 2 | Set feature bit 0x100000; if background is 0 or 1 also set 0x80000; pack background into high bits. |
| 8 | Set `ahBoolean=true`, then fall through to code 9 behavior. |
| 9 | If original low byte is not 8, set water-breathing-potion load flag; set feature bit 0x10000000; increment hashtable[background]; pack background into high bits while retaining low byte. |
| 7 | If background != -1, write x/y into door-coordinate arrays indexed by background; pack background and code. Index bounds are not checked in this branch. |
| 30 | Set feature bit 0x40000000, increment entity count n11, then fall through to code 1/26 packing. |
| 1 / 26 | Pack background into high bits, retain low-byte code. |
| 0 | Increment n11 and pack background + code. |
| 4 | Increment n9, set feature bit 0x10, pack background + code. |
| 5 | Store background narrowed to byte in pByte. |
| 28 | Store background narrowed to byte in qByte. |
| 3 | Set cByte=127. If background>0, rewrite packed foreground high field as background+1; otherwise leave foreground as authored. |
| 6 | Increment hashtable[background], set feature bit 0x20000000, pack background + code. |
| 20..25 (default range) | Retain foreground value without packing; for world 0 set feature bit 0x10, for world 1 set feature bit 0x200000. Other worlds exit this foreground branch. |
| 34 | Set feature bit 0x400000000; no foreground rewrite in this case. |
| Other codes 0..79 not matched above | Clear foreground to -1 when code is in (0,80); otherwise leave unchanged. |

### Foreground branch cautions

- The case 17 branch uses `background` as a group/table key; the resulting group count is later materialized into `mByteArr` and `lByteArr`. It is not equivalent to assigning one group slot per encountered cell.
- Cases 31→19, 8→9, and 30→1/26 are deliberate fall-through.
- For code 14/33, `fBoolean()` is evaluated in more than one place; do not cache one result unless proven pure and stable for that cell.
- The foreground branch may mutate player layer before the player switch reads `player`; however, `player` is a local byte captured before foreground mutations. Preserve this captured value for the subsequent switch, as the source does.

## 3. Player/object-layer mutation ledger

The player switch uses the captured signed byte `player`. The effects below are ordered and include fall-through where present.

| Player code | Observed effects |
|---|---|
| 48 | Set background runtime to 16 if `background & 7 == 4`, else 0; increment awInt; set feature bits 0x10000000000 and 0x100000000; convert cell above to player code 48 with background runtime 8; invoke neighborhood reset `kVoid(x,y-1)`. |
| 47 | Set cByte=48, background runtime=0; set feature bit 0x800000. |
| 46 | Set background runtime=0, cByte=24, bByte=0; set feature bit 0x2000000000. |
| 45 | Set background runtime=0, cByte=24; set feature bit 0x800000000. |
| 44 | Set cByte=24, background runtime=0; set feature bit 0x400000000. |
| 42 | Increment n11 and n8; set feature bits 0x80000000 and 0x40000000; call object initializer `iVoid(x,y)`. |
| 41 | If background runtime <=0, set it to 1; add resulting value to aYInt. |
| 40 | Set feature bits 0x40000000 and 0x8000000; increment n11; set lBoolean=true; call `iVoid(x,y)`; increment n8. |
| 12 | Clear player cell; set checkpoint/start-related scalar candidates abInt=x, acInt=y, aaInt=background. Exact semantics remain provisional. |
| 36 | Set background runtime to 0 unless it already equals 1; set feature bit 0x100. |
| 18 | Reset ceInt/cfInt; set feature bits 0x8000000000 and 0x80. |
| 34 | Clear player cell; set foreground=15; set feature bit 0x1000000. |
| 35 | Set player cell to 35; clear foreground; set feature bit 0x1000000 and fan-pot load flag; fall through to codes 31/33 (no additional effect). |
| 31 / 33 | No further mutation in this switch. |
| 39 | Set lBoolean=true; set feature bit 0x4000000. |
| 38 | Set lBoolean=true; set feature bits 0x4000000 and 0x40; rewrite foreground=27. |
| 14 | Set feature bit 0x1000; background runtime becomes 8 iff authored background equals 4, otherwise 0; set cByte=24. |
| 28 | Set feature bit 0x800; if background runtime >10, divide by 11 then OR 8; set cByte=24. |
| 79 | Set crtPlayerX=0, crtPlayerY=y, ayInt=x; clear player cell; reset cInt/aInt and set dInt/bInt to crtPlayerY*24-160. This exact coordinate interpretation requires fixture/runtime confirmation. |
| 11 | Set background runtime to 16 iff authored background==1, else 0; set cByte=48; set feature bit 0x4000. |
| 49 | Set feature bit 0x20000000000; set cByte=48. |
| 43 | Set world-dependent feature bit 17 (world 1) or 15 (otherwise); OR 1 into buInt; rewrite background runtime with mask `(value & 0xFFFE7FFF) | 0x10000`; set cByte=48. |
| 19 | Set same world-dependent feature bit; OR 2 into buInt; set cByte=48. |
| 22 / 23 | Set feature bits 0x200 and 0x400; set cByte=48. |
| 30 | Set feature bit 0x80; set background runtime=0. |
| 37 | Set feature bit 0x2000000; set background runtime=0. |
| 10 | Set background runtime=0; OR 0x2 into dLong. |
| 16 | If player cell below is not 16, set player cell above to 16 and copy authored background into background runtime above; set feature bit 0x2000. |
| 6 | Increment n8; call `iVoid(x,y)`; fall through to code 7. |
| 7 | Set feature bit 0x10; set background runtime=0; fall through to code 0. |
| 26 | Set feature bit 0x10000000000; fall through to 24/27. |
| 24 / 27 | Increment n11 and n8; set feature bits 0x40000000 and 0x80000; call `iVoid(x,y)`. |
| 8 | Set feature bits 0x20 and 0x8; fall through to code 4. |
| 4 | If captured player value !=8, call `iVoid(x,y)`; fall through to code 5. |
| 5 | Set feature bit 0x4; fall through to code 2. |
| 2 | Increment n8; fall through to code 0. |
| 0 | Set cByte=48, background runtime=0; OR 0x1 into dLong. |
| 1 | Increment aYInt; set cByte=48, background runtime=0; fall through to empty cases 53,51,52. |
| 53 / 51 / 52 | No additional mutation; continue scan. |
| Default | If captured player byte is 0..79, clear player cell to -1; otherwise leave it unchanged. |

## 4. Post-scan allocations and group materialization

After the full X-major/Y-minor scan:
- Allocate `dByteArr` with length `n8 << 1`; initialize every byte to zero.
- Allocate `aClasscArr` and `pByteArr` with length n11.
- Allocate `kByteArr` with length `(n9 + 1) << 1`.
- Determine group-array length by finding the highest set bit in n10 and adding one. If positive, allocate `mByteArr` and `lByteArr` at that length.
- For each nonnegative group key in hashtable2, set mByteArr[group] to its count (or zero if absent), narrowed to byte; set lByteArr[group] to hashtable3[group] (or default 57), narrowed to byte.
- Group-table construction and the later per-group assignment must preserve byte narrowing and source map semantics. See DEV-003W9 group/checkpoint audit for countdown and snapshot behavior.

## 5. Spawn, door and object initialization boundaries

- Foreground code 7 records door coordinates using the background value as an array index. A native parser/processor must validate the index before writing, but must not silently reinterpret malformed authored data.
- Player code 79 is the explicit observed spawn marker in this pass. It mutates several coordinate/camera fields and clears the marker. Keep all of these effects together in a named spawn-initialization operation, but do not assign semantic names to `ayInt` or the camera fields without further trace.
- Player codes 40, 42, 6, 24, 27 invoke `iVoid(x,y)` or related object allocation paths; this pass records calls but does not yet define their full object identity or runtime lifecycle.
- Player codes 48, 47, 46, 45, 44, 49, 43, 19, 22, 23 initialize moving/animated/runtime-state channels. Do not collapse these into foreground tile codes.
- The loop is X-major then Y-minor, and the player switch uses a captured local byte. Both facts are parity requirements.

## 6. Deterministic fixture plan

Create synthetic stage fixtures only for behaviors whose inputs and outputs are fully defined by this ledger. Include:
1. A cell for each foreground code and each player code, with all required neighboring cells initialized.
2. Fall-through assertions for 31→19, 8→9, 30→1/26, 35→31/33, 6→7→0, 7→0, 26→24/27, 8→4→5→2→0, and 1→53/51/52.
3. X-major/Y-minor scan order where multiple cells update shared counters or group hash tables.
4. Captured-player-value case where foreground preprocessing mutates the player cell before the player switch.
5. Group hash-table count/default and signed-byte boundary cases.
6. Door-index valid and invalid values.
7. Player code 79 spawn marker with nonzero x/y, asserting every directly observed field mutation without guessing its game meaning.
8. Byte-state initialization (zero at each cell, then branch-specific 127/24/48 values).
9. Object initializer call recording for codes 40,42,6,24,27, without inventing object contents.

No production implementation is added by this audit. Fixture expectations that depend on `fBoolean`, `aBoolean`, `iVoid`, or special-world configuration must be supplied by separate audited contracts rather than guessed.

## 7. Next

DEV-003W10.2 should trace `iVoid(x,y)` and all object/entity allocation paths reached by this preprocessing pass, including slot counts, constructor selection, packed metadata, and per-cell ownership. Keep this as a separate source trace; do not implement object runtime until allocation and lifecycle invariants are established.
