package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, bounded binary codec for the native cross-table checkpoint model.
 * This is a DIRU-NA container format, not the original game's save-file format.
 */
public final class CrossTableCheckpointBinaryCodec {
    private static final int MAGIC = 0x44495255; // DIRU
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_ENCODED_BYTES = 16 * 1024 * 1024;
    private static final int MAX_TILE_LINKS = 1_000_000;
    private static final int MAX_RECORDS = ObjectRecordTable.OBJECT_SLOT_CAPACITY;
    private static final int MAX_ASSOCIATIONS = ObjectRecordTable.OBJECT_SLOT_CAPACITY;

    private CrossTableCheckpointBinaryCodec() { }

    public static byte[] encode(CrossTableCheckpoint checkpoint) throws IOException {
        if (checkpoint == null) throw new IllegalArgumentException("checkpoint is required");
        CrossTableLifecycleCoordinator validated = new CrossTableLifecycleCoordinator();
        if (checkpoint.restoreInto(validated) != CrossTableLifecycleCoordinator.Status.OK)
            throw new IllegalArgumentException("checkpoint failed integrity validation");

        CrossTableLifecycleCoordinator.Snapshot lifecycle = checkpoint.lifecycleSnapshot();
        ObjectRecordTable.Snapshot objects = lifecycle.objectSnapshot();
        List<ObjectRecordTable.RecordSnapshot> records = new ArrayList<>(objects.records());
        records.sort(Comparator.comparingInt(ObjectRecordTable.RecordSnapshot::slotId));
        List<Map.Entry<ObjectRecordTable.TileGroup, Integer>> links =
                new ArrayList<>(objects.tileLinks().entrySet());
        links.sort(Map.Entry.comparingByKey(Comparator
                .comparingInt(ObjectRecordTable.TileGroup::x)
                .thenComparingInt(ObjectRecordTable.TileGroup::y)
                .thenComparingInt(ObjectRecordTable.TileGroup::group)));
        List<Map.Entry<Long, Integer>> associations =
                new ArrayList<>(lifecycle.associations().entrySet());
        associations.sort(Map.Entry.comparingByKey());
        List<Map.Entry<ObjectRecordTable.TileGroup, Integer>> metadata =
                new ArrayList<>(checkpoint.packedMetadataWords().entrySet());
        metadata.sort(Map.Entry.comparingByKey(Comparator
                .comparingInt(ObjectRecordTable.TileGroup::x)
                .thenComparingInt(ObjectRecordTable.TileGroup::y)
                .thenComparingInt(ObjectRecordTable.TileGroup::group)));
        if (records.size() > MAX_RECORDS || links.size() > MAX_TILE_LINKS
                || associations.size() > MAX_ASSOCIATIONS || metadata.size() > MAX_TILE_LINKS)
            throw new IllegalArgumentException("checkpoint exceeds codec limits");
        // Preflight the exact fixed-width payload size before allocating or writing.
        long estimatedSize = 20L
                + records.size() * 32L
                + 4L + ObjectRecordTable.OBJECT_SLOT_CAPACITY * 4L
                + 4L + links.size() * 20L
                + 4L + ObjectRecordTable.OBJECT_SLOT_CAPACITY * 8L
                + 4L + associations.size() * 12L
                + 4L + metadata.size() * 20L;
        if (estimatedSize > MAX_ENCODED_BYTES)
            throw new IllegalArgumentException("encoded checkpoint exceeds byte limit");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) estimatedSize);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT_VERSION);
            out.writeInt(checkpoint.schemaVersion());
            out.writeInt(objects.schemaVersion());
            out.writeInt(records.size());
            for (ObjectRecordTable.RecordSnapshot record : records) {
                out.writeInt(record.slotId());
                out.writeInt(record.generation());
                out.writeInt(record.type().ordinal());
                out.writeInt(record.anchorX());
                out.writeInt(record.anchorY());
                out.writeInt(record.anchorGroup());
                out.writeLong(record.rawLegacyPayload());
            }
            writeIntArray(out, objects.generations(), ObjectRecordTable.OBJECT_SLOT_CAPACITY);
            out.writeInt(links.size());
            for (Map.Entry<ObjectRecordTable.TileGroup, Integer> entry : links) {
                writeTile(out, entry.getKey());
                out.writeInt(entry.getValue());
            }
            writeLongArray(out, objects.auxiliaryEntries(), ObjectRecordTable.OBJECT_SLOT_CAPACITY);
            out.writeInt(associations.size());
            for (Map.Entry<Long, Integer> entry : associations) {
                out.writeLong(entry.getKey());
                out.writeInt(entry.getValue());
            }
            out.writeInt(metadata.size());
            for (Map.Entry<ObjectRecordTable.TileGroup, Integer> entry : metadata) {
                writeTile(out, entry.getKey());
                out.writeInt(entry.getValue());
            }
            out.flush();
        }
        byte[] encoded = bytes.toByteArray();
        if (encoded.length > MAX_ENCODED_BYTES)
            throw new IllegalArgumentException("encoded checkpoint exceeds byte limit");
        return encoded;
    }

    public static CrossTableCheckpoint decode(byte[] encoded) throws IOException {
        if (encoded == null || encoded.length == 0 || encoded.length > MAX_ENCODED_BYTES)
            throw new IOException("invalid checkpoint byte length");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid checkpoint magic");
            int formatVersion = in.readInt();
            if (formatVersion != FORMAT_VERSION)
                throw new IOException("unsupported binary format version: " + formatVersion);
            int envelopeVersion = in.readInt();
            int objectSchemaVersion = in.readInt();
            int recordCount = readCount(in, MAX_RECORDS, "records");
            List<ObjectRecordTable.RecordSnapshot> records = new ArrayList<>(recordCount);
            for (int i = 0; i < recordCount; i++) {
                int slot = in.readInt();
                int generation = in.readInt();
                int typeOrdinal = in.readInt();
                if (typeOrdinal < 0 || typeOrdinal >= ObjectRecordTable.RecordType.values().length)
                    throw new IOException("invalid record type ordinal");
                int x = in.readInt();
                int y = in.readInt();
                int group = in.readInt();
                long rawPayload = in.readLong();
                records.add(new ObjectRecordTable.RecordSnapshot(slot, generation,
                        ObjectRecordTable.RecordType.values()[typeOrdinal], x, y, group, rawPayload));
            }
            int[] generations = readIntArray(in, ObjectRecordTable.OBJECT_SLOT_CAPACITY);
            int linkCount = readCount(in, MAX_TILE_LINKS, "tile links");
            Map<ObjectRecordTable.TileGroup, Integer> links = new HashMap<>();
            for (int i = 0; i < linkCount; i++) {
                ObjectRecordTable.TileGroup tile = readTile(in);
                int slot = in.readInt();
                if (links.put(tile, slot) != null) throw new IOException("duplicate tile link key");
            }
            long[] auxiliaryEntries = readLongArray(in, ObjectRecordTable.OBJECT_SLOT_CAPACITY);
            int associationCount = readCount(in, MAX_ASSOCIATIONS, "associations");
            Map<Long, Integer> associations = new HashMap<>();
            for (int i = 0; i < associationCount; i++) {
                long handle = in.readLong();
                int slot = in.readInt();
                if (associations.put(handle, slot) != null)
                    throw new IOException("duplicate association handle");
            }
            int metadataCount = readCount(in, MAX_TILE_LINKS, "packed metadata");
            Map<ObjectRecordTable.TileGroup, Integer> metadata = new HashMap<>();
            for (int i = 0; i < metadataCount; i++) {
                ObjectRecordTable.TileGroup tile = readTile(in);
                int word = in.readInt();
                if (metadata.put(tile, word) != null)
                    throw new IOException("duplicate packed metadata key");
            }
            if (in.read() != -1) throw new IOException("trailing bytes after checkpoint payload");
            ObjectRecordTable.Snapshot objectSnapshot = new ObjectRecordTable.Snapshot(
                    objectSchemaVersion, records, generations, links, auxiliaryEntries);
            CrossTableLifecycleCoordinator.Snapshot lifecycle =
                    new CrossTableLifecycleCoordinator.Snapshot(objectSnapshot, associations);
            CrossTableCheckpoint checkpoint = new CrossTableCheckpoint(
                    envelopeVersion, lifecycle, metadata);
            CrossTableLifecycleCoordinator validated = new CrossTableLifecycleCoordinator();
            if (checkpoint.restoreInto(validated) != CrossTableLifecycleCoordinator.Status.OK)
                throw new IOException("checkpoint payload failed integrity validation");
            return checkpoint;
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw new IOException("malformed checkpoint payload", ex);
        } catch (java.io.EOFException ex) {
            throw new IOException("truncated checkpoint payload", ex);
        }
    }

    private static void writeTile(DataOutputStream out, ObjectRecordTable.TileGroup tile)
            throws IOException {
        if (tile == null) throw new IllegalArgumentException("null tile key");
        out.writeInt(tile.x());
        out.writeInt(tile.y());
        out.writeInt(tile.group());
    }

    private static ObjectRecordTable.TileGroup readTile(DataInputStream in) throws IOException {
        try {
            return new ObjectRecordTable.TileGroup(in.readInt(), in.readInt(), in.readInt());
        } catch (IllegalArgumentException ex) {
            throw new IOException("invalid tile key", ex);
        }
    }

    private static void writeIntArray(DataOutputStream out, int[] values, int expected)
            throws IOException {
        if (values == null || values.length != expected)
            throw new IllegalArgumentException("invalid generation array");
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    private static int[] readIntArray(DataInputStream in, int expected) throws IOException {
        int length = readCount(in, expected, "generation array");
        if (length != expected) throw new IOException("wrong generation array length");
        int[] values = new int[length];
        for (int i = 0; i < length; i++) values[i] = in.readInt();
        return values;
    }

    private static void writeLongArray(DataOutputStream out, long[] values, int expected)
            throws IOException {
        if (values == null || values.length != expected)
            throw new IllegalArgumentException("invalid auxiliary entry array");
        out.writeInt(values.length);
        for (long value : values) out.writeLong(value);
    }

    private static long[] readLongArray(DataInputStream in, int expected) throws IOException {
        int length = readCount(in, expected, "auxiliary entry array");
        if (length != expected) throw new IOException("wrong auxiliary entry array length");
        long[] values = new long[length];
        for (int i = 0; i < length; i++) values[i] = in.readLong();
        return values;
    }

    private static int readCount(DataInputStream in, int maximum, String label)
            throws IOException {
        int count = in.readInt();
        if (count < 0 || count > maximum)
            throw new IOException("invalid " + label + " count: " + count);
        return count;
    }
}