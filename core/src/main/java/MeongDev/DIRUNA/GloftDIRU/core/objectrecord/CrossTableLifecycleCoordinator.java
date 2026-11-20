package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Isolated coordinator for object-record and auxiliary-table lifecycle tests.
 *
 * The object-to-auxiliary association is kept as an explicit logical mapping.
 * This is intentionally NOT a byte-exact emulation of the packed legacy
 * aLongArr field; that field integration remains a separate task.
 */
public final class CrossTableLifecycleCoordinator {
    public enum Status {
        OK, INVALID_ARGUMENT, OBJECT_ALLOCATION_FAILED, TILE_LINK_FAILED,
        AUXILIARY_SLOT_OCCUPIED, UNREPRESENTABLE_LINK, INVALID_AUXILIARY_STATE,
        NO_ASSOCIATION, TRANSITION_REJECTED, CONSUME_REJECTED, INVALID_SNAPSHOT
    }

    public record AttachResult(Status status, ObjectRecordTable.ObjectHandle objectHandle,
                               int auxiliarySlot) {
        public boolean succeeded() { return status == Status.OK; }
    }

    public record OperationResult(Status status, long auxiliaryRaw) {
        public boolean succeeded() { return status == Status.OK; }
    }

    public static final class Snapshot {
        private final ObjectRecordTable.Snapshot objectSnapshot;
        private final Map<Long, Integer> associations;

        public Snapshot(ObjectRecordTable.Snapshot objectSnapshot,
                        Map<Long, Integer> associations) {
            this.objectSnapshot = objectSnapshot;
            this.associations = Collections.unmodifiableMap(new HashMap<>(
                    associations == null ? Map.of() : associations));
        }

        public ObjectRecordTable.Snapshot objectSnapshot() { return objectSnapshot; }
        public Map<Long, Integer> associations() { return associations; }
    }

    private final ObjectRecordTable objects;
    private final Map<Long, Integer> associations = new HashMap<>();

    public CrossTableLifecycleCoordinator() {
        this(new ObjectRecordTable());
    }

    public CrossTableLifecycleCoordinator(ObjectRecordTable objects) {
        if (objects == null) throw new IllegalArgumentException("objects is required");
        this.objects = objects;
    }

    /**
     * Allocates an object and associates a prebuilt state-1 auxiliary word.
     * The operation rolls back the object table if any later step fails.
     */
    public synchronized AttachResult attach(ObjectRecordTable.RecordType type,
                                            int anchorX, int anchorY, int anchorGroup,
                                            ObjectRecordTable.TileGroup tile,
                                            int auxiliarySlot, long stateOneRaw) {
        if (type == null || tile == null)
            return new AttachResult(Status.INVALID_ARGUMENT, null, -1);

        AuxiliaryLinkCodec.EncodeResult encoded = AuxiliaryLinkCodec.encodeSafe(auxiliarySlot);
        if (!encoded.succeeded()) {
            Status status = encoded.status() == AuxiliaryLinkCodec.Status.UNREPRESENTABLE_SLOT
                    ? Status.UNREPRESENTABLE_LINK : Status.INVALID_ARGUMENT;
            return new AttachResult(status, null, auxiliarySlot);
        }
        if (AuxiliaryEntryStateMachine.state(stateOneRaw)
                != AuxiliaryEntryStateMachine.STATE_ALLOCATED)
            return new AttachResult(Status.INVALID_AUXILIARY_STATE, null, auxiliarySlot);
        if (objects.auxiliaryEntryRaw(auxiliarySlot) != 0L)
            return new AttachResult(Status.AUXILIARY_SLOT_OCCUPIED, null, auxiliarySlot);
        if (associations.containsValue(auxiliarySlot))
            return new AttachResult(Status.AUXILIARY_SLOT_OCCUPIED, null, auxiliarySlot);

        ObjectRecordTable.Snapshot before = objects.snapshot();
        ObjectRecordTable.AllocationResult allocation =
                objects.allocate(type, anchorX, anchorY, anchorGroup);
        if (!allocation.succeeded())
            return new AttachResult(Status.OBJECT_ALLOCATION_FAILED, null, auxiliarySlot);

        ObjectRecordTable.Status linkStatus = objects.link(tile, allocation.handle());
        if (linkStatus != ObjectRecordTable.Status.OK) {
            objects.restore(before);
            return new AttachResult(Status.TILE_LINK_FAILED, null, auxiliarySlot);
        }

        objects.setAuxiliaryEntryRaw(auxiliarySlot, stateOneRaw);
        associations.put(allocation.handle().value(), auxiliarySlot);
        return new AttachResult(Status.OK, allocation.handle(), auxiliarySlot);
    }

