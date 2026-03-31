package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import collection.ApiCommand;
import dragon.Dragon;

public class RequestClient implements AutoCloseable {

    public RequestClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.commandQueue = new ClientCommandQueue("./.client_cache");
        System.out.println("[CLIENT] Loading persisted command queue...");
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout((int) RESPONSE_TIMEOUT.toMillis());
            System.out.println("[CLIENT] RequestClient initialized: " + host + ":" + port);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize UDP client", e);
        }
    }

    public byte[] send(ApiCommand command, Stream<Dragon> stream) {
        List<Dragon> dragons = (stream == null ? Stream.<Dragon>empty() : stream).toList();
        String dragonData = serializeDragons(dragons);

        // If there are previously buffered commands and the server is reachable, flush them first.
        if (!commandQueue.isEmpty()) {
            try {
                int flushed = flushBufferedCommands();
                if (flushed > 0) {
                    System.out.println("[CLIENT] Flushed " + flushed + " buffered commands before processing: " + command);
                }
            } catch (Exception e) {
                System.out.println("[CLIENT] Unable to flush queued commands before sending current command: " + e.getMessage());
            }
        }

        try {
            // Try to send directly to server
            return sendDirect(command, dragons);
        } catch (IOException e) {
            if (isBufferableCommand(command)) {
                System.out.println("[CLIENT] Server unavailable, buffering command: " + command);
                commandQueue.enqueue(command, dragonData);
                return new byte[0];
            } else {
                System.out.println("[CLIENT] Server unavailable, cannot buffer command: " + command);
                throw new IllegalStateException("Server unavailable (read-only command): " + command, e);
            }
        }
    }

    private boolean isBufferableCommand(ApiCommand command) {
        return switch (command) {
            case ADD, UPDATE_BY_ID, CLEAR, REMOVE_IF -> true;
            case GET_STREAM, COUNT_IF -> false;
        };
    }

    /**
     * Sends a command directly to the server.
     */
    private byte[] sendDirect(ApiCommand command, List<Dragon> dragons) throws IOException {
        byte[] payload = encode(command, dragons);
        InetAddress serverIp = InetAddress.getByName(serverHost);
        DatagramPacket sendPacket = new DatagramPacket(payload, payload.length, serverIp, serverPort);
        socket.send(sendPacket);

        byte[] receiveData = new byte[64 * 1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        byte[] response = new byte[receivePacket.getLength()];
        System.arraycopy(receiveData, 0, response, 0, receivePacket.getLength());
        return response;
    }

    /**
     * Attempts to flush all buffered commands to the server.
     * Returns the number of successfully sent commands.
     */
    public int flushBufferedCommands() {
        if (commandQueue.isEmpty()) {
            System.out.println("[CLIENT] Command queue is empty, nothing to flush.");
            return 0;
        }

        System.out.println("[CLIENT] Attempting to flush " + commandQueue.size() + " buffered commands...");
        int sent = 0;
        int failed = 0;

        while (!commandQueue.isEmpty()) {
            BufferedCommand buffered = commandQueue.peek();
            try {
                System.out.println("[CLIENT] Sending buffered: " + buffered.getCommand() + " (attempt " + (buffered.getRetryCount() + 1) + ")");
                
                List<Dragon> dragons = List.of();
                if (buffered.getDragonData() != null) {
                    try {
                        dragons = deserializeDragons(buffered.getDragonData());
                        System.out.println("[CLIENT] Deserialized " + dragons.size() + " dragons for command");
                    } catch (Exception e) {
                        System.err.println("[CLIENT] Failed to deserialize dragons: " + e.getMessage());
                        e.printStackTrace();
                        dragons = List.of();
                    }
                }
                
                byte[] response = sendDirect(buffered.getCommand(), dragons);
                
                commandQueue.dequeue(); // Remove from queue after success
                sent++;
                System.out.println("[CLIENT] Successfully sent buffered command: " + buffered.getCommand());
            } catch (IOException e) {
                System.out.println("[CLIENT] IOException while flushing: " + e.getMessage());
                buffered.incrementRetryCount();
                System.out.println("[CLIENT] Server still unavailable, stopping flush");
                break;
            } catch (Exception e) {
                System.err.println("[CLIENT] Unexpected error while flushing: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                buffered.incrementRetryCount();
                if (buffered.getRetryCount() > MAX_RETRIES) {
                    commandQueue.dequeue();
                    failed++;
                } else {
                    break;
                }
            }
        }

        System.out.println("[CLIENT] Flush complete: " + sent + " sent, " + failed + " failed, " + 
            commandQueue.size() + " remaining in queue");
        return sent;
    }

    private byte[] encode(ApiCommand command, List<Dragon> dragons) throws IOException {
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

    /**
     * Returns the number of buffered commands waiting to be sent.
     */
    public int getBufferedCommandCount() {
        return commandQueue.size();
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Serializes a list of dragons to a string.
     */
    private String serializeDragons(List<Dragon> dragons) {
        if (dragons == null || dragons.isEmpty()) {
            return null;
        }
        try {
            var baos = new ByteArrayOutputStream();
            var oos = new ObjectOutputStream(baos);
            oos.writeInt(dragons.size());
            for (Dragon dragon : dragons) {
                oos.writeObject(dragon);
            }
            oos.flush();
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Deserializes dragons from a string.
     */
    private List<Dragon> deserializeDragons(String data) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(data);
            var bais = new ByteArrayInputStream(bytes);
            var ois = new java.io.ObjectInputStream(bais);
            int size = ois.readInt();
            List<Dragon> result = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                result.add((Dragon) ois.readObject());
            }
            return result;
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    private final String serverHost;
    private final int serverPort;
    private final DatagramSocket socket;
    private final ClientCommandQueue commandQueue;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(3);
    private static final int MAX_RETRIES = 3;
}
