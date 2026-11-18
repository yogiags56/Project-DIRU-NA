package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bounds-checked, branch-specific read boundary for synthetic or native stage grids.
 * The caller selects a branch; this class only captures raw values and never
 * decides whether a movement is allowed.
 */
public final class LegacyCollisionNeighborhoodCapture {
    public interface Grid {
        int width();
        int height();
        int foregroundRawAt(int x, int y);
        int playerLayerRawAt(int x, int y);
        int packedMetadataRawAt(int x, int y);
        boolean hasAuxiliaryGrid();
        int auxiliaryRawAt(int x, int y);
    }

    public enum Status {
        COMPLETE,
        OUT_OF_BOUNDS,
        TYPE19_ANCHOR_NOT_FOUND,
        TYPE19_FOREGROUND_RUN_NOT_TERMINATED
    }

    public static final class Result {
        private final Status status;
        private final LegacyCollisionNeighborhoodSnapshot snapshot;
        private final LegacyCollisionCoordinateResolver.Coordinate failedCoordinate;

        private Result(Status status, LegacyCollisionNeighborhoodSnapshot snapshot,
                       LegacyCollisionCoordinateResolver.Coordinate failedCoordinate) {
            this.status = status;
            this.snapshot = snapshot;
            this.failedCoordinate = failedCoordinate;
        }
        public Status getStatus() { return status; }
        public boolean isComplete() { return status == Status.COMPLETE; }
        public LegacyCollisionNeighborhoodSnapshot getSnapshot() { return snapshot; }
        public LegacyCollisionCoordinateResolver.Coordinate getFailedCoordinate() {
            return failedCoordinate;
        }
    }

    private final LegacyCollisionCoordinateResolver.StageBounds bounds;

    public LegacyCollisionNeighborhoodCapture(int width, int height) {
        this.bounds = new LegacyCollisionCoordinateResolver.StageBounds(width, height);
    }

    public Result capture(LegacyCollisionBranchInputValidator.Branch branch,
                          Grid grid, int playerX, int playerY, int dx, int dy) {
        if (branch == null || grid == null) {
            throw new IllegalArgumentException("branch and grid are required");
        }
        if (grid.width() != bounds.getWidth() || grid.height() != bounds.getHeight()) {
            throw new IllegalArgumentException("grid dimensions do not match capture bounds");
        }
        Map<LegacyCollisionNeighborhoodSnapshot.Role,
                LegacyCollisionNeighborhoodSnapshot.CellSample> samples =
                new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                        LegacyCollisionNeighborhoodSnapshot.CellSample>(
                        LegacyCollisionNeighborhoodSnapshot.Role.class);
        LegacyCollisionCoordinateResolver.Coordinate target;
        try {
            target = LegacyCollisionCoordinateResolver.target(
                    playerX, playerY, dx, dy, bounds);
            put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TARGET, grid, target);

            if (branch == LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_0_8_9_47
                    || branch == LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_48) {
                LegacyCollisionCoordinateResolver.Coordinate two =
                        LegacyCollisionCoordinateResolver.twoSteps(
                                playerX, playerY, dx, dy, bounds);
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS, grid, two);
            }

