package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;

import org.apache.log4j.Logger;

import core.Defaults;
import core.Request;
import core.Response;

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
        byte[] payload = encode(request);
        logger.debug("Encoded payload size: " + payload.length + " bytes");

        InetAddress serverIp = InetAddress.getByName(serverHost);
        DatagramPacket sendPacket =
                new DatagramPacket(payload, payload.length, serverIp, serverPort);
        socket.send(sendPacket);
        logger.debug("Packet sent to " + serverHost + ":" + serverPort);

        byte[] receive = new byte[64 * 1024]; // 64KB buffer for response
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);
        socket.receive(receivePacket);
        logger.debug("Received " + receivePacket.getLength() + " bytes from server");

        return decode(receivePacket.getData());
    }

    /** Encodes a command line as a serialized object. */
    private byte[] encode(Request request) throws IOException {
        try (var baos = new ByteArrayOutputStream();
                var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(request);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to encode command: " + e.getMessage(), e);
            throw new IllegalStateException(
                    "Failed to encode request payload: " + e.getMessage(), e);
        }
    }

    private Optional<Response> decode(byte[] raw) {
        try (var bais = new ByteArrayInputStream(raw);
                var ois = new ObjectInputStream(bais)) {
            return Optional.of((Response) ois.readObject());
        } catch (Exception e) {
            logger.error(e.getStackTrace());
        }
        return Optional.empty();
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
}
