package server;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        int serverPort = parseServerPort(args);

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

        // Create thread pool: use number of CPU cores as thread count
        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        logger.info("Thread pool created with " + threadPoolSize + " threads");

        logger.info("Starting server on UDP port: " + serverPort);

        try (var connectionHandler = new ConnectionHandler(serverPort)) {
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

                // Submit client request handling to thread pool
                ClientRequestHandler handler = new ClientRequestHandler(
                        context,
                        reader,
                        authHandler,
                        commandsHandler,
                        packer,
                        sender);
                executorService.submit(handler);
            }
        } catch (Exception e) {
            logger.error("Fatal server error", e);
        } finally {
            // Graceful shutdown of thread pool
            logger.info("Shutting down thread pool...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Thread pool did not terminate in time, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for thread pool shutdown", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Server stopped");
        }
    }

    private static int parseServerPort(String[] args) {
        int serverPort = Defaults.SERVER_PORT;

        if (args == null) {
            return serverPort;
        }

        for (String arg : args) {
            if (arg == null) {
                continue;
            }

            if (arg.startsWith("--port=")) {
                String value = arg.substring("--port=".length());
                serverPort = parsePortValue(value, serverPort);
                continue;
            }

            if ("--port".equals(arg)) {
                logger.warn("Use --port=<value> format. Falling back to default port: " + serverPort);
            }
        }

        return serverPort;
    }

    private static int parsePortValue(String value, int fallbackPort) {
        try {
            int parsedPort = Integer.parseInt(value);
            if (parsedPort <= 0 || parsedPort > 65535) {
                logger.warn("Invalid server port: " + value + ", using default: " + fallbackPort);
                return fallbackPort;
            }
            return parsedPort;
        } catch (NumberFormatException e) {
            logger.warn("Invalid server port: " + value + ", using default: " + fallbackPort);
            return fallbackPort;
        }
    }
}
