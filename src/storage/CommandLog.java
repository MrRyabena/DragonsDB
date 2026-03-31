package storage;

import java.io.Serializable;
import java.time.LocalDateTime;

import collection.ApiCommand;

/**
 * Represents a single command entry in the Write-Ahead Log (WAL). Contains metadata about a command
 * modification to the collection.
 */
public class CommandLog implements Serializable {

    public enum Status {
        PENDING, // Command logged but not yet executed
        COMMITTED, // Command successfully executed and committed
        ROLLED_BACK // Command was rolled back
    }

    public CommandLog(long transactionId, long logId, ApiCommand command, String dragonData) {
        this.transactionId = transactionId;
        this.logId = logId;
        this.timestamp = LocalDateTime.now();
        this.command = command;
        this.dragonData = dragonData;
        this.status = Status.PENDING;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public long getLogId() {
        return logId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ApiCommand getCommand() {
        return command;
    }

    public String getDragonData() {
        return dragonData;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format(
                "[%d] TXN:%d CMD:%s STATUS:%s TIME:%s DRAGON:%s",
                logId,
                transactionId,
                command.name(),
                status,
                timestamp,
                dragonData != null
                        ? dragonData.substring(0, Math.min(30, dragonData.length()))
                        : "null");
    }

    private static final long serialVersionUID = 1L;
    private final long transactionId; // Groups commands from same script
    private final long logId; // Unique log entry ID
    private final LocalDateTime timestamp;
    private final ApiCommand command;
    private final String dragonData; // Serialized dragon or null
    private Status status;
}
