package storage;

import collection.RemoteCollection;
import core.Defaults;
import dragon.view.JsonView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class RemoteStorage implements Storage {

    public RemoteStorage() {
        this(Defaults.SERVER_HOST, Defaults.SERVER_PORT);
    }

    public RemoteStorage(String host, int port) {
        this.collection = new RemoteCollection(host, port);
    }

    @Override
    public void save(InputStream stream) {
        try {
            var bytes = stream.readAllBytes();
            var buffer = new ByteArrayOutputStream();
            buffer.write(bytes);
            collection.clear();
            collection.add(new JsonView().fromView(buffer));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save collection remotely", e);
        }
    }

    @Override
    public OutputStream load() {
        var stream = new JsonView().toView(collection.getStream());
        var out = new ByteArrayOutputStream();
        try {
            stream.transferTo(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load collection remotely", e);
        }
        return out;
    }

    @Override
    public Date getDateCreated() {
        return null;
    }

    @Override
    public Date getDateModified() {
        return new Date();
    }

    private final RemoteCollection collection;
}
