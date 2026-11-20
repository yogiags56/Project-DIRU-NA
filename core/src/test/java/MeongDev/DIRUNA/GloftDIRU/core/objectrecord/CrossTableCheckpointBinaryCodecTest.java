package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CrossTableCheckpointBinaryCodecTest {
    private static ObjectRecordTable.TileGroup tile(int x, int y, int group) {
        return new ObjectRecordTable.TileGroup(x, y, group);
    }

    private static long stateOne(int x, int y) {
        return 1L | ((long) x << 13) | ((long) y << 20);
    }

    private static CrossTableCheckpoint sampleCheckpoint() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        ObjectRecordTable.TileGroup a = tile(2, 3, 0);
        ObjectRecordTable.TileGroup b = tile(4, 5, 2);
        CrossTableLifecycleCoordinator.AttachResult first = source.attach(
                ObjectRecordTable.RecordType.TYPE_2, 2, 3, 0, a, 3, stateOne(2, 3));
        CrossTableLifecycleCoordinator.AttachResult second = source.attach(
                ObjectRecordTable.RecordType.TYPE_5, 4, 5, 2, b, 4, stateOne(4, 5));
        assertTrue(first.succeeded());
        assertTrue(second.succeeded());
        Map<ObjectRecordTable.TileGroup, Integer> words = new HashMap<>();
        Map<ObjectRecordTable.TileGroup, Integer> links =
                source.snapshot().objectSnapshot().tileLinks();
        words.put(a, PackedTileMetadataCodec.writeObjectId(0, 0, links.get(a)).rawWord());
        words.put(b, PackedTileMetadataCodec.writeObjectId(0, 2, links.get(b)).rawWord());
        return new CrossTableCheckpoint(source.snapshot(), words);
    }

    @Test
    public void binaryRoundTripPreservesCheckpointAndIsDeterministic() throws Exception {
        CrossTableCheckpoint checkpoint = sampleCheckpoint();
        byte[] first = CrossTableCheckpointBinaryCodec.encode(checkpoint);
        byte[] second = CrossTableCheckpointBinaryCodec.encode(checkpoint);
        assertArrayEquals(first, second);
        CrossTableCheckpoint decoded = CrossTableCheckpointBinaryCodec.decode(first);
        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, decoded.restoreInto(restored));
        assertEquals(checkpoint.lifecycleSnapshot().objectSnapshot().records(),
                decoded.lifecycleSnapshot().objectSnapshot().records());
        assertEquals(checkpoint.lifecycleSnapshot().objectSnapshot().tileLinks(),
                decoded.lifecycleSnapshot().objectSnapshot().tileLinks());
        assertEquals(checkpoint.packedMetadataWords(), decoded.packedMetadataWords());
        assertEquals(checkpoint.lifecycleSnapshot().associations(),
                decoded.lifecycleSnapshot().associations());
    }

    @Test
    public void emptyWorldRoundTrips() throws Exception {
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(
                new CrossTableLifecycleCoordinator().snapshot(), Map.of());
        CrossTableCheckpoint decoded = CrossTableCheckpointBinaryCodec.decode(
                CrossTableCheckpointBinaryCodec.encode(checkpoint));
        assertTrue(decoded.lifecycleSnapshot().objectSnapshot().records().isEmpty());
        assertTrue(decoded.lifecycleSnapshot().objectSnapshot().tileLinks().isEmpty());
    }

    @Test
    public void truncatedPayloadIsRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        byte[] truncated = java.util.Arrays.copyOf(encoded, encoded.length - 3);
        try {
            CrossTableCheckpointBinaryCodec.decode(truncated);
            fail("truncated payload must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("truncated"));
        }
    }

    @Test
    public void trailingBytesAreRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        byte[] extended = java.util.Arrays.copyOf(encoded, encoded.length + 1);
        try {
            CrossTableCheckpointBinaryCodec.decode(extended);
            fail("trailing bytes must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("trailing"));
        }
    }

    @Test
    public void unknownBinaryFormatVersionIsRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        encoded[7] = (byte) (CrossTableCheckpointBinaryCodec.FORMAT_VERSION + 1);
        try {
            CrossTableCheckpointBinaryCodec.decode(encoded);
            fail("unknown format version must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("version"));
        }
    }

    @Test
    public void invalidMagicAndOversizedInputAreRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        encoded[0] = 0;
        try {
            CrossTableCheckpointBinaryCodec.decode(encoded);
            fail("invalid magic must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("magic"));
        }
        try {
            CrossTableCheckpointBinaryCodec.decode(
                    new byte[CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES + 1]);
            fail("oversized input must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("length"));
        }
    }
}