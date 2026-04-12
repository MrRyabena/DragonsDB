package storage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.log4j.Logger;

import dragon.view.JsonView;

/** Storage adapter backed by PostgreSQL. */
public class DatabaseStorage implements Storage {
    private static final Logger logger = Logger.getLogger(DatabaseStorage.class);

    public DatabaseStorage() {
        this.jsonView = new JsonView();
    }

    @Override
    public void save(InputStream stream) {
        logger.debug("PostgreSQL storage is already synchronized by the collection layer");
    }

    @Override
    public OutputStream load() {
        try (var input = jsonView.toView(PostgresDragonRepository.loadAll().stream())) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(input.readAllBytes());
            return outputStream;
        } catch (Exception e) {
            logger.error("Failed to load PostgreSQL collection snapshot", e);
            return new ByteArrayOutputStream();
        }
    }

    @Override
    public Date getDateCreated() {
        return PostgresDragonRepository.getDateCreated();
    }

    @Override
    public Date getDateModified() {
        return PostgresDragonRepository.getDateModified();
    }

    private final JsonView jsonView;
}
