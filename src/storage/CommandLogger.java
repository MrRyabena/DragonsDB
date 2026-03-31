package storage;

import collection.ApiCommand;
import java.util.List;

/**
 * Interface for Write-Ahead Log (WAL) management.
 * Defines operations for logging, committing, and recovering from transaction failures.
 */
public interface CommandLogger {

    /**
     * Logs a command as part of a transaction.
     * The command is written before execution (write-ahead).
     *
     * @param transactionId  transaction/script ID
     * @param command        the command to log
     * @param dragonData     serialized dragon data (for add/update/remove commands)
     * @return               the log entry ID
     */
    long logCommand(long transactionId, ApiCommand command, String dragonData);

    /**
     * Marks a command as successfully committed.
     *
     * @param logId the log entry ID
     */
    void commitCommand(long logId);

    /**
     * Marks a command as rolled back.
     *
     * @param logId the log entry ID
     */
    void rollbackCommand(long logId);

    /**
     * Commits an entire transaction (script execution).
     * All pending commands in the transaction are marked as committed.
     *
     * @param transactionId the transaction ID
     */
    void commitTransaction(long transactionId);

    /**
     * Rolls back an entire transaction.
     * All commands from this transaction are reverted.
     *
     * @param transactionId the transaction ID
     */
    void rollbackTransaction(long transactionId);

    /**
     * Gets all logs for a specific transaction.
     *
     * @param transactionId the transaction ID
     * @return list of command logs
     */
    List<CommandLog> getTransactionLogs(long transactionId);

    /**
     * Gets all pending (not committed) logs from most recent backwards.
     * Used for recovery after crash.
     *
     * @return list of pending logs in reverse chronological order
     */
    List<CommandLog> getPendingLogsForRecovery();

    /**
     * Clears the entire log file.
     * Should be used with caution - typically only after stable checkpoint.
     */
    void clearLog();

    /**
     * Gets the current transaction ID for marking script start.
     *
     * @return new unique transaction ID
     */
    long generateTransactionId();
}
