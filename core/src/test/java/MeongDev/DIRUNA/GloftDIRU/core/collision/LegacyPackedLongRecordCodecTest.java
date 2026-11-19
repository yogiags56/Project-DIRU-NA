package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LegacyPackedLongRecordCodecTest {
    @Test
    public void motionLayoutAllocatesZeroBasedSlotAndWritesObservedFields() {
        LegacyPackedLongRecordCodec codec =
                new LegacyPackedLongRecordCodec(new long[15]);
        LegacyPackedLongRecordCodec.MotionLayout motion =
                LegacyPackedLongRecordCodec.MotionLayout.on(codec);

        int slot = motion.allocate((byte) 42, (byte) 17, (byte) 9, (byte) 2);

        assertEquals(0, slot);
        assertEquals(1, motion.readType(slot));
        assertEquals(2, motion.readSubtype(slot));
        assertEquals(42, motion.readFieldA(slot));
        assertEquals(17, motion.readFieldB(slot));
        assertEquals(9, motion.readFieldC(slot));
        assertEquals(42, motion.readFieldD(slot));
    }

    @Test
    public void motionLayoutReturnsMinusOneWhenAllSlotsAreOccupied() {
        long[] words = new long[15];
        for (int i = 0; i < words.length; i++) words[i] = 1L;
        LegacyPackedLongRecordCodec.MotionLayout motion =
                LegacyPackedLongRecordCodec.MotionLayout.on(
                        new LegacyPackedLongRecordCodec(words));

        assertEquals(-1, motion.allocate((byte) 1, (byte) 2, (byte) 3, (byte) 0));
    }

    @Test
    public void objectLayoutAllocatesOneBasedIdAndPreservesFieldLayout() {
        LegacyPackedLongRecordCodec codec =
                new LegacyPackedLongRecordCodec(new long[15]);
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(codec);

        int id = objects.allocateTypeOne(37, 22, 2, 1, 5);

        assertEquals(1, id);
        assertEquals(1, objects.readType(id));
        assertEquals(37, objects.readX(id));
        assertEquals(22, objects.readY(id));
        assertEquals(2, objects.readGroup(id));
        assertEquals(1, objects.readDirection(id));
        assertEquals(5, objects.readMode(id));
    }

    @Test
    public void objectLayoutNormalizesNegativeDirectionToTwo() {
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(
                        new LegacyPackedLongRecordCodec(new long[15]));

        int id = objects.allocateTypeOne(1, 2, 0, -1, 0);

        assertEquals(2, objects.readDirection(id));
    }

    @Test
    public void rawFieldWritePreservesSourceUnmaskedLongShiftBehavior() {
        LegacyPackedLongRecordCodec codec =
                new LegacyPackedLongRecordCodec(new long[15]);

        codec.write(0, 0x1FFL, 6, 7);

        assertEquals(0x1FFL << 6, codec.backingArray()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidOneBasedObjectRecordId() {
        LegacyPackedLongRecordCodec.ObjectLayout objects =
                LegacyPackedLongRecordCodec.ObjectLayout.on(
                        new LegacyPackedLongRecordCodec(new long[15]));
        objects.readType(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsFieldPastEndOfLong() {
        LegacyPackedLongRecordCodec codec =
                new LegacyPackedLongRecordCodec(new long[15]);
        codec.write(0, 1, 63, 2);
    }
}
