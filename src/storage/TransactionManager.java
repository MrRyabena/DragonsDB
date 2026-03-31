package storage;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages transaction lifecycle during script execution.
 * Tracks nested transactions and handles commit/rollback.
 */
public class TransactionManager {
    private final CommandLogger logger;
    private final Deque<Long> transactionStack = new ArrayDeque<>();
    private long currentTransactionId = 0;

    public TransactionManager(CommandLogger logger) {
        this.logger = logger;
    }

    /**
     * Begins a new transaction (e.g., when starting a script).
     * Supports nested transactions.
     *
     * @return the transaction ID
     */
    public long beginTransaction() {
        long txnId = logger.generateTransactionId();
        transactionStack.push(txnId);
        currentTransactionId = txnId;
        return txnId;
    }

    /**
     * Commits the current transaction.
     *
     * @return true if a transaction was committed, false if no active transaction
     */
    public boolean commitTransaction() {
        if (transactionStack.isEmpty()) {
            return false;
        }
        long txnId = transactionStack.pop();
        logger.commitTransaction(txnId);
        currentTransactionId = transactionStack.peek() != null ? transactionStack.peek() : 0;
        return true;
    }

    /**
     * Rolls back the current transaction.
     *
     * @return true if a transaction was rolled back, false if no active transaction
     */
    public boolean rollbackTransaction() {
        if (transactionStack.isEmpty()) {
            return false;
        }
        long txnId = transactionStack.pop();
        logger.rollbackTransaction(txnId);
        currentTransactionId = transactionStack.peek() != null ? transactionStack.peek() : 0;
        return true;
    }

    /**
     * Gets the current active transaction ID.
     * If no transaction is active, returns 0.
     *
     * @return the transaction ID
     */
    public long getCurrentTransactionId() {
        return currentTransactionId;
    }

    /**
     * Checks if a transaction is currently active.
     *
     * @return true if inside a transaction
     */
    public boolean isInTransaction() {
        return !transactionStack.isEmpty();
    }

    /**
     * Gets the transaction nesting depth.
     *
     * @return nesting level (0 = no transaction)
     */
    public int getTransactionDepth() {
        return transactionStack.size();
    }
}
