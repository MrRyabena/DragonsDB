package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import core.Defaults;
import core.Request;
import core.Response;
import core.WireFrame;

/**
 * Simplified UDP client for sending commands to the server. Converts command strings and script
 * content into serialized packets and handles responses.
 */
public class RequestClient implements Closeable, AutoCloseable {

    public RequestClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;

        logger.info("Initializing RequestClient...");
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout((int) RESPONSE_TIMEOUT.toMillis());
            logger.info("RequestClient initialized: " + host + ":" + port);
        } catch (IOException e) {
            logger.error("Failed to initialize UDP client", e);
            throw new IllegalStateException("Failed to initialize UDP client", e);
        }
    }

    /**
     * Sends a command string to the server and returns the response.
     *
     * @param commandLine the command line to send
     * @return the server's response as bytes
     */
    public Response sendRequest(Request request) throws IllegalStateException {
        logger.debug("Sending new request");
        Optional<Response> response = Optional.empty();
        try {
            response = send(request);
        } catch (IOException e) {
            logger.error("Failed to send command: " + e.getMessage(), e);
            throw new IllegalStateException("Server unavailable", e);
        }

        if (response.isPresent()) return response.get();
        else throw new IllegalStateException("Received empty response from server");
    }

    /** Sends a command directly to the server via UDP. */
    private Optional<Response> send(Request request) throws IOException {
        drainStalePackets();

        long requestId = nextRequestId.incrementAndGet();
        byte[] payload = WireFrame.wrapRequest(request, requestId).toBytes();
        logger.debug(
                "Encoded payload size: " + payload.length + " bytes (requestId=" + requestId + ")");

        InetAddress serverIp = InetAddress.getByName(serverHost);
        DatagramPacket sendPacket =
                new DatagramPacket(payload, payload.length, serverIp, serverPort);
        socket.send(sendPacket);
        logger.debug("Packet sent to " + serverHost + ":" + serverPort);

        byte[] receive = new byte[64 * 1024]; // 64KB buffer for response
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);
            socket.receive(receivePacket);
            logger.debug("Received " + receivePacket.getLength() + " bytes from server");

            Optional<Response> decoded = decode(receivePacket.getData(), receivePacket.getLength(), requestId);
            if (decoded.isPresent()) {
                return decoded;
            }

            logger.warn("Discarded UDP response with unexpected header or requestId");
        }
    }

    /**
     * Clears delayed UDP packets from previous retries/requests.
     * Without this, stale responses may be read as replies for a different request.
     */
    private void drainStalePackets() {
        int originalTimeout;
        try {
            originalTimeout = socket.getSoTimeout();
        } catch (IOException e) {
            logger.warn("Failed to read socket timeout while draining stale packets", e);
            return;
        }

        int drained = 0;
        try {
            socket.setSoTimeout(1);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[64 * 1024], 64 * 1024);
                socket.receive(packet);
                drained++;
            }
        } catch (java.net.SocketTimeoutException ignored) {
            // Queue drained.
        } catch (IOException e) {
            logger.warn("Failed while draining stale packets", e);
        } finally {
            try {
                socket.setSoTimeout(originalTimeout);
            } catch (IOException e) {
                logger.warn("Failed to restore socket timeout after draining", e);
            }
            if (drained > 0) {
                logger.info("Discarded " + drained + " stale UDP packet(s) before new request");
            }
        }
    }

    private Optional<Response> decode(byte[] raw, int length, long expectedRequestId) {
        try {
            WireFrame frame = WireFrame.fromBytes(raw, length);
            if (frame.kind != WireFrame.Kind.RESPONSE) {
                logger.warn("Ignoring non-response frame: " + frame.kind);
                return Optional.empty();
            }
            if (frame.requestId != expectedRequestId) {
                logger.warn(
                        "Ignoring response for unexpected requestId "
                                + frame.requestId
                                + ", expected "
                                + expectedRequestId);
                return Optional.empty();
            }
            return Optional.of(frame.unwrapResponse());
        } catch (Exception e) {
            logger.error("Failed to decode response frame", e);
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            logger.debug("Closing RequestClient socket");
            socket.close();
        }
    }

    private final String serverHost;
    private final int serverPort;
    private final DatagramSocket socket;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(Defaults.RESPONSE_TIMEOUT);
    private static final Logger logger = Logger.getLogger(RequestClient.class);
    private final AtomicLong nextRequestId = new AtomicLong(0);
}
