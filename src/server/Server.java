package server;

import java.util.Optional;

import org.apache.log4j.Logger;

import collection.DatabaseCollection;
import core.Defaults;
import storage.CommandLogger;
import storage.DatabaseStorage;
import storage.FileCommandLogger;
import storage.RecoveryManager;
import storage.TransactionManager;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class);

    public static void main(String[] args) {
        String storagePath = System.getenv("STORAGE_PATH");
        if (storagePath == null || storagePath.isBlank()) {
            storagePath = Defaults.STORAGE_PATH;
            logger.warn("STORAGE_PATH is not set. Using default path: " + storagePath);
        }

        var storage = new DatabaseStorage();
        var collection = new DatabaseCollection();

        CommandLogger commandLogger = new FileCommandLogger(storagePath);
        TransactionManager transactionManager = new TransactionManager(commandLogger);
        RecoveryManager recoveryManager = new RecoveryManager(commandLogger, collection);

        logger.info("Performing recovery from WAL...");
        int recovered = recoveryManager.performRecovery();
        if (recovered > 0) {
            logger.info(
                    "Recovered and rolled back "
                            + recovered
                            + " commands from incomplete transactions");
        }

        try (var connectionHandler = new ConnectionHandler()) {
            var reader = new RequestReader();
            var sessionManager = new SessionManager();
            var authService = new AuthService();
            var authHandler = new AuthenticationHandler(sessionManager, authService);
            var commandsHandler =
                    new CommandsHandler(
                            collection,
                            storage,
                            commandLogger,
                            transactionManager,
                            sessionManager);
            var packer = new ResponsePacker();
            var sender = new ResponseSender(connectionHandler.getChannel());
            while (true) {
                Optional<ServerContext> newContext = connectionHandler.get();
                if (newContext.isEmpty()) {
                    continue;
                }

                ServerContext context = newContext.get();
                reader.accept(context);
                authHandler.accept(context);
                if (!context.skipCommandHandling) {
                    commandsHandler.handleRequest(context);
                }
                packer.accept(context);
                sender.accept(context);
            }
        } catch (Exception e) {
            logger.error("Fatal server error", e);
        }
    }
}
