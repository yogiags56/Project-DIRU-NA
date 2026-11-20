package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.ObjectRecordTable.*;
import static org.junit.Assert.*;

public final class ObjectRecordTableTest {
    private static TileGroup tile(int x, int y, int group) {
        return new TileGroup(x, y, group);
    }

    @Test
    public void allocatesFifteenIndependentObjectSlots() {
        ObjectRecordTable table = new ObjectRecordTable();
        for (int i = 1; i <= 15; i++) {
            AllocationResult result = table.allocate(RecordType.TYPE_5, i, 0, 0);
            assertEquals(Status.OK, result.status());
            assertEquals(i, result.slotId());
        }
        assertEquals(Status.NO_CAPACITY,
                table.allocate(RecordType.TYPE_5, 0, 0, 0).status());
    }

    @Test
    public void tileMetadataCanLinkOnlyRepresentableSlots() {
        ObjectRecordTable table = new ObjectRecordTable();
        AllocationResult lastRepresentable = null;
        for (int i = 1; i <= 7; i++) {
            lastRepresentable = table.allocate(RecordType.TYPE_5, i, 0, 0);
        }
        assertEquals(Status.OK, table.link(tile(10, 10, 0),
                lastRepresentable.handle()));

        AllocationResult slotEight = table.allocate(RecordType.TYPE_5, 8, 0, 0);
        assertEquals(8, slotEight.slotId());
        assertEquals(Status.UNREPRESENTABLE_TILE_ID,
                table.link(tile(11, 10, 0), slotEight.handle()));
        assertEquals(0, table.referenceCount(slotEight.handle()));
        assertEquals(Status.NO_LINK, table.lookup(tile(11, 10, 0)).status());
    }

    @Test
    public void zeroOrMissingTileIdIsNoLink() {
        ObjectRecordTable table = new ObjectRecordTable();
        assertEquals(Status.NO_LINK, table.lookup(tile(0, 0, 0)).status());
        assertEquals(Status.NO_LINK, table.unlink(tile(0, 0, 0)));
    }

    @Test
    public void multipleTileGroupsCanShareOneRecord() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_2, 4, 5, 1).handle();
        TileGroup first = tile(4, 5, 1);
        TileGroup second = tile(5, 5, 1);

        assertEquals(Status.OK, table.link(first, handle));
        assertEquals(Status.OK, table.link(second, handle));
        assertEquals(2, table.referenceCount(handle));
        assertEquals(Status.OK, table.unlink(first));
        assertEquals(1, table.referenceCount(handle));
        assertEquals(Status.OBJECT_STILL_REFERENCED, table.release(handle));
        assertEquals(Status.OK, table.lookup(second).status());
    }

    @Test
    public void releaseInvalidatesOldGenerationHandle() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle oldHandle = table.allocate(RecordType.TYPE_3, 0, 0, 0).handle();
        assertEquals(Status.OK, table.release(oldHandle));

        ObjectHandle newHandle = table.allocate(RecordType.TYPE_4, 0, 0, 0).handle();
        assertNotEquals(oldHandle, newHandle);
        assertEquals(Status.STALE_HANDLE, table.release(oldHandle));
        assertTrue(table.typeOf(newHandle).isPresent());
    }

    @Test
    public void reclassificationPreservesHandleAndReferences() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_2, 1, 1, 0).handle();
        TileGroup link = tile(1, 1, 0);
        assertEquals(Status.OK, table.link(link, handle));

        assertEquals(Status.OK, table.reclassify(handle, RecordType.TYPE_5));
        assertEquals(RecordType.TYPE_5, table.typeOf(handle).orElseThrow());
        assertEquals(1, table.referenceCount(handle));
        assertEquals(handle, table.lookup(link).handle());
    }

    @Test
    public void anchorValidationIsNotAppliedToEverySharedTileReference() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_5, 2, 3, 0).handle();
        assertEquals(Status.OK, table.link(tile(2, 3, 0), handle));
        assertEquals(Status.OK, table.link(tile(3, 3, 0), handle));

        assertTrue(table.matchesAnchor(handle, 2, 3, 0));
        assertFalse(table.matchesAnchor(handle, 3, 3, 0));
        assertEquals(Status.OK, table.lookup(tile(3, 3, 0)).status());
    }

    @Test
    public void duplicateLinkDoesNotDoubleCountAndOccupiedGroupCannotBeReplaced() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle first = table.allocate(RecordType.TYPE_5, 0, 0, 0).handle();
        ObjectHandle second = table.allocate(RecordType.TYPE_4, 1, 0, 0).handle();
        TileGroup tile = tile(0, 0, 0);

        assertEquals(Status.OK, table.link(tile, first));
        assertEquals(Status.ALREADY_LINKED, table.link(tile, first));
        assertEquals(Status.TILE_GROUP_OCCUPIED, table.link(tile, second));
        assertEquals(1, table.referenceCount(first));
        assertEquals(0, table.referenceCount(second));
    }

    @Test
    public void releaseSucceedsOnlyAfterAllLinksAreRemoved() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_1, 0, 0, 0).handle();
        TileGroup a = tile(0, 0, 0);
        TileGroup b = tile(1, 0, 0);
        assertEquals(Status.OK, table.link(a, handle));
        assertEquals(Status.OK, table.link(b, handle));

        assertEquals(Status.OK, table.unlink(a));
        assertEquals(Status.OBJECT_STILL_REFERENCED, table.release(handle));
        assertEquals(Status.OK, table.unlink(b));
        assertEquals(Status.OK, table.release(handle));
        assertEquals(Status.STALE_HANDLE, table.release(handle));
    }
}
