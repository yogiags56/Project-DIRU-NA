# DIRU-NA-DEV-003T — Type 19 Safe Script Scan Planner

**Status:** Non-mutating bounded scan planner and synthetic tests committed; script side effects not implemented
**Repository:** yogiags56/Project-DIRU-NA
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881
**Source anchor:** `i.java:12113–12139`

## CI baseline

Run #36 passed: core JVM tests, Android debug APK assembly, and artifact upload.

## Source behavior

When probe is false, foreground type 19 computes `anchorX = targetX + 3`, searches upward from `targetY` in the player layer for raw value 39, then increments from the anchor row and checks foreground at `targetX`. Each subsequent cell whose low byte is 19 is cleared in the original source; the first non-19 cell terminates the loop. The anchor row itself is not part of the downward clear loop.

The source has no explicit bounds checks for either scan. Probe mode exits the type-19 case before scanning or mutation.

## Native safety policy

`LegacyType19ScriptScanPlanner` models the scan without writing data. It rejects an out-of-bounds anchor column, a missing upward anchor, and a downward run that reaches the stage bottom without a non-19 terminator. On any incomplete downward scan it returns no clear rows, so callers cannot accidentally apply a partial mutation.

## Tests

Synthetic tests cover the anchor and clear-row sequence, probe suppression, out-of-bounds anchor column, missing anchor, unterminated downward run, and non-type-19 input.

## Boundary

No `arVoid`, `aByte`, `mVoid`, `lBoolean/alBoolean` mutation, auxiliary-grid creation, stage writes, or movement result is executed. Those effects require separate source-grounded modeling.

## Next

**DEV-003U — Type 19 Side-Effect Intent Record.** Map the exact callback and flag ordering into an immutable plan before any integration layer is allowed to apply changes.