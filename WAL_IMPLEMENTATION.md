# Write-Ahead Log (WAL) Implementation Guide

## Архитектура

### Компоненты

1. **CommandLog** - представляет одну запись в логе с метаданными
   - `transactionId` - группирует команды из одного скрипта
   - `logId` - уникальный идентификатор записи
   - `status` - PENDING, COMMITTED, ROLLED_BACK

2. **CommandLogger** (интерфейс) - определяет операции логирования
   - `logCommand()` - добавить команду до выполнения (write-ahead)
   - `commitCommand()` - пометить как успешно выполненную
   - `rollbackCommand()` - откатить команду

3. **FileCommandLogger** - реализация на файловой системе
   - Хранит логи в `wal.log` в текстовом формате
   - Восстанавливает ID счетчиков при старте

4. **TransactionManager** - управляет транзакциями
   - `beginTransaction()` - начать транзакцию при запуске скрипта
   - `commitTransaction()` - завершить с успехом
   - `rollbackTransaction()` - откатить все команды транзакции

5. **RecoveryManager** - восстановление после краша
   - `performRecovery()` - сканирует логи и откатывает незавершенные транзакции
   - Запускается при старте приложения

6. **ClientCommandQueue** - очередь команд на клиенте
   - Буферизирует команды при недоступности сервера
   - Персистирует в `command_queue.log`
   - Отправляет при восстановлении соединения

7. **BufferedCommand** - команда в очереди ожидания


## Как использовать на СЕРВЕРЕ

### 1. Инициализация при запуске
```java
public class Server {
    private CommandLogger commandLogger;
    private TransactionManager txnManager;
    private RecoveryManager recovery;

    public void start(int port) throws IOException {
        // Инициализировать логгер
        commandLogger = new FileCommandLogger("./data");
        txnManager = new TransactionManager(commandLogger);
        recovery = new RecoveryManager(commandLogger, collection);
        
        // Выполнить восстановление после возможного краша
        recovery.performRecovery();
        
        // ... остальной код сервера
    }
}
```

### 2. Логирование команды перед выполнением
```java
private void handleCommand(ApiCommand cmd, Dragon dragon) {
    // Получить текущую транзакцию (обычно 0 для отдельных команд)
    long txnId = txnManager.getCurrentTransactionId();
    if (txnId == 0) {
        txnId = txnManager.beginTransaction(); // Создать фиксированную транзакцию
    }
    
    try {
        // Log BEFORE execution (write-ahead)
        String dragonData = serializeDragon(dragon);
        long logId = commandLogger.logCommand(txnId, cmd, dragonData);
        
        // Execute command
        executeCommand(cmd, dragon);
        
        // Commit log entry
        commandLogger.commitCommand(logId);
    } catch (Exception e) {
        // Rollback log entry
        commandLogger.rollbackCommand(logId);
        throw e;
    }
}
```

### 3. При выполнении скрипта (транзакция)
```java
private void executeScript(List<ApiCommand> commands) {
    long txnId = txnManager.beginTransaction();
    System.out.println("Started transaction: " + txnId);
    
    try {
        for (ApiCommand cmd : commands) {
            // Log and execute each command
            long logId = commandLogger.logCommand(txnId, cmd, null);
            executeCommand(cmd);
            commandLogger.commitCommand(logId);
        }
        // All commands executed successfully
        txnManager.commitTransaction();
    } catch (Exception e) {
        // Rollback entire transaction
        txnManager.rollbackTransaction();
        System.out.println("Transaction rolled back due to error");
        throw e;
    }
}
```


## Как использовать на КЛИЕНТЕ

### 1. Инициализация
```java
public class Client {
    private ClientCommandQueue commandQueue;
    private RequestClient requestClient;

    public Client(String host, int port) {
        commandQueue = new ClientCommandQueue("./client_cache");
        requestClient = new RequestClient(host, port);
    }
}
```

### 2. Отправка команды с fallback на очередь
```java
public byte[] sendCommandWithFallback(ApiCommand cmd, String dragonData) {
    try {
        // Try to send to server
        return requestClient.send(cmd, dragonData);
    } catch (IOException e) {
        // Server unavailable - buffer the command
        System.out.println("Server unavailable, buffering command: " + cmd);
        commandQueue.enqueue(cmd, dragonData);
        return null;
    }
}
```

