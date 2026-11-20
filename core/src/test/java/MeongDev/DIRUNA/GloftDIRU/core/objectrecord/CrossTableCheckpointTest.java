package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.ObjectRecordTable.*;

public final class CrossTableCheckpointTest {
    private static long stateOne(int x, int y) {
        return ((long) y << 20) | ((long) x << 13) | 1L;
    }

    private static TileGroup tile(int x, int y, int group) {
        return new TileGroup(x, y, group);
    }

    @Test
    public void checkpointRestoresAllPackedMetadataLinksAndCoordinatorState() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        TileGroup a = tile(4, 5, 0);
        TileGroup b = tile(6, 7, 1);
        CrossTableLifecycleCoordinator.AttachResult first = source.attach(
                RecordType.TYPE_2, 4, 5, 0, a, 2, stateOne(4, 5));
        CrossTableLifecycleCoordinator.AttachResult second = source.attach(
                RecordType.TYPE_5, 6, 7, 1, b, 3, stateOne(6, 7));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, first.status());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, second.status());

        Map<TileGroup, Integer> words = new HashMap<>();
        words.put(a, PackedTileMetadataCodec.writeObjectId(0, 0,
                (int) (first.objectHandle().value() & 0x0F)).rawWord());
        words.put(b, PackedTileMetadataCodec.writeObjectId(0, 1,
                (int) (second.objectHandle().value() & 0x0F)).rawWord());
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(source.snapshot(), words);

        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, checkpoint.restoreInto(restored));
        assertEquals(first.objectHandle(), restored.objects().lookup(a).handle());
        assertEquals((int) (first.objectHandle().value() & 0x0F),
                PackedTileMetadataCodec.readObjectId(words.get(a), a.group()).value());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.objects().lookup(a, words.get(a)).status());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.objects().lookup(b, words.get(b)).status());
        assertEquals(2, restored.associatedSlot(first.objectHandle()));
        assertEquals(3, restored.associatedSlot(second.objectHandle()));
    }

    @Test
    public void mismatchedPackedMetadataRejectsCheckpointWithoutChangingDestination() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        TileGroup tile = tile(8, 9, 0);
        source.attach(RecordType.TYPE_2, 8, 9, 0, tile, 4, stateOne(8, 9));
        int wrongWord = PackedTileMetadataCodec.writeObjectId(0, 0, 2).rawWord();
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(source.snapshot(),
                Map.of(tile, wrongWord));

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult live = destination.attach(
                RecordType.TYPE_1, 1, 2, 0, tile(1, 2, 0), 1, stateOne(1, 2));
        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                checkpoint.restoreInto(destination));
        assertEquals(1, destination.associatedSlot(live.objectHandle()));
        assertEquals(RecordType.TYPE_1,
                destination.objects().typeOf(live.objectHandle()).orElseThrow());
    }

    @Test
    public void missingPackedMetadataEntryRejectsCheckpointAtomically() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        TileGroup tile = tile(2, 3, 2);
        CrossTableLifecycleCoordinator.AttachResult attached = source.attach(
                RecordType.TYPE_5, 2, 3, 2, tile, 5, stateOne(2, 3));
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(source.snapshot(), Map.of());

        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                checkpoint.restoreInto(destination));
        assertEquals(-1, destination.associatedSlot(attached.objectHandle()));
        assertEquals(Status.NO_LINK, destination.objects().lookup(tile).status());
    }

    @Test
    public void checkpointRejectsNullDestination() {
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(
                new CrossTableLifecycleCoordinator().snapshot(), Map.of());
        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                checkpoint.restoreInto(null));
    }
}
