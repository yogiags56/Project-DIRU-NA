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
        Map<TileGroup, Integer> serializedLinks =
                source.snapshot().objectSnapshot().tileLinks();
        words.put(a, PackedTileMetadataCodec.writeObjectId(0, 0,
                serializedLinks.get(a)).rawWord());
        words.put(b, PackedTileMetadataCodec.writeObjectId(0, 1,
                serializedLinks.get(b)).rawWord());
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

    @Test
    public void unsupportedCheckpointSchemaIsRejectedWithoutMutation() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(
                CrossTableCheckpoint.CURRENT_SCHEMA_VERSION + 1,
                source.snapshot(), Map.of());
        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult live = destination.attach(
                RecordType.TYPE_1, 1, 1, 0, tile(1, 1, 0), 1, stateOne(1, 1));
        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                checkpoint.restoreInto(destination));
        assertEquals(1, destination.associatedSlot(live.objectHandle()));
        assertEquals(RecordType.TYPE_1,
                destination.objects().typeOf(live.objectHandle()).orElseThrow());
    }

    @Test
    public void checkpointDefensivelyCopiesPackedMetadataMap() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        TileGroup tile = tile(3, 4, 0);
        CrossTableLifecycleCoordinator.AttachResult attached = source.attach(
                RecordType.TYPE_5, 3, 4, 0, tile, 2, stateOne(3, 4));
        int slotId = (int) (attached.objectHandle().value() & 0x0F);
        Map<TileGroup, Integer> sourceWords = new HashMap<>();
        sourceWords.put(tile, PackedTileMetadataCodec.writeObjectId(0, 0, slotId).rawWord());
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(source.snapshot(), sourceWords);
        sourceWords.clear();

        assertEquals(1, checkpoint.packedMetadataWords().size());
        try {
            checkpoint.packedMetadataWords().clear();
            fail("checkpoint metadata map must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, checkpoint.restoreInto(restored));
    }

    @Test
    public void nullMetadataMapBecomesEmptyAndCannotMatchNonemptyTileLinks() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        TileGroup tile = tile(5, 6, 1);
        source.attach(RecordType.TYPE_2, 5, 6, 1, tile, 3, stateOne(5, 6));
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(source.snapshot(), null);
        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                checkpoint.restoreInto(destination));
        assertEquals(Status.NO_LINK, destination.objects().lookup(tile).status());
    }
}
