package server;

import collection.Collection;
import core.Defaults;
import dragon.view.JsonView;
import java.nio.file.FileSystems;
import java.util.Optional;
import org.apache.log4j.Logger;
import storage.FileStorage;

public class Server {
    public static void main(String[] args) {
        String storagePath = System.getenv("STORAGE_PATH");
        if (storagePath == null || storagePath.isBlank()) {
            storagePath = Defaults.STORAGE_PATH;
            logger.warn("STORAGE_PATH is not set. Using default path: " + storagePath);
        }

        var storage = new FileStorage(FileSystems.getDefault().getPath(storagePath));
        var collection = new Collection();
        var jsonView = new JsonView();
        collection.add(jsonView.fromView(storage.load()));

        try (var connection_handler = new ConnectionHandler()) {
            var reader = new RequestReader();
            var commands_handler = new CommandsHandler(collection, storage);
            var sender = new ResponseSender(connection_handler.getChannel());
            while (true) {
                Optional<ServerContext> new_context = connection_handler.get();
                if (new_context.isPresent()) {
                    ServerContext context = new_context.get();
                    reader.accept(context);
                    commands_handler.accept(context);
                    sender.accept(context);
                }
            }
        } catch (Exception e) {
            logger.error("Fatal server error", e);
        }
    }

    static {
        logger = Logger.getLogger(Server.class);
    }
    static private Logger logger;
}
