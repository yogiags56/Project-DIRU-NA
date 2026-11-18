# DIRU-NA-AUDIT-031 — Special-Stage Mechanics & Gameplay Configuration

## Scope and evidence boundary

This audit follows explicit stage constants and the stage initialization/game-loop branches in src/main/java/i.java from the read-only candidate kubikaugustyn/DiamondRushSource pinned at commit 215c71d04057d66a1b08419b1a4b1043ae3e0881. It records only behaviors directly supported by the inspected source. Obfuscated method/field names are retained where semantic naming is not established. This is static analysis, not runtime verification.

## 1. Special-stage configuration overview

| World | Stage index | Constant | Configuration observed |
|---|---:|---|---|
| Angkor | 5 | STAGE_ANGKOR_FALLING_TORCHES | kByte=1; alInt initialized to 816; amInt=0 |
| Angkor | 8 | STAGE_ANGKOR_GREAT_ANACONDA | kByte=4; initializes aoInt, aqInt, arInt, apInt and bounds O/P/Q/R; disables kBoolean; sets cLong flags |
| Angkor | 13 | STAGE_ANGKOR_DEMO | kByte=2; tBoolean=false; separate demo launch flow |
| Bavaria UI / Scotland resource | 9 | STAGE_BAVARIA_EVIL_TEUTONIC_KNIGHT | calls yVoid(); configures kByte=5 and a multi-state encounter |
| Siberia UI / Tibet resource | 10 | STAGE_SIBERIA_YETTI | calls adVoid(); sets aoInt=-1, aqInt=5, asInt=360, kByte=3, and cLong flags |

Important: `kByte` is a source-internal mode selector. Its semantic meaning is not globally named and it must not be treated as a stable public stage type or copied as a literal Native save value.

## 2. Angkor — Falling Torches (stage 5)

Initialization sets kByte=1, alInt=816, and amInt=0. The render/update path uses alInt as a vertical/world-position-like quantity relative to stage height and camera offset. It draws a repeated animated pattern using sprite animation slot 1 when the relevant position condition is met. A separate animation slot 2 controls transitions; when it reaches animation 2, the stage updates the vertical value and cycles animation slot 1.

While animation slot 1 is in its active state, alInt advances toward a cap of 1704, subject to camera/stage bounds. A collision condition compares stageHeight*24 - alInt against the player's vertical tile coordinate and calls hurtHero with damage parameters (64, 1).

Supported interpretation: this stage has a dedicated vertical hazard/animation sequence tied to a rising/falling boundary and player damage. The source excerpt does not establish the exact player-facing English description, timing in seconds, or full hazard trigger sequence.

## 3. Angkor — Great Anaconda (stage 8)

Initialization selects kByte=4 and sets several encounter parameters: aoInt=0, aqInt=3, arInt=0, apInt=0, kBoolean=false, and bounds/coordinates OInt=2, PInt=12, QInt=15, RInt=5. Rendering and update logic uses sprite animator slot 5 and an encounter state machine in aoInt.

Observed behavior in the encounter update:
- aoInt controls multiple movement/animation states (including 0–14 and terminal/transition states).
- The entity moves horizontally between world-coordinate bounds around 360 and 504.
- It reacts to player position and vertical position, including attack/collision checks that call hurtHero.
- State transitions use animation completion and a randomized timer (`jLong` seeded with a random interval 340–441 in the source time units).
- Additional stage objects/entities are spawned or changed at fixed grid positions; certain object states reduce aqInt, and when aqInt reaches zero the encounter transitions into aoInt=12 before a terminal state.
- Stage-specific rendering uses the animated entity and an effect sprite; exact visual identity is not independently confirmed from decoded image assets in this audit.

Supported interpretation: Great Anaconda is a dedicated multi-state boss/encounter sequence with movement, timed transitions, player collision/damage, and stage-object interactions. Exact attack names, health semantics, and win-condition presentation remain unverified.

