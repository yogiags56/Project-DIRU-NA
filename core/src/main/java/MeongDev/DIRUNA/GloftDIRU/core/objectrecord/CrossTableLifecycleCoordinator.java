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
        NO_ASSOCIATION, TRANSITION_REJECTED, CONSUME_REJECTED, INVALID_SNAPSHOT,
        SOURCE_ASSOCIATION_MISMATCH, TARGET_ALREADY_ASSOCIATED, TARGET_AUXILIARY_CONFLICT
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

        ObjectRecordTable.Status rawLinkStatus =
                objects.writeAuxiliaryLink(allocation.handle(), auxiliarySlot);
        if (rawLinkStatus != ObjectRecordTable.Status.OK) {
            objects.restore(before);
            return new AttachResult(Status.UNREPRESENTABLE_LINK, null, auxiliarySlot);
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

        LegacyObjectRecordLinkCodec.ReadResult packedLink =
                objects.readAuxiliaryLink(handle);
        if (!packedLink.succeeded() || packedLink.auxiliarySlot() != slot)
            return new OperationResult(Status.NO_ASSOCIATION, objects.auxiliaryEntryRaw(slot));
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

        LegacyObjectRecordLinkCodec.ReadResult packedLink =
                objects.readAuxiliaryLink(handle);
        if (!packedLink.succeeded() || packedLink.auxiliarySlot() != slot)
            return new OperationResult(Status.NO_ASSOCIATION, objects.auxiliaryEntryRaw(slot));
        long raw = objects.auxiliaryEntryRaw(slot);
        AuxiliaryEntryStateMachine.ConsumeResult result =
                AuxiliaryEntryStateMachine.consume(raw);
        if (!result.succeeded())
            return new OperationResult(Status.CONSUME_REJECTED, raw);

        objects.setAuxiliaryEntryRaw(slot, result.remainingRawEntry());
        associations.remove(handle.value());
        return new OperationResult(Status.OK, 0L);
    }

    /**
     * Replaces the object currently referenced by targetTile with Type-2,
     * transferring the source object's auxiliary association to that target.
     * The source must already have no tile references, as required by the
     * source-observed release ordering.
     */
    public synchronized OperationResult replaceFromTileMetadata(
            ObjectRecordTable.ObjectHandle sourceHandle,
            ObjectRecordTable.TileGroup targetTile) {
        if (sourceHandle == null || targetTile == null)
            return new OperationResult(Status.INVALID_ARGUMENT, 0L);

        Integer sourceSlot = associations.get(sourceHandle.value());
        if (sourceSlot == null)
            return new OperationResult(Status.NO_ASSOCIATION, 0L);
        if (objects.typeOf(sourceHandle).isEmpty())
            return new OperationResult(Status.NO_ASSOCIATION, 0L);

        LegacyObjectRecordLinkCodec.ReadResult sourceLink =
                objects.readAuxiliaryLink(sourceHandle);
        if (!sourceLink.succeeded() || sourceLink.auxiliarySlot() != sourceSlot)
            return new OperationResult(Status.SOURCE_ASSOCIATION_MISMATCH,
                    objects.auxiliaryEntryRaw(sourceSlot));

        int sourceState = AuxiliaryEntryStateMachine.state(
                objects.auxiliaryEntryRaw(sourceSlot));
        if (sourceState != AuxiliaryEntryStateMachine.STATE_ALLOCATED
                && sourceState != AuxiliaryEntryStateMachine.STATE_READY)
            return new OperationResult(Status.INVALID_AUXILIARY_STATE,
                    objects.auxiliaryEntryRaw(sourceSlot));

        ObjectRecordTable.LookupResult targetLookup = objects.lookup(targetTile);
        if (!targetLookup.succeeded())
            return new OperationResult(Status.TRANSITION_REJECTED, 0L);
        ObjectRecordTable.ObjectHandle targetHandle = targetLookup.handle();
        if (sourceHandle.equals(targetHandle))
            return new OperationResult(Status.TRANSITION_REJECTED, 0L);
        if (associations.containsKey(targetHandle.value()))
            return new OperationResult(Status.TARGET_ALREADY_ASSOCIATED,
                    objects.auxiliaryEntryRaw(associations.get(targetHandle.value())));

        // Do not silently overwrite a target's independently active auxiliary
        // entry. A matching raw slot is the source association being transferred.
        if (targetLookup.type() != ObjectRecordTable.RecordType.TYPE_7) {
            long targetRaw = objects.rawLegacyPayload(targetHandle);
            int targetSlot = LegacyObjectRecordLinkCodec.readRawField(targetRaw);
            int targetState = AuxiliaryEntryStateMachine.state(
                    objects.auxiliaryEntryRaw(targetSlot));
            if (targetSlot != sourceSlot
                    && targetState != AuxiliaryEntryStateMachine.STATE_EMPTY)
                return new OperationResult(Status.TARGET_AUXILIARY_CONFLICT,
                        objects.auxiliaryEntryRaw(targetSlot));
        }

        ObjectRecordTable.Status replaced =
                objects.replaceFromTileMetadata(sourceHandle, targetTile);
        if (replaced != ObjectRecordTable.Status.OK)
            return new OperationResult(Status.TRANSITION_REJECTED,
                    objects.auxiliaryEntryRaw(sourceSlot));

        associations.remove(sourceHandle.value());
        associations.put(targetHandle.value(), sourceSlot);
        return new OperationResult(Status.OK, objects.auxiliaryEntryRaw(sourceSlot));
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
            int auxiliaryState = AuxiliaryEntryStateMachine.state(
                    candidate.auxiliaryEntryRaw(slot));
            LegacyObjectRecordLinkCodec.ReadResult packedLink =
                    candidate.readAuxiliaryLink(handle);
            if (candidate.typeOf(handle).isEmpty()
                    || !packedLink.succeeded() || packedLink.auxiliarySlot() != slot
                    || (auxiliaryState != AuxiliaryEntryStateMachine.STATE_ALLOCATED
                    && auxiliaryState != AuxiliaryEntryStateMachine.STATE_READY)
                    || validated.containsValue(slot))
                return Status.INVALID_SNAPSHOT;
            validated.put(handleValue, slot);
        }


        // Reverse invariant: a nonzero packed link is meaningful and must have
        // exactly the matching sidecar owner. Zero remains ambiguous because
        // it can represent either an unassociated default record or slot 0.
        for (ObjectRecordTable.RecordSnapshot record : snapshot.objectSnapshot().records()) {
            if (record == null || record.type() == ObjectRecordTable.RecordType.TYPE_7)
                continue;
            int packedSlot = LegacyObjectRecordLinkCodec.readRawField(
                    record.rawLegacyPayload());
            if (packedSlot == 0) continue;
            long handleValue = (((long) record.generation()) << 4) | record.slotId();
            Integer associatedSlot = validated.get(handleValue);
            if (associatedSlot == null || associatedSlot != packedSlot)
                return Status.INVALID_SNAPSHOT;
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
