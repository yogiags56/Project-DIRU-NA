package MeongDev.DIRUNA.GloftDIRU.core.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable, role-labelled neighborhood samples for one legacy collision query.
 * Samples preserve raw layer values; this class performs no collision logic.
 */
public final class LegacyCollisionNeighborhoodSnapshot {
    public enum Role {
        TARGET,
        TARGET_TWO_STEPS,
        TARGET_ABOVE_PLAYER_ROW,
        TARGET_BELOW,
        TARGET_ABOVE,
        TARGET_LEFT,
        TARGET_RIGHT,
        TYPE48_DESTINATION_A,
        TYPE48_DESTINATION_B,
        TYPE48_SOURCE_CLEAR_A,
        TYPE48_SOURCE_CLEAR_B,
        TYPE48_SUPPORT
    }

    public static final class CellSample {
        private final int x;
        private final int y;
        private final int foregroundRaw;
        private final int playerLayerRaw;
        private final int packedMetadataRaw;
        private final boolean auxiliaryGridPresent;
        private final int auxiliaryRaw;

        public CellSample(int x, int y, int foregroundRaw, int playerLayerRaw,
                          int packedMetadataRaw, boolean auxiliaryGridPresent,
                          int auxiliaryRaw) {
            this.x = x;
            this.y = y;
            this.foregroundRaw = foregroundRaw;
            this.playerLayerRaw = playerLayerRaw;
            this.packedMetadataRaw = packedMetadataRaw;
            this.auxiliaryGridPresent = auxiliaryGridPresent;
            this.auxiliaryRaw = auxiliaryRaw;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getForegroundRaw() { return foregroundRaw; }
        public int getPlayerLayerRaw() { return playerLayerRaw; }
        public int getPackedMetadataRaw() { return packedMetadataRaw; }
        public boolean isAuxiliaryGridPresent() { return auxiliaryGridPresent; }
        public int getAuxiliaryRaw() { return auxiliaryRaw; }
    }

    private final Map<Role, CellSample> samples;
    private final CellSample type19Anchor;
    private final List<CellSample> type19Scan;
    private final List<CellSample> type19ForegroundRun;

    public LegacyCollisionNeighborhoodSnapshot(
            Map<Role, CellSample> samples,
            CellSample type19Anchor,
            List<CellSample> type19Scan) {
        this(samples, type19Anchor, type19Scan, null);
    }

    public LegacyCollisionNeighborhoodSnapshot(
            Map<Role, CellSample> samples,
            CellSample type19Anchor,
            List<CellSample> type19Scan,
            List<CellSample> type19ForegroundRun) {
        if (samples == null) {
            throw new IllegalArgumentException("samples must not be null");
        }
        EnumMap<Role, CellSample> copy = new EnumMap<Role, CellSample>(Role.class);
        for (Map.Entry<Role, CellSample> entry : samples.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("sample roles and values must not be null");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        this.samples = Collections.unmodifiableMap(copy);
        this.type19Anchor = type19Anchor;
        if (type19Scan == null) {
            this.type19Scan = Collections.emptyList();
        } else {
            this.type19Scan = Collections.unmodifiableList(
                    new ArrayList<CellSample>(type19Scan));
            for (CellSample sample : this.type19Scan) {
                if (sample == null) {
                    throw new IllegalArgumentException("type19 scan samples must not be null");
                }
            }
        }
        if (type19ForegroundRun == null) {
            this.type19ForegroundRun = Collections.emptyList();
        } else {
            this.type19ForegroundRun = Collections.unmodifiableList(
                    new ArrayList<CellSample>(type19ForegroundRun));
            for (CellSample sample : this.type19ForegroundRun) {
                if (sample == null) {
                    throw new IllegalArgumentException("type19 foreground-run samples must not be null");
                }
            }
        }
    }

    public CellSample get(Role role) { return samples.get(role); }
    public boolean contains(Role role) { return samples.containsKey(role); }
    public Map<Role, CellSample> getSamples() { return samples; }
    public CellSample getType19Anchor() { return type19Anchor; }
    public List<CellSample> getType19Scan() { return type19Scan; }
    public List<CellSample> getType19ForegroundRun() { return type19ForegroundRun; }
}
