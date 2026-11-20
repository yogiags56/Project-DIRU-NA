package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import org.junit.Test;

import static org.junit.Assert.*;

public final class PackedTileMetadataCodecTest {
    @Test
    public void objectIdWritesPreserveOtherGroupsAndFields() {
        int raw = 0x5A5A5A5A;
        int original = raw;
        for (int group = 0; group < PackedTileMetadataCodec.GROUP_COUNT; group++) {
            PackedTileMetadataCodec.WriteResult result =
                    PackedTileMetadataCodec.writeObjectId(raw, group, group + 1);
            assertEquals(PackedTileMetadataCodec.Status.OK, result.status());
            raw = result.rawWord();
            assertEquals(group + 1,
                    PackedTileMetadataCodec.readObjectId(raw, group).value());
        }
        for (int group = 0; group < PackedTileMetadataCodec.GROUP_COUNT; group++) {
            assertEquals((original >>> (group * 9 + 3)) & 0x3F,
                    (raw >>> (group * 9 + 3)) & 0x3F);
        }
    }

    @Test
    public void clearingOneGroupPreservesTheOtherTwoGroups() {
        int raw = 0x7FFFFFFF;
        PackedTileMetadataCodec.WriteResult cleared =
                PackedTileMetadataCodec.clearGroup(raw, 1);
        assertEquals(PackedTileMetadataCodec.Status.OK, cleared.status());
        assertEquals(0, PackedTileMetadataCodec.readField(
                cleared.rawWord(), 1, 0, 9).value());
        assertEquals(raw & 0x1FF, cleared.rawWord() & 0x1FF);
        assertEquals((raw >>> 18) & 0x1FF, (cleared.rawWord() >>> 18) & 0x1FF);
    }

    @Test
    public void supportsFieldWidthsUsedByLegacyNineBitGroup() {
        int raw = 0;
        raw = PackedTileMetadataCodec.writeField(raw, 2, 3, 4, 0xD).rawWord();
        raw = PackedTileMetadataCodec.writeField(raw, 0, 7, 2, 3).rawWord();
        assertEquals(0xD, PackedTileMetadataCodec.readField(raw, 2, 3, 4).value());
        assertEquals(3, PackedTileMetadataCodec.readField(raw, 0, 7, 2).value());
    }

    @Test
    public void validatesGroupFieldAndValueBoundariesWithoutMutation() {
        int raw = 0x12345678;
        assertEquals(PackedTileMetadataCodec.Status.INVALID_GROUP,
                PackedTileMetadataCodec.readObjectId(raw, 3).status());
        assertEquals(PackedTileMetadataCodec.Status.INVALID_FIELD,
                PackedTileMetadataCodec.writeField(raw, 0, 8, 2, 1).status());
        PackedTileMetadataCodec.WriteResult negative =
                PackedTileMetadataCodec.writeObjectId(raw, 1, -1);
        assertEquals(PackedTileMetadataCodec.Status.VALUE_OUT_OF_RANGE, negative.status());
        assertEquals(raw, negative.rawWord());
        assertEquals(PackedTileMetadataCodec.Status.VALUE_OUT_OF_RANGE,
                PackedTileMetadataCodec.writeObjectId(raw, 1, 8).status());
    }

    @Test
    public void fieldAccessUsesUnsignedShiftsForNegativeWords() {
        int raw = -1;
        assertEquals(7, PackedTileMetadataCodec.readObjectId(raw, 2).value());
        PackedTileMetadataCodec.WriteResult changed =
                PackedTileMetadataCodec.writeObjectId(raw, 2, 0);
        assertEquals(0, PackedTileMetadataCodec.readObjectId(changed.rawWord(), 2).value());
        assertEquals(-1 & ~(7 << 18), changed.rawWord());
    }
}