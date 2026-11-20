package MeongDev.DIRUNA.GloftDIRU.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CheckpointRecoveryPolicyTest {
    @Test public void missingCheckpointOffersNewGameOrImportWithoutResetting() {
        assertEquals(CheckpointRecoveryPolicy.Action.OFFER_NEW_GAME_OR_IMPORT,
                CheckpointRecoveryPolicy.actionFor(AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.MISSING, "missing")));
    }

    @Test public void corruptAndInvalidCheckpointArePreservedForDiagnosis() {
        assertEquals(CheckpointRecoveryPolicy.Action.OFFER_RETRY_OR_PRESERVE_FOR_DIAGNOSIS,
                CheckpointRecoveryPolicy.actionFor(AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.CORRUPT, "corrupt")));
        assertEquals(CheckpointRecoveryPolicy.Action.OFFER_RETRY_OR_PRESERVE_FOR_DIAGNOSIS,
                CheckpointRecoveryPolicy.actionFor(AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.INVALID_SNAPSHOT, "invalid")));
    }

    @Test public void storageErrorOffersRetry() {
        assertEquals(CheckpointRecoveryPolicy.Action.OFFER_STORAGE_RETRY,
                CheckpointRecoveryPolicy.actionFor(AndroidCheckpointLoadResult.of(
                        AndroidCheckpointLoadResult.Status.STORAGE_ERROR, "io")));
    }
}
