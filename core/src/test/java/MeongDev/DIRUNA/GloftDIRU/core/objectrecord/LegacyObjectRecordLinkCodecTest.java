package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static org.junit.Assert.*;
import static MeongDev.DIRUNA.GloftDIRU.core.objectrecord.LegacyObjectRecordLinkCodec.*;

public final class LegacyObjectRecordLinkCodecTest {
    @Test
    public void writesAndReadsRepresentableZeroBasedAuxiliarySlots() {
        for (int slot = 0; slot <= 7; slot++) {
            WriteResult write = write(0xA5A5000000000000L,
                    ObjectRecordTable.RecordType.TYPE_2, slot);
            assertEquals(Status.OK, write.status());
            assertEquals(slot, readRawField(write.rawRecord()));
            assertEquals(slot, read(write.rawRecord(),
                    ObjectRecordTable.RecordType.TYPE_2).auxiliarySlot());
        }
    }

    @Test
    public void writePreservesEveryBitOutsideTheThreeBitLinkField() {
        long raw = 0xFEDCBA9876543210L;
        WriteResult result = write(raw, ObjectRecordTable.RecordType.TYPE_5, 6);
        assertEquals(Status.OK, result.status());
        assertEquals(raw & ~LINK_MASK, result.rawRecord() & ~LINK_MASK);
    }

    @Test
    public void rejectsSlotsEightThroughFourteenWithoutChangingRawRecord() {
        long raw = 0x123456789ABCDEFL;
        for (int slot = 8; slot <= 14; slot++) {
            WriteResult result = write(raw, ObjectRecordTable.RecordType.TYPE_2, slot);
            assertEquals(Status.UNREPRESENTABLE_SLOT, result.status());
            assertEquals(raw, result.rawRecord());
        }
    }

    @Test
    public void rejectsInvalidSlotsAndNullTypeWithoutMutation() {
        long raw = 0x76543210FEDCBL;
        assertEquals(Status.INVALID_SLOT,
                write(raw, ObjectRecordTable.RecordType.TYPE_2, -1).status());
        assertEquals(Status.INVALID_SLOT,
                write(raw, ObjectRecordTable.RecordType.TYPE_2, 15).status());
        assertEquals(Status.INVALID_TYPE, write(raw, null, 1).status());
        assertEquals(raw, write(raw, null, 1).rawRecord());
    }

    @Test
    public void typeSevenIsExplicitlyLinklessAndPreservesRawPayload() {
        long raw = 0xCAFEBABE12345678L;
        WriteResult write = write(raw, ObjectRecordTable.RecordType.TYPE_7, 2);
        assertEquals(Status.TYPE7_HAS_NO_LINK, write.status());
        assertEquals(raw, write.rawRecord());
        assertEquals(Status.TYPE7_HAS_NO_LINK,
                read(raw, ObjectRecordTable.RecordType.TYPE_7).status());
    }

    @Test
    public void clearOnlyRemovesTheThreeLinkBits() {
        long raw = 0xFEDCBA9876543210L;
        assertEquals(raw & ~LINK_MASK, clearField(raw));
    }

    @Test
    public void reclassificationPolicyIsExplicitAndDoesNotImplicitlyRewriteRawBits() {
        long raw = write(0xA000000000000001L,
                ObjectRecordTable.RecordType.TYPE_2, 5).rawRecord();
        assertEquals(5, readRawField(raw));
        // The codec has no implicit type-change side effect; callers choose
        // whether a type reclassification retains or clears the packed field.
        assertEquals(5, readRawField(raw));
        assertEquals(0, readRawField(clearField(raw)));
    }
}
