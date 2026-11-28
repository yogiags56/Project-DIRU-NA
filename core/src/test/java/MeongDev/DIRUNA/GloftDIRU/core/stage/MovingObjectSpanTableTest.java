package MeongDev.DIRUNA.GloftDIRU.core.stage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MovingObjectSpanTableTest {
    @Test public void preservesRawSignedByteTriplesAndSlotOrder() {
        MovingObjectSpanTable table = new MovingObjectSpanTable(
                new byte[]{2, 3, 7, 0, 0, -1, 5, 4, 9});
        assertEquals(3, table.size());
        assertEquals(new MovingObjectSpanTable.Span((byte) 2, (byte) 3, (byte) 7),
                table.spanAt(0));
        assertEquals(new MovingObjectSpanTable.Span((byte) 0, (byte) 0, (byte) -1),
                table.spanAt(1));
        assertEquals(new MovingObjectSpanTable.Span((byte) 5, (byte) 4, (byte) 9),
                table.spanAt(2));
    }

    @Test public void constructorAndRawExportAreDefensivelyCopied() {
        byte[] source = {1, 2, 3};
        MovingObjectSpanTable table = new MovingObjectSpanTable(source);
        source[0] = 99;
        assertEquals(1, table.spanAt(0).firstX());
        byte[] copy = table.copyRawTriples();
        copy[1] = 88;
        assertEquals(2, table.spanAt(0).secondX());
    }

    @Test public void rejectsNullAndPartialTriples() {
        try {
            new MovingObjectSpanTable(null);
            fail("null input must be rejected");
        } catch (IllegalArgumentException expected) { }
        try {
            new MovingObjectSpanTable(new byte[]{1, 2});
            fail("partial triple must be rejected");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsOutOfRangeSlots() {
        MovingObjectSpanTable table = new MovingObjectSpanTable(new byte[]{1, 2, 3});
        try { table.spanAt(-1); fail("negative slot must be rejected"); }
        catch (IndexOutOfBoundsException expected) { }
        try { table.spanAt(1); fail("slot past end must be rejected"); }
        catch (IndexOutOfBoundsException expected) { }
    }
}
