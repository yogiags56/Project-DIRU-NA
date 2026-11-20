package MeongDev.DIRUNA.GloftDIRU.storage;

import android.util.AtomicFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CheckpointStore;
import MeongDev.DIRUNA.GloftDIRU.core.objectrecord.CrossTableCheckpointBinaryCodec;

/**
 * Android app-private checkpoint adapter backed by platform AtomicFile.
 * AtomicFile manages its backup/recovery protocol; no external-storage
 * permission or non-atomic fallback is used.
 */
public final class AndroidAtomicCheckpointStore implements CheckpointStore {
    private final AtomicFile atomicFile;
    private final File baseFile;

    public AndroidAtomicCheckpointStore(File filesDirectory, String fileName) {
        if (filesDirectory == null || fileName == null || fileName.isEmpty()
                || fileName.contains("/") || fileName.contains("\\")
                || ".".equals(fileName) || "..".equals(fileName)) {
            throw new IllegalArgumentException("a simple app-private checkpoint filename is required");
        }
        if (!filesDirectory.isDirectory() && !filesDirectory.mkdirs()
                && !filesDirectory.isDirectory()) {
            throw new IllegalArgumentException("cannot create app-private files directory");
        }
        this.baseFile = new File(filesDirectory, fileName);
        this.atomicFile = new AtomicFile(baseFile);
    }

    @Override
    public synchronized void write(byte[] encodedCheckpoint) throws IOException {
        if (encodedCheckpoint == null || encodedCheckpoint.length == 0
                || encodedCheckpoint.length > CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES) {
            throw new IOException("invalid checkpoint byte length");
        }

        FileOutputStream output = null;
        try {
            output = atomicFile.startWrite();
            output.write(encodedCheckpoint);
            atomicFile.finishWrite(output);
            output = null;
        } catch (IOException | RuntimeException failure) {
            if (output != null) atomicFile.failWrite(output);
            if (failure instanceof IOException) throw (IOException) failure;
            throw new IOException("Android atomic checkpoint write failed", failure);
        }
    }

    @Override
    public synchronized byte[] read() throws IOException {
        try (InputStream input = atomicFile.openRead();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES) {
                    throw new IOException("checkpoint file exceeds byte limit");
                }
                output.write(buffer, 0, count);
            }
            if (total == 0) throw new IOException("checkpoint file is empty");
            return output.toByteArray();
        }
    }

    /** True when either the primary file or AtomicFile backup is present. */
    public synchronized boolean hasRecoverableFile() {
        return baseFile.exists() || new File(baseFile.getPath() + ".bak").exists();
    }

    /** Removes the primary checkpoint and any AtomicFile backup. */
    public synchronized void delete() {
        atomicFile.delete();
    }
}
