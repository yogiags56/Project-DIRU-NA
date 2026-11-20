package MeongDev.DIRUNA.GloftDIRU.core.objectrecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * JVM/NIO file adapter. Writes a sibling temporary file, forces its contents,
 * then atomically replaces the destination. It deliberately fails rather than
 * falling back to a non-atomic replacement when the filesystem lacks support.
 */
public final class AtomicFileCheckpointStore implements CheckpointStore {
    interface AtomicMover {
        void move(Path source, Path destination) throws IOException;
    }

    private static final AtomicMover NIO_ATOMIC_MOVER = (source, target) ->
            Files.move(source, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

    private final Path destination;
    private final AtomicMover mover;

    public AtomicFileCheckpointStore(Path destination) {
        this(destination, NIO_ATOMIC_MOVER);
    }

    AtomicFileCheckpointStore(Path destination, AtomicMover mover) {
        if (destination == null || destination.getFileName() == null)
            throw new IllegalArgumentException("destination file is required");
        if (mover == null) throw new IllegalArgumentException("atomic mover is required");
        this.destination = destination.toAbsolutePath().normalize();
        this.mover = mover;
    }

    @Override
    public synchronized void write(byte[] encodedCheckpoint) throws IOException {
        if (encodedCheckpoint == null || encodedCheckpoint.length == 0
                || encodedCheckpoint.length > CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES)
            throw new IOException("invalid checkpoint byte length");

        Path parent = destination.getParent();
        if (parent == null) throw new IOException("destination parent is unavailable");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent,
                destination.getFileName().toString() + ".", ".tmp");
        boolean replaced = false;
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encodedCheckpoint);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                mover.move(temporary, destination);
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException("atomic checkpoint replacement is not supported", ex);
            }
            replaced = true;
        } finally {
            if (!replaced) Files.deleteIfExists(temporary);
        }
    }

    @Override
    public synchronized byte[] read() throws IOException {
        if (!Files.exists(destination)) throw new IOException("checkpoint file does not exist");
        long size = Files.size(destination);
        if (size <= 0 || size > CrossTableCheckpointBinaryCodec.MAX_ENCODED_BYTES)
            throw new IOException("invalid checkpoint file length");
        return Files.readAllBytes(destination);
    }

    public Path destination() { return destination; }
}
