package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoOpcodeOperandDescriptorTest {
    @Test public void exposesOnlyExplicitlyAuditedOpcodes() {
        assertNotNull(DemoOpcodeOperandDescriptor.forOpcode(1));
        assertNotNull(DemoOpcodeOperandDescriptor.forOpcode(25));
        assertNotNull(DemoOpcodeOperandDescriptor.forOpcode(27));
        assertNull(DemoOpcodeOperandDescriptor.forOpcode(3));
        assertNull(DemoOpcodeOperandDescriptor.forOpcode(28));
    }

    @Test public void recordsKnownFallThroughChainsInSourceOrder() {
        assertEquals(java.util.Arrays.asList(25,26,5,8,9,27),
                DemoOpcodeOperandDescriptor.forOpcode(25).switchLabels());
        assertEquals(java.util.Arrays.asList(11,10,13),
                DemoOpcodeOperandDescriptor.forOpcode(11).switchLabels());
        assertEquals(java.util.Arrays.asList(16,14,12),
                DemoOpcodeOperandDescriptor.forOpcode(16).switchLabels());
    }

    @Test public void computesMaximumPhysicalReadSpan() {
        assertEquals(12, DemoOpcodeOperandDescriptor.forOpcode(1).minimumBackingSpan());
        assertEquals(16, DemoOpcodeOperandDescriptor.forOpcode(4).minimumBackingSpan());
        assertEquals(6, DemoOpcodeOperandDescriptor.forOpcode(6).minimumBackingSpan());
        assertEquals(11, DemoOpcodeOperandDescriptor.forOpcode(26).minimumBackingSpan());
        assertEquals(11, DemoOpcodeOperandDescriptor.forOpcode(27).minimumBackingSpan());
    }

    @Test public void opcode25DescriptorCapturesFallThroughOverreadSpan() {
        DemoOpcodeOperandDescriptor descriptor =
                DemoOpcodeOperandDescriptor.forOpcode(25);
        assertEquals(11, descriptor.minimumBackingSpan());
        boolean hasCollisionValueRead = false;
        for (DemoOpcodeOperandDescriptor.OperandRead read : descriptor.reads()) {
            if (read.opcodeLabel() == 26 && read.offset() == 6
                    && read.kind() == DemoOpcodeOperandDescriptor.ReadKind.U32_LE) {
                hasCollisionValueRead = true;
            }
        }
        assertTrue(hasCollisionValueRead);
        boolean hasTrailingTextRead = false;
        for (DemoOpcodeOperandDescriptor.OperandRead read : descriptor.reads()) {
            if (read.opcodeLabel() == 2 && read.offset() == 10
                    && read.kind() == DemoOpcodeOperandDescriptor.ReadKind.U8) {
                hasTrailingTextRead = true;
            }
        }
        assertTrue(hasTrailingTextRead);
    }

    @Test public void descriptorsAreImmutableAndReadsRemainInspectable() {
        DemoOpcodeOperandDescriptor descriptor =
                DemoOpcodeOperandDescriptor.forOpcode(9);
        assertFalse(descriptor.reads().isEmpty());
        try {
            descriptor.switchLabels().add(99);
            fail("descriptor labels must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        try {
            descriptor.reads().clear();
            fail("descriptor reads must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
