package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Standalone, platform-independent safety model for legacy object records.
 *
 * <p>The legacy object table has 15 one-based slots, while a tile metadata
 * group can encode only a three-bit record ID (1..7; zero means no link).
 * This class deliberately keeps those domains separate and never truncates
 * an object slot into a tile ID.</p>
 *
 * <p>This is a native-core design/test harness, not yet wired into gameplay
 * and not claimed to reproduce unresolved original-JAR behavior.</p>
 */
public final class ObjectRecordTable {
    public static final int OBJECT_SLOT_CAPACITY = 15;
    public static final int TILE_RECORD_ID_MAX = 7;
    public static final int TILE_GROUP_COUNT = 3;

    public enum RecordType {
        TYPE_1, TYPE_2, TYPE_3, TYPE_4, TYPE_5, TYPE_6, TYPE_7
    }

    public enum Status {
        OK,
        NO_CAPACITY,
        INVALID_ARGUMENT,
        STALE_HANDLE,
        NO_LINK,
        INVALID_TILE_ID,
        UNREPRESENTABLE_TILE_ID,
        TILE_GROUP_OCCUPIED,
        OBJECT_STILL_REFERENCED,
        ALREADY_LINKED
    }

    public record ObjectHandle(long value) {
        public ObjectHandle {
            if (value == 0) {
                throw new IllegalArgumentException("zero is not a valid object handle");
            }
        }
    }

    public record TileGroup(int x, int y, int group) {
        public TileGroup {
            if (x < 0 || y < 0 || group < 0 || group >= TILE_GROUP_COUNT) {
                throw new IllegalArgumentException("invalid tile coordinate or group");
            }
        }
    }

    public record AllocationResult(Status status, ObjectHandle handle, int slotId) {
        public boolean succeeded() {
            return status == Status.OK;
        }
    }

    public record LookupResult(Status status, ObjectHandle handle, RecordType type) {
        public boolean succeeded() {
            return status == Status.OK;
        }
    }

    private static final class Record {
        private final int generation;
        private RecordType type;
        private final int anchorX;
        private final int anchorY;
        private final int anchorGroup;
        private final Set<TileGroup> references = new LinkedHashSet<>();

        private Record(int generation, RecordType type,
                       int anchorX, int anchorY, int anchorGroup) {
            this.generation = generation;
            this.type = type;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorGroup = anchorGroup;
        }
    }

    private final Record[] slots = new Record[OBJECT_SLOT_CAPACITY];
    private final int[] generations = new int[OBJECT_SLOT_CAPACITY];
    private final Map<TileGroup, Integer> tileLinks = new HashMap<>();

    public ObjectRecordTable() {
        for (int i = 0; i < generations.length; i++) {
            generations[i] = 1;
        }
    }

