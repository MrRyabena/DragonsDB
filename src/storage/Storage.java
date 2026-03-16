package storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Storage {
    // void save(collection.Collection collection) throws IOException;
    void save(InputStream stream) throws IOException;

    OutputStream load() throws IOException;
}
