package server;

import org.apache.log4j.Logger;

/**
 * Handles processing of a single client request in a separate thread.
 * Performs all stages: reading request, authentication, command handling, packing response, sending back.
 */
public class ClientRequestHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientRequestHandler.class);

    private final ServerContext context;
    private final RequestReader reader;
    private final AuthenticationHandler authHandler;
    private final CommandsHandler commandsHandler;
    private final ResponsePacker packer;
    private final ResponseSender sender;

    public ClientRequestHandler(
            ServerContext context,
            RequestReader reader,
            AuthenticationHandler authHandler,
            CommandsHandler commandsHandler,
            ResponsePacker packer,
            ResponseSender sender) {
        this.context = context;
        this.reader = reader;
        this.authHandler = authHandler;
        this.commandsHandler = commandsHandler;
        this.packer = packer;
        this.sender = sender;
    }

    @Override
    public void run() {
        String clientInfo = context.clientAddress != null ? context.clientAddress.toString() : "unknown";
        try {
            logger.info("Thread-" + Thread.currentThread().getId() + " processing request from " + clientInfo);

            // Process request pipeline
            reader.accept(context);
            authHandler.accept(context);

            if (!context.skipCommandHandling) {
                commandsHandler.handleRequest(context);
            }

            packer.accept(context);
            sender.accept(context);

            logger.info("Thread-" + Thread.currentThread().getId() + " finished request from " + clientInfo);
        } catch (Exception e) {
            logger.error("Thread-" + Thread.currentThread().getId() + " error processing request from " + clientInfo, e);
        }
    }
}
