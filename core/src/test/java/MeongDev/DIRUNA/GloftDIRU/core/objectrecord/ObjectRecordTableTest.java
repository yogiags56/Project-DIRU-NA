package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.ObjectRecordTable.*;
import static org.junit.Assert.*;

public final class ObjectRecordTableTest {
    private static TileGroup tile(int x, int y, int group) {
        return new TileGroup(x, y, group);
    }

    @Test
    public void allocatesFifteenIndependentObjectSlots() {
        ObjectRecordTable table = new ObjectRecordTable();
        for (int i = 1; i <= 15; i++) {
            AllocationResult result = table.allocate(RecordType.TYPE_5, i, 0, 0);
            assertEquals(Status.OK, result.status());
            assertEquals(i, result.slotId());
        }
        assertEquals(Status.NO_CAPACITY,
                table.allocate(RecordType.TYPE_5, 0, 0, 0).status());
    }

    @Test
    public void tileMetadataCanLinkOnlyRepresentableSlots() {
        ObjectRecordTable table = new ObjectRecordTable();
        AllocationResult lastRepresentable = null;
        for (int i = 1; i <= 7; i++) {
            lastRepresentable = table.allocate(RecordType.TYPE_5, i, 0, 0);
        }
        assertEquals(Status.OK, table.link(tile(10, 10, 0),
                lastRepresentable.handle()));

        AllocationResult slotEight = table.allocate(RecordType.TYPE_5, 8, 0, 0);
        assertEquals(8, slotEight.slotId());
        assertEquals(Status.UNREPRESENTABLE_TILE_ID,
                table.link(tile(11, 10, 0), slotEight.handle()));
        assertEquals(0, table.referenceCount(slotEight.handle()));
        assertEquals(Status.NO_LINK, table.lookup(tile(11, 10, 0)).status());
    }

    @Test
    public void zeroOrMissingTileIdIsNoLink() {
        ObjectRecordTable table = new ObjectRecordTable();
        assertEquals(Status.NO_LINK, table.lookup(tile(0, 0, 0)).status());
        assertEquals(Status.NO_LINK, table.unlink(tile(0, 0, 0)));
    }