    /**
     * Allocates the lowest free one-based slot. Slots 8..15 remain valid
     * object handles, but cannot be linked through the legacy three-bit tile ID.
     */
    public synchronized AllocationResult allocate(RecordType type,
                                                  int anchorX, int anchorY,
                                                  int anchorGroup) {
        if (type == null || anchorX < 0 || anchorY < 0
                || anchorGroup < 0 || anchorGroup >= TILE_GROUP_COUNT) {
            return new AllocationResult(Status.INVALID_ARGUMENT, null, 0);
        }

        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == null) {
                int slotId = index + 1;
                int generation = generations[index];
                slots[index] = new Record(generation, type,
                        anchorX, anchorY, anchorGroup);
                return new AllocationResult(Status.OK,
                        encodeHandle(slotId, generation), slotId);
            }
        }
        return new AllocationResult(Status.NO_CAPACITY, null, 0);
    }

    /**
     * Links a tile group to a record. The reverse-reference set and tile map
     * are updated together under this object's monitor.
     */
    public synchronized Status link(TileGroup tile, ObjectHandle handle) {
        if (tile == null || handle == null) {
            return Status.INVALID_ARGUMENT;
        }
        Record record = resolve(handle);
        if (record == null) {
            return Status.STALE_HANDLE;
        }

        int slotId = decodeSlot(handle);
        if (slotId > TILE_RECORD_ID_MAX) {
            return Status.UNREPRESENTABLE_TILE_ID;
        }

        Integer currentSlot = tileLinks.get(tile);
        if (currentSlot != null) {
            return currentSlot == slotId ? Status.ALREADY_LINKED
                    : Status.TILE_GROUP_OCCUPIED;
        }

        tileLinks.put(tile, slotId);
        record.references.add(tile);
        return Status.OK;
    }

    /**
     * Resolves a legacy tile link. A linked record can be shared by multiple
     * tile groups, so this method does not compare each linked tile with the
     * record's single canonical anchor coordinate.
     */
    public synchronized LookupResult lookup(TileGroup tile) {
        if (tile == null) {
            return new LookupResult(Status.INVALID_ARGUMENT, null, null);
        }
        Integer tileId = tileLinks.get(tile);
        if (tileId == null || tileId == 0) {
            return new LookupResult(Status.NO_LINK, null, null);
        }
        if (tileId < 1 || tileId > TILE_RECORD_ID_MAX) {
            return new LookupResult(Status.INVALID_TILE_ID, null, null);
        }

        Record record = slots[tileId - 1];
        if (record == null) {
            return new LookupResult(Status.STALE_HANDLE, null, null);
        }
        return new LookupResult(Status.OK,
                encodeHandle(tileId, record.generation), record.type);
    }

    /** Removes only this tile-group link; it never frees the object record. */
    public synchronized Status unlink(TileGroup tile) {
        if (tile == null) {
            return Status.INVALID_ARGUMENT;
        }
        Integer slotId = tileLinks.remove(tile);
        if (slotId == null) {
            return Status.NO_LINK;
        }
        if (slotId < 1 || slotId > OBJECT_SLOT_CAPACITY) {
            return Status.INVALID_TILE_ID;
        }
        Record record = slots[slotId - 1];
        if (record != null) {
            record.references.remove(tile);
        }
        return Status.OK;
    }

    public synchronized Status reclassify(ObjectHandle handle, RecordType newType) {
        if (newType == null) {
            return Status.INVALID_ARGUMENT;
        }
        Record record = resolve(handle);
        if (record == null) {
            return Status.STALE_HANDLE;
        }
        record.type = newType;
        return Status.OK;
    }

    /** Canonical anchor validation is separate from shared tile-link lookup. */
    public synchronized boolean matchesAnchor(ObjectHandle handle,
                                              int x, int y, int group) {
        Record record = resolve(handle);
        return record != null && record.anchorX == x
                && record.anchorY == y && record.anchorGroup == group;
    }

    public synchronized Status release(ObjectHandle handle) {
        if (handle == null) {
            return Status.INVALID_ARGUMENT;
        }
        int slotId = decodeSlot(handle);
        if (slotId < 1 || slotId > OBJECT_SLOT_CAPACITY) {
            return Status.STALE_HANDLE;
        }
        Record record = resolve(handle);
        if (record == null) {
            return Status.STALE_HANDLE;
        }
        if (!record.references.isEmpty()) {
            return Status.OBJECT_STILL_REFERENCED;
        }

        int index = slotId - 1;
        slots[index] = null;
        generations[index] = nextGeneration(generations[index]);
        return Status.OK;
    }

    public synchronized int referenceCount(ObjectHandle handle) {
        Record record = resolve(handle);
        return record == null ? -1 : record.references.size();
    }

    public synchronized Optional<RecordType> typeOf(ObjectHandle handle) {
        Record record = resolve(handle);
        return record == null ? Optional.empty() : Optional.of(record.type);
    }

    private Record resolve(ObjectHandle handle) {
        int slotId = decodeSlot(handle);
        if (slotId < 1 || slotId > OBJECT_SLOT_CAPACITY) {
            return null;
        }
        Record record = slots[slotId - 1];
        if (record == null || record.generation != decodeGeneration(handle)) {
            return null;
        }
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
