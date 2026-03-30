package server;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.function.Consumer;

public class ResponseSender implements Consumer<ServerContext> {

    public ResponseSender() {
    }
    
        static {
        logger = java.util.logging.Logger.getLogger(ConnectionHandler.class);
    }

    @Override
    public void accept(ServerContext context) {
        try (var channel = DatagramChannel.open()) {
            channel.bind(context.clientAddress);
            channel.configureBlocking(false);
            channel.write(context.response);
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        }

    }
    static private Logger logger;
}
