package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Optional;
import java.util.function.Supplier;
import core.Defaults;
import org.apache.log4j.Logger;

public class ConnectionHandler implements Supplier<Optional<ServerContext>>, AutoCloseable {

    public ConnectionHandler() {
        logger.info("Starting...");

        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(Defaults.SERVER_PORT));
            channel.configureBlocking(false);
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        } catch (IllegalArgumentException e) {
            logger.error(e.getStackTrace());
        }
        context = new ServerContext();
    }

    static {
        logger = java.util.logging.Logger.getLogger(ConnectionHandler.class);
    }

    @Override
    public Optional<ServerContext> get() {
        try {
            SocketAddress address = channel.receive(context.requestData);

            if (address != null) {
                logger.info("Has new client: (" + address.toString() + ").");

                context.requestData.flip();
                context.clientAddress = address;

                var out = Optional.of(context);
                context = new ServerContext();
                return out;
            }
        } catch (IOException e) {
            logger.error(e.printStackTrace());
        }

        return Optional.empty();
    }

    @Override
    public void close() {
        try {
            channel.close();
            logger.info("Closed.");
        } catch (IOException e) {
            logger.error(e.printStackTrace());
        }
    }

    private DatagramChannel channel;
    private ServerContext context;
    static private Logger logger;
}
