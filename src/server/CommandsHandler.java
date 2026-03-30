package server;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import collection.API;
import collection.ApiCommand;
import dragon.Dragon;
import storage.Storage;

public class CommandsHandler implements Consumer<ServerContext> {

    public CommandsHandler(API collection, Storage storage) {
        this.collection = collection;
        this.storage = storage;
    }

    @Override
    public void accept(ServerContext context) {
        if (context.command == null) {
            context.response = textResponse("Invalid request: command is missing.");
            return;
        }

        var payload = context.stream != null ? context.stream : Stream.<Dragon>empty();
        List<Dragon> requestDragons = payload.collect(Collectors.toList());

        switch (context.command) {
            case ADD -> {
                collection.add(requestDragons.stream());
                saveCollection();
                context.response = textResponse("Added " + requestDragons.size() + " dragons.");
            }
            case UPDATE_BY_ID -> {
                if (requestDragons.isEmpty()) {
                    context.response = textResponse("No dragon provided for update.");
                    return;
                }
                collection.updateById(requestDragons.get(0));
                saveCollection();
                context.response = textResponse("Dragon updated.");
            }
            case CLEAR -> {
                collection.clear();
                saveCollection();
                context.response = textResponse("Collection cleared.");
            }
            case REMOVE_IF -> {
                Set<Long> ids = requestDragons.stream().map(Dragon::getId).collect(Collectors.toSet());
                int before = collection.countIf(d -> true);
                collection.removeIf(d -> ids.contains(d.getId()));
                int removed = before - collection.countIf(d -> true);
                saveCollection();
                context.response = textResponse("Removed " + removed + " dragons.");
            }
            case COUNT_IF -> {
                Set<Long> ids = requestDragons.stream().map(Dragon::getId).collect(Collectors.toSet());
                int count = collection.countIf(d -> ids.contains(d.getId()));
                context.response = textResponse(Integer.toString(count));
            }
            case GET_STREAM -> context.response = streamResponse(collection.getStream());
            default -> context.response = textResponse("Unsupported command: " + context.command);
        }
    }

    private ByteBuffer textResponse(String value) {
        return ByteBuffer.wrap(value.getBytes(core.Defaults.CHARSET));
    }

    private ByteBuffer streamResponse(Stream<Dragon> stream) {
        try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
            stream.forEach(dragon -> {
                try {
                    oos.writeObject(dragon);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            oos.flush();
            return ByteBuffer.wrap(baos.toByteArray());
        } catch (Exception e) {
            return textResponse("Failed to serialize collection stream.");
        }
    }

    private void saveCollection() {
        storage.save(new dragon.view.JsonView().toView(collection.getStream()));
    }

    private final API collection;
    private final Storage storage;
}