    /** Applies the guarded state-1 -> state-3 transition for an associated object. */
    public synchronized OperationResult markReady(ObjectRecordTable.ObjectHandle handle) {
        if (handle == null) return new OperationResult(Status.INVALID_ARGUMENT, 0L);
        Integer slot = associations.get(handle.value());
        if (slot == null) return new OperationResult(Status.NO_ASSOCIATION, 0L);
        if (objects.typeOf(handle).isEmpty())
            return new OperationResult(Status.NO_ASSOCIATION, 0L);

        long raw = objects.auxiliaryEntryRaw(slot);
        AuxiliaryEntryStateMachine.TransitionResult result =
                AuxiliaryEntryStateMachine.markReady(raw, slot, slot);
        if (!result.succeeded())
            return new OperationResult(Status.TRANSITION_REJECTED, raw);

        objects.setAuxiliaryEntryRaw(slot, result.rawEntry());
        return new OperationResult(Status.OK, result.rawEntry());
    }

    /** Consumes and clears an associated state-3 entry, then drops its mapping. */
    public synchronized OperationResult consume(ObjectRecordTable.ObjectHandle handle) {
        if (handle == null) return new OperationResult(Status.INVALID_ARGUMENT, 0L);
        Integer slot = associations.get(handle.value());
        if (slot == null) return new OperationResult(Status.NO_ASSOCIATION, 0L);
        if (objects.typeOf(handle).isEmpty())
            return new OperationResult(Status.NO_ASSOCIATION, 0L);

        long raw = objects.auxiliaryEntryRaw(slot);
        AuxiliaryEntryStateMachine.ConsumeResult result =
                AuxiliaryEntryStateMachine.consume(raw);
        if (!result.succeeded())
            return new OperationResult(Status.CONSUME_REJECTED, raw);

        objects.setAuxiliaryEntryRaw(slot, result.remainingRawEntry());
        associations.remove(handle.value());
        return new OperationResult(Status.OK, 0L);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(objects.snapshot(), associations);
    }

    /** Validates association entries before atomically restoring both model layers. */
    public synchronized Status restore(Snapshot snapshot) {
        if (snapshot == null || snapshot.objectSnapshot() == null)
            return Status.INVALID_SNAPSHOT;

        ObjectRecordTable candidate = new ObjectRecordTable();
        if (candidate.restore(snapshot.objectSnapshot()) != ObjectRecordTable.Status.OK)
            return Status.INVALID_SNAPSHOT;

        Map<Long, Integer> validated = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : snapshot.associations().entrySet()) {
            Long handleValue = entry.getKey();
            Integer slot = entry.getValue();
            if (handleValue == null || slot == null
                    || !AuxiliaryLinkCodec.encodeSafe(slot).succeeded())
                return Status.INVALID_SNAPSHOT;
            ObjectRecordTable.ObjectHandle handle;
            try {
                handle = new ObjectRecordTable.ObjectHandle(handleValue);
            } catch (IllegalArgumentException ex) {
                return Status.INVALID_SNAPSHOT;
            }
            if (candidate.typeOf(handle).isEmpty()
                    || AuxiliaryEntryStateMachine.state(candidate.auxiliaryEntryRaw(slot))
                    == AuxiliaryEntryStateMachine.STATE_EMPTY
                    || validated.containsValue(slot))
                return Status.INVALID_SNAPSHOT;
            validated.put(handleValue, slot);
        }

        ObjectRecordTable.Status restored = objects.restore(snapshot.objectSnapshot());
        if (restored != ObjectRecordTable.Status.OK) return Status.INVALID_SNAPSHOT;
        associations.clear();
        associations.putAll(validated);
        return Status.OK;
    }

    public ObjectRecordTable objects() { return objects; }

    public synchronized int associatedSlot(ObjectRecordTable.ObjectHandle handle) {
        if (handle == null) return -1;
        return associations.getOrDefault(handle.value(), -1);
    }
}