## 4. Bavaria — Evil Teutonic Knight (stage 9)

Initialization calls yVoid(), which sets aoInt=13, aqInt=4, atInt=408, kByte=5, cLong flags 0x80 and 0x8, aeBoolean=false, and encounter bounds/coordinates OInt=16, PInt=16, QInt=19, RInt=18.

The kByte=5 update path is a state machine using sprite animator slot 5 and aoInt. It includes movement states, attack/animation states, player-position checks, collision damage, and transitions driven by animation completion. The source uses atInt as a moving horizontal coordinate and checks positions around 360 and 504. It also creates/updates player-layer objects at fixed cells (including rows 16 and 19, columns 10 and 26) as encounter state changes.

Supported interpretation: this is a dedicated moving enemy/boss encounter with multiple movement/attack phases and arena-object interactions. The source does not provide a reliable human-readable mapping of every aoInt value to a named attack.

## 5. Siberia — Yetti (stage 10)

Initialization calls adVoid(), setting aoInt=-1 and aqInt=5, then sets asInt=360 and kByte=3, plus cLong flags 0x80 and 0x8.

The kByte=3 update/render path references sprite animator slot 5, positions the entity at asInt, and uses stage/camera coordinates around vertical coordinate 504. State transitions change asInt and aoInt, check player position, and call hurtHero on contact/attack conditions. When a timer threshold is reached (kLong + 80 in the inspected update), a fixed rectangular region of stage cells is cleared/reconfigured (x 14–21 at y 15), with player-layer code 44 and related timers/foreground state updated.

Supported interpretation: Yetti has a dedicated encounter state and a timed stage-layout/environment change. Exact visual identity, intended puzzle explanation, and complete defeat condition are not established by this source excerpt alone.

## 6. Demo stage — Angkor index 13

Demo configuration remains distinct: kByte=2 and tBoolean=false. Other code paths branch on kByte=2 for demo-specific UI and demo resource rendering, and startup assigns world 0/stage 13. It must not be conflated with the four named encounter stages above or inserted as a regular World Map node.

## 7. Native Android design requirements

- Implement each special encounter as a separate stage-specific controller/configuration, not as hardcoded assumptions in the general tile renderer.
- Preserve original stage indices and keep mode flags internal to the legacy compatibility layer only.
- Represent encounter state machines with explicit enums and deterministic update steps; isolate timers and randomization behind a reproducible game clock/random source for testing.
- Preserve collision/damage timing, animation transitions, arena object mutations, and stage-specific coordinates.
- Keep camera/render coordinates separate from logical tile coordinates.
- Do not label undocumented counters such as aqInt as health without further proof.
- Do not infer attack names or win conditions solely from decompiled variable names.
- Keep demo launch/UI paths separate from retail World Map navigation.

## 8. Open work

- Inspect sprite slots and animation tables used by these encounters; visually verify entity identity and animation meanings after provenance review.
- Trace all reads/writes of aoInt, aqInt, atInt, asInt, alInt, kLong, jLong, and relevant stage arrays within each special mode.
- Identify exact encounter completion/failure conditions and their progression/save effects.
- Determine whether Falling Torches alInt sequence is triggered by stage start, player progress, or another event.
- Confirm whether Great Anaconda aqInt and Evil Knight aqInt represent hit counts, phases, or another counter; do not assume health.
- Trace Yetti's region mutation and determine its gameplay consequence.
- Add deterministic unit tests for each encounter state machine and collision boundaries.
- Validate the four encounters against an original legally usable JAR/runtime and exact target variant.

## 9. Audit status

Completed at static-source level: extracted initialization parameters and dedicated update/render behavior for Falling Torches, Great Anaconda, Evil Teutonic Knight, and Yetti; confirmed demo mode remains separate. Partial semantics only: several counters, animation meanings, exact encounter completion conditions, and visual identities remain unresolved.

No Android build, APK, or runtime test was performed.