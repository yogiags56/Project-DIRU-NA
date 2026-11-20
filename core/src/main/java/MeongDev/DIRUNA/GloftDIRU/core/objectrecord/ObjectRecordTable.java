package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Standalone, platform-independent safety model for legacy object records.
 * This model is not wired into gameplay and does not claim unresolved
 * original-JAR behavior as established fact.
 */
public final class ObjectRecordTable {
    public static final int OBJECT_SLOT_CAPACITY = 15;
    public static final int TILE_RECORD_ID_MAX = 7;
    public static final int TILE_GROUP_COUNT = 3;
    private static final int SNAPSHOT_SCHEMA_VERSION = 1;

    public enum RecordType { TYPE_1, TYPE_2, TYPE_3, TYPE_4, TYPE_5, TYPE_6, TYPE_7 }

    public enum Status {
        OK, NO_CAPACITY, INVALID_ARGUMENT, STALE_HANDLE, NO_LINK,
        INVALID_TILE_ID, UNREPRESENTABLE_TILE_ID, TILE_GROUP_OCCUPIED,
        OBJECT_STILL_REFERENCED, ALREADY_LINKED, INVALID_SNAPSHOT, INVALID_TRANSITION
    }

    public record ObjectHandle(long value) {
        public ObjectHandle {
            if (value == 0) throw new IllegalArgumentException("zero is not a valid object handle");
        }
    }

    public record TileGroup(int x, int y, int group) {
        public TileGroup {
            if (x < 0 || y < 0 || group < 0 || group >= TILE_GROUP_COUNT)
                throw new IllegalArgumentException("invalid tile coordinate or group");
        }
    }

    public record AllocationResult(Status status, ObjectHandle handle, int slotId) {
        public boolean succeeded() { return status == Status.OK; }
    }

    public record LookupResult(Status status, ObjectHandle handle, RecordType type) {
        public boolean succeeded() { return status == Status.OK; }
    }

    /** Immutable serialized record data; slotId is one-based. */
    public record RecordSnapshot(int slotId, int generation, RecordType type,
                                 int anchorX, int anchorY, int anchorGroup,
                                 long rawLegacyPayload) {
        /** Backward-compatible constructor for schema-1 test fixtures. */
        public RecordSnapshot(int slotId, int generation, RecordType type,
                              int anchorX, int anchorY, int anchorGroup) {
            this(slotId, generation, type, anchorX, anchorY, anchorGroup,
                    legacyTypeBits(type));
        }
    }

    /**
     * Immutable checkpoint payload. Reverse references are intentionally
     * omitted: restore rebuilds them from the authoritative tile-link map.
     * Public construction permits validation of corrupted payloads in tests.
     */
    public static final class Snapshot {
        private final int schemaVersion;
        private final List<RecordSnapshot> records;
        private final int[] generations;
        private final Map<TileGroup, Integer> tileLinks;
        private final long[] auxiliaryEntries;

        public Snapshot(int schemaVersion, List<RecordSnapshot> records,
                        int[] generations, Map<TileGroup, Integer> tileLinks) {
            this(schemaVersion, records, generations, tileLinks, new long[OBJECT_SLOT_CAPACITY]);
        }

        public Snapshot(int schemaVersion, List<RecordSnapshot> records,
                        int[] generations, Map<TileGroup, Integer> tileLinks,
                        long[] auxiliaryEntries) {
            this.schemaVersion = schemaVersion;
            this.records = Collections.unmodifiableList(new ArrayList<>(
                    records == null ? List.of() : records));
            this.generations = generations == null ? null : generations.clone();
            this.tileLinks = Collections.unmodifiableMap(new HashMap<>(
                    tileLinks == null ? Map.of() : tileLinks));
            this.auxiliaryEntries = auxiliaryEntries == null ? null : auxiliaryEntries.clone();
        }

        public int schemaVersion() { return schemaVersion; }
        public List<RecordSnapshot> records() { return records; }
        public int[] generations() { return generations == null ? null : generations.clone(); }
        public Map<TileGroup, Integer> tileLinks() { return tileLinks; }
        public long[] auxiliaryEntries() {
            return auxiliaryEntries == null ? null : auxiliaryEntries.clone();
        }
    }

    private static final class Record {
        private final int generation;
        private RecordType type;
        private final int anchorX;
        private final int anchorY;
        private final int anchorGroup;
        private long rawLegacyPayload;
        private final Set<TileGroup> references = new LinkedHashSet<>();

        private Record(int generation, RecordType type, int anchorX, int anchorY, int anchorGroup) {
            this.generation = generation;
            this.type = type;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorGroup = anchorGroup;
            this.rawLegacyPayload = legacyTypeBits(type);
        }
    }

