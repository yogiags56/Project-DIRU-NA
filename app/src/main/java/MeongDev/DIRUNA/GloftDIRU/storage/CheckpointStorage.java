package MeongDev.DIRUNA.GloftDIRU.storage;

import java.io.IOException;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpoint;

/** Storage boundary consumed by the serialized Android checkpoint I/O dispatcher. */
interface CheckpointStorage {
    void save(CrossTableCheckpoint checkpoint) throws IOException;
    AndroidCheckpointLoadResult loadCandidate();
}
