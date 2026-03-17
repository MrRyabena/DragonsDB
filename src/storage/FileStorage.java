package storage;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * File-based {@link Storage} implementation.
 *
 * <p>Stores the serialized collection (as bytes) in a single file. Metadata such as creation and
 * modification time is obtained from the file system rather than being serialized.
 */
public class FileStorage implements Storage {

    /**
     * Creates a file storage bound to the specified path.
     *
     * @param filename file path to read from and write to
     */
    public FileStorage(Path filename) {
        this.filename = filename;
    }

    /**
     * Writes the provided stream contents to the file.
     *
     * @param stream data to save
     */
    @Override
    public void save(InputStream stream) {
        try (var reader = new InputStreamReader(stream, core.Defaults.CHARSET);
                var writer = new BufferedWriter(new FileWriter(filename.toFile()))) {
            reader.transferTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the file contents into a {@link ByteArrayOutputStream}.
     *
     * @return output stream containing file bytes
     */
    @Override
    public OutputStream load() {

        var baos = new ByteArrayOutputStream();

        try (var reader = new InputStreamReader(new FileInputStream(filename.toFile()));
                var writer = new OutputStreamWriter(baos, core.Defaults.CHARSET)) {
            reader.transferTo(writer);
        }
         catch (IOException e) {
            System.err.println("Error reading file (the data is lost): " + e.getMessage());
        }
        return baos;
    }

    /**
     * Returns the file creation time as reported by the file system.
     *
     * @return creation date, or null if it cannot be determined
     */
    @Override
    public Date getDateCreated() {
        try {
            new Date(((FileTime) (Files.getAttribute(filename, "creationTime"))).toMillis());
        } catch (IOException e) {
            System.err.println("Error getting creation date: " + e.getMessage());
            }
        return null;
    }

    /**
     * Returns the file modification time.
     *
     * @return last modified date
     */
    @Override
    public Date getDateModified() {
        return new Date(filename.toFile().lastModified());
    }

    /** Backing file path. */
    private final Path filename;
}
