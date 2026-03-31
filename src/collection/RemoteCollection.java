package collection;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import client.RequestClient;
import core.Defaults;
import dragon.Dragon;
import dragon.view.SerializedView;

public class RemoteCollection implements API, AutoCloseable {

    public RemoteCollection() {
        this(Defaults.SERVER_HOST, Defaults.SERVER_PORT);
    }

    public RemoteCollection(String host, int port) {
        client = new RequestClient(host, port);
        isOwnerOfClient = true;
    }

    /**
     * Creates a remote collection using an existing RequestClient.
     * The client is NOT closed when this collection is closed.
     */
    public RemoteCollection(RequestClient existingClient) {
        client = existingClient;
        isOwnerOfClient = false;
    }

    @Override
    public void add(Dragon element) {
        if (element == null) {
            return;
        }
        client.send(ApiCommand.ADD, Stream.of(element));
    }

    @Override
    public void add(Stream<Dragon> elements) {
        client.send(ApiCommand.ADD, elements);
    }

    @Override
    public void updateById(Dragon element) {
        if (element == null) {
            return;
        }
        client.send(ApiCommand.UPDATE_BY_ID, Stream.of(element));
    }

    @Override
    public void clear() {
        client.send(ApiCommand.CLEAR, Stream.empty());
    }

    @Override
    public Stream<Dragon> getStream() {
        byte[] bytes = client.send(ApiCommand.GET_STREAM, Stream.empty());
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Server unavailable for GET_STREAM");
        }
        return serializedView.fromView(RequestClient.toOutputStream(bytes));
    }

    @Override
    public void removeIf(Predicate<? super Dragon> filter) {
        var snapshot = getStream().toList();
        Set<Long> idsToRemove = snapshot.stream().filter(filter).map(Dragon::getId).collect(Collectors.toSet());
        client.send(ApiCommand.REMOVE_IF, snapshot.stream().filter(d -> idsToRemove.contains(d.getId())));
    }

    @Override
    public int countIf(Predicate<? super Dragon> filter) {
        var snapshot = getStream().toList();
        Set<Long> ids = snapshot.stream().filter(filter).map(Dragon::getId).collect(Collectors.toSet());
        byte[] response = client.send(ApiCommand.COUNT_IF, snapshot.stream().filter(d -> ids.contains(d.getId())));
        if (response == null || response.length == 0) {
            throw new IllegalStateException("Server unavailable for COUNT_IF");
        }
        try {
            return Integer.parseInt(new String(response, Defaults.CHARSET).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void close() {
        if (isOwnerOfClient && client != null) {
            client.close();
        }
    }

    private final RequestClient client;
    private final boolean isOwnerOfClient;
    private final SerializedView serializedView = new SerializedView();
}
