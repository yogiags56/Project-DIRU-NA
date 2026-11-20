package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Checkpoint envelope that keeps packed tile metadata words beside the
 * cross-table lifecycle snapshot. The legacy words remain unchanged; this
 * wrapper validates them against the restored object-table link map.
 */
public final class CrossTableCheckpoint {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final CrossTableLifecycleCoordinator.Snapshot lifecycleSnapshot;
    private final Map<ObjectRecordTable.TileGroup, Integer> packedMetadataWords;

    public CrossTableCheckpoint(
            CrossTableLifecycleCoordinator.Snapshot lifecycleSnapshot,
            Map<ObjectRecordTable.TileGroup, Integer> packedMetadataWords) {
        this(CURRENT_SCHEMA_VERSION, lifecycleSnapshot, packedMetadataWords);
    }

    public CrossTableCheckpoint(int schemaVersion,
            CrossTableLifecycleCoordinator.Snapshot lifecycleSnapshot,
            Map<ObjectRecordTable.TileGroup, Integer> packedMetadataWords) {
        this.schemaVersion = schemaVersion;
        this.lifecycleSnapshot = lifecycleSnapshot;
        this.packedMetadataWords = Collections.unmodifiableMap(new HashMap<>(
                packedMetadataWords == null ? Map.of() : packedMetadataWords));
    }

    public int schemaVersion() { return schemaVersion; }

    public CrossTableLifecycleCoordinator.Snapshot lifecycleSnapshot() {
        return lifecycleSnapshot;
    }

    public Map<ObjectRecordTable.TileGroup, Integer> packedMetadataWords() {
        return packedMetadataWords;
    }

    /**
     * Validates the entire checkpoint against an isolated candidate first.
     * The destination coordinator is mutated only after every packed word
     * resolves to the same object ID recorded in the snapshot tile-link map.
     */
    public CrossTableLifecycleCoordinator.Status restoreInto(
            CrossTableLifecycleCoordinator destination) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || destination == null || lifecycleSnapshot == null
                || lifecycleSnapshot.objectSnapshot() == null)
            return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;

        ObjectRecordTable.Snapshot objectSnapshot = lifecycleSnapshot.objectSnapshot();
        Map<ObjectRecordTable.TileGroup, Integer> links = objectSnapshot.tileLinks();
        if (links == null || packedMetadataWords.size() != links.size()
                || !packedMetadataWords.keySet().equals(links.keySet()))
            return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;

        CrossTableLifecycleCoordinator candidate = new CrossTableLifecycleCoordinator();
        if (candidate.restore(lifecycleSnapshot)
                != CrossTableLifecycleCoordinator.Status.OK)
            return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;

        for (Map.Entry<ObjectRecordTable.TileGroup, Integer> entry
                : packedMetadataWords.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;
            ObjectRecordTable.LookupResult lookup = candidate.objects().lookup(
                    entry.getKey(), entry.getValue());
            if (lookup.status() != ObjectRecordTable.Status.OK
                    || !links.get(entry.getKey()).equals(
                    PackedTileMetadataCodec.readObjectId(
                            entry.getValue(), entry.getKey().group()).value()))
                return CrossTableLifecycleCoordinator.Status.INVALID_SNAPSHOT;
        }

        return destination.restore(lifecycleSnapshot);
    }
}
