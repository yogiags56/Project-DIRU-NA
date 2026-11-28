package MeongDev.DIRUNA.GloftDIRU.core.demo;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class DemoOpcodeDryRunEngineTest {
    private static DemoLegacyBufferLayout packedGroup() {
        return DemoLegacyBufferLayout.packedGroup(new byte[]{
                0,2, 8,0,0,0, 10,0,0,0,
                25,0,1,0,2,0,0x34,0x12,
                26,0,3,0,4,0,0x78,0x56,0x34,0x12
        });
    }

    @Test public void descriptorGatedReaderAllowsDeclaredU16Read() {
        DemoLegacyBufferLayout.RecordSlot slot = DemoLegacyBufferLayout.topLevelLeaf(
                new byte[]{25,0,1,0,2,0,0x34,0x12}).slot(0);
        DemoOperandAccessReader reader = new DemoOperandAccessReader(
                slot, DemoOpcodeOperandDescriptor.forOpcode(25));
        assertEquals(1, reader.readU16LE(2));
        assertEquals(8, reader.logicalRecordLength());
        assertEquals(8, reader.physicalBackingLength());
    }

    @Test public void descriptorGatedReaderRejectsUndeclaredReads() {
        DemoLegacyBufferLayout.RecordSlot slot = DemoLegacyBufferLayout.topLevelLeaf(
                new byte[]{25,0,1,0,2,0,0x34,0x12}).slot(0);
        DemoOperandAccessReader reader = new DemoOperandAccessReader(
                slot, DemoOpcodeOperandDescriptor.forOpcode(25));
        try {
            reader.readU32LE(2);
            fail("U32 at +2 is not declared for opcode 25");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not declared"));
        }
    }

    @Test public void readerPreservesPhysicalGroupBoundarySemantics() {
        DemoLegacyBufferLayout.RecordSlot first = packedGroup().slot(0);
        DemoOperandAccessReader reader = new DemoOperandAccessReader(
                first, DemoOpcodeOperandDescriptor.forOpcode(25));
        assertFalse(reader.isWithinLogicalRecord(6,4));
        assertEquals(0x001A1234L, reader.readU32LE(6));
    }

    @Test public void dryRunTracesOpcode25FallThroughWithoutMutation() {
        DemoLegacyBufferLayout.RecordSlot slot = packedGroup().slot(0);
        DemoOpcodeDryRunEngine engine = new DemoOpcodeDryRunEngine(
                DemoOpcodeHandlerRegistry.dryRunRegistry());
        DemoOpcodeDryRunEngine.Result result = engine.execute(slot, false);
        assertEquals(25, result.opcode());
        assertFalse(result.isSkipped());
        assertEquals(Arrays.asList(25,26,5,8,9,27,2), result.visitedLabels());
    }

    @Test public void dryRunHonorsVerifiedSkipAllowList() {
        DemoLegacyBufferLayout.RecordSlot slot = packedGroup().slot(0);
        DemoOpcodeDryRunEngine engine = new DemoOpcodeDryRunEngine(
                DemoOpcodeHandlerRegistry.dryRunRegistry());
        DemoOpcodeDryRunEngine.Result result = engine.execute(slot, true);
        assertFalse(result.isSkipped()); // opcode 25 is explicitly allowed
        assertEquals(Arrays.asList(25,26,5,8,9,27,2), result.visitedLabels());
    }

    @Test public void rejectsUnsupportedOpcodeBeforeDispatch() {
        DemoLegacyBufferLayout.RecordSlot slot = DemoLegacyBufferLayout.topLevelLeaf(
                new byte[]{7,0,1,0,2,0,3,0}).slot(0);
        DemoOpcodeDryRunEngine engine = new DemoOpcodeDryRunEngine(
                DemoOpcodeHandlerRegistry.dryRunRegistry());
        try {
            engine.execute(slot, false);
            fail("opcode 7 does not yet have a source-audited operand descriptor");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no audited dry-run handler"));
        }
    }

    @Test public void contextDoesNotExposeMutableTrace() {
        DemoLegacyBufferLayout.RecordSlot slot = DemoLegacyBufferLayout.topLevelLeaf(
                new byte[]{1,0,0,0,0,0,0,0,0,0,0,0}).slot(0);
        DemoOpcodeDryRunEngine engine = new DemoOpcodeDryRunEngine(
                DemoOpcodeHandlerRegistry.dryRunRegistry());
        DemoOpcodeDryRunEngine.Result result = engine.execute(slot, false);
        try {
            result.visitedLabels().add(99);
            fail("trace must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
