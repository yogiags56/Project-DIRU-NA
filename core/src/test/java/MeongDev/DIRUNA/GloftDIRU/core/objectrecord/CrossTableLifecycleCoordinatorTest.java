package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static org.junit.Assert.*;
import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.ObjectRecordTable.*;

public final class CrossTableLifecycleCoordinatorTest {
    private static long stateOne(int direction, int movement, int x, int y) {
        return ((long) y << 20) | ((long) x << 13) | ((long) movement << 6)
                | ((long) direction << 4) | 1L;
    }

    private static TileGroup tile(int x, int y, int group) {
        return new TileGroup(x, y, group);
    }

    @Test
    public void attachThenMarkReadyThenConsumeRunsAsOneLifecycle() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_2, 4, 5, 0, tile(4, 5, 0), 3,
                stateOne(2, 0, 4, 5));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, attached.status());
        assertEquals(3, coordinator.associatedSlot(attached.objectHandle()));

        CrossTableLifecycleCoordinator.OperationResult ready =
                coordinator.markReady(attached.objectHandle());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, ready.status());
        assertEquals(3, AuxiliaryEntryStateMachine.state(ready.auxiliaryRaw()));

        CrossTableLifecycleCoordinator.OperationResult consumed =
                coordinator.consume(attached.objectHandle());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, consumed.status());
        assertEquals(0L, coordinator.objects().auxiliaryEntryRaw(3));
        assertEquals(-1, coordinator.associatedSlot(attached.objectHandle()));
    }

    @Test
    public void unrepresentableSlotIsRejectedBeforeObjectAllocation() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult result = coordinator.attach(
                RecordType.TYPE_2, 1, 1, 0, tile(1, 1, 0), 8,
                stateOne(0, 0, 1, 1));
        assertEquals(CrossTableLifecycleCoordinator.Status.UNREPRESENTABLE_LINK, result.status());
        assertEquals(ObjectRecordTable.Status.NO_LINK,
                coordinator.objects().lookup(tile(1, 1, 0)).status());
        assertEquals(0L, coordinator.objects().auxiliaryEntryRaw(0));
    }

    @Test
    public void occupiedTileRollsBackNewObjectAndAuxiliaryEntry() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult first = coordinator.attach(
                RecordType.TYPE_2, 1, 1, 0, tile(1, 1, 0), 0,
                stateOne(0, 0, 1, 1));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, first.status());
        int referencesBefore = coordinator.objects().referenceCount(first.objectHandle());

        CrossTableLifecycleCoordinator.AttachResult second = coordinator.attach(
                RecordType.TYPE_5, 2, 2, 0, tile(1, 1, 0), 1,
                stateOne(1, 0, 2, 2));
        assertEquals(CrossTableLifecycleCoordinator.Status.TILE_LINK_FAILED, second.status());
        assertEquals(referencesBefore,
                coordinator.objects().referenceCount(first.objectHandle()));
        assertEquals(0L, coordinator.objects().auxiliaryEntryRaw(1));
        assertEquals(1, coordinator.objects().referenceCount(first.objectHandle()));
    }

    @Test
    public void duplicateAuxiliarySlotCannotBeAssociatedTwice() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                coordinator.attach(RecordType.TYPE_2, 1, 1, 0, tile(1, 1, 0), 2,
                        stateOne(0, 0, 1, 1)).status());
        CrossTableLifecycleCoordinator.AttachResult duplicate = coordinator.attach(
                RecordType.TYPE_5, 2, 2, 0, tile(2, 2, 0), 2,
                stateOne(0, 0, 2, 2));
        assertEquals(CrossTableLifecycleCoordinator.Status.AUXILIARY_SLOT_OCCUPIED,
                duplicate.status());
        assertEquals(ObjectRecordTable.Status.NO_LINK,
                coordinator.objects().lookup(tile(2, 2, 0)).status());
    }

    @Test
    public void nonzeroMovementRejectsTransitionWithoutChangingAnyTable() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_2, 5, 6, 0, tile(5, 6, 0), 1,
                stateOne(1, 7, 5, 6));
        long before = coordinator.objects().auxiliaryEntryRaw(1);

        CrossTableLifecycleCoordinator.OperationResult result =
                coordinator.markReady(attached.objectHandle());
        assertEquals(CrossTableLifecycleCoordinator.Status.TRANSITION_REJECTED, result.status());
        assertEquals(before, coordinator.objects().auxiliaryEntryRaw(1));
        assertEquals(1, AuxiliaryEntryStateMachine.state(
                coordinator.objects().auxiliaryEntryRaw(1)));
    }

    @Test
    public void snapshotRestorePreservesInFlightAssociationAndCanContinueLifecycle() {
        CrossTableLifecycleCoordinator source = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = source.attach(
                RecordType.TYPE_5, 7, 8, 1, tile(7, 8, 1), 4,
                stateOne(3, 0, 7, 8));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, attached.status());
        CrossTableLifecycleCoordinator.Snapshot checkpoint = source.snapshot();

        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, restored.restore(checkpoint));
        assertEquals(4, restored.associatedSlot(attached.objectHandle()));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.markReady(attached.objectHandle()).status());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.consume(attached.objectHandle()).status());
    }

    @Test
    public void invalidAssociationSnapshotDoesNotReplaceLiveCoordinator() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult live = coordinator.attach(
                RecordType.TYPE_1, 1, 2, 0, tile(1, 2, 0), 0,
                stateOne(0, 0, 1, 2));
        CrossTableLifecycleCoordinator.Snapshot valid = coordinator.snapshot();
        java.util.Map<Long, Integer> bad = new java.util.HashMap<>(valid.associations());
        bad.put(live.objectHandle().value(), 8);
        CrossTableLifecycleCoordinator.Snapshot invalid =
                new CrossTableLifecycleCoordinator.Snapshot(valid.objectSnapshot(), bad);

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                coordinator.restore(invalid));
        assertEquals(0, coordinator.associatedSlot(live.objectHandle()));
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().lookup(tile(1, 2, 0)).status());
    }

    @Test
    public void staleObjectHandleCannotTransitionOrConsume() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_3, 0, 0, 0, tile(0, 0, 0), 1,
                stateOne(0, 0, 0, 0));
        ObjectHandle handle = attached.objectHandle();
        assertEquals(ObjectRecordTable.Status.OK, coordinator.objects().unlink(tile(0, 0, 0)));
        assertEquals(ObjectRecordTable.Status.OK, coordinator.objects().release(handle));

        assertEquals(CrossTableLifecycleCoordinator.Status.NO_ASSOCIATION,
                coordinator.markReady(handle).status());
        assertEquals(CrossTableLifecycleCoordinator.Status.NO_ASSOCIATION,
                coordinator.consume(handle).status());
    }

    @Test
    public void restoreRejectsAssociationToAuxiliaryEntryInUnclassifiedState() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult live = coordinator.attach(
                RecordType.TYPE_2, 3, 4, 0, tile(3, 4, 0), 1,
                stateOne(0, 0, 3, 4));
        CrossTableLifecycleCoordinator.Snapshot valid = coordinator.snapshot();
        long[] rawEntries = valid.objectSnapshot().auxiliaryEntries();
        rawEntries[1] = (rawEntries[1] & ~0xFL) | 2L;
        ObjectRecordTable.Snapshot corruptedObjects = new ObjectRecordTable.Snapshot(
                valid.objectSnapshot().schemaVersion(),
                valid.objectSnapshot().records(),
                valid.objectSnapshot().generations(),
                valid.objectSnapshot().tileLinks(), rawEntries);
        CrossTableLifecycleCoordinator.Snapshot corrupted =
                new CrossTableLifecycleCoordinator.Snapshot(
                        corruptedObjects, valid.associations());

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                coordinator.restore(corrupted));
        assertEquals(1, coordinator.associatedSlot(live.objectHandle()));
        assertEquals(1, AuxiliaryEntryStateMachine.state(
                coordinator.objects().auxiliaryEntryRaw(1)));
    }

    @Test
    public void coordinatorStoresPackedLinkAndUsesItAsAssociationGuard() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_5, 9, 10, 0, tile(9, 10, 0), 5,
                stateOne(1, 0, 9, 10));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, attached.status());
        assertEquals(5, coordinator.objects().readAuxiliaryLink(
                attached.objectHandle()).auxiliarySlot());
        assertEquals(5, coordinator.associatedSlot(attached.objectHandle()));

        long raw = coordinator.objects().rawLegacyPayload(attached.objectHandle());
        coordinator.objects().writeAuxiliaryLink(attached.objectHandle(), 4);
        assertEquals(CrossTableLifecycleCoordinator.Status.NO_ASSOCIATION,
                coordinator.markReady(attached.objectHandle()).status());
        assertEquals(raw & ~LegacyObjectRecordLinkCodec.LINK_MASK,
                coordinator.objects().rawLegacyPayload(attached.objectHandle())
                        & ~LegacyObjectRecordLinkCodec.LINK_MASK);
    }

    @Test
    public void coordinatorRestoreRejectsSidecarAndPackedLinkDisagreementAtomically() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_2, 11, 12, 0, tile(11, 12, 0), 2,
                stateOne(0, 0, 11, 12));
        CrossTableLifecycleCoordinator.Snapshot valid = coordinator.snapshot();
        java.util.List<ObjectRecordTable.RecordSnapshot> records =
                new java.util.ArrayList<>(valid.objectSnapshot().records());
        ObjectRecordTable.RecordSnapshot item = records.get(0);
        long changedLink = LegacyObjectRecordLinkCodec.write(item.rawLegacyPayload(),
                RecordType.TYPE_2, 3).rawRecord();
        records.set(0, new ObjectRecordTable.RecordSnapshot(item.slotId(), item.generation(),
                item.type(), item.anchorX(), item.anchorY(), item.anchorGroup(), changedLink));
        ObjectRecordTable.Snapshot mismatchedObjects = new ObjectRecordTable.Snapshot(
                valid.objectSnapshot().schemaVersion(), records,
                valid.objectSnapshot().generations(), valid.objectSnapshot().tileLinks(),
                valid.objectSnapshot().auxiliaryEntries());
        CrossTableLifecycleCoordinator.Snapshot corrupted =
                new CrossTableLifecycleCoordinator.Snapshot(
                        mismatchedObjects, valid.associations());

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                coordinator.restore(corrupted));
        assertEquals(2, coordinator.associatedSlot(attached.objectHandle()));
        assertEquals(2, coordinator.objects().readAuxiliaryLink(
                attached.objectHandle()).auxiliarySlot());
    }
}
