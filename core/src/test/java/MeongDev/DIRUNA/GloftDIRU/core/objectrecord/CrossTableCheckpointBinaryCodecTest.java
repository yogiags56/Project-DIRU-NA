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

    @Test
    public void roundTripPreservesAllOpaqueAuxiliaryEntryBits() throws Exception {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        long[] values = {
                0L, 1L, -1L, 0x0123456789ABCDEFL, 0xFEDCBA9876543210L,
                Long.MIN_VALUE, Long.MAX_VALUE, 0x00000000FFFFFFFFL,
                0xFFFFFFFF00000000L, 0x5555555555555555L,
                0xAAAAAAAAAAAAAAAAL, 0x7F00FF00AA55CC33L,
                0x0102030405060708L, 0x1020304050607080L,
                0xCAFEBABEDEADBEEFL
        };
        for (int i = 0; i < values.length; i++)
            source.objects().setAuxiliaryEntryRaw(i, values[i]);
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(source.snapshot(), Map.of());
        CrossTableCheckpoint decoded = CrossTableCheckpointBinaryCodec.decode(
                CrossTableCheckpointBinaryCodec.encode(checkpoint));
        assertArrayEquals(values,
                decoded.lifecycleSnapshot().objectSnapshot().auxiliaryEntries());
    }

    @Test
    public void recordRawPayloadWithOpaqueBitsSurvivesRoundTrip() throws Exception {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectRecordTable.AllocationResult allocated =
                table.allocate(ObjectRecordTable.RecordType.TYPE_2, 12, 34, 1);
        assertTrue(allocated.succeeded());
        assertEquals(ObjectRecordTable.Status.OK, table.link(tile(12, 34, 1), allocated.handle()));
        ObjectRecordTable.Snapshot snapshot = table.snapshot();
        ObjectRecordTable.RecordSnapshot original = snapshot.records().get(0);
        long opaqueHighBits = 0x1234567800000000L & ~LegacyObjectRecordLinkCodec.LINK_MASK;
        long raw = original.rawLegacyPayload() | opaqueHighBits | 0x00000000000055AAL;
        ObjectRecordTable.RecordSnapshot withOpaque = new ObjectRecordTable.RecordSnapshot(
                original.slotId(), original.generation(), original.type(),
                original.anchorX(), original.anchorY(), original.anchorGroup(), raw);
        ObjectRecordTable.Snapshot rawSnapshot = new ObjectRecordTable.Snapshot(
                snapshot.schemaVersion(), java.util.List.of(withOpaque),
                snapshot.generations(), snapshot.tileLinks(), snapshot.auxiliaryEntries());
        CrossTableLifecycleCoordinator.Snapshot lifecycle =
                new CrossTableLifecycleCoordinator.Snapshot(rawSnapshot, Map.of());
        int packed = PackedTileMetadataCodec.writeObjectId(0, 1, allocated.slotId()).rawWord();
        CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(lifecycle,
                Map.of(tile(12, 34, 1), packed));
        CrossTableCheckpoint decoded = CrossTableCheckpointBinaryCodec.decode(
                CrossTableCheckpointBinaryCodec.encode(checkpoint));
        assertEquals(raw, decoded.lifecycleSnapshot().objectSnapshot()
                .records().get(0).rawLegacyPayload());
    }

    @Test
    public void corruptionAtEveryHeaderBoundaryIsRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        for (int length = 0; length < 16; length++) {
            byte[] truncated = java.util.Arrays.copyOf(encoded, length);
            try {
                CrossTableCheckpointBinaryCodec.decode(truncated);
                fail("header truncation must be rejected at length " + length);
            } catch (IOException expected) {
                // expected
            }
        }
    }

    @Test
    public void negativeAndExcessiveCollectionCountsAreRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        byte[] negativeRecords = encoded.clone();
        java.util.Arrays.fill(negativeRecords, 16, 20, (byte) 0xFF);
        assertDecodeRejected(negativeRecords);
        byte[] excessiveRecords = encoded.clone();
        excessiveRecords[16] = 0x7F;
        excessiveRecords[17] = (byte) 0xFF;
        excessiveRecords[18] = (byte) 0xFF;
        excessiveRecords[19] = (byte) 0xFF;
        assertDecodeRejected(excessiveRecords);
    }

    @Test
    public void invalidRecordTypeOrdinalIsRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        // Header is 20 bytes; each record starts with slot, generation, then type ordinal.
        encoded[28] = 0x7F;
        encoded[29] = (byte) 0xFF;
        encoded[30] = (byte) 0xFF;
        encoded[31] = (byte) 0xFF;
        assertDecodeRejected(encoded);
    }

    @Test
    public void unknownOuterAndInnerSchemasAreRejected() throws Exception {
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        byte[] unknownOuter = encoded.clone();
        unknownOuter[11] = 0x7F;
        assertDecodeRejected(unknownOuter);
        byte[] unknownInner = encoded.clone();
        unknownInner[15] = 0x7F;
        assertDecodeRejected(unknownInner);
    }


    @Test
    public void decodeIntoRejectsMalformedPayloadWithoutMutatingLiveDestination() throws Exception {
        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        ObjectRecordTable.TileGroup liveTile = tile(40, 41, 2);
        CrossTableLifecycleCoordinator.AttachResult live = destination.attach(
                ObjectRecordTable.RecordType.TYPE_1, 40, 41, 2,
                liveTile, 3, stateOne(40, 41));
        assertTrue(live.succeeded());
        byte[] malformed = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        malformed[0] ^= 0x55;
        try {
            CrossTableCheckpointBinaryCodec.decodeInto(malformed, destination);
            fail("invalid magic must throw");
        } catch (IOException expected) {
            assertEquals(ObjectRecordTable.Status.OK, destination.objects().lookup(liveTile).status());
            assertEquals(ObjectRecordTable.RecordType.TYPE_1,
                    destination.objects().typeOf(live.objectHandle()).orElseThrow());
            assertEquals(3, destination.associatedSlot(live.objectHandle()));
        }
    }

    @Test
    public void decodeIntoRestoresOnlyAfterSuccessfulDecodeAndValidation() throws Exception {
        CrossTableCheckpoint source = sampleCheckpoint();
        byte[] encoded = CrossTableCheckpointBinaryCodec.encode(source);
        CrossTableLifecycleCoordinator destination = new CrossTableLifecycleCoordinator();
        ObjectRecordTable.TileGroup liveTile = tile(50, 51, 1);
        CrossTableLifecycleCoordinator.AttachResult live = destination.attach(
                ObjectRecordTable.RecordType.TYPE_1, 50, 51, 1,
                liveTile, 1, stateOne(50, 51));
        assertTrue(live.succeeded());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                CrossTableCheckpointBinaryCodec.decodeInto(encoded, destination));
        assertEquals(ObjectRecordTable.Status.NO_LINK, destination.objects().lookup(liveTile).status());
        assertEquals(-1, destination.associatedSlot(live.objectHandle()));
        for (ObjectRecordTable.TileGroup expectedTile :
                source.lifecycleSnapshot().objectSnapshot().tileLinks().keySet()) {
            assertEquals(ObjectRecordTable.Status.OK, destination.objects().lookup(expectedTile).status());
        }
    }

    @Test
    public void deterministicEncodingIgnoresMapInsertionOrder() throws Exception {
        CrossTableCheckpoint first = sampleCheckpoint();
        CrossTableLifecycleCoordinator.Snapshot snapshot = first.lifecycleSnapshot();
        Map<ObjectRecordTable.TileGroup, Integer> reversed = new java.util.LinkedHashMap<>();
        java.util.List<Map.Entry<ObjectRecordTable.TileGroup, Integer>> entries =
                new java.util.ArrayList<>(first.packedMetadataWords().entrySet());
        java.util.Collections.reverse(entries);
        for (Map.Entry<ObjectRecordTable.TileGroup, Integer> entry : entries)
            reversed.put(entry.getKey(), entry.getValue());
        CrossTableCheckpoint second = new CrossTableCheckpoint(snapshot, reversed);
        assertArrayEquals(CrossTableCheckpointBinaryCodec.encode(first),
                CrossTableCheckpointBinaryCodec.encode(second));
    }

    @Test
    public void deterministicEncodingIgnoresTileLinkInsertionOrder() throws Exception {
        CrossTableCheckpoint first = sampleCheckpoint();
        ObjectRecordTable.Snapshot original = first.lifecycleSnapshot().objectSnapshot();
        Map<ObjectRecordTable.TileGroup, Integer> reversedLinks = new java.util.LinkedHashMap<>();
        java.util.List<Map.Entry<ObjectRecordTable.TileGroup, Integer>> entries =
                new java.util.ArrayList<>(original.tileLinks().entrySet());
        java.util.Collections.reverse(entries);
        for (Map.Entry<ObjectRecordTable.TileGroup, Integer> entry : entries)
            reversedLinks.put(entry.getKey(), entry.getValue());
        ObjectRecordTable.Snapshot reordered = new ObjectRecordTable.Snapshot(
                original.schemaVersion(), original.records(), original.generations(),
                reversedLinks, original.auxiliaryEntries());
        CrossTableLifecycleCoordinator.Snapshot reorderedLifecycle =
                new CrossTableLifecycleCoordinator.Snapshot(reordered,
                        first.lifecycleSnapshot().associations());
        CrossTableCheckpoint second = new CrossTableCheckpoint(
                reorderedLifecycle, first.packedMetadataWords());
        assertArrayEquals(CrossTableCheckpointBinaryCodec.encode(first),
                CrossTableCheckpointBinaryCodec.encode(second));
    }

    @Test
    public void deterministicByteMutationSweepNeverLeaksUncheckedRuntimeFailure() throws Exception {
        byte[] valid = CrossTableCheckpointBinaryCodec.encode(sampleCheckpoint());
        int rejected = 0;
        for (int index = 0; index < valid.length; index++) {
            byte[] mutated = valid.clone();
            mutated[index] ^= (byte) 0xA5;
            try {
                CrossTableCheckpointBinaryCodec.decode(mutated);
            } catch (IOException expected) {
                rejected++;
            }
        }
        assertTrue("mutation sweep should reject at least some mutated payloads", rejected > 0);
    }

    private static void assertDecodeRejected(byte[] bytes) throws Exception {
        try {
            CrossTableCheckpointBinaryCodec.decode(bytes);
            fail("malformed checkpoint must be rejected");
        } catch (IOException expected) {
            // expected
        }
    }
}
