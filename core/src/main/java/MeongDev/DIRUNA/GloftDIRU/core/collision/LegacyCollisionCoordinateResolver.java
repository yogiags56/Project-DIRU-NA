package MeongDev.DIRUNA.GloftDIRU.core.collision;

/**
 * Computes source-derived sample coordinates using overflow-safe arithmetic.
 * This class never reads a grid; callers must bounds-check before array access.
 */
public final class LegacyCollisionCoordinateResolver {
    public static final class Coordinate {
        private final int x;
        private final int y;
        public Coordinate(int x, int y) { this.x = x; this.y = y; }
        public int getX() { return x; }
        public int getY() { return y; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Coordinate)) return false;
            Coordinate c = (Coordinate) other;
            return x == c.x && y == c.y;
        }
        @Override public int hashCode() { return 31 * x + y; }
        @Override public String toString() { return "(" + x + "," + y + ")"; }
    }

    public static final class StageBounds {
        private final int width;
        private final int height;
        public StageBounds(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("stage dimensions must be positive");
            }
            this.width = width;
            this.height = height;
        }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean contains(Coordinate c) {
            return c != null && c.x >= 0 && c.x < width && c.y >= 0 && c.y < height;
        }
        public void requireInside(Coordinate c) {
            if (!contains(c)) {
                throw new IndexOutOfBoundsException(
                        "coordinate " + c + " outside stage " + width + "x" + height);
            }
        }
    }

    private LegacyCollisionCoordinateResolver() { }

    public static Coordinate checked(long x, long y, StageBounds bounds) {
        if (bounds == null) throw new IllegalArgumentException("bounds are required");
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE
                || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("coordinate arithmetic overflow");
        }
        Coordinate c = new Coordinate((int) x, (int) y);
        bounds.requireInside(c);
        return c;
    }

    public static Coordinate target(int playerX, int playerY, int dx, int dy,
                                    StageBounds bounds) {
        return checked((long) playerX + dx, (long) playerY + dy, bounds);
    }

    public static Coordinate twoSteps(int playerX, int playerY, int dx, int dy,
                                      StageBounds bounds) {
        return checked((long) playerX + 2L * dx, (long) playerY + 2L * dy, bounds);
    }

    public static Coordinate targetAbovePlayerRow(int playerX, int playerY, int dx,
                                                   StageBounds bounds) {
        return checked((long) playerX + dx, (long) playerY + 1L, bounds);
    }

    public static Coordinate targetAbove(Coordinate target, StageBounds bounds) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return checked(target.x, (long) target.y - 1L, bounds);
    }

    public static Coordinate targetBelow(Coordinate target, StageBounds bounds) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return checked(target.x, (long) target.y + 1L, bounds);
    }

    public static Coordinate type19AnchorColumn(Coordinate target, StageBounds bounds) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return checked((long) target.x + 3L, target.y, bounds);
    }

    public static Coordinate type19ScanNext(Coordinate current, StageBounds bounds) {
        if (current == null) throw new IllegalArgumentException("current is required");
        return checked(current.x, (long) current.y - 1L, bounds);
    }

    public static Coordinate type19ForegroundRunNext(Coordinate target, int currentY,
                                                       StageBounds bounds) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return checked(target.x, (long) currentY + 1L, bounds);
    }

    public static Coordinate type48Destination(Coordinate twoStep, int verticalOffset,
                                                StageBounds bounds) {
        if (twoStep == null) throw new IllegalArgumentException("two-step target is required");
        return checked(twoStep.x, (long) twoStep.y + verticalOffset, bounds);
    }

    public static Coordinate type48SourceClear(Coordinate target, int verticalOffset,
                                                 StageBounds bounds) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return checked(target.x, (long) target.y + verticalOffset, bounds);
    }

    public static Coordinate type48Support(Coordinate target, int lowerOffset,
                                            StageBounds bounds) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return checked(target.x, (long) target.y + lowerOffset + 1L, bounds);
    }
}
