package storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public interface Storage {
    void save(InputStream stream);

    OutputStream load();

    Date getDateCreated();

    Date getDateModified();
}