    @Test
    public void multipleTileGroupsCanShareOneRecord() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_2, 4, 5, 1).handle();
        TileGroup first = tile(4, 5, 1);
        TileGroup second = tile(5, 5, 1);

        assertEquals(Status.OK, table.link(first, handle));
        assertEquals(Status.OK, table.link(second, handle));
        assertEquals(2, table.referenceCount(handle));
        assertEquals(Status.OK, table.unlink(first));
        assertEquals(1, table.referenceCount(handle));
        assertEquals(Status.OBJECT_STILL_REFERENCED, table.release(handle));
        assertEquals(Status.OK, table.lookup(second).status());
    }

    @Test
    public void releaseInvalidatesOldGenerationHandle() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle oldHandle = table.allocate(RecordType.TYPE_3, 0, 0, 0).handle();
        assertEquals(Status.OK, table.release(oldHandle));

        ObjectHandle newHandle = table.allocate(RecordType.TYPE_4, 0, 0, 0).handle();
        assertNotEquals(oldHandle, newHandle);
        assertEquals(Status.STALE_HANDLE, table.release(oldHandle));
        assertTrue(table.typeOf(newHandle).isPresent());
    }

    @Test
    public void reclassificationPreservesHandleAndReferences() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_2, 1, 1, 0).handle();
        TileGroup link = tile(1, 1, 0);
        assertEquals(Status.OK, table.link(link, handle));

        assertEquals(Status.OK, table.reclassify(handle, RecordType.TYPE_5));
        assertEquals(RecordType.TYPE_5, table.typeOf(handle).orElseThrow());
        assertEquals(1, table.referenceCount(handle));
        assertEquals(handle, table.lookup(link).handle());
    }

    @Test
    public void anchorValidationIsNotAppliedToEverySharedTileReference() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_5, 2, 3, 0).handle();
        assertEquals(Status.OK, table.link(tile(2, 3, 0), handle));
        assertEquals(Status.OK, table.link(tile(3, 3, 0), handle));

        assertTrue(table.matchesAnchor(handle, 2, 3, 0));
        assertFalse(table.matchesAnchor(handle, 3, 3, 0));
        assertEquals(Status.OK, table.lookup(tile(3, 3, 0)).status());
    }

    @Test
    public void duplicateLinkDoesNotDoubleCountAndOccupiedGroupCannotBeReplaced() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle first = table.allocate(RecordType.TYPE_5, 0, 0, 0).handle();
        ObjectHandle second = table.allocate(RecordType.TYPE_4, 1, 0, 0).handle();
        TileGroup tile = tile(0, 0, 0);

        assertEquals(Status.OK, table.link(tile, first));
        assertEquals(Status.ALREADY_LINKED, table.link(tile, first));
        assertEquals(Status.TILE_GROUP_OCCUPIED, table.link(tile, second));
        assertEquals(1, table.referenceCount(first));
        assertEquals(0, table.referenceCount(second));
    }

    @Test
    public void releaseSucceedsOnlyAfterAllLinksAreRemoved() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_1, 0, 0, 0).handle();
        TileGroup a = tile(0, 0, 0);
        TileGroup b = tile(1, 0, 0);
        assertEquals(Status.OK, table.link(a, handle));
        assertEquals(Status.OK, table.link(b, handle));

        assertEquals(Status.OK, table.unlink(a));
        assertEquals(Status.OBJECT_STILL_REFERENCED, table.release(handle));
        assertEquals(Status.OK, table.unlink(b));
        assertEquals(Status.OK, table.release(handle));
        assertEquals(Status.STALE_HANDLE, table.release(handle));
    }

    @Test
    public void checkpointRoundTripRestoresRecordsLinksAndReferenceOwnership() {
        ObjectRecordTable source = new ObjectRecordTable();
        ObjectHandle shared = source.allocate(RecordType.TYPE_5, 2, 3, 0).handle();
        ObjectHandle other = source.allocate(RecordType.TYPE_2, 8, 9, 2).handle();
        TileGroup first = tile(2, 3, 0);
        TileGroup second = tile(3, 3, 0);
        TileGroup third = tile(8, 9, 2);
        assertEquals(Status.OK, source.link(first, shared));
        assertEquals(Status.OK, source.link(second, shared));
        assertEquals(Status.OK, source.link(third, other));

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(source.snapshot()));
        assertEquals(2, restored.referenceCount(shared));
        assertEquals(1, restored.referenceCount(other));
        assertEquals(RecordType.TYPE_5, restored.lookup(second).type());
        assertEquals(RecordType.TYPE_2, restored.lookup(third).type());
        assertEquals(Status.OBJECT_STILL_REFERENCED, restored.release(shared));
    }

    @Test
    public void restoreRebuildsReverseReferencesAfterLinksWereRemoved() {
        ObjectRecordTable source = new ObjectRecordTable();
        ObjectHandle handle = source.allocate(RecordType.TYPE_5, 0, 0, 0).handle();
        TileGroup a = tile(0, 0, 0);
        TileGroup b = tile(1, 0, 0);
        assertEquals(Status.OK, source.link(a, handle));
        assertEquals(Status.OK, source.link(b, handle));
        assertEquals(Status.OK, source.unlink(a));

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(source.snapshot()));
        assertEquals(1, restored.referenceCount(handle));
        assertEquals(Status.OBJECT_STILL_REFERENCED, restored.release(handle));
        assertEquals(Status.OK, restored.unlink(b));
        assertEquals(Status.OK, restored.release(handle));
    }

    @Test
    public void invalidSnapshotDoesNotPartiallyReplaceLiveState() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle live = table.allocate(RecordType.TYPE_4, 4, 4, 1).handle();
        TileGroup liveTile = tile(4, 4, 1);
        assertEquals(Status.OK, table.link(liveTile, live));

        ObjectRecordTable.Snapshot valid = table.snapshot();
        java.util.List<ObjectRecordTable.RecordSnapshot> badRecords =
                new java.util.ArrayList<>(valid.records());
        ObjectRecordTable.RecordSnapshot original = badRecords.get(0);
        badRecords.set(0, new ObjectRecordTable.RecordSnapshot(
                original.slotId(), original.generation() + 1, original.type(),
                original.anchorX(), original.anchorY(), original.anchorGroup()));
        ObjectRecordTable.Snapshot invalid = new ObjectRecordTable.Snapshot(
                valid.schemaVersion(), badRecords, valid.generations(), valid.tileLinks());

        assertEquals(Status.INVALID_SNAPSHOT, table.restore(invalid));
        assertEquals(RecordType.TYPE_4, table.lookup(liveTile).type());
        assertEquals(1, table.referenceCount(live));
    }

    @Test
    public void restoredGenerationRejectsHandleFromReleasedCheckpointSlot() {
        ObjectRecordTable source = new ObjectRecordTable();
        ObjectHandle oldHandle = source.allocate(RecordType.TYPE_1, 0, 0, 0).handle();
        assertEquals(Status.OK, source.release(oldHandle));
        ObjectRecordTable.Snapshot afterRelease = source.snapshot();

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(afterRelease));
        ObjectHandle newHandle = restored.allocate(RecordType.TYPE_2, 1, 1, 1).handle();
        assertNotEquals(oldHandle, newHandle);
        assertEquals(Status.STALE_HANDLE, restored.release(oldHandle));
        assertEquals(RecordType.TYPE_2, restored.typeOf(newHandle).orElseThrow());
    }

    @Test
    public void corruptedTileLinkToMissingOrUnrepresentableSlotIsRejectedAtomically() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle live = table.allocate(RecordType.TYPE_3, 0, 0, 0).handle();
        TileGroup tile = tile(0, 0, 0);
        assertEquals(Status.OK, table.link(tile, live));
        ObjectRecordTable.Snapshot valid = table.snapshot();

        java.util.Map<TileGroup, Integer> corruptedLinks =
                new java.util.HashMap<>(valid.tileLinks());
        corruptedLinks.put(tile(9, 9, 0), 8);
        ObjectRecordTable.Snapshot corrupted = new ObjectRecordTable.Snapshot(
                valid.schemaVersion(), valid.records(), valid.generations(), corruptedLinks);

        assertEquals(Status.INVALID_SNAPSHOT, table.restore(corrupted));
        assertEquals(Status.OK, table.lookup(tile).status());
        assertEquals(1, table.referenceCount(live));
    }


    @Test
    public void checkpointRoundTripPreservesOpaqueAuxiliaryEntriesBitForBit() {
        ObjectRecordTable source = new ObjectRecordTable();
        long[] values = {
                0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE,
                0x0123456789ABCDEFL, 0xFEDCBA9876543210L,
                0x0000000F00000003L, 0x7F00000000000001L,
                0x8000000000000001L, 42L, 99L, 123456789L,
                0x5555555555555555L, 0xAAAAAAAAAAAAAAAAL
        };
        for (int i = 0; i < values.length; i++) source.setAuxiliaryEntryRaw(i, values[i]);

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(source.snapshot()));
        for (int i = 0; i < values.length; i++)
            assertEquals(values[i], restored.auxiliaryEntryRaw(i));
    }

    @Test
    public void auxiliaryEntryAccessIsBoundedAndSnapshotArraysAreDefensiveCopies() {
        ObjectRecordTable table = new ObjectRecordTable();
        table.setAuxiliaryEntryRaw(0, 77L);
        ObjectRecordTable.Snapshot snapshot = table.snapshot();

        long[] exported = snapshot.auxiliaryEntries();
        exported[0] = 999L;
        assertEquals(77L, snapshot.auxiliaryEntries()[0]);
        assertEquals(77L, table.auxiliaryEntryRaw(0));

        try {
            table.auxiliaryEntryRaw(15);
            fail("index 15 must be rejected");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
        try {
            table.setAuxiliaryEntryRaw(-1, 1L);
            fail("negative index must be rejected");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void invalidAuxiliaryArrayLengthRejectsRestoreWithoutChangingLiveState() {
        ObjectRecordTable table = new ObjectRecordTable();
        table.setAuxiliaryEntryRaw(2, 314L);
        ObjectRecordTable.Snapshot valid = table.snapshot();
        ObjectRecordTable.Snapshot invalid = new ObjectRecordTable.Snapshot(
                valid.schemaVersion(), valid.records(), valid.generations(),
                valid.tileLinks(), new long[14]);

        assertEquals(Status.INVALID_SNAPSHOT, table.restore(invalid));
        assertEquals(314L, table.auxiliaryEntryRaw(2));
    }


    @Test
    public void rawLegacyLinkIsStoredReadAndPreservedAcrossReclassification() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocate(RecordType.TYPE_2, 4, 5, 0).handle();
        long before = table.rawLegacyPayload(handle);

        assertEquals(Status.OK, table.writeAuxiliaryLink(handle, 6));
        long linked = table.rawLegacyPayload(handle);
        assertEquals(6, table.readAuxiliaryLink(handle).auxiliarySlot());
        assertEquals(before & ~LegacyObjectRecordLinkCodec.LINK_MASK,
                linked & ~LegacyObjectRecordLinkCodec.LINK_MASK);

        assertEquals(Status.OK, table.reclassify(handle, RecordType.TYPE_5));
        assertEquals(RecordType.TYPE_5, table.typeOf(handle).orElseThrow());
        assertEquals(6, table.readAuxiliaryLink(handle).auxiliarySlot());
        assertEquals(linked & ~ (0x7L << 28),
                table.rawLegacyPayload(handle) & ~(0x7L << 28));
    }

    @Test
    public void typeSevenRejectsLinkWritesAndReportsExplicitLinklessStatus() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocateType7(1, 2, 0).handle();
        long before = table.rawLegacyPayload(handle);
        assertEquals(Status.INVALID_ARGUMENT, table.writeAuxiliaryLink(handle, 0));
        assertEquals(before, table.rawLegacyPayload(handle));
        assertEquals(LegacyObjectRecordLinkCodec.Status.TYPE7_HAS_NO_LINK,
                table.readAuxiliaryLink(handle).status());
    }

    @Test
    public void rawLegacyPayloadSurvivesCheckpointRestoreAndStaleHandlesAreRejected() {
        ObjectRecordTable source = new ObjectRecordTable();
        ObjectHandle handle = source.allocate(RecordType.TYPE_2, 8, 9, 1).handle();
        assertEquals(Status.OK, source.writeAuxiliaryLink(handle, 4));
        long expected = source.rawLegacyPayload(handle);

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(source.snapshot()));
        assertEquals(expected, restored.rawLegacyPayload(handle));
        assertEquals(4, restored.readAuxiliaryLink(handle).auxiliarySlot());

        assertEquals(Status.OK, restored.release(handle));
        try {
            restored.rawLegacyPayload(handle);
            fail("stale handle must not expose raw payload");
        } catch (IllegalArgumentException expectedFailure) {
            // expected
        }
    }

    @Test
    public void invalidSnapshotTypeBitsDoNotReplaceLiveRawPayload() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle live = table.allocate(RecordType.TYPE_5, 2, 3, 0).handle();
        assertEquals(Status.OK, table.writeAuxiliaryLink(live, 3));
        long before = table.rawLegacyPayload(live);
        ObjectRecordTable.Snapshot valid = table.snapshot();
        java.util.List<ObjectRecordTable.RecordSnapshot> records =
                new java.util.ArrayList<>(valid.records());
        ObjectRecordTable.RecordSnapshot item = records.get(0);
        records.set(0, new ObjectRecordTable.RecordSnapshot(item.slotId(), item.generation(),
                RecordType.TYPE_5, item.anchorX(), item.anchorY(), item.anchorGroup(),
                item.rawLegacyPayload() ^ (1L << 28)));
        ObjectRecordTable.Snapshot corrupted = new ObjectRecordTable.Snapshot(
                valid.schemaVersion(), records, valid.generations(), valid.tileLinks(),
                valid.auxiliaryEntries());
        assertEquals(Status.INVALID_SNAPSHOT, table.restore(corrupted));
        assertEquals(before, table.rawLegacyPayload(live));
        assertEquals(3, table.readAuxiliaryLink(live).auxiliarySlot());
    }

    @Test
    public void replacementTransfersPackedLinkThenReleasesOldRecord() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_6, 1, 1, 0).handle();
        ObjectHandle target = table.allocate(RecordType.TYPE_1, 2, 2, 0).handle();
        TileGroup targetTile = tile(2, 2, 0);
        assertEquals(Status.OK, table.link(targetTile, target));
        assertEquals(Status.OK, table.writeAuxiliaryLink(source, 7));

        assertEquals(Status.OK,
                table.replaceAndTransferLink(source, target, RecordType.TYPE_2));
        assertEquals(Status.STALE_HANDLE, table.release(source));
        assertEquals(RecordType.TYPE_2, table.typeOf(target).orElseThrow());
        assertEquals(7, table.readAuxiliaryLink(target).auxiliarySlot());
        assertEquals(Status.OK, table.lookup(targetTile).status());
        assertEquals(1, table.referenceCount(target));
    }

    @Test
    public void failedReplacementRollsBackTargetAndKeepsSourceWhenStillReferenced() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_1, 1, 1, 0).handle();
        ObjectHandle target = table.allocate(RecordType.TYPE_6, 2, 2, 0).handle();
        assertEquals(Status.OK, table.writeAuxiliaryLink(source, 4));
        assertEquals(Status.OK, table.link(tile(1, 1, 0), source));
        long targetBefore = table.rawLegacyPayload(target);

        assertEquals(Status.OBJECT_STILL_REFERENCED,
                table.replaceAndTransferLink(source, target, RecordType.TYPE_2));
        assertEquals(RecordType.TYPE_6, table.typeOf(target).orElseThrow());
        assertEquals(targetBefore, table.rawLegacyPayload(target));
        assertEquals(4, table.readAuxiliaryLink(source).auxiliarySlot());
        assertEquals(1, table.referenceCount(source));
    }

    @Test
    public void replacementRejectsTypeSevenAndSelfReplacement() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_2, 1, 1, 0).handle();
        ObjectHandle target = table.allocate(RecordType.TYPE_1, 2, 2, 0).handle();
        ObjectHandle typeSeven = table.allocateType7(3, 3, 0).handle();

        assertEquals(Status.INVALID_TRANSITION,
                table.replaceAndTransferLink(source, target, RecordType.TYPE_7));
        assertEquals(Status.INVALID_TRANSITION,
                table.replaceAndTransferLink(source, source, RecordType.TYPE_2));
        assertEquals(Status.INVALID_TRANSITION,
                table.replaceAndTransferLink(typeSeven, target, RecordType.TYPE_2));
        assertEquals(RecordType.TYPE_7, table.typeOf(typeSeven).orElseThrow());
    }

    @Test
    public void typeSevenHasDedicatedAllocationAndCannotBeReclassified() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle handle = table.allocateType7(4, 5, 0).handle();
        assertEquals(RecordType.TYPE_7, table.typeOf(handle).orElseThrow());
        assertEquals(0L, table.rawLegacyPayload(handle)
                & LegacyObjectRecordLinkCodec.LINK_MASK);
        assertEquals(Status.INVALID_TRANSITION,
                table.reclassify(handle, RecordType.TYPE_2));
        assertEquals(Status.INVALID_TRANSITION,
                table.reclassify(table.allocate(RecordType.TYPE_2, 6, 7, 0).handle(),
                        RecordType.TYPE_7));
    }

    @Test
    public void replacementAndTransferSurviveCheckpointRestore() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_1, 1, 1, 0).handle();
        ObjectHandle target = table.allocate(RecordType.TYPE_6, 2, 2, 0).handle();
        assertEquals(Status.OK, table.writeAuxiliaryLink(source, 2));
        assertEquals(Status.OK,
                table.replaceAndTransferLink(source, target, RecordType.TYPE_2));

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(table.snapshot()));
        assertEquals(RecordType.TYPE_2, restored.typeOf(target).orElseThrow());
        assertEquals(2, restored.readAuxiliaryLink(target).auxiliarySlot());
        assertEquals(Status.STALE_HANDLE, restored.release(source));
    }

    @Test
    public void genericAllocatorCannotCreateTypeSeven() {
        ObjectRecordTable table = new ObjectRecordTable();
        assertEquals(Status.INVALID_TRANSITION,
                table.allocate(RecordType.TYPE_7, 1, 2, 0).status());
        assertEquals(RecordType.TYPE_7,
                table.typeOf(table.allocateType7(1, 2, 0).handle()).orElseThrow());
    }

    @Test
    public void restoreRejectsTypeSevenSnapshotWithNonzeroPackedLinkBits() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle live = table.allocateType7(1, 2, 0).handle();
        ObjectRecordTable.Snapshot valid = table.snapshot();
        ObjectRecordTable.RecordSnapshot item = valid.records().get(0);
        long corruptedPayload = item.rawLegacyPayload()
                | (3L << LegacyObjectRecordLinkCodec.LINK_BIT_OFFSET);
        java.util.List<ObjectRecordTable.RecordSnapshot> records =
                new java.util.ArrayList<>(valid.records());
        records.set(0, new ObjectRecordTable.RecordSnapshot(item.slotId(),
                item.generation(), item.type(), item.anchorX(), item.anchorY(),
                item.anchorGroup(), corruptedPayload));
        ObjectRecordTable.Snapshot corrupted = new ObjectRecordTable.Snapshot(
                valid.schemaVersion(), records, valid.generations(),
                valid.tileLinks(), valid.auxiliaryEntries());

        assertEquals(Status.INVALID_SNAPSHOT, table.restore(corrupted));
        assertEquals(0L, table.rawLegacyPayload(live)
                & LegacyObjectRecordLinkCodec.LINK_MASK);
        assertEquals(RecordType.TYPE_7, table.typeOf(live).orElseThrow());
    }

    @Test
    public void replacementMayOverwriteResolvedTypeSevenTargetAsTypeTwo() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_6, 1, 1, 0).handle();
        ObjectHandle target = table.allocateType7(2, 2, 0).handle();
        assertEquals(Status.OK, table.writeAuxiliaryLink(source, 5));

        assertEquals(Status.OK,
                table.replaceAndTransferLink(source, target, RecordType.TYPE_2));
        assertEquals(RecordType.TYPE_2, table.typeOf(target).orElseThrow());
        assertEquals(5, table.readAuxiliaryLink(target).auxiliarySlot());
        assertEquals(Status.STALE_HANDLE, table.release(source));
    }

    @Test
    public void replacementResolvesTargetFromExactTileGroupMetadata() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_6, 1, 1, 0).handle();
        ObjectHandle decoy = table.allocate(RecordType.TYPE_1, 5, 6, 0).handle();
        ObjectHandle target = table.allocateType7(5, 6, 1).handle();
        TileGroup groupZero = tile(5, 6, 0);
        TileGroup groupOne = tile(5, 6, 1);
        assertEquals(Status.OK, table.link(groupZero, decoy));
        assertEquals(Status.OK, table.link(groupOne, target));
        assertEquals(Status.OK, table.writeAuxiliaryLink(source, 6));

        assertEquals(Status.OK, table.replaceFromTileMetadata(source, groupOne));
        assertEquals(RecordType.TYPE_1, table.typeOf(decoy).orElseThrow());
        assertEquals(RecordType.TYPE_2, table.typeOf(target).orElseThrow());
        assertEquals(6, table.readAuxiliaryLink(target).auxiliarySlot());
        assertEquals(Status.OK, table.lookup(groupOne).status());
        assertEquals(target, table.lookup(groupOne).handle());
        assertEquals(Status.STALE_HANDLE, table.release(source));
    }

    @Test
    public void replacementMetadataLookupDoesNotFallBackToAnotherGroup() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_1, 2, 3, 0).handle();
        ObjectHandle otherGroup = table.allocate(RecordType.TYPE_2, 8, 9, 0).handle();
        assertEquals(Status.OK, table.link(tile(8, 9, 2), otherGroup));
        assertEquals(Status.NO_LINK,
                table.replaceFromTileMetadata(source, tile(8, 9, 1)));
        assertEquals(RecordType.TYPE_1, table.typeOf(source).orElseThrow());
        assertEquals(RecordType.TYPE_2, table.typeOf(otherGroup).orElseThrow());
    }

    @Test
    public void replacementFromMetadataRollsBackWhenSourceStillHasTileReferences() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_6, 1, 1, 0).handle();
        ObjectHandle target = table.allocate(RecordType.TYPE_1, 4, 4, 1).handle();
        TileGroup sourceTile = tile(1, 1, 0);
        TileGroup targetTile = tile(4, 4, 1);
        assertEquals(Status.OK, table.link(sourceTile, source));
        assertEquals(Status.OK, table.link(targetTile, target));
        assertEquals(Status.OK, table.writeAuxiliaryLink(source, 3));
        long targetBefore = table.rawLegacyPayload(target);

        assertEquals(Status.OBJECT_STILL_REFERENCED,
                table.replaceFromTileMetadata(source, targetTile));
        assertEquals(RecordType.TYPE_6, table.typeOf(source).orElseThrow());
        assertEquals(RecordType.TYPE_1, table.typeOf(target).orElseThrow());
        assertEquals(targetBefore, table.rawLegacyPayload(target));
        assertEquals(3, table.readAuxiliaryLink(source).auxiliarySlot());
        assertEquals(1, table.referenceCount(source));
        assertEquals(1, table.referenceCount(target));
    }

    @Test
    public void replacementMetadataRejectsSelfTargetAndStaleSource() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle source = table.allocate(RecordType.TYPE_1, 3, 3, 0).handle();
        TileGroup tile = tile(3, 3, 0);
        assertEquals(Status.OK, table.link(tile, source));
        assertEquals(Status.INVALID_TRANSITION,
                table.replaceFromTileMetadata(source, tile));
        assertEquals(Status.OK, table.unlink(tile));
        assertEquals(Status.OK, table.release(source));
        assertEquals(Status.STALE_HANDLE,
                table.replaceFromTileMetadata(source, tile));
    }

    @Test
    public void guardedMetadataLookupResolvesMatchingNonzeroId() {
        ObjectRecordTable table = new ObjectRecordTable();
        AllocationResult allocated = table.allocate(RecordType.TYPE_5, 4, 5, 1);
        TileGroup tile = tile(4, 5, 1);
        assertEquals(Status.OK, table.link(tile, allocated.handle()));
        int raw = PackedTileMetadataCodec.writeObjectId(0, 1, allocated.slotId()).rawWord();

        LookupResult result = table.lookup(tile, raw);
        assertEquals(Status.OK, result.status());
        assertEquals(allocated.handle(), result.handle());
        assertEquals(RecordType.TYPE_5, result.type());
    }

    @Test
    public void guardedMetadataLookupTreatsZeroAsNoLinkWithoutIndexing() {
        ObjectRecordTable table = new ObjectRecordTable();
        TileGroup tile = tile(4, 5, 1);
        LookupResult result = table.lookup(tile, 0);
        assertEquals(Status.NO_LINK, result.status());
        assertNull(result.handle());
    }

    @Test
    public void guardedMetadataLookupRejectsMismatchBetweenRawWordAndLinkMap() {
        ObjectRecordTable table = new ObjectRecordTable();
        AllocationResult first = table.allocate(RecordType.TYPE_1, 1, 1, 0);
        AllocationResult second = table.allocate(RecordType.TYPE_2, 2, 2, 0);
        TileGroup tile = tile(1, 1, 0);
        assertEquals(Status.OK, table.link(tile, first.handle()));
        int mismatchedRaw = PackedTileMetadataCodec.writeObjectId(0, 0,
                second.slotId()).rawWord();

        assertEquals(Status.TILE_METADATA_MISMATCH,
                table.lookup(tile, mismatchedRaw).status());
        assertEquals(RecordType.TYPE_1, table.typeOf(first.handle()).orElseThrow());
        assertEquals(RecordType.TYPE_2, table.typeOf(second.handle()).orElseThrow());
    }

    @Test
    public void guardedMetadataLookupRejectsStaleLinkMapEntry() {
        ObjectRecordTable table = new ObjectRecordTable();
        AllocationResult allocated = table.allocate(RecordType.TYPE_1, 1, 1, 0);
        TileGroup tile = tile(1, 1, 0);
        assertEquals(Status.OK, table.link(tile, allocated.handle()));
        int raw = PackedTileMetadataCodec.writeObjectId(0, 0, allocated.slotId()).rawWord();
        assertEquals(Status.OK, table.unlink(tile));

        assertEquals(Status.TILE_METADATA_MISMATCH, table.lookup(tile, raw).status());
    }

    @Test
    public void packedMetadataRoundTripsAndClearsEachGroupIndependently() {
        ObjectRecordTable table = new ObjectRecordTable();
        int raw = 0x15555555;
        for (int group = 0; group < ObjectRecordTable.TILE_GROUP_COUNT; group++) {
            TileGroup tile = tile(20 + group, 30, group);
            AllocationResult allocated =
                    table.allocate(RecordType.TYPE_5, 20 + group, 30, group);
            assertEquals(Status.OK, table.link(tile, allocated.handle()));
            PackedTileMetadataCodec.WriteResult written =
                    PackedTileMetadataCodec.writeObjectId(raw, group, allocated.slotId());
            assertEquals(PackedTileMetadataCodec.Status.OK, written.status());
            raw = written.rawWord();

            LookupResult found = table.lookup(tile, raw);
            assertEquals(Status.OK, found.status());
            assertEquals(allocated.handle(), found.handle());
        }

        for (int group = 0; group < ObjectRecordTable.TILE_GROUP_COUNT; group++) {
            TileGroup tile = tile(20 + group, 30, group);
            int beforeClear = raw;
            PackedTileMetadataCodec.WriteResult cleared =
                    PackedTileMetadataCodec.clearGroup(raw, group);
            assertEquals(PackedTileMetadataCodec.Status.OK, cleared.status());
            raw = cleared.rawWord();
            assertEquals(Status.NO_LINK, table.lookup(tile, raw).status());
            assertEquals(Status.OK, table.unlink(tile));
            assertEquals(0, PackedTileMetadataCodec.readField(
                    raw, group, 0, 9).value());
            for (int other = 0; other < ObjectRecordTable.TILE_GROUP_COUNT; other++) {
                if (other == group) continue;
                assertEquals(PackedTileMetadataCodec.readField(
                                beforeClear, other, 0, 9).value(),
                        PackedTileMetadataCodec.readField(raw, other, 0, 9).value());
            }
        }
    }

    @Test
    public void packedReferenceAfterUnlinkIsRejectedBeforeSlotReuse() {
        ObjectRecordTable table = new ObjectRecordTable();
        TileGroup tile = tile(7, 8, 1);
        AllocationResult allocated = table.allocate(RecordType.TYPE_2, 7, 8, 1);
        assertEquals(Status.OK, table.link(tile, allocated.handle()));
        int oldWord = PackedTileMetadataCodec.writeObjectId(0, 1,
                allocated.slotId()).rawWord();
        assertEquals(Status.OK, table.lookup(tile, oldWord).status());

        assertEquals(Status.OK, table.unlink(tile));
        assertEquals(Status.OK, table.release(allocated.handle()));
        assertEquals(Status.TILE_METADATA_MISMATCH,
                table.lookup(tile, oldWord).status());
    }

    @Test
    public void legacyPackedIdCannotDistinguishSameSlotAfterReuse() {
        ObjectRecordTable table = new ObjectRecordTable();
        TileGroup tile = tile(9, 10, 0);
        AllocationResult first = table.allocate(RecordType.TYPE_1, 9, 10, 0);
        assertEquals(Status.OK, table.link(tile, first.handle()));
        int oldWord = PackedTileMetadataCodec.writeObjectId(0, 0,
                first.slotId()).rawWord();
        assertEquals(Status.OK, table.unlink(tile));
        assertEquals(Status.OK, table.release(first.handle()));

        AllocationResult replacement = table.allocate(RecordType.TYPE_6, 9, 10, 0);
        assertEquals(first.slotId(), replacement.slotId());
        assertNotEquals(first.handle(), replacement.handle());
        assertEquals(Status.OK, table.link(tile, replacement.handle()));

        // The legacy packed field carries only the slot ID, not its generation.
        // Once the same slot is reused and relinked, old bits are indistinguishable.
        LookupResult ambiguous = table.lookup(tile, oldWord);
        assertEquals(Status.OK, ambiguous.status());
        assertEquals(replacement.handle(), ambiguous.handle());
        assertEquals(RecordType.TYPE_6, ambiguous.type());
    }

    @Test
    public void packedMetadataLinksSurviveSnapshotRestoreWithOneClearedGroup() {
        ObjectRecordTable source = new ObjectRecordTable();
        int raw = 0;
        TileGroup first = tile(31, 40, 0);
        TileGroup second = tile(32, 40, 1);
        TileGroup third = tile(33, 40, 2);
        TileGroup[] tiles = {first, second, third};
        ObjectHandle[] handles = new ObjectHandle[3];

        for (int group = 0; group < 3; group++) {
            AllocationResult allocated = source.allocate(
                    RecordType.TYPE_5, 31 + group, 40, group);
            handles[group] = allocated.handle();
            assertEquals(Status.OK, source.link(tiles[group], handles[group]));
            raw = PackedTileMetadataCodec.writeObjectId(raw, group,
                    allocated.slotId()).rawWord();
        }
        raw = PackedTileMetadataCodec.clearGroup(raw, 1).rawWord();
        assertEquals(Status.OK, source.unlink(second));

        ObjectRecordTable restored = new ObjectRecordTable();
        assertEquals(Status.OK, restored.restore(source.snapshot()));
        assertEquals(Status.NO_LINK,
                restored.lookup(second, raw).status());
        assertEquals(Status.OK, restored.lookup(first, raw).status());
        assertEquals(Status.OK, restored.lookup(third, raw).status());
        assertEquals(0, restored.referenceCount(handles[1]));
        assertEquals(1, restored.referenceCount(handles[0]));
        assertEquals(1, restored.referenceCount(handles[2]));
    }

    @Test
    public void snapshotRestorePreservesSlotReuseGenerationAndOldHandleStaysStale() {
        ObjectRecordTable source = new ObjectRecordTable();
        ObjectHandle oldHandle = source.allocate(RecordType.TYPE_1, 1, 1, 0).handle();
        assertEquals(Status.OK, source.release(oldHandle));
        ObjectHandle replacement = source.allocate(RecordType.TYPE_6, 2, 2, 1).handle();
        assertNotEquals(oldHandle, replacement);
        ObjectRecordTable restored = new ObjectRecordTable();

        assertEquals(Status.OK, restored.restore(source.snapshot()));
        assertEquals(Status.STALE_HANDLE, restored.release(oldHandle));
        assertEquals(RecordType.TYPE_6, restored.typeOf(replacement).orElseThrow());
    }

    @Test
    public void missingRecordTileLinkSnapshotRestoreIsAtomic() {
        ObjectRecordTable table = new ObjectRecordTable();
        ObjectHandle live = table.allocate(RecordType.TYPE_4, 4, 5, 0).handle();
        TileGroup liveTile = tile(4, 5, 0);
        assertEquals(Status.OK, table.link(liveTile, live));
        Snapshot valid = table.snapshot();
        java.util.Map<TileGroup, Integer> brokenLinks =
                new java.util.HashMap<>(valid.tileLinks());
        brokenLinks.put(tile(6, 7, 1), 7);
        Snapshot invalid = new Snapshot(valid.schemaVersion(), valid.records(),
                valid.generations(), brokenLinks, valid.auxiliaryEntries());

        assertEquals(Status.INVALID_SNAPSHOT, table.restore(invalid));
        assertEquals(Status.OK, table.lookup(liveTile).status());
        assertEquals(1, table.referenceCount(live));
    }
}
