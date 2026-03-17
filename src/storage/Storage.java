package storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Persistence abstraction for storing and loading serialized data.
 */
public interface Storage {
    /**
     * Saves the provided input stream content to the storage destination.
     *
     * @param stream data to save
     */
    void save(InputStream stream);

    /**
     * Loads data from storage into an in-memory buffer.
     *
     * @return output stream containing loaded bytes
     */
    OutputStream load();

    /**
     * Returns the creation date of the underlying storage resource.
     *
     * @return creation date, or null if not available
     */
    Date getDateCreated();

    /**
     * Returns the last modification date of the underlying storage resource.
     *
     * @return last modification date
     */
    Date getDateModified();
}
