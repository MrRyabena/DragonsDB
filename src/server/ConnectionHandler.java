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
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to initialize UDP channel on port " + Defaults.SERVER_PORT, e);
        }
        context = new ServerContext();
    }

    static {
        logger = Logger.getLogger(ConnectionHandler.class);
    }

    @Override
    public Optional<ServerContext> get() {
        if (channel == null) {
            return Optional.empty();
        }

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
            logger.error("Failed to receive request", e);
        }

        return Optional.empty();
    }

    @Override
    public void close() {
        try {
            channel.close();
            logger.info("Closed.");
        } catch (IOException e) {
            logger.error("Failed to close connection handler", e);
        }
    }

    DatagramChannel getChannel() {
        return channel;
    }

    private DatagramChannel channel;
    private ServerContext context;
    static private Logger logger;
}
