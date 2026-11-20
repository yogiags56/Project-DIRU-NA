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

    @Test
    public void replacementTransfersAssociationToMetadataResolvedTarget() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        TileGroup sourceTile = tile(1, 1, 0);
        TileGroup targetTile = tile(8, 9, 2);
        CrossTableLifecycleCoordinator.AttachResult source = coordinator.attach(
                RecordType.TYPE_6, 1, 1, 0, sourceTile, 4,
                stateOne(1, 0, 1, 1));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, source.status());

        ObjectRecordTable.AllocationResult targetAllocation =
                coordinator.objects().allocateType7(8, 9, 2);
        assertEquals(ObjectRecordTable.Status.OK, targetAllocation.status());
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().link(targetTile, targetAllocation.handle()));
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().unlink(sourceTile));

        CrossTableLifecycleCoordinator.OperationResult replaced =
                coordinator.replaceFromTileMetadata(source.objectHandle(), targetTile);
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, replaced.status());
        assertEquals(-1, coordinator.associatedSlot(source.objectHandle()));
        assertEquals(4, coordinator.associatedSlot(targetAllocation.handle()));
        assertEquals(RecordType.TYPE_2,
                coordinator.objects().typeOf(targetAllocation.handle()).orElseThrow());
        assertEquals(4, coordinator.objects().readAuxiliaryLink(
                targetAllocation.handle()).auxiliarySlot());
        assertEquals(3, AuxiliaryEntryStateMachine.state(
                coordinator.markReady(targetAllocation.handle()).auxiliaryRaw()));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                coordinator.consume(targetAllocation.handle()).status());
    }

    @Test
    public void replacementRejectsTargetWithIndependentAssociation() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        TileGroup sourceTile = tile(1, 1, 0);
        TileGroup targetTile = tile(2, 2, 1);
        CrossTableLifecycleCoordinator.AttachResult source = coordinator.attach(
                RecordType.TYPE_1, 1, 1, 0, sourceTile, 1,
                stateOne(0, 0, 1, 1));
        CrossTableLifecycleCoordinator.AttachResult target = coordinator.attach(
                RecordType.TYPE_2, 2, 2, 1, targetTile, 2,
                stateOne(0, 0, 2, 2));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, source.status());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, target.status());
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().unlink(sourceTile));

        CrossTableLifecycleCoordinator.OperationResult result =
                coordinator.replaceFromTileMetadata(source.objectHandle(), targetTile);
        assertEquals(CrossTableLifecycleCoordinator.Status.TARGET_ALREADY_ASSOCIATED,
                result.status());
        assertEquals(1, coordinator.associatedSlot(source.objectHandle()));
        assertEquals(2, coordinator.associatedSlot(target.objectHandle()));
        assertEquals(RecordType.TYPE_1,
                coordinator.objects().typeOf(source.objectHandle()).orElseThrow());
    }

    @Test
    public void replacementPreservesAuxiliaryPayloadInStateOneAndStateThree() {
        for (boolean ready : new boolean[]{false, true}) {
            CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
            TileGroup sourceTile = tile(1, 2, 0);
            TileGroup targetTile = tile(5, 6, 1);
            CrossTableLifecycleCoordinator.AttachResult source = coordinator.attach(
                    RecordType.TYPE_6, 1, 2, 0, sourceTile, 3,
                    stateOne(2, 0, 11, 13));
            ObjectRecordTable.AllocationResult target =
                    coordinator.objects().allocateType7(5, 6, 1);
            assertEquals(ObjectRecordTable.Status.OK,
                    coordinator.objects().link(targetTile, target.handle()));
            assertEquals(ObjectRecordTable.Status.OK,
                    coordinator.objects().unlink(sourceTile));
            if (ready) {
                assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                        coordinator.markReady(source.objectHandle()).status());
            }
            long auxiliaryBefore = coordinator.objects().auxiliaryEntryRaw(3);

            assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                    coordinator.replaceFromTileMetadata(source.objectHandle(), targetTile).status());
            assertEquals(auxiliaryBefore, coordinator.objects().auxiliaryEntryRaw(3));
            assertEquals(3, coordinator.associatedSlot(target.handle()));
            assertEquals(ready ? AuxiliaryEntryStateMachine.STATE_READY
                            : AuxiliaryEntryStateMachine.STATE_ALLOCATED,
                    AuxiliaryEntryStateMachine.state(
                            coordinator.objects().auxiliaryEntryRaw(3)));
        }
    }

    @Test
    public void replacementWithRemainingSourceReferenceRollsBackAllCoordinatorState() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        TileGroup sourceTile = tile(3, 3, 0);
        TileGroup sourceSecondTile = tile(3, 4, 0);
        TileGroup targetTile = tile(7, 7, 1);
        CrossTableLifecycleCoordinator.AttachResult source = coordinator.attach(
                RecordType.TYPE_6, 3, 3, 0, sourceTile, 5,
                stateOne(2, 0, 3, 3));
        ObjectRecordTable.AllocationResult target =
                coordinator.objects().allocateType7(7, 7, 1);
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().link(targetTile, target.handle()));
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().link(sourceSecondTile, source.objectHandle()));
        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().unlink(sourceTile));
        long targetBefore = coordinator.objects().rawLegacyPayload(target.handle());
        long auxiliaryBefore = coordinator.objects().auxiliaryEntryRaw(5);

        CrossTableLifecycleCoordinator.OperationResult result =
                coordinator.replaceFromTileMetadata(source.objectHandle(), targetTile);
        assertEquals(CrossTableLifecycleCoordinator.Status.TRANSITION_REJECTED,
                result.status());
        assertEquals(5, coordinator.associatedSlot(source.objectHandle()));
        assertEquals(-1, coordinator.associatedSlot(target.handle()));
        assertEquals(targetBefore, coordinator.objects().rawLegacyPayload(target.handle()));
        assertEquals(auxiliaryBefore, coordinator.objects().auxiliaryEntryRaw(5));
        assertEquals(1, coordinator.objects().referenceCount(source.objectHandle()));
        assertEquals(RecordType.TYPE_6,
                coordinator.objects().typeOf(source.objectHandle()).orElseThrow());
    }

    @Test
    public void replacementRejectsMissingTargetAndSourcePackedLinkMismatch() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        TileGroup sourceTile = tile(1, 2, 0);
        TileGroup missingTarget = tile(9, 9, 2);
        CrossTableLifecycleCoordinator.AttachResult source = coordinator.attach(
                RecordType.TYPE_1, 1, 2, 0, sourceTile, 3,
                stateOne(0, 0, 1, 2));
        assertEquals(CrossTableLifecycleCoordinator.Status.TRANSITION_REJECTED,
                coordinator.replaceFromTileMetadata(source.objectHandle(), missingTarget).status());
        assertEquals(3, coordinator.associatedSlot(source.objectHandle()));

        assertEquals(ObjectRecordTable.Status.OK,
                coordinator.objects().writeAuxiliaryLink(source.objectHandle(), 4));
        assertEquals(CrossTableLifecycleCoordinator.Status.SOURCE_ASSOCIATION_MISMATCH,
                coordinator.replaceFromTileMetadata(source.objectHandle(), missingTarget).status());
        assertEquals(3, coordinator.associatedSlot(source.objectHandle()));
    }

    @Test
    public void replacementAssociationSurvivesCoordinatorCheckpointRestore() {
        CrossTableLifecycleCoordinator sourceCoordinator = new CrossTableLifecycleCoordinator();
        TileGroup sourceTile = tile(4, 4, 0);
        TileGroup targetTile = tile(6, 6, 1);
        CrossTableLifecycleCoordinator.AttachResult source = sourceCoordinator.attach(
                RecordType.TYPE_6, 4, 4, 0, sourceTile, 6,
                stateOne(1, 0, 4, 4));
        ObjectRecordTable.AllocationResult target =
                sourceCoordinator.objects().allocateType7(6, 6, 1);
        assertEquals(ObjectRecordTable.Status.OK,
                sourceCoordinator.objects().link(targetTile, target.handle()));
        assertEquals(ObjectRecordTable.Status.OK,
                sourceCoordinator.objects().unlink(sourceTile));
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                sourceCoordinator.replaceFromTileMetadata(source.objectHandle(), targetTile).status());

        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.restore(sourceCoordinator.snapshot()));
        assertEquals(6, restored.associatedSlot(target.handle()));
        assertEquals(RecordType.TYPE_2, restored.objects().typeOf(target.handle()).orElseThrow());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.markReady(target.handle()).status());
        assertEquals(CrossTableLifecycleCoordinator.Status.OK,
                restored.consume(target.handle()).status());
    }

    @Test
    public void restoreRejectsNonzeroPackedLinkWithoutSidecarAssociation() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_1, 2, 3, 0, tile(2, 3, 0), 2,
                stateOne(0, 0, 2, 3));
        CrossTableLifecycleCoordinator.Snapshot valid = coordinator.snapshot();
        java.util.Map<Long, Integer> missing = new java.util.HashMap<>();
        CrossTableLifecycleCoordinator.Snapshot corrupted =
                new CrossTableLifecycleCoordinator.Snapshot(valid.objectSnapshot(), missing);

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                coordinator.restore(corrupted));
        assertEquals(2, coordinator.associatedSlot(attached.objectHandle()));
    }

    @Test
    public void restoreRejectsDuplicatePackedLinkOwnershipEvenWithUniqueSidecarSlots() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult first = coordinator.attach(
                RecordType.TYPE_1, 1, 1, 0, tile(1, 1, 0), 2,
                stateOne(0, 0, 1, 1));
        CrossTableLifecycleCoordinator.AttachResult second = coordinator.attach(
                RecordType.TYPE_2, 2, 2, 1, tile(2, 2, 1), 3,
                stateOne(0, 0, 2, 2));
        CrossTableLifecycleCoordinator.Snapshot valid = coordinator.snapshot();
        java.util.Map<Long, Integer> swapped = new java.util.HashMap<>();
        swapped.put(first.objectHandle().value(), 3);
        swapped.put(second.objectHandle().value(), 2);
        CrossTableLifecycleCoordinator.Snapshot corrupted =
                new CrossTableLifecycleCoordinator.Snapshot(valid.objectSnapshot(), swapped);

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                coordinator.restore(corrupted));
        assertEquals(2, coordinator.associatedSlot(first.objectHandle()));
        assertEquals(3, coordinator.associatedSlot(second.objectHandle()));
    }

    @Test
    public void restoreAllowsUnassociatedDefaultZeroLinkRecord() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        ObjectRecordTable.AllocationResult plain = coordinator.objects().allocate(
                RecordType.TYPE_1, 5, 6, 0);
        assertEquals(ObjectRecordTable.Status.OK, plain.status());
        CrossTableLifecycleCoordinator.Snapshot checkpoint = coordinator.snapshot();

        CrossTableLifecycleCoordinator restored = new CrossTableLifecycleCoordinator();
        assertEquals(CrossTableLifecycleCoordinator.Status.OK, restored.restore(checkpoint));
        assertEquals(-1, restored.associatedSlot(plain.handle()));
    }

    @Test
    public void restoreStillRejectsAssociationToEmptyAuxiliaryEntry() {
        CrossTableLifecycleCoordinator coordinator = new CrossTableLifecycleCoordinator();
        CrossTableLifecycleCoordinator.AttachResult attached = coordinator.attach(
                RecordType.TYPE_2, 7, 8, 0, tile(7, 8, 0), 4,
                stateOne(0, 0, 7, 8));
        CrossTableLifecycleCoordinator.Snapshot valid = coordinator.snapshot();
        long[] entries = valid.objectSnapshot().auxiliaryEntries();
        entries[4] = 0L;
        ObjectRecordTable.Snapshot objects = new ObjectRecordTable.Snapshot(
                valid.objectSnapshot().schemaVersion(), valid.objectSnapshot().records(),
                valid.objectSnapshot().generations(), valid.objectSnapshot().tileLinks(), entries);
        CrossTableLifecycleCoordinator.Snapshot corrupted =
                new CrossTableLifecycleCoordinator.Snapshot(objects, valid.associations());

        assertEquals(CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT,
                coordinator.restore(corrupted));
        assertEquals(4, coordinator.associatedSlot(attached.objectHandle()));
    }
}
