# Technical Audit — Initial Findings

## Scope

This document records preliminary findings from the candidate repository `kubikaugustyn/DiamondRushSource`. It is an audit note, not a verification that the candidate builds or that its contents may be redistributed.

## Repository inventory

- Approximately 329 Java source files.
- Approximately 49 files under `src/main/resources`.
- A very large decompiled `i.java` gameplay class (about 842 KB).
- Java ME, Nokia, Siemens, Mascot Micro3D, M3G, and FreeJ2ME-related compatibility classes are present.
- Desktop launcher and platform infrastructure use Java SE/AWT APIs and are not directly suitable for a Native Android runtime.

## Initial technical observations

- `GloftDIRU.java` is the decompiled MIDlet entry point.
- `i.java` extends Java ME `Canvas` and implements `Runnable`; it is a primary candidate for gameplay-flow analysis.
- `f_Sprite.java` contains custom sprite decoding paths, including several pixel and indexed/RLE formats.
- `j_SoundManager.java` uses Java ME media APIs and loads `/snd.f`.
- Resource names include `*.f`, `*.bin`, language files, map files, and UI-related files.
- The manifest identifies MIDP 2.0 / CLDC 1.0 and a Nokia MIDlet.
- Desktop launcher configuration and drawing code include 240×320 evidence, while the historical target for this project was 320×240. This discrepancy remains unresolved.

## Build and dependency caveats

- The candidate's Maven metadata appears to contain a self-dependency; this needs confirmation.
- Its batch compilation script appears to rely on external Java ME compiler/API artifacts and has path/recursion concerns.
- Compiled files in `target/classes` do not prove that the current source tree can be reproduced from a clean checkout.
- No clean build or APK build has been performed as part of this initial audit.

## Licensing and provenance

- No top-level LICENSE/COPYING/NOTICE file was identified in the initial inventory.
- Some FreeJ2ME-related files contain GPLv3 headers.
- The rights status of decompiled Gameloft code and original game assets is unresolved.
- Do not copy candidate source or proprietary resources into this repository until file-level provenance and redistribution permissions are reviewed.

## Required next checks

1. Map every resource load call site in gameplay and rendering classes.
2. Identify file formats and decode rules for `.f`, `.bin`, and map data.
3. Trace the main loop, input handling, collision, animation, and save operations.
4. Enumerate only the Java ME/vendor APIs actually reached by the game.
5. Resolve orientation and logical viewport behavior from code and runtime evidence.
6. Classify each dependency as retain, adapt, replace, or exclude.
7. Record reproducible tests and source references for each conclusion.

## Evidence policy

Every future audit update should include the file path, relevant symbol or line range, observation, confidence level, and test evidence where applicable. Unknowns must remain explicitly marked as unknown.
