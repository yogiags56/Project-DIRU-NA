# DIRU-NA DEV-003W9.1 — Step-9 mutation ledger

## Source identity and scope

Static inspection target: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at commit `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

This ledger records observable operations in the decompiled candidate. It is not proof of retail-device equivalence. Numeric tile IDs and decompiler variable names remain raw evidence until independently mapped to game semantics.

## Critical correction: there are multiple scan orders

Do not describe all of step 9 as one x-major pass.

1. **Foreground/player preprocessing pass:** x outer, y inner (`for x < width`, then `for y < height`). At each cell it resets `bByteArrArr[x][y]` and `cByteArrArr[x][y]`, reads foreground/background/player values, and performs ordered foreground and player switch effects. Neighbor writes can affect later cells in this same traversal.
2. **Derived foreground/object pass:** y outer, x inner (`for y < height`, then `for x < width`). It reads the packed foreground value and player layer, builds arrays/tables, and can mutate neighboring foreground and cByte cells. This pass is a different traversal and must remain separate.

The source's legacy grids are x-major Java arrays `grid[x][y]`. Native `StageGrid` is flat row-major `x + y * width`; conversion must be explicit.

## Pass A — x-major/y-minor preprocessing

### Per-cell initialization
- Reset `bByteArrArr[x][y]` to 0.
- Reset `cByteArrArr[x][y]` to 0.
- Capture current foreground, background runtime and signed player byte before entering the corresponding switch logic.

### Foreground branch evidence
- Foreground codes 31, 1, 2, 3, 4, 5, 6 and 34 have dedicated branches in the inspected section; selected codes are packed with background in high bits.
- Code 3 sets cByte to 127; when background > 0 it packs `(background + 1) << 8 | 3`.
- Code 6 increments a Hashtable count keyed by background, sets a feature bit, and packs `background << 8 | 6`.
- The default path preserves foreground values 20..25 and sets a world-dependent feature bit for supported worlds; other values in the range 0..79 are normalized to -1.
- The full switch must be transcribed before implementation; this list is not a complete branch inventory.

### Player/object branch evidence
- Code 48 derives background runtime from `background & 7`, increments `awInt`, sets feature bits, writes player code 48 and background value 8 at y-1, then invokes `kVoid(x,y-1)`. This has an above-cell precondition and is order-sensitive.
- Code 47 sets cByte 48, clears background runtime, and sets a feature bit.
- Code 46 clears background, sets cByte 24, clears bByte, and sets a feature bit.
- Code 45 clears background and sets cByte 24.
- Code 44 sets cByte 24 and clears background.
- Code 42 increments entity counters and invokes `iVoid(x,y)`.
- Code 41 ensures background is at least 1, then adds its value to aYInt.
- Code 40 sets feature/mode state, increments counters and invokes `iVoid(x,y)`.
- Code 12 clears its player cell and stores x/y/background in auxiliary scalar fields.
- Code 36 preserves background value 1, otherwise clears it, and sets a feature bit.
- Code 18 resets two scalar fields and sets feature bits.
- Code 34 clears player cell, writes foreground 15 and sets a feature bit.
- Code 35 writes player 35, clears foreground, sets a feature bit and fan-pot flag; it falls through to cases 31/33 which continue.
- Codes 39 and 38 set optional-mode state; code 38 also writes foreground 27 and sets another feature bit.
- Code 14 conditionally rewrites background (4 becomes 8, otherwise 0), sets cByte 24 and a feature bit.
- Code 28 conditionally divides background by 11 and ORs 8 when its captured background value exceeds 10; sets cByte 24 and a feature bit.
- Code 79 stores player start/camera-related values, clears the player marker and resets two motion offsets.
- Code 11 sets background to 16 only when captured background equals 1, otherwise 0; sets cByte 48 and a feature bit.
- Code 49 sets cByte 48 and a feature bit.
- Code 43 writes a world-dependent feature bit, a bit in `buInt`, masks/sets background bits and sets cByte 48.
- Code 19 writes a world-dependent feature bit, another `buInt` bit and cByte 48.
- Codes 22/23 set feature bits and cByte 48.
- Codes 30/37/10 clear background with distinct feature or dLong side effects.
- Code 16 checks player layer at y+1; if it is not 16, writes player 16 and captured background at y-1. It sets a feature bit.
- Codes 6,7,26,24,27,8,4,5,2,0,1 have significant Java switch fall-through. Preserve the exact ordered side effects; do not turn them into independent handlers.
- Default player values in 0..79 are normalized to -1 through the deferred byte-array write; other values continue unchanged.

### Post-pass allocation evidence
After Pass A, source allocates arrays from counters accumulated during the pass, including:
- `dByteArr` sized from n8.
- `aClasscArr` and `pByteArr` sized from n11.
- `kByteArr` sized from n9.
- `mByteArr` and `lByteArr` sized from the highest set bit in n10.
- `eByteArr` sized from awInt when awInt > 0.

These allocations depend on counters and feature bits; they cannot be safely fabricated from tile dimensions alone.

## Pass B — y-major/x-minor derived foreground and object tables

- Reads unsigned low byte of packed foreground as code and arithmetic-shifted high portion as group/background payload.
- Codes 0 and 30 append payload values to pByteArr.
- Code 7 consults the group Hashtable, repacks the foreground entry, checks neighbors at y-1, y+1, x+1 and x-1 for code 17 / 26 conditions, may set cByte to 24, and may clear the foreground cell above.
- Code 4 stores x/y into kByteArr using payload as index.
- Code 5 stores x/y at the n9 slot in kByteArr.
- Code 17 uses group Hashtables to populate mByteArr and lByteArr; missing dialog selector values default to 57 in the inspected code.
- A later player-layer switch handles code 48 and uses background flags plus neighboring cells to create eByteArr coordinate records; the complete branch body remains to be traced.

### Neighbor safety
Pass B directly reads y-1, y+1, x-1 and x+1 around code-7 foreground entries. Pass A code 48 writes y-1 and code 16 may access y+1/y-1. Do not silently clamp, wrap or skip. Determine source asset preconditions and source exception behavior; then decide whether the native loader should reject malformed boundary layouts before preprocessing.

## Implementation gates

Before a production executor is allowed:
1. Complete the remaining foreground switch inventory, including all branches before and after the captured excerpt.
2. Complete the full player-layer switch and every helper invoked (`iVoid`, `kVoid`, `dInt` and relevant object constructors).
3. Trace all counter and Hashtable initialization, keys, insertion order assumptions and allocation consumers.
4. Trace Pass B player-layer switch and eByteArr record semantics.
5. Reconcile checkpointed vs rebuilt arrays and scalar fields.
6. Build fixtures with provenance and expected before/after grids.
7. Preserve Pass A x-major/y-minor and Pass B y-major/x-minor order independently.

## Current milestone

W9.1 is documentation/evidence only. No production preprocessing algorithm, collision activation or GameSession wiring is introduced. Next slice: finish Pass B player-object record trace and enumerate the remaining Pass A foreground switch before writing deterministic transformation code.
