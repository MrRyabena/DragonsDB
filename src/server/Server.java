package server;

import java.util.Optional;
import org.apache.log4j.Logger;
import storage.FileStorage;

public class Server {
    public static void main(String[] args) {
        try (var connection_handler = new ConnectionHandler()) {
            var reader = new RequestReader();
            var commands_handler = new CommandsHandler();
            var sender = new ResponseSender();
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
            logger.error(e.getStackTrace());
        }
    }

    static {
        logger = java.util.logging.Logger.getLogger(ConnectionHandler.class);
    }
    static private Logger logger;
}
