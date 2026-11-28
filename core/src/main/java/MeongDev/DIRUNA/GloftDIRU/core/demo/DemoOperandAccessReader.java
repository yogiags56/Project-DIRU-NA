package MeongDev.DIRUNA.GloftDIRU.core.demo;

/**
 * Descriptor-gated read-only access to a legacy physical record slot.
 * Reads may cross a logical child boundary only when the slot belongs to a
 * packed group backing array, matching the legacy shared-array visibility.
 */
public final class DemoOperandAccessReader {
    private final DemoLegacyBufferLayout.RecordSlot slot;
    private final DemoOpcodeOperandDescriptor descriptor;

    public DemoOperandAccessReader(DemoLegacyBufferLayout.RecordSlot slot,
                                   DemoOpcodeOperandDescriptor descriptor) {
        if (slot == null || descriptor == null)
            throw new IllegalArgumentException("slot and descriptor are required");
        if (slot.opcode() != descriptor.entryOpcode())
            throw new IllegalArgumentException("descriptor opcode does not match record slot");
        this.slot = slot;
        this.descriptor = descriptor;
    }

    public int readU8(int offset) {
        requireDeclaredRead(offset, DemoOpcodeOperandDescriptor.ReadKind.U8);
        return slot.readBackingU8(offset);
    }

    public int readU16LE(int offset) {
        requireDeclaredRead(offset, DemoOpcodeOperandDescriptor.ReadKind.U16_LE);
        return slot.readBackingU16(offset);
    }

    public long readU32LE(int offset) {
        requireDeclaredRead(offset, DemoOpcodeOperandDescriptor.ReadKind.U32_LE);
        return slot.readBackingU32(offset);
    }

    public boolean isWithinLogicalRecord(int offset, int width) {
        return slot.isWithinLogicalRecord(offset, width);
    }

    public int logicalRecordLength() { return slot.logicalLength(); }
    public int physicalBackingLength() { return slot.backingLength(); }
    public int recordOffsetInContainer() { return slot.offsetInContainer(); }

    private void requireDeclaredRead(int offset, DemoOpcodeOperandDescriptor.ReadKind kind) {
        for (DemoOpcodeOperandDescriptor.OperandRead read : descriptor.reads()) {
            if (read.offset() == offset && read.kind() == kind) return;
        }
        throw new IllegalArgumentException("read is not declared by opcode "
                + descriptor.entryOpcode() + " descriptor: " + kind + " at " + offset);
    }
}
