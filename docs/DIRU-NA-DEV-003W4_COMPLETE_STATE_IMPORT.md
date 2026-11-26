# DIRU-NA DEV-003W4 — Complete collision state import

## Added

- `LegacyCollisionStageStateSeed` is an immutable, defensive-copy import of the decoded StageGrid, packed-metadata cells, auxiliary cells, foreground-cache cells, player tile/direction/interpolation fields, animation ID/frame, and movement-block flag.
- The production `LegacyCollisionStageStateStore(LegacyCollisionStageStateSeed)` constructor now imports those exact values into its initial root.
- The convenience `LegacyCollisionStageStateStore(StageGrid)` constructor remains for isolated tests/prototypes and explicitly seeds the additional layers/state to zero. It is not a production decode path.
- Seed construction validates all per-cell array lengths and defensively copies inputs. The store copies them again into its private root.
- Tests verify decoded metadata/auxiliary/cache values, all scalar state channels, defensive copies, and invalid layer lengths.

## Integration boundary

This closes the missing initial-state import API, but no production decoder currently constructs the seed from verified legacy stage/session data. The constructor is ready for that adapter; it is not proof that the decoded legacy metadata layout is already wired.

`GameSession` still owns its existing immutable StageGrid and local movement fields independently. Do not create two competing gameplay owners. Integration must transfer ownership or refactor GameSession to publish/read through one store on its owner thread.

## Next

DEV-003W5: audit the stage decoder's packed metadata and auxiliary formats, add a source-derived seed factory from decoded stage/session state, then map only fully verified evaluator decisions into plans. Add round-trip fixtures from real stage assets before enabling any movement branch.
