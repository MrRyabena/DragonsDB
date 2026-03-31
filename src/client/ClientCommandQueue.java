package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import collection.ApiCommand;

/**
 * Manages a queue of commands waiting to be sent to the server.
 * Commands are persisted to disk so they survive client restarts.
 * Used when server is temporarily unavailable.
 */
public class ClientCommandQueue {
    private static final Logger logger = Logger.getLogger(ClientCommandQueue.class);
    private static final String QUEUE_FILE_NAME = "command_queue.log";
    private static final String SEPARATOR = " | ";

    private final Path queueFilePath;
    private final Queue<BufferedCommand> queue = new LinkedBlockingQueue<>();


    public ClientCommandQueue(String clientCacheDirectory) {
        try {
            Path dir = Paths.get(clientCacheDirectory);
            Files.createDirectories(dir);
            this.queueFilePath = dir.resolve(QUEUE_FILE_NAME);
            
            // Load persisted queue from file
            loadQueueFromFile();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize client command queue", e);
        }
    }

    /**
     * Adds a command to the queue and persists it to disk.
     *
     * @param command the command to queue
     * @param dragonData serialized dragon data (if applicable)
     */
    public void enqueue(ApiCommand command, String dragonData) {
        BufferedCommand buffered = new BufferedCommand(command, dragonData);
        queue.offer(buffered);
        System.out.println("[QUEUE] Added to queue: " + command + " (queue size: " + queue.size() + ")");
        persistCommand(buffered);
    }

    /**
     * Retrieves and removes the next command from the queue.
     *
     * @return the next buffered command, or null if queue is empty
     */
    public BufferedCommand dequeue() {
        BufferedCommand cmd = queue.poll();
        if (cmd != null) {
            removeCommandFromFile(cmd);
        }
        return cmd;
    }

    /**
     * Gets the next command without removing it from the queue.
     *
     * @return the next buffered command, or null if queue is empty
     */
    public BufferedCommand peek() {
        return queue.peek();
    }

    /**
     * Returns the number of commands waiting in the queue.
     *
     * @return queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if no commands are waiting
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns a copy of all queued commands.
     *
     * @return list of all buffered commands
     */
    public List<BufferedCommand> getAllCommands() {
        return new ArrayList<>(queue);
    }

    /**
     * Clears all commands from the queue (after successful batch send).
     */
    public void clear() {
        queue.clear();
        try {
            Files.write(queueFilePath, new byte[0]);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clear command queue file", e);
        }
    }

    /**
     * Persists a single command to the queue file.
     */
    private void persistCommand(BufferedCommand cmd) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(queueFilePath.toFile(), true))) {
            String line = formatCommandLine(cmd);
            writer.append(line).append("\n");
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist command to queue", e);
        }
    }

    /**
     * Formats a command for storage.
     */
    private String formatCommandLine(BufferedCommand cmd) {
        return String.format("%s%s%s%s%d%s%d%s%s",
                cmd.getId(),
                SEPARATOR,
                cmd.getCommand().name(),
                SEPARATOR,
                cmd.getTimestamp(),
                SEPARATOR,
                cmd.getRetryCount(),
                SEPARATOR,
                cmd.getDragonData() != null ? cmd.getDragonData() : "NULL");
    }

    /**
     * Removes a command entry from the queue file by rewriting it.
     */
    private void removeCommandFromFile(BufferedCommand cmd) {
        try {
            List<String> lines = Files.readAllLines(queueFilePath);
            List<String> filtered = new ArrayList<>();

            for (String line : lines) {
                // Only remove the line with matching unique ID
                if (!line.startsWith(cmd.getId() + SEPARATOR)) {
                    filtered.add(line);
                }
            }

            Files.write(queueFilePath, filtered);
        } catch (IOException e) {
            // Log but don't fail - queue in memory is correct
            logger.warn("Failed to update queue file: " + e.getMessage());
        }
    }

    /**
     * Loads the persisted queue from file on startup.
     */
    private void loadQueueFromFile() {
        if (!Files.exists(queueFilePath)) {
            System.out.println("[QUEUE] Queue file does not exist, starting with empty queue");
            return;
        }

        System.out.println("[QUEUE] Loading persisted queue from: " + queueFilePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(queueFilePath.toFile()))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                BufferedCommand cmd = parseCommandLine(line);
                if (cmd != null) {
                    queue.offer(cmd);
                    count++;
                }
            }
            System.out.println("[QUEUE] Loaded " + count + " persisted commands from file");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load command queue from file", e);
        }
    }

    /**
     * Parses a command line from the queue file.
     */
    private BufferedCommand parseCommandLine(String line) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }

            String[] parts = line.split("\\s*\\|\\s*");
            if (parts.length < 5) {
                return null;
            }

            String id = parts[0].trim();
            ApiCommand cmd = ApiCommand.valueOf(parts[1].trim());
            long timestamp = Long.parseLong(parts[2].trim());
            int retryCount = Integer.parseInt(parts[3].trim());
            String dragonData = parts[4].trim().equals("NULL") ? null : parts[4].trim();

            BufferedCommand buffered = new BufferedCommand(id, cmd, timestamp, retryCount, dragonData);
            return buffered;
        } catch (Exception e) {
            // Skip malformed lines
            return null;
        }
    }
}
