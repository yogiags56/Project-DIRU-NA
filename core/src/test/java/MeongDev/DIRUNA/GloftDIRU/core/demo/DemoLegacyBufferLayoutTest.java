package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoLegacyBufferLayoutTest {
    private static byte[] packedGroup() {
        // Group header + two u32 lengths + opcode 25 (8 bytes) + opcode 26 (10 bytes).
        return new byte[]{
                0,2,
                8,0,0,0,
                10,0,0,0,
                25,0, 1,0, 2,0, 0x34,0x12,
                26,0, 3,0, 4,0, 0x78,0x56,0x34,0x12
        };
    }

    @Test public void topLevelLeafHasAnIsolatedBackingArray() {
        DemoLegacyBufferLayout layout = DemoLegacyBufferLayout.topLevelLeaf(
                new byte[]{25,0,1,0,2,0,0x34,0x12});
        assertFalse(layout.isPackedGroup());
        assertEquals(1, layout.slotCount());
        DemoLegacyBufferLayout.RecordSlot slot = layout.slot(0);
        assertEquals(8, slot.logicalLength());
        assertEquals(8, slot.backingLength());
        assertTrue(slot.isWithinLogicalRecord(6, 2));
        assertFalse(slot.isWithinLogicalRecord(6, 4));
        try {
            slot.readBackingU32(6);
            fail("isolated top-level buffer must reject physical overread");
        } catch (DemoLegacyBufferLayout.BufferBoundsException expected) {
            assertEquals(8, expected.backingLength());
        }
    }

    @Test public void groupChildrenShareOneBackingArrayAndRetainDeclaredLengths() {
        DemoLegacyBufferLayout layout = DemoLegacyBufferLayout.packedGroup(packedGroup());
        assertTrue(layout.isPackedGroup());
        assertEquals(2, layout.slotCount());
        DemoLegacyBufferLayout.RecordSlot first = layout.slot(0);
        DemoLegacyBufferLayout.RecordSlot second = layout.slot(1);
        assertEquals(10, first.offsetInContainer());
        assertEquals(8, first.logicalLength());
        assertEquals(18, second.offsetInContainer());
        assertEquals(10, second.logicalLength());
        assertEquals(28, layout.backingLength());
        assertEquals(layout.backingLength(), first.backingLength());
        assertEquals(layout.backingLength(), second.backingLength());
    }

    @Test public void sharedGroupReadCanCrossChildBoundaryButNotContainerBoundary() {
        DemoLegacyBufferLayout.RecordSlot first =
                DemoLegacyBufferLayout.packedGroup(packedGroup()).slot(0);
        assertFalse(first.isWithinLogicalRecord(6, 4));
        // Legacy switch fall-through reads bytes 6..9 from the same packed array:
        // bytes 6..7 are opcode-25 payload; bytes 8..9 are next child header 26,0.
        assertEquals(0x001A1234L, first.readBackingU32(6));
        try {
            first.readBackingU32(18);
            fail("read must not cross the physical group array end");
        } catch (DemoLegacyBufferLayout.BufferBoundsException expected) {
            assertEquals(28, expected.backingLength());
        }
    }

    @Test public void validatesGroupLengthTableAndLeafHeaders() {
        for (byte[] malformed : new byte[][]{
                null,
                new byte[]{0},
                new byte[]{0,1,8,0,0,0,25,0},
                new byte[]{0,1,3,0,0,0,25,0,1},
                new byte[]{0,1,2,0,0,0,0,0}
        }) {
            try {
                DemoLegacyBufferLayout.packedGroup(malformed);
                fail("expected malformed group rejection");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test public void rejectsGroupAsTopLevelLeafAndLeafAsGroup() {
        try {
            DemoLegacyBufferLayout.topLevelLeaf(new byte[]{0,0});
            fail("expected group rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            DemoLegacyBufferLayout.packedGroup(new byte[]{25,0});
            fail("expected leaf rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
