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
import dragon.Dragon;
import storage.CommandLogger;
import storage.Storage;
import storage.TransactionManager;

public class CommandsHandler implements Consumer<ServerContext> {

    public CommandsHandler(API collection, Storage storage, CommandLogger commandLogger, TransactionManager transactionManager) {
        this.collection = collection;
        this.storage = storage;
        this.commandLogger = commandLogger;
        this.transactionManager = transactionManager;
    }

    @Override
    public void accept(ServerContext context) {
        if (context.command == null) {
            context.response = textResponse("Invalid request: command is missing.");
            return;
        }

        var payload = context.stream != null ? context.stream : Stream.<Dragon>empty();
        List<Dragon> requestDragons = payload.collect(Collectors.toList());

        // Log command before execution (write-ahead)
        long txnId = transactionManager.getCurrentTransactionId();
        if (txnId == 0) {
            txnId = transactionManager.beginTransaction();
        }
        
        String dragonData = serializeDragons(requestDragons);
        long logId = commandLogger.logCommand(txnId, context.command, dragonData);

        try {
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
            
            // Commit the log entry after successful execution
            commandLogger.commitCommand(logId);
        } catch (Exception e) {
            // Rollback the log entry on error
            commandLogger.rollbackCommand(logId);
            context.response = textResponse("Command execution failed: " + e.getMessage());
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

    /**
     * Serializes a list of dragons to a Base64 string for WAL logging.
     */
    private String serializeDragons(List<Dragon> dragons) {
        if (dragons == null || dragons.isEmpty()) {
            return null;
        }
        try {
            var baos = new ByteArrayOutputStream();
            var oos = new ObjectOutputStream(baos);
            oos.writeInt(dragons.size());
            for (Dragon dragon : dragons) {
                oos.writeObject(dragon);
            }
            oos.flush();
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private final API collection;
    private final Storage storage;
    private final CommandLogger commandLogger;
    private final TransactionManager transactionManager;
}
