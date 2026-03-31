package client;

import collection.ApiCommand;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a command buffered on the client while waiting for server availability.
 */
public class BufferedCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final ApiCommand command;
    private final String dragonData;  // Serialized dragon data
    private final long timestamp;
    private int retryCount = 0;

    public BufferedCommand(ApiCommand command, String dragonData) {
        this.id = java.util.UUID.randomUUID().toString();
        this.command = Objects.requireNonNull(command);
        this.dragonData = dragonData;
        this.timestamp = System.currentTimeMillis();
    }

    public BufferedCommand(String id, ApiCommand command, long timestamp, int retryCount, String dragonData) {
        this.id = Objects.requireNonNull(id);
        this.command = Objects.requireNonNull(command);
        this.timestamp = timestamp;
        this.retryCount = retryCount;
        this.dragonData = dragonData;
    }

    public String getId() {
        return id;
    }

    public ApiCommand getCommand() {
        return command;
    }

    public String getDragonData() {
        return dragonData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void incrementRetryCount() {
        retryCount++;
    }

    @Override
    public String toString() {
        return String.format("BufferedCommand{id=%s, cmd=%s, retries=%d, age=%dms}",
                id, command.name(), retryCount, System.currentTimeMillis() - timestamp);
    }
}