### 3. Восстановление соединения
```java
public void flushBufferedCommands() {
    int sent = 0;
    while (!commandQueue.isEmpty()) {
        BufferedCommand buffered = commandQueue.peek();
        try {
            byte[] response = requestClient.send(
                buffered.getCommand(), 
                buffered.getDragonData()
            );
            
            commandQueue.dequeue(); // Remove from queue after success
            sent++;
        } catch (IOException e) {
            buffered.incrementRetryCount();
            if (buffered.getRetryCount() > MAX_RETRIES) {
                System.err.println("Command failed after retries: " + buffered);
                commandQueue.dequeue(); // Give up
            }
            break; // Stop trying if connection still fails
        }
    }
    System.out.println("Flushed " + sent + " buffered commands");
}
```


## Формат файла wal.log

```
LOGID | TXNID | TIMESTAMP | COMMAND | STATUS | DRAGON_DATA
1 | 100 | 2024-03-31T10:30:45.123 | ADD | COMMITTED | <serialized_dragon>
2 | 100 | 2024-03-31T10:30:46.456 | UPDATE_BY_ID | COMMITTED | <serialized_dragon>
3 | 101 | 2024-03-31T10:30:47.789 | REMOVE_BY_ID | PENDING | NULL
```

При падении во время выполнения транзакции 101, logId 3 останется в статусе PENDING и будет откачен при следующем старте.


## Сценария использования

### Сценарий 1: Нормальное выполнение команды
```
1. Client: execute add dragon
2. Server: BEGIN TRANSACTION 100
3. Server: LOG_COMMAND(100, ADD, dragon_data)  -> logId=5
4. Server: EXECUTE ADD dragon
5. Server: COMMIT logId=5
6. Server: COMMIT TRANSACTION 100
```

### Сценарий 2: Падение сервера во время выполнения
```
1. Client: execute script with 3 commands
2. Server: BEGIN TRANSACTION 101
3. Server: LOG COMMAND 1 -> logId=6
4. Server: EXECUTE COMMAND 1 ✓
5. Server: COMMIT logId=6
6. Server: LOG COMMAND 2 -> logId=7
7. Server: EXECUTE COMMAND 2 ✓
8. Server: COMMIT logId=7
9. Server: LOG COMMAND 3 -> logId=8
10. Server: EXECUTE COMMAND 3 <- SERVER CRASHES HERE
11. [Server restarts]
12. Server: RECOVERY MANAGER scans wal.log
13. Server: Found PENDING transaction 101 with commands 6,7,8
14. Server: ROLLBACK commands in reverse: 8, 7, 6
15. Collection restored to state before transaction 101 started
```

### Сценарий 3: Недоступность сервера на клиенте
```
1. Client: add dragon
2. Client: Try to send to server -> IOException
3. Client: Buffer command in ClientCommandQueue
4. Client: "Server currently unavailable"
5. [Later, when server is back]
6. Client: Detect server is responsive
7. Client: flushBufferedCommands()
8. Client: Send all buffered commands in order
9. Server: Process and commit each command with logging
```


## Оптимизации (для будущего)

1. **Checkpoints** - периодически сохранять "снимок" состояния и очищать старые логи
2. **Binary format** - вместо текстового для ускорения чтения/записи
3. **MVCC** - multi-version concurrency control для параллельных команд
4. **Command undo/redo** - сохранять предыдущее состояние для UPDATE/REMOVE
5. **Async logging** - писать логи в отдельном потоке для производительности
6. **Log rotation** - разбивать большие логи на несколько файлов


## Интеграция с существующим кодом

### В TextUIHandler.java
```java
public class TextUIHandler implements Runnable {
    private TransactionManager txnManager;
    
    @Override
    public void run() {
        long scriptTxnId = txnManager.beginTransaction();
        try {
            // ... execute commands from script
            txnManager.commitTransaction();
        } catch (Exception e) {
            txnManager.rollbackTransaction();
            throw e;
        }
    }
}
```

### В Server.java
```java
public class Server {
    private CommandLogger commandLogger;
    
    private void executeCommand(ApiCommand cmd, Dragon dragon) {
        try {
            long txnId = commandLogger.generateTransactionId();
            long logId = commandLogger.logCommand(txnId, cmd, serializeDragon(dragon));
            
            collection.add(dragon); // или другая операция
            
            commandLogger.commitCommand(logId);
        } catch (Exception e) {
            commandLogger.rollbackCommand(logId);
            throw e;
        }
    }
}
```
