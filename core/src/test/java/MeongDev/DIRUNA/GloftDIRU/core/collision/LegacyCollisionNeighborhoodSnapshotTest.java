package MeongDev.DIRUNA.GloftDIRU.core.collision;

import org.junit.Test;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public final class LegacyCollisionNeighborhoodSnapshotTest {
    private static LegacyCollisionNeighborhoodSnapshot.CellSample cell(
            int x, int y, int foreground, int player, int metadata,
            boolean auxiliaryPresent, int auxiliary) {
        return new LegacyCollisionNeighborhoodSnapshot.CellSample(
                x, y, foreground, player, metadata, auxiliaryPresent, auxiliary);
    }

    @Test public void keepsNamedNeighborSamplesAndRawValues() {
        Map<LegacyCollisionNeighborhoodSnapshot.Role,
                LegacyCollisionNeighborhoodSnapshot.CellSample> samples =
                new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                        LegacyCollisionNeighborhoodSnapshot.CellSample>(
                        LegacyCollisionNeighborhoodSnapshot.Role.class);
        samples.put(LegacyCollisionNeighborhoodSnapshot.Role.TARGET,
                cell(5, 7, 0x0402, 9, 0x1234, true, 6));
        samples.put(LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS,
                cell(6, 7, 7, -1, 0x00F0, false, 0));
        LegacyCollisionNeighborhoodSnapshot snapshot =
                new LegacyCollisionNeighborhoodSnapshot(samples, null, null);

        assertEquals(0x0402, snapshot.get(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET).getForegroundRaw());
        assertEquals(0x1234, snapshot.get(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET).getPackedMetadataRaw());
        assertEquals(6, snapshot.get(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET).getAuxiliaryRaw());
        assertFalse(snapshot.get(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET_TWO_STEPS)
                .isAuxiliaryGridPresent());
    }

    @Test public void preservesRepeatedType19ScanSamplesInOrder() {
        List<LegacyCollisionNeighborhoodSnapshot.CellSample> scan =
                new ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample>();
        scan.add(cell(8, 7, 19, 0, 0, false, 0));
        scan.add(cell(8, 6, 19, 0, 0, false, 0));
        scan.add(cell(8, 5, 19, 39, 0x0003, false, 0));
        LegacyCollisionNeighborhoodSnapshot snapshot =
                new LegacyCollisionNeighborhoodSnapshot(
                        new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                                LegacyCollisionNeighborhoodSnapshot.CellSample>(
                                LegacyCollisionNeighborhoodSnapshot.Role.class),
                        scan.get(2), scan);

        assertEquals(3, snapshot.getType19Scan().size());
        assertEquals(6, snapshot.getType19Scan().get(1).getY());
        assertEquals(39, snapshot.getType19Anchor().getPlayerLayerRaw());
    }

    @Test public void doesNotInventMissingNeighborSamples() {
        LegacyCollisionNeighborhoodSnapshot snapshot =
                new LegacyCollisionNeighborhoodSnapshot(
                        new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                                LegacyCollisionNeighborhoodSnapshot.CellSample>(
                                LegacyCollisionNeighborhoodSnapshot.Role.class),
                        null, null);
        assertFalse(snapshot.contains(
                LegacyCollisionNeighborhoodSnapshot.Role.TYPE48_SUPPORT));
        assertNull(snapshot.getType19Anchor());
        assertTrue(snapshot.getType19Scan().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sampleMapIsUnmodifiable() {
        LegacyCollisionNeighborhoodSnapshot snapshot =
                new LegacyCollisionNeighborhoodSnapshot(
                        new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                                LegacyCollisionNeighborhoodSnapshot.CellSample>(
                                LegacyCollisionNeighborhoodSnapshot.Role.class),
                        null, null);
        snapshot.getSamples().put(
                LegacyCollisionNeighborhoodSnapshot.Role.TARGET,
                cell(0, 0, 0, 0, 0, false, 0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void type19ScanListIsUnmodifiable() {
        List<LegacyCollisionNeighborhoodSnapshot.CellSample> scan =
                new ArrayList<LegacyCollisionNeighborhoodSnapshot.CellSample>();
        scan.add(cell(1, 1, 19, 0, 0, false, 0));
        LegacyCollisionNeighborhoodSnapshot snapshot =
                new LegacyCollisionNeighborhoodSnapshot(
                        new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                                LegacyCollisionNeighborhoodSnapshot.CellSample>(
                                LegacyCollisionNeighborhoodSnapshot.Role.class),
                        null, scan);
        snapshot.getType19Scan().clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullSampleValue() {
        Map<LegacyCollisionNeighborhoodSnapshot.Role,
                LegacyCollisionNeighborhoodSnapshot.CellSample> samples =
                new EnumMap<LegacyCollisionNeighborhoodSnapshot.Role,
                        LegacyCollisionNeighborhoodSnapshot.CellSample>(
                        LegacyCollisionNeighborhoodSnapshot.Role.class);
        samples.put(LegacyCollisionNeighborhoodSnapshot.Role.TARGET, null);
        new LegacyCollisionNeighborhoodSnapshot(samples, null, null);
    }
}
