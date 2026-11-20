package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.io.IOException;

/** Platform-independent boundary for storing a complete native checkpoint blob. */
public interface CheckpointStore {
    void write(byte[] encodedCheckpoint) throws IOException;
    byte[] read() throws IOException;
}
