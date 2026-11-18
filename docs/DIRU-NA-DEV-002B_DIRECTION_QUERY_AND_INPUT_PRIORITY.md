# DIRU-NA-DEV-002B — Direction Query and Input Priority

**Status:** Resolver and regression tests committed; CI execution pending  
**Date:** 2026-11-18  
**Repository:** yogiags56/Project-DIRU-NA

## 1. Scope

Implement the normal in-stage directional query order as a pure-core resolver. This does not implement Android key mapping, touch input, special movement, center/action handling, interaction, or player movement.

## 2. Pinned source evidence

Read-only candidate: kubikaugustyn/DiamondRushSource  
Revision: 215c71d04057d66a1b08419b1a4b1043ae3e0881  
Primary file: src/main/java/i.java  
Relevant handler: handleKeyPresses(), in-stage state branch (bByte == 1), around source lines 1258–1265.

Before the normal directional branch, the source applies several gates:
- selected status flags clear the global held-key mask;
- scripted/out-of-stage movement conditions can override ordinary input;
- an active interaction object receives center/confirm input and then clears the held-key mask;
- a special movement timer (aTInt > 0) uses a separate directional-offset path.

Only when those gates are passed and aTInt <= 0 does the ordinary direction branch execute.

## 3. Exact normal movement priority

The source uses this ordered if / else-if chain:

1. UP mask 4097 -> pending direction 1
2. DOWN mask 262146 -> pending direction 3
3. LEFT mask 16388 -> pending direction 4
4. RIGHT mask 65544 -> pending direction 2
5. CENTER / OK mask 32784 -> clear input and execute checkpoint/context-action logic

The order is behaviorally significant. If multiple directional masks are simultaneously held, only the first matching branch is selected. The resolver reproduces the first four direction checks and deliberately leaves CENTER / OK to the higher-level action dispatcher.

The broad confirm mask 32944 is not part of this ordinary directional chain. In the special movement branch it is considered after directional and center input, where it invokes aeVoid() and clears input.

## 4. Special movement branch is separate

When aTInt > 0, the source uses a distinct priority chain:
- UP sets avInt = -5;
- DOWN sets avInt = 5;
- LEFT sets auInt = -5;
- RIGHT sets auInt = 5;
- CENTER / OK is checked next;
- broad confirm 32944 is checked only after those options.

A selected input consumes one unit of aTInt and clears the held-key mask. The resolver in this commit does not represent these offset mutations and must not be used as a substitute for this branch.

## 5. Implemented type

LegacyDirectionResolver.resolveNormalStageDirection(HeldKeyState) returns the source's pending direction code:
- NONE = 0;
- UP = 1;
- RIGHT = 2;
- DOWN = 3;
- LEFT = 4.

The implementation preserves the normal branch order UP, DOWN, LEFT, RIGHT. It reads but does not mutate the held-key state.

## 6. Regression coverage

Eleven JUnit 4 test methods cover:
- no directional input;
- all four individual directions;
- all six two-direction priority combinations;
- all four directions held simultaneously;
- no mutation of the held-key state;
- action/non-direction bits not creating a direction.

These are deterministic unit tests for the resolver only. They do not claim Android input integration or gameplay movement validation.

## 7. Next work item

**DIRU-NA-DEV-002C — State-aware Input Gate and Action Dispatch Contract.** Model the source's pre-direction gates and keep center, broad confirm, special movement, and normal direction resolution separate. Do not connect to Android callbacks until lifecycle cancellation and held-key clearing are specified.
