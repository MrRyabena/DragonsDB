package client;

import core.Defaults;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;

/**
 * Simplified UDP client for sending commands to the server.
 * Converts command strings and script content into serialized packets and handles responses.
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
    public byte[] send(String commandLine) {
        logger.debug("Sending command: " + commandLine);
        try {
            return sendDirect(commandLine);
        } catch (IOException e) {
            logger.error("Failed to send command: " + e.getMessage(), e);
            throw new IllegalStateException("Server unavailable", e);
        }
    }


    /**
     * Sends a command directly to the server via UDP.
     */
    private byte[] sendDirect(String line) throws IOException {
        byte[] payload = encode(line);
        logger.debug("Encoded payload size: " + payload.length + " bytes");
        
        InetAddress serverIp = InetAddress.getByName(serverHost);
        DatagramPacket sendPacket = new DatagramPacket(payload, payload.length, serverIp, serverPort);
        socket.send(sendPacket);
        logger.debug("Packet sent to " + serverHost + ":" + serverPort);

        byte[] receiveData = new byte[64 * 1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        logger.debug("Received " + receivePacket.getLength() + " bytes from server");

        byte[] response = new byte[receivePacket.getLength()];
        System.arraycopy(receiveData, 0, response, 0, receivePacket.getLength());
        return response;
    }


    /**
     * Encodes a command line as a serialized object.
     */
    private byte[] encode(String commandLine) throws IOException {
        try (var baos = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(commandLine);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to encode command: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to encode request payload: " + e.getMessage(), e);
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
}

