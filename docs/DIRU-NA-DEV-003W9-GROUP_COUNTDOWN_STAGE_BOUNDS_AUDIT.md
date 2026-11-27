# DIRU-NA DEV-003W9.7 — Group countdown, mode gating, and stage-boundary audit

## Source identity
Static source: `kubikaugustyn/DiamondRushSource/src/main/java/i.java` at `215c71d04057d66a1b08419b1a4b1043ae3e0881`.

Static decompiled-source audit only. No binary stage corpus or retail runtime was executed in this milestone.

## 1. Group countdown allocation and initialization

- `mByteArr` is populated while parsing stage package records (record type 17).
- The group index is carried in `n43`; the initial countdown comes from a parsed group-value table (`hashtable2`), defaulting to zero when absent.
- A parallel `lByteArr` entry is populated from a second parsed group table (`hashtable3`), defaulting to 57 when absent.
- The source assigns parsed values using Java byte narrowing. Native code must preserve signed-byte interpretation where later compared/decremented.
- The arrays are stage-derived metadata and are not independent runtime-created object lists.

## 2. cmInt selection and activation

- On the foreground interaction path for tile code 26, the active group index is extracted from the foreground cell high bits: `cmInt = foreground[x][y] >> 8`.
- The group index is range-checked against `mByteArr.length`; invalid values set `cmInt = -1`.
- For a valid group, the game starts the associated interaction/audio state, scans interior stage cells (Y-major, then X-minor) for foreground low-byte code 0x11 with matching high-byte group ID, and resolves an associated door/foreground target.
- The scan may use player-layer code 18 or the foreground tile above (codes 7, 14, 33) to resolve target coordinates. The inspected excerpt confirms that matching group members are processed and the activating foreground tile is cleared to -1 afterward.
- This is stronger evidence that cmInt/mByteArr represents a group-linked timed interaction, likely a door mechanism, rather than a generic object counter. Keep semantic naming descriptive (e.g. `activeForegroundGroupId`) until every package record and transition is mapped.

## 3. alVoid countdown completion and mode gating

- `alVoid()` returns early when `kByte` is 3, 4, or 5 and `aqInt > 0`.
- Otherwise, if `cmInt >= 0`, it decrements the selected signed byte countdown in `mByteArr`.
- When the resulting byte equals zero, it plays sound ID 8 and scans the interior cells for group members (foreground low byte 0x11, matching high-byte group ID).
- For each match, it inspects the foreground tile above:
  - code 7: if the packed high payload's 0xF0 bits are clear, sets payload bit 0x10 and cByte=24;
  - code 14 or 33: replaces the packed foreground value with the low-byte code, clearing group metadata.
- The exact early-return mode guard is tied to special gameplay modes. Do not replace it with a generic pause flag.
- `aqInt` is written by multiple mode-specific state transitions and used elsewhere as a mode/phase counter; it is not a door countdown. Keep it separate from `mByteArr`.

## 4. Checkpoint interaction

- The checkpoint restore path sets `cmInt = cnInt` and copies `oByteArr` into `mByteArr` when the latter exists.
- Thus the active group selector and countdown values have explicit checkpoint restoration behavior.
- The checkpoint's source arrays must remain the authority; do not independently reconstruct countdowns from foreground tiles after restore.
- A full audit of cnInt/oByteArr writers and snapshot capture ordering remains open.

## 5. Stage boundary findings

- The relevant group activation and completion scans intentionally iterate interior cells only: X=1..width-2 and Y=1..height-2.
- This proves the scan itself excludes the outer border, but does not prove all stage assets have a fully populated safe border for other routines.
- Other helpers use different bounds and direct neighbor reads. A global "one-cell solid border" invariant cannot be asserted from this source excerpt alone.
- Do not add automatic border tiles, coordinate clamping, or out-of-bounds fallback behavior without binary stage evidence.

## 6. Native model/test requirements

- Preserve group ID extraction as arithmetic right shift of packed foreground int, and preserve low-byte tile-code checks.
- Preserve signed byte countdown semantics and byte narrowing at parse time.
- Keep `activeForegroundGroupId`, parsed group countdown array, parallel group metadata array, and mode-specific `aqInt` conceptually distinct.
- Tests: missing group table value defaults; 57 metadata default; byte values 0, 1, 127, 128, 255; invalid group index; multiple matching group cells; each above-tile case (7, 14, 33, other); mode guard combinations (kByte 3/4/5 with aqInt positive/zero); checkpoint restore of cmInt and countdown array.
- Stage-boundary tests must be based on actual legally usable stage fixtures, not synthetic assumption of a universal border.

## 7. Milestone boundary and next work

W9.7 is documentation/source audit only. No production group system, checkpoint serializer, collision activation, stage parser rewrite, or GameSession integration is added.

Next audit: trace `cnInt`, `oByteArr`, `lByteArr` lifecycle and checkpoint capture order; validate stage package record type 17 against legally usable binary fixtures; then design deterministic tests before implementing the native group mechanism.
