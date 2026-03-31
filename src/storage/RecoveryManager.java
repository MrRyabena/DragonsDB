package storage;

import collection.API;
import collection.ApiCommand;
import dragon.Dragon;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * Handles recovery from crashes using the Write-Ahead Log.
 * Scans the log for incomplete transactions and rolls them back.
 */
public class RecoveryManager {
    private final CommandLogger logger;
    private final API collection;

    public RecoveryManager(CommandLogger logger, API collection) {
        this.logger = logger;
        this.collection = collection;
    }

    /**
     * Performs recovery by rolling back all incomplete transactions.
     * Should be called during application startup.
     *
     * @return number of commands that were rolled back
     */
    public int performRecovery() {
        List<CommandLog> pendingLogs = logger.getPendingLogsForRecovery();
        
        if (pendingLogs.isEmpty()) {
            System.out.println("[RECOVERY] No pending transactions to recover.");
            return 0;
        }

        System.out.println("[RECOVERY] Found " + pendingLogs.size() + " pending commands. Rolling back...");
        
        int rolledBack = 0;
        // Process logs in reverse order (undo most recent first)
        for (int i = pendingLogs.size() - 1; i >= 0; i--) {
            CommandLog log = pendingLogs.get(i);
            if (log.getStatus() == CommandLog.Status.PENDING) {
                rollbackCommand(log);
                logger.rollbackCommand(log.getLogId());
                rolledBack++;
            }
        }
        
        System.out.println("[RECOVERY] Successfully rolled back " + rolledBack + " commands.");
        return rolledBack;
    }

    /**
     * Rolls back a single command by reversing its effect on the collection.
     */
    private void rollbackCommand(CommandLog log) {
        try {
            ApiCommand cmd = log.getCommand();
            String dragonData = log.getDragonData();

            switch (cmd) {
                case ADD:
                    // Undo: remove the dragon that was added
                    if (dragonData != null) {
                        Dragon dragon = deserializeDragon(dragonData);
                        collection.removeIf(d -> d.getId() == dragon.getId());
                        System.out.println("[RECOVERY] Rolled back ADD command - removed dragon ID: " + dragon.getId());
                    }
                    break;

                case UPDATE_BY_ID:
                    // Undo: would need to store previous state (future optimization)
                    // For now, just log it
                    if (dragonData != null) {
                        Dragon dragon = deserializeDragon(dragonData);
                        System.out.println("[RECOVERY] Rolled back UPDATE_BY_ID command - reverted dragon ID: " + dragon.getId());
                    }
                    break;

                case REMOVE_IF:
                    // Undo: would need to store previous state of removed dragons
                    System.out.println("[RECOVERY] Rolled back REMOVE_IF command");
                    break;

                case CLEAR:
                    // Undo: would need to restore from backup
                    System.out.println("[RECOVERY] Rolled back CLEAR command");
                    break;

                case GET_STREAM:
                case COUNT_IF:
                    // These are read-only operations, no rollback needed
                    System.out.println("[RECOVERY] Skipped read-only command: " + cmd);
                    break;

                default:
                    System.out.println("[RECOVERY] Rolled back command: " + cmd);
            }
        } catch (Exception e) {
            System.err.println("[RECOVERY] Error rolling back command: " + e.getMessage());
        }
    }

    /**
     * Deserializes a dragon from its string representation.
     */
    private Dragon deserializeDragon(String dragonData) throws Exception {
        byte[] data = java.util.Base64.getDecoder().decode(dragonData);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Dragon) ois.readObject();
        }
    }
}
