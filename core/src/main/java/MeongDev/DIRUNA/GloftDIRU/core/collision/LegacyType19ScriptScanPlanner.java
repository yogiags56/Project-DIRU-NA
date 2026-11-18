package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounds-safe, non-mutating plan for the legacy foreground type-19 script scan.
 * It does not execute callbacks or write foreground/player layers.
 */
public final class LegacyType19ScriptScanPlanner {
    public enum Status { READY, PROBE_SKIPPED, NOT_TYPE_19, ANCHOR_COLUMN_OUT_OF_BOUNDS, ANCHOR_NOT_FOUND, RUN_NOT_TERMINATED }
    public interface Grid {
        int width();
        int height();
        int playerLayerRawAt(int x, int y);
        int foregroundRawAt(int x, int y);
    }
    public static final class Plan {
        private final Status status;
        private final int anchorX, anchorY;
        private final List<Integer> foregroundRowsToClear;
        private Plan(Status s, int x, int y, List<Integer> rows) {
            status=s; anchorX=x; anchorY=y;
            foregroundRowsToClear=Collections.unmodifiableList(new ArrayList<Integer>(rows));
        }
        public Status getStatus(){return status;}
        public boolean isReady(){return status==Status.READY;}
        public int getAnchorX(){return anchorX;}
        public int getAnchorY(){return anchorY;}
        public List<Integer> getForegroundRowsToClear(){return foregroundRowsToClear;}
    }
    private LegacyType19ScriptScanPlanner(){}
    public static Plan plan(int targetX, int targetY, int foregroundRaw, boolean probe, Grid grid) {
        if (grid == null || grid.width() < 0 || grid.height() < 0)
            throw new IllegalArgumentException("valid grid is required");
        if ((foregroundRaw & 0xFF) != 19)
            return new Plan(Status.NOT_TYPE_19, -1, -1, Collections.<Integer>emptyList());
        if (probe)
            return new Plan(Status.PROBE_SKIPPED, -1, -1, Collections.<Integer>emptyList());
        int anchorX = targetX + 3;
        if (anchorX < 0 || anchorX >= grid.width() || targetY < 0 || targetY >= grid.height())
            return new Plan(Status.ANCHOR_COLUMN_OUT_OF_BOUNDS, anchorX, -1, Collections.<Integer>emptyList());
        int anchorY = targetY;
        while (anchorY >= 0 && grid.playerLayerRawAt(anchorX, anchorY) != 39) anchorY--;
        if (anchorY < 0)
            return new Plan(Status.ANCHOR_NOT_FOUND, anchorX, -1, Collections.<Integer>emptyList());
        List<Integer> rows = new ArrayList<Integer>();
        int y = anchorY;
        while (true) {
            y++;
            if (y >= grid.height())
                return new Plan(Status.RUN_NOT_TERMINATED, anchorX, anchorY, Collections.<Integer>emptyList());
            if ((grid.foregroundRawAt(targetX, y) & 0xFF) != 19)
                return new Plan(Status.READY, anchorX, anchorY, rows);
            rows.add(y);
        }
    }
}