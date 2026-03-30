package client;

import collection.ApiCommand;
import dragon.Dragon;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

public class RequestClient implements AutoCloseable {

    public RequestClient(String host, int port) {
        this.serverAddress = new InetSocketAddress(host, port);
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize UDP client", e);
        }
    }

    public byte[] send(ApiCommand command, Stream<Dragon> stream) {
        List<Dragon> dragons = (stream == null ? Stream.<Dragon>empty() : stream).toList();

        try {
            byte[] payload = encode(command, dragons);
            channel.send(ByteBuffer.wrap(payload), serverAddress);

            ByteBuffer responseBuffer = ByteBuffer.allocate(64 * 1024);
            long deadlineNanos = System.nanoTime() + RESPONSE_TIMEOUT.toNanos();
            while (System.nanoTime() < deadlineNanos) {
                var address = channel.receive(responseBuffer);
                if (address != null) {
                    break;
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
            if (responseBuffer.position() == 0) {
                throw new IllegalStateException("Server did not respond within timeout.");
            }
            responseBuffer.flip();
            byte[] response = new byte[responseBuffer.remaining()];
            responseBuffer.get(response);
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send request to server", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for server response", e);
        }
    }

    private byte[] encode(ApiCommand command, List<Dragon> dragons) {
        try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(command);
            for (Dragon dragon : dragons) {
                oos.writeObject(dragon);
            }
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode request payload: " + e.getMessage(), e);
        }
    }

    public static ByteArrayOutputStream toOutputStream(byte[] bytes) {
        var baos = new ByteArrayOutputStream();
        try {
            new ByteArrayInputStream(bytes).transferTo(baos);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert response bytes", e);
        }
        return baos;
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    private final InetSocketAddress serverAddress;
    private final DatagramChannel channel;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(3);
    private static final long POLL_INTERVAL_MS = 20;
}
