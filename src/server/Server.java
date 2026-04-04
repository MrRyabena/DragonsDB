package server;

import java.nio.file.FileSystems;
import java.util.Optional;

import org.apache.log4j.Logger;

import collection.Collection;
import core.Defaults;
import dragon.view.JsonView;
import storage.CommandLogger;
import storage.FileCommandLogger;
import storage.FileStorage;
import storage.RecoveryManager;
import storage.TransactionManager;

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

        // Initialize WAL components
        CommandLogger commandLogger = new FileCommandLogger(storagePath);
        TransactionManager transactionManager = new TransactionManager(commandLogger);
        RecoveryManager recoveryManager = new RecoveryManager(commandLogger, collection);
        
        // Perform recovery from any incomplete transactions
        logger.info("Performing recovery from WAL...");
        int recovered = recoveryManager.performRecovery();
        if (recovered > 0) {
            logger.info("Recovered and rolled back " + recovered + " commands from incomplete transactions");
        }

        try (var connection_handler = new ConnectionHandler()) {
            var reader = new RequestReader();
            var commands_handler = new CommandsHandler(collection, storage, commandLogger, transactionManager);
            var packer = new ResponsePacker();
            var sender = new ResponseSender(connection_handler.getChannel());
            while (true) {
                Optional<ServerContext> new_context = connection_handler.get();
                if (new_context.isPresent()) {
                    ServerContext context = new_context.get();
                    reader.accept(context);
                    commands_handler.handleRequest(context);
                    packer.accept(context);
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
