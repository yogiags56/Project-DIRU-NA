# DIRU-NA-DEV-003L — Foreground and Player-Layer Classifiers

**Status:** Pure classifiers and synthetic tests committed; no combined movement evaluator  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchors:** `i.java:12113–12239`, `i.java:12243–12300`, `i.java:12344–12413`

## 1. Foreground classifier

The legacy helper switches on `(byte) foregroundRaw`, equivalent to the low byte. The native classifier exposes `raw & 0xFF` and labels explicit cases:
- 19 scripted path;
- 1, 7, 4, 28, 5, 2, 6, and 3;
- all other values remain OTHER.

These are branch labels only. They do not infer passability. Foreground type 7 additionally reads metadata bits; type 4 uses checkpoint progression; type 2 uses inventory state; type 19 performs script/array operations.

## 2. Player-layer classifier

The helper's explicit switch cases are preserved as separate labels:
- -1 empty-cell branch;
- 34/35 probe-dependent;
- 31/49 branch assigns false to the local collision flag;
- explicit blocking set: 1,2,4,5,6,7,11,14,19,24,26,27,33,40,41,42,43,45,50,51,52,53;
- 10 mode-dependent on `xByte == 3`;
- 0,8,9,47 use multi-cell/two-step logic;
- 48 has dedicated object geometry and mutation;
- 28 has dedicated state handling;
- default values remain OTHER_DEFAULT.

The labels do not replace probe mode, timers, coordinate checks, or mutation behavior. In particular, 31/49 are not described as globally walkable, and 34/35 are not assigned a fixed result.

## 3. Tests

Tests verify every explicit foreground case, low-byte masking, all player-layer labels and explicit blocking members, special branches, and unknown-value handling.

## 4. Boundaries

These are classifiers, not a combined collision evaluator. No movement, stage mutation, animation, object push, or Android runtime behavior is implemented by this milestone.

## 5. CI

Run #27 (DEV-003J out-of-bounds fixture correction) passed. Run #28 (DEV-003K empty-cell predicate) passed, including core JVM tests, Android debug APK assembly, and artifact upload.

## 6. Next

**DEV-003M — Pure Classification Matrix and Probe Semantics.** Document how foreground classification, player-layer classification, and probe mode combine in the helper, while retaining special-case branches and avoiding a misleading single passability boolean.