            if (branch == LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_0_8_9_47
                    || branch == LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_EMPTY) {
                LegacyCollisionCoordinateResolver.Coordinate abovePlayer =
                        LegacyCollisionCoordinateResolver.targetAbovePlayerRow(
                                playerX, playerY, dx, bounds);
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE_PLAYER_ROW,
                        grid, abovePlayer);
            }

            if (branch == LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_EMPTY) {
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TARGET_ABOVE,
                        grid, LegacyCollisionCoordinateResolver.targetAbove(target, bounds));
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TARGET_BELOW,
                        grid, LegacyCollisionCoordinateResolver.targetBelow(target, bounds));
            }

            if (branch == LegacyCollisionBranchInputValidator.Branch.PLAYER_LAYER_TYPE_48) {
                int upper = (samples.get(LegacyCollisionNeighborhoodSnapshot.Role.TARGET)
                        .getPackedMetadataRaw() & 0x8) != 0 ? 0 : -1;
                int lower = upper + 1;
                LegacyCollisionCoordinateResolver.Coordinate two =
                        LegacyCollisionCoordinateResolver.twoSteps(
                                playerX, playerY, dx, dy, bounds);
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_DESTINATION_A,
                        grid, LegacyCollisionCoordinateResolver.type48Destination(two, upper, bounds));
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_DESTINATION_B,
                        grid, LegacyCollisionCoordinateResolver.type48Destination(two, lower, bounds));
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SOURCE_CLEAR_A,
                        grid, LegacyCollisionCoordinateResolver.type48SourceClear(target, upper, bounds));
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SOURCE_CLEAR_B,
                        grid, LegacyCollisionCoordinateResolver.type48SourceClear(target, lower, bounds));
                put(samples, LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SUPPORT,
                        grid, LegacyCollisionCoordinateResolver.type48Support(target, lower, bounds));
            }

            if (branch == LegacyCollisionBranchInputValidator.Branch.FOREGROUND_TYPE_19) {
                LegacyCollisionCoordinateResolver.Coordinate cursor =
                        LegacyCollisionCoordinateResolver.type19AnchorColumn(target, bounds);
                List<LegacyCollisionNeighborhoodSnapshot.CellSample> scan =
                        new ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample>();
                LegacyCollisionNeighborhoodSnapshot.CellSample anchor = null;
                while (true) {
                    LegacyCollisionNeighborhoodSnapshot.CellSample current = read(grid, cursor);
                    scan.add(current);
                    if (current.getPlayerLayerRaw() == 39) {
                        anchor = current;
                        break;
                    }
                    try {
                        cursor = LegacyCollisionCoordinateResolver.type19ScanNext(cursor, bounds);
                    } catch (IndexOutOfBoundsException e) {
                        return incomplete(Status.TYPE19_ANCHOR_NOT_FOUND, cursor);
                    }
                }
                List<LegacyCollisionNeighborhoodSnapshot.CellSample> run =
                        new ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample>();
                int runY = anchor.getY();
                while (true) {
                    LegacyCollisionCoordinateResolver.Coordinate next =
                            LegacyCollisionCoordinateResolver.type19ForegroundRunNext(
                                    target, runY, bounds);
                    LegacyCollisionNeighborhoodSnapshot.CellSample current = read(grid, next);
                    run.add(current);
                    runY = next.getY();
                    if ((current.getForegroundRaw() & 0xFF) != 19) break;
                }
                LegacyCollisionNeighborhoodSnapshot snapshot =
                        new LegacyCollisionNeighborhoodSnapshot(samples, anchor, scan, run);
                return new Result(Status.COMPLETE, snapshot, null);
            }
            return new Result(Status.COMPLETE,
                    new LegacyCollisionNeighborhoodSnapshot(samples, null, null), null);
        } catch (IndexOutOfBoundsException e) {
            return incomplete(Status.OUT_OF_BOUNDS, null);
        }
    }

    private void put(Map<LegacyCollisionNeighborhoodSnapshot.Role,
            LegacyCollisionNeighborhoodSnapshot.CellSample> samples,
            LegacyCollisionNeighborhoodSnapshot.Role role, Grid grid,
            LegacyCollisionCoordinateResolver.Coordinate coordinate) {
        samples.put(role, read(grid, coordinate));
    }

    private LegacyCollisionNeighborhoodSnapshot.CellSample read(
            Grid grid, LegacyCollisionCoordinateResolver.Coordinate coordinate) {
        bounds.requireInside(coordinate);
        boolean auxiliaryPresent = grid.hasAuxiliaryGrid();
        int auxiliary = auxiliaryPresent ? grid.auxiliaryRawAt(
                coordinate.getX(), coordinate.getY()) : 0;
        return new LegacyCollisionNeighborhoodSnapshot.CellSample(
                coordinate.getX(), coordinate.getY(),
                grid.foregroundRawAt(coordinate.getX(), coordinate.getY()),
                grid.playerLayerRawAt(coordinate.getX(), coordinate.getY()),
                grid.packedMetadataRawAt(coordinate.getX(), coordinate.getY()),
                auxiliaryPresent, auxiliary);
    }

    private Result incomplete(Status status,
            LegacyCollisionCoordinateResolver.Coordinate failedCoordinate) {
        return new Result(status, null, failedCoordinate);
    }
}
