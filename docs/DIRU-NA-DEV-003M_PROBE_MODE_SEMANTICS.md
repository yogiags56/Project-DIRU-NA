# DIRU-NA-DEV-003M — Probe Mode Semantics

**Status:** Probe policy helpers and synthetic tests committed; combined collision evaluator not implemented  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchor:** `i.java:12073–12300`

## 1. Meaning of the boolean parameter

The legacy method is `aBoolean(int dx, int dy, boolean probe)`. The boolean is not a global "ignore collision" switch. It changes selected branches and suppresses selected side effects, while other checks still execute.

## 2. Source-grounded behavior matrix

| Branch | probe=false | probe=true |
|---|---|---|
| Foreground type 19 | Run scripted scan/mutation path | Skip that scripted path |
| Player layer 34/35 | Local collision value false | Local collision value true |
| Foreground type 1 | Apply timer/counter and recursive clear effects | Suppress those effects |
| Foreground checkpoint type 4 | Update checkpoint progression/save state when threshold passes | Suppress checkpoint state update |
| Player layer type 10 | Initialize marker only if xByte==3 and auxiliary value <=0 | Suppress marker write |
| Player layer type 28 or 5 | Apply local state changes | Suppress those state changes |

This matrix records only the visible probe-dependent statements. It is not a complete movement truth table and does not imply that probe mode bypasses bounds or every collision branch.

## 3. Implementation

`LegacyCollisionProbeSemantics` exposes narrow pure functions for the conditions above. It does not read stage data, mutate the stage, or decide movement success.

## 4. Tests

Synthetic tests cover both probe states for type 19, player-layer 34/35, type 1, checkpoint, type 10 marker conditions, and type 28/5 effects.

## 5. CI

Run #29 for DEV-003L completed successfully, including core JVM tests, Android debug APK assembly, and artifact upload.

## 6. Next

**DEV-003N — Foreground/Player-Layer Precedence Matrix.** Build a source-anchored decision table for which foreground branches short-circuit player-layer evaluation and which allow it to continue. Keep metadata thresholds and special branches explicit. Do not collapse the legacy method into a single generic passability classifier.
