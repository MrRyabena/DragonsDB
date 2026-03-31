package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import collection.ApiCommand;

/**
 * File-based implementation of Write-Ahead Log (WAL).
 * Stores command logs in a text file with format:
 * LOGID | TXNID | TIMESTAMP | COMMAND_CODE | STATUS | DRAGON_DATA
 */
public class FileCommandLogger implements CommandLogger {
    private static final String LOG_FILE_NAME = "wal.log";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final String SEPARATOR = " | ";

    private final Path logFilePath;
    private long nextLogId = 1;
    private long nextTransactionId = 1;

    /**
     * Creates a file-based command logger.
     *
     * @param storageDirectory directory where the WAL file will be stored
     */
    public FileCommandLogger(String storageDirectory) {
        try {
            Path path = Paths.get(storageDirectory);
            
            // If the path is a file, use its parent directory
            Path dir;
            if (Files.exists(path) && Files.isRegularFile(path)) {
                dir = path.getParent();
            } else if (Files.exists(path) && Files.isDirectory(path)) {
                dir = path;
            } else {
                // Path doesn't exist - assume it's a directory if it has no extension, otherwise use parent
                if (path.getFileName().toString().contains(".")) {
                    dir = path.getParent();
                } else {
                    dir = path;
                }
            }
            
            // Ensure directory exists
            if (dir != null) {
                Files.createDirectories(dir);
            } else {
                dir = Paths.get(".");
            }
            
            this.logFilePath = dir.resolve(LOG_FILE_NAME);
            
            // Create file if not exists
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
            }
            
            // Initialize IDs based on existing log
            initializeIds();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize WAL file: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize log ID and transaction ID counters from existing file.
     */
    private void initializeIds() {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s*\\|\\s*");
                if (parts.length >= 2) {
                    try {
                        long logId = Long.parseLong(parts[0].trim());
                        long txnId = Long.parseLong(parts[1].trim());
                        nextLogId = Math.max(nextLogId, logId + 1);
                        nextTransactionId = Math.max(nextTransactionId, txnId + 1);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public long logCommand(long transactionId, ApiCommand command, String dragonData) {
        long logId = nextLogId++;
        CommandLog log = new CommandLog(transactionId, logId, command, dragonData);
        appendLogToFile(log);
        return logId;
    }

    @Override
    public void commitCommand(long logId) {
        updateLogStatus(logId, CommandLog.Status.COMMITTED);
    }

    @Override
    public void rollbackCommand(long logId) {
        updateLogStatus(logId, CommandLog.Status.ROLLED_BACK);
    }

    @Override
    public void commitTransaction(long transactionId) {
        List<CommandLog> logs = getTransactionLogs(transactionId);
        for (CommandLog log : logs) {
            if (log.getStatus() == CommandLog.Status.PENDING) {
                commitCommand(log.getLogId());
            }
        }
    }

    @Override
    public void rollbackTransaction(long transactionId) {
        List<CommandLog> logs = getTransactionLogs(transactionId);
        for (CommandLog log : logs) {
            if (log.getStatus() != CommandLog.Status.ROLLED_BACK) {
                rollbackCommand(log.getLogId());
            }
        }
    }

    @Override
    public List<CommandLog> getTransactionLogs(long transactionId) {
        List<CommandLog> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                CommandLog log = parseLogLine(line);
                if (log != null && log.getTransactionId() == transactionId) {
                    result.add(log);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read WAL file", e);
        }
        return result;
    }

    @Override
    public List<CommandLog> getPendingLogsForRecovery() {
        List<CommandLog> allLogs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                CommandLog log = parseLogLine(line);
                if (log != null) {
                    allLogs.add(log);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read WAL file", e);
        }

        // Find pending transactions (those with PENDING commands)
        List<Long> pendingTransactions = allLogs.stream()
                .filter(log -> log.getStatus() == CommandLog.Status.PENDING)
                .map(CommandLog::getTransactionId)
                .distinct()
                .collect(Collectors.toList());

        // Return all logs for pending transactions, in reverse order
        List<CommandLog> result = new LinkedList<>();
        for (long txnId : pendingTransactions) {
            List<CommandLog> txnLogs = getTransactionLogs(txnId);
            result.addAll(0, txnLogs); // Prepend for reverse order
        }
        return result;
    }

    @Override
    public void clearLog() {
        try {
            Files.write(logFilePath, new byte[0]);
            nextLogId = 1;
            nextTransactionId = 1;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clear WAL file", e);
        }
    }

    @Override
    public long generateTransactionId() {
        return nextTransactionId++;
    }

    /**
     * Appends a command log entry to the file.
     */
    private void appendLogToFile(CommandLog log) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath.toFile(), true))) {
            String line = formatLogLine(log);
            writer.append(line).append("\n");
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write to WAL file", e);
        }
    }

    /**
     * Updates the status of a log entry.
     * This rewrites the entire file (inefficient but simple for now).
     */
    private void updateLogStatus(long logId, CommandLog.Status newStatus) {
        List<CommandLog> allLogs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                CommandLog log = parseLogLine(line);
                if (log != null) {
                    if (log.getLogId() == logId) {
                        log.setStatus(newStatus);
                    }
                    allLogs.add(log);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read WAL file", e);
        }

        // Rewrite entire file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath.toFile(), false))) {
            for (CommandLog log : allLogs) {
                writer.append(formatLogLine(log)).append("\n");
            }
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write to WAL file", e);
        }
    }

    /**
     * Formats a log line for storage.
     */
    private String formatLogLine(CommandLog log) {
        return String.format("%d%s%d%s%s%s%s%s%s%s%s",
                log.getLogId(),
                SEPARATOR,
                log.getTransactionId(),
                SEPARATOR,
                log.getTimestamp().format(FORMATTER),
                SEPARATOR,
                log.getCommand().name(),
                SEPARATOR,
                log.getStatus().name(),
                SEPARATOR,
                log.getDragonData() != null ? log.getDragonData() : "NULL");
    }

    /**
     * Parses a log line from the file.
     */
    private CommandLog parseLogLine(String line) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            
            String[] parts = line.split("\\s*\\|\\s*");
            if (parts.length < 5) {
                return null;
            }

            long logId = Long.parseLong(parts[0].trim());
            long txnId = Long.parseLong(parts[1].trim());
            LocalDateTime timestamp = LocalDateTime.parse(parts[2].trim(), FORMATTER);
            ApiCommand command = ApiCommand.valueOf(parts[3].trim());
            CommandLog.Status status = CommandLog.Status.valueOf(parts[4].trim());
            String dragonData = parts.length > 5 && !parts[5].trim().equals("NULL") ? parts[5].trim() : null;

            CommandLog log = new CommandLog(txnId, logId, command, dragonData);
            log.setStatus(status);
            return log;
        } catch (Exception e) {
            // Skip malformed lines
            return null;
        }
    }
}
