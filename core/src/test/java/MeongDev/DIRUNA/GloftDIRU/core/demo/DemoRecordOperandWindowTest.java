package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoRecordOperandWindowTest {
    @Test public void readsUnsignedAndSignedLittleEndianValues() {
        DemoRecordOperandWindow window = DemoRecordOperandWindow.fromNormalizedBytes(
                new byte[]{5,0, (byte)0xFE, (byte)0xFF, 0x34, 0x12, (byte)0x80});
        assertEquals(5, window.opcode());
        assertEquals(7, window.recordLength());
        assertEquals(254, window.readU8(2));
        assertEquals(65534, window.readU16(2));
        assertEquals((short)-2, window.readI16(2));
        assertEquals(0x1234, window.readU16(4));
        assertEquals(128, window.readU8(6));
    }

    @Test public void readsUnsigned24And32BitValuesWithoutSignExtension() {
        DemoRecordOperandWindow window = DemoRecordOperandWindow.fromNormalizedBytes(
                new byte[]{26,0, 0x78,0x56,0x34,(byte)0xF2, 0x01,0x02,0x03,(byte)0xF4});
        assertEquals(0xF2345678L & 0xFFFFFFL, window.readU24(2));
        assertEquals(0xF4030201L, window.readU32(6));
    }

    @Test public void returnsDetachedCopies() {
        byte[] input = new byte[]{5,0,1,2,3,4,5};
        DemoRecordOperandWindow window = DemoRecordOperandWindow.fromNormalizedBytes(input);
        input[2] = 99;
        byte[] copy = window.copyBytes(2, 2);
        copy[0] = 88;
        assertEquals(1, window.readU8(2));
        assertEquals(1, window.copyBytes(2, 1)[0]);
    }

    @Test public void rejectsReadPastRecordEndInsteadOfReadingAdjacentRecord() {
        DemoRecordOperandWindow window = DemoRecordOperandWindow.fromNormalizedBytes(
                new byte[]{5,0, 1,0, 2,0, 3});
        try {
            window.readU16(6);
            fail("expected record-local bounds failure");
        } catch (DemoRecordOperandWindow.OperandBoundsException e) {
            assertEquals(6, e.offset());
            assertEquals(2, e.length());
            assertEquals(7, e.recordLength());
        }
    }

    @Test public void rejectsNegativeAndOverflowingRanges() {
        DemoRecordOperandWindow window = DemoRecordOperandWindow.fromNormalizedBytes(
                new byte[]{6,0, 1,2,3,4});
        for (int[] range : new int[][]{{-1,1},{2,-1},{Integer.MAX_VALUE,4}}) {
            try {
                window.copyBytes(range[0], range[1]);
                fail("expected bounds rejection");
            } catch (DemoRecordOperandWindow.OperandBoundsException expected) {
                // expected
            }
        }
    }

    @Test public void rejectsMalformedHeadersAndGroupRecords() {
        for (byte[] bytes : new byte[][]{null, new byte[]{5}, new byte[]{5,1}, new byte[]{0,0}}) {
            try {
                DemoRecordOperandWindow.fromNormalizedBytes(bytes);
                fail("expected invalid record rejection");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }
}
