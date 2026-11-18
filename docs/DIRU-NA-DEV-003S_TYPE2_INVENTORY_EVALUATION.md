# DIRU-NA-DEV-003S — Foreground Type 2 Inventory Evaluation

**Status:** Pure decision model and synthetic tests committed; no inventory mutation or full movement evaluator  
**Date:** 2026-11-19  
**Repository:** yogiags56/Project-DIRU-NA  
**Read-only source:** kubikaugustyn/DiamondRushSource@215c71d04057d66a1b08419b1a4b1043ae3e0881  
**Source anchor:** `i.java:12181–12224`

## 1. CI #35 checkpoint fixture correction

Run #35 failed because the equality-threshold fixture encoded raw value 4, which is foreground type 4 with checkpoint high-value 0. The correct packed cell for foreground type 4 and checkpoint value 4 is `0x00000404`. The production extraction `(raw & 0xFFFFFF00) >> 8` is unchanged.

## 2. Source behavior

The foreground type-2 branch enters its body only when probe is false. It sets auxiliary local flag `b3=true`, then switches on signed `raw >> 8`:
- high value 0: inventory slot 9 >= 1 selects `aDInt=7, aEInt=0`; otherwise `aDInt=13, aEInt=1`;
- high value 1: inventory slot 9 >= 2 selects `aDInt=7, aEInt=1`; otherwise `aDInt=13, aEInt=1`;
- all other signed high values take the labelled default exit, preserving existing `aDInt/aEInt`, while `b3` has already been set.

Probe mode skips the type-2 body, so it does not set `b3` or update either counter. The outer foreground branch exits the foreground switch in both modes, so player-layer evaluation is skipped.

## 3. Implementation

`LegacyForegroundType2Evaluator` returns proposed counter values and auxiliary-flag intent only. It does not access inventory storage, update fields, or execute gameplay side effects.

## 4. Tests

Tests cover high values 0/1, inventory thresholds 1/2, insufficient inventory, signed default values, probe suppression, and player-layer flow.

## 5. Next

**DEV-003T — Type 19 Scripted Foreground Boundary.** Model only the preconditions and safe bounded scan request for type 19; do not execute script mutation in this pure stage.
