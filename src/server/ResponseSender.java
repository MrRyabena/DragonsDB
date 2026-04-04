package server;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

public class ResponseSender implements Consumer<ServerContext> {

    public ResponseSender(DatagramChannel channel) {
        this.channel = channel;
    }

    static {
        logger = Logger.getLogger(ResponseSender.class);
    }

    @Override
    public void accept(ServerContext context) {
        try {
            if (channel == null || context.clientAddress == null || context.response == null) {
                return;
            }
            channel.send(context.responseData, context.clientAddress);
        } catch (IOException e) {
            logger.error("Failed to send response", e);
        }

    }

    private final DatagramChannel channel;
    static private Logger logger;
}
