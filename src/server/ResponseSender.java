package server;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

/**
 * Sends a pre-packed response datagram to the client endpoint from {@link ServerContext}.
 */
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
            // Guard against partially built pipeline state.
            if (channel == null
                    || context.clientAddress == null
                    || context.response == null
                    || context.responseData == null
                    || !context.responseData.hasRemaining()) {
                return;
            }
            // UDP send is one-shot for the current response buffer.
            channel.send(context.responseData, context.clientAddress);
        } catch (IOException e) {
            logger.error("Failed to send response", e);
        }

    }

    private final DatagramChannel channel;
    private static final Logger logger;
}
