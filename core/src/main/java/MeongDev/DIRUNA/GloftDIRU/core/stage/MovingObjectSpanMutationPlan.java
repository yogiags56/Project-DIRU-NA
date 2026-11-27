package MeongDev.DIRUNA.GloftDIRU.core.stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Detached plan for the recognized-object portion of legacy VVoid/aBoolean.
 *
 * It describes player-layer conversion, packed metadata updates, the 3x3
 * cByteArrArr marker writes from kVoid(x,y), and the cVoid(x,y,1) interaction
 * queue events. It does not mutate StageGrid or execute hero-collision damage.
 */
public final class MovingObjectSpanMutationPlan {
    public record CellMutation(int x, int y, int expectedPlayerCode, int expectedBackgroundMetadata, int playerCode, int backgroundMetadata) { }
    public record MarkerWrite(int x, int y, int value) { }
    public record InteractionEvent(int x, int y, int type) { }

    private final List<CellMutation> cellMutations;
    private final List<MarkerWrite> markerWrites;
    private final List<InteractionEvent> interactionEvents;

    private MovingObjectSpanMutationPlan(List<CellMutation> cells,
                                         List<MarkerWrite> markers,
                                         List<InteractionEvent> events) {
        cellMutations = List.copyOf(cells);
        markerWrites = List.copyOf(markers);
        interactionEvents = List.copyOf(events);
    }

    public List<CellMutation> cellMutations() { return cellMutations; }
    public List<MarkerWrite> markerWrites() { return markerWrites; }
    public List<InteractionEvent> interactionEvents() { return interactionEvents; }

    public static MovingObjectSpanMutationPlan plan(StageGrid stage,
                                                     MovingObjectSpanTable spans) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        if (spans == null) throw new IllegalArgumentException("span table is required");

        List<CellMutation> cells = new ArrayList<>();
        List<MarkerWrite> markers = new ArrayList<>();
        int[] workingPlayer = stage.copyPlayerLayer();
        int[] workingBackground = stage.copyBackgroundLayer();
        List<InteractionEvent> events = new ArrayList<>();

        for (int slot = 0; slot < spans.size(); slot++) {
            MovingObjectSpanTable.Span span = spans.spanAt(slot);
            int y = span.rowY();
            if (y <= 0) continue;
            int left = span.firstX();
            int right = span.secondX();
            if (y >= stage.height() || left < 0 || right < left
                    || right >= stage.width()) {
                throw new IllegalArgumentException("invalid active span at slot " + slot);
            }

            for (int x = left; x <= right; x++) {
                int cellIndex = x + y * stage.width();
                int occupant = workingPlayer[cellIndex];
                int encoded = encodedObjectType(occupant);
                if (encoded < 0) continue;

                if (x == 0 || y == 0 || x + 1 >= stage.width() || y + 1 >= stage.height()) {
                    throw new IllegalArgumentException(
                            "recognized moving-span occupant lacks a complete 3x3 marker region at " + x + "," + y);
                }

                int snakeVariantFlag = encoded == 37 && occupant == 43
                        ? 0x10000000 : 0;
                int oldMetadata = workingBackground[cellIndex];
                int nextMetadata = (oldMetadata & 0xF03FFFFF)
                        | (encoded << 22) | snakeVariantFlag;
                cells.add(new CellMutation(x, y, occupant, oldMetadata, 9, nextMetadata));
                // The legacy loop mutates the live player layer immediately.
                // Subsequent overlapping spans therefore see code 9, not the
                // original recognized occupant, and must not enqueue it twice.
                workingPlayer[cellIndex] = 9;
                workingBackground[cellIndex] = nextMetadata;
                for (int markerY = y - 1; markerY <= y + 1; markerY++) {
                    for (int markerX = x - 1; markerX <= x + 1; markerX++) {
                        markers.add(new MarkerWrite(markerX, markerY, 48));
                    }
                }
                events.add(new InteractionEvent(x, y, 1));
            }
        }
        return new MovingObjectSpanMutationPlan(cells, markers, events);
    }

    private static int encodedObjectType(int code) {
        switch (code) {
            case 1: return 34;
            case 45: return 35;
            case 19:
            case 43: return 37;
            case 46: return 36;
            case 49: return 39;
            default: return -1;
        }
    }
}