    private final Record[] slots = new Record[OBJECT_SLOT_CAPACITY];
    private final int[] generations = new int[OBJECT_SLOT_CAPACITY];
    private final Map<TileGroup, Integer> tileLinks = new HashMap<>();
    // Opaque 64-bit auxiliary entries mirroring the legacy bLongArr table.
    private final long[] auxiliaryEntries = new long[OBJECT_SLOT_CAPACITY];

    public ObjectRecordTable() {
        for (int i = 0; i < generations.length; i++) generations[i] = 1;
    }

    public synchronized AllocationResult allocate(RecordType type, int anchorX,
                                                  int anchorY, int anchorGroup) {
        if (type == RecordType.TYPE_7)
            return new AllocationResult(Status.INVALID_TRANSITION, null, 0);
        return allocateRecord(type, anchorX, anchorY, anchorGroup);
    }

    /** Dedicated Type-7 allocator branch; a fresh record starts with no link bits. */
    public synchronized AllocationResult allocateType7(int anchorX, int anchorY,
                                                        int anchorGroup) {
        return allocateRecord(RecordType.TYPE_7, anchorX, anchorY, anchorGroup);
    }

    private AllocationResult allocateRecord(RecordType type, int anchorX,
                                             int anchorY, int anchorGroup) {
        if (type == null || anchorX < 0 || anchorY < 0
                || anchorGroup < 0 || anchorGroup >= TILE_GROUP_COUNT)
            return new AllocationResult(Status.INVALID_ARGUMENT, null, 0);

        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == null) {
                int slotId = index + 1;
                int generation = generations[index];
                slots[index] = new Record(generation, type, anchorX, anchorY, anchorGroup);
                return new AllocationResult(Status.OK, encodeHandle(slotId, generation), slotId);
            }
        }
        return new AllocationResult(Status.NO_CAPACITY, null, 0);
    }

    public synchronized Status link(TileGroup tile, ObjectHandle handle) {
        if (tile == null || handle == null) return Status.INVALID_ARGUMENT;
        Record record = resolve(handle);
        if (record == null) return Status.STALE_HANDLE;
        int slotId = decodeSlot(handle);
        if (slotId > TILE_RECORD_ID_MAX) return Status.UNREPRESENTABLE_TILE_ID;

        Integer currentSlot = tileLinks.get(tile);
        if (currentSlot != null)
            return currentSlot == slotId ? Status.ALREADY_LINKED : Status.TILE_GROUP_OCCUPIED;

        tileLinks.put(tile, slotId);
        record.references.add(tile);
        return Status.OK;
    }

    public synchronized LookupResult lookup(TileGroup tile) {
        if (tile == null) return new LookupResult(Status.INVALID_ARGUMENT, null, null);
        Integer tileId = tileLinks.get(tile);
        if (tileId == null || tileId == 0)
            return new LookupResult(Status.NO_LINK, null, null);
        if (tileId < 1 || tileId > TILE_RECORD_ID_MAX)
            return new LookupResult(Status.INVALID_TILE_ID, null, null);
        Record record = slots[tileId - 1];
        if (record == null) return new LookupResult(Status.STALE_HANDLE, null, null);
        return new LookupResult(Status.OK, encodeHandle(tileId, record.generation), record.type);
    }

    /** Removes one tile-group link; it never frees the object record. */
    public synchronized Status unlink(TileGroup tile) {
        if (tile == null) return Status.INVALID_ARGUMENT;
        Integer slotId = tileLinks.remove(tile);
        if (slotId == null) return Status.NO_LINK;
        if (slotId < 1 || slotId > OBJECT_SLOT_CAPACITY) return Status.INVALID_TILE_ID;
        Record record = slots[slotId - 1];
        if (record != null) record.references.remove(tile);
        return Status.OK;
    }

    public synchronized Status reclassify(ObjectHandle handle, RecordType newType) {
        if (newType == null) return Status.INVALID_ARGUMENT;
        Record record = resolve(handle);
        if (record == null) return Status.STALE_HANDLE;
        // Type-7 is a dedicated allocator path and terminal source path, not a
        // generic in-place reclassification target or source.
        if (record.type == RecordType.TYPE_7 || newType == RecordType.TYPE_7)
            return Status.INVALID_TRANSITION;
        record.type = newType;
        // Source type-field writes are field-local: retain bits 54..56.
        record.rawLegacyPayload = (record.rawLegacyPayload & ~LEGACY_TYPE_MASK)
                | legacyTypeBits(newType);
        return Status.OK;
    }

    /**
     * Models the source's explicit replacement path: transfer the three-bit
     * auxiliary link from a live non-Type-7 source to an existing target,
     * reclassify that target, then release the source. All changes roll back
     * if the source cannot be released.
     */
    public synchronized Status replaceAndTransferLink(ObjectHandle sourceHandle,
                                                       ObjectHandle targetHandle,
                                                       RecordType targetType) {
        if (sourceHandle == null || targetHandle == null || targetType == null)
            return Status.INVALID_ARGUMENT;
        if (sourceHandle.equals(targetHandle) || targetType != RecordType.TYPE_2)
            return Status.INVALID_TRANSITION;
        Record source = resolve(sourceHandle);
        Record target = resolve(targetHandle);
        if (source == null || target == null) return Status.STALE_HANDLE;
        if (source.type != RecordType.TYPE_1 && source.type != RecordType.TYPE_6)
            return Status.INVALID_TRANSITION;
        // The legacy branch overwrites the resolved target's type field with
        // Type-2 without first branching on that target's previous type.

        Snapshot before = snapshot();
        long transferredLink = source.rawLegacyPayload & LegacyObjectRecordLinkCodec.LINK_MASK;
        target.rawLegacyPayload = (target.rawLegacyPayload
                & ~LegacyObjectRecordLinkCodec.LINK_MASK) | transferredLink;
        target.type = targetType;
        target.rawLegacyPayload = (target.rawLegacyPayload & ~LEGACY_TYPE_MASK)
                | legacyTypeBits(targetType);

        Status releaseStatus = release(sourceHandle);
        if (releaseStatus != Status.OK) {
            restore(before);
            return releaseStatus;
        }
        return Status.OK;
    }

    private static final long LEGACY_TYPE_MASK = 0x7L << 28;

    private static long legacyTypeBits(RecordType type) {
        if (type == null) return 0L;
        return ((long) (type.ordinal() + 1) & 0x7L) << 28;
    }

    /** Returns the packed legacy aLongArr payload for forensic/native integration. */
    public synchronized long rawLegacyPayload(ObjectHandle handle) {
        Record record = resolve(handle);
        if (record == null) throw new IllegalArgumentException("stale object handle");
        return record.rawLegacyPayload;
    }

    /** Writes the packed auxiliary link field while preserving all other bits. */
    public synchronized Status writeAuxiliaryLink(ObjectHandle handle, int auxiliarySlot) {
        Record record = resolve(handle);
        if (record == null) return Status.STALE_HANDLE;
        LegacyObjectRecordLinkCodec.WriteResult result = LegacyObjectRecordLinkCodec.write(
                record.rawLegacyPayload, record.type, auxiliarySlot);
        if (!result.succeeded()) return Status.INVALID_ARGUMENT;
        record.rawLegacyPayload = result.rawRecord();
        return Status.OK;
    }

    /** Reads the semantic auxiliary link from the packed legacy payload. */
    public synchronized LegacyObjectRecordLinkCodec.ReadResult readAuxiliaryLink(
            ObjectHandle handle) {
        Record record = resolve(handle);
        if (record == null)
            return new LegacyObjectRecordLinkCodec.ReadResult(
                    LegacyObjectRecordLinkCodec.Status.INVALID_TYPE, -1);
        return LegacyObjectRecordLinkCodec.read(record.rawLegacyPayload, record.type);
    }

    public synchronized boolean matchesAnchor(ObjectHandle handle, int x, int y, int group) {
        Record record = resolve(handle);
        return record != null && record.anchorX == x
                && record.anchorY == y && record.anchorGroup == group;
    }

    public synchronized Status release(ObjectHandle handle) {
        if (handle == null) return Status.INVALID_ARGUMENT;
        int slotId = decodeSlot(handle);
        if (slotId < 1 || slotId > OBJECT_SLOT_CAPACITY) return Status.STALE_HANDLE;
        Record record = resolve(handle);
        if (record == null) return Status.STALE_HANDLE;
        if (!record.references.isEmpty()) return Status.OBJECT_STILL_REFERENCED;

        int index = slotId - 1;
        slots[index] = null;
        generations[index] = nextGeneration(generations[index]);
        return Status.OK;
    }

    public synchronized int referenceCount(ObjectHandle handle) {
        Record record = resolve(handle);
        return record == null ? -1 : record.references.size();
    }

    /** Returns an opaque auxiliary entry by zero-based table index. */
    public synchronized long auxiliaryEntryRaw(int index) {
        checkAuxiliaryIndex(index);
        return auxiliaryEntries[index];
    }

    /** Replaces an opaque auxiliary entry without interpreting its packed fields. */
    public synchronized void setAuxiliaryEntryRaw(int index, long value) {
        checkAuxiliaryIndex(index);
        auxiliaryEntries[index] = value;
    }

    private static void checkAuxiliaryIndex(int index) {
        if (index < 0 || index >= OBJECT_SLOT_CAPACITY)
            throw new IndexOutOfBoundsException("auxiliary index must be 0..14");
    }

    public synchronized Optional<RecordType> typeOf(ObjectHandle handle) {
        Record record = resolve(handle);
        return record == null ? Optional.empty() : Optional.of(record.type);
    }

    /** Creates an immutable snapshot of occupied records, generations and tile links. */
    public synchronized Snapshot snapshot() {
        List<RecordSnapshot> recordSnapshots = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            Record record = slots[i];
            if (record != null) {
                recordSnapshots.add(new RecordSnapshot(i + 1, record.generation, record.type,
                        record.anchorX, record.anchorY, record.anchorGroup,
                        record.rawLegacyPayload));
            }
        }
        return new Snapshot(SNAPSHOT_SCHEMA_VERSION, recordSnapshots, generations,
                tileLinks, auxiliaryEntries);
    }

    /**
     * Validates a snapshot in a temporary table, then atomically replaces this
     * table's state. Reverse-reference sets are rebuilt from tileLinks.
     */
    public synchronized Status restore(Snapshot snapshot) {
        ObjectRecordTable candidate = fromSnapshot(snapshot);
        if (candidate == null) return Status.INVALID_SNAPSHOT;

        System.arraycopy(candidate.slots, 0, slots, 0, slots.length);
        System.arraycopy(candidate.generations, 0, generations, 0, generations.length);
        tileLinks.clear();
        tileLinks.putAll(candidate.tileLinks);
        System.arraycopy(candidate.auxiliaryEntries, 0, auxiliaryEntries, 0, auxiliaryEntries.length);
        return Status.OK;
    }

    private static ObjectRecordTable fromSnapshot(Snapshot snapshot) {
        if (snapshot == null || snapshot.schemaVersion != SNAPSHOT_SCHEMA_VERSION
                || snapshot.generations == null
                || snapshot.generations.length != OBJECT_SLOT_CAPACITY
                || snapshot.records == null || snapshot.tileLinks == null
                || snapshot.auxiliaryEntries == null
                || snapshot.auxiliaryEntries.length != OBJECT_SLOT_CAPACITY) return null;

        ObjectRecordTable candidate = new ObjectRecordTable();
        System.arraycopy(snapshot.auxiliaryEntries, 0, candidate.auxiliaryEntries, 0,
                candidate.auxiliaryEntries.length);
        for (int i = 0; i < OBJECT_SLOT_CAPACITY; i++) {
            if (snapshot.generations[i] <= 0) return null;
            candidate.generations[i] = snapshot.generations[i];
        }

        boolean[] seen = new boolean[OBJECT_SLOT_CAPACITY];
        for (RecordSnapshot item : snapshot.records) {
            if (item == null || item.slotId() < 1 || item.slotId() > OBJECT_SLOT_CAPACITY
                    || item.generation() <= 0 || item.type() == null
                    || item.anchorX() < 0 || item.anchorY() < 0
                    || item.anchorGroup() < 0 || item.anchorGroup() >= TILE_GROUP_COUNT)
                return null;
            int index = item.slotId() - 1;
            if (seen[index] || candidate.generations[index] != item.generation()) return null;
            seen[index] = true;
            candidate.slots[index] = new Record(item.generation(), item.type(),
                    item.anchorX(), item.anchorY(), item.anchorGroup());
            candidate.slots[index].rawLegacyPayload = item.rawLegacyPayload();
            if ((item.rawLegacyPayload() & LEGACY_TYPE_MASK) != legacyTypeBits(item.type())
                    || (item.type() == RecordType.TYPE_7
                    && (item.rawLegacyPayload() & LegacyObjectRecordLinkCodec.LINK_MASK) != 0L))
                return null;
        }

        for (Map.Entry<TileGroup, Integer> entry : snapshot.tileLinks.entrySet()) {
            TileGroup tile = entry.getKey();
            Integer slotId = entry.getValue();
            if (tile == null || slotId == null || slotId < 1
                    || slotId > TILE_RECORD_ID_MAX) return null;
            Record record = candidate.slots[slotId - 1];
            if (record == null) return null;
            if (!record.references.add(tile)) return null;
            if (candidate.tileLinks.put(tile, slotId) != null) return null;
        }
        return candidate;
    }

    private Record resolve(ObjectHandle handle) {
        int slotId = decodeSlot(handle);
        if (slotId < 1 || slotId > OBJECT_SLOT_CAPACITY) return null;
        Record record = slots[slotId - 1];
        if (record == null || record.generation != decodeGeneration(handle)) return null;
        return record;
    }

    private static ObjectHandle encodeHandle(int slotId, int generation) {
        return new ObjectHandle((((long) generation) << 4) | slotId);
    }

    private static int decodeSlot(ObjectHandle handle) {
        return (int) (handle.value() & 0x0F);
    }

    private static int decodeGeneration(ObjectHandle handle) {
        return (int) (handle.value() >>> 4);
    }

    private static int nextGeneration(int generation) {
        return generation == Integer.MAX_VALUE ? 1 : generation + 1;
    }
}
