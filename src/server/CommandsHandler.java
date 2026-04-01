package server;

import collection.API;
import collection.ApiCommand;
import dragon.Dragon;
import dragon.view.JsonView;
import dragon.view.StringView;
import dragon.view.View;
import storage.CommandLogger;
import storage.Storage;
import storage.TransactionManager;
import ui.Commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

/**
 * Processes commands from the client and manages collection operations.
 * Parses command line strings and executes appropriate actions.
 */
public class CommandsHandler implements Consumer<ServerContext> {

    public CommandsHandler(
            API collection,
            Storage storage,
            CommandLogger commandLogger,
            TransactionManager transactionManager) {
        this.collection = collection;
        this.storage = storage;
        this.commandLogger = commandLogger;
        this.transactionManager = transactionManager;
        this.storageView = new JsonView();
        this.outView = new StringView();
        this.logger = Logger.getLogger(CommandsHandler.class);
    }

    @Override
    public void accept(ServerContext context) {
        if (context.commandLine == null || context.commandLine.isBlank()) {
            context.response = textResponse("Invalid request: command is missing.");
            return;
        }

        // Parse command
        String[] parts = context.commandLine.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";

        Commands command = null;
        for (Commands c : Commands.values()) {
            if (c.getText().equals(cmd)) {
                command = c;
                break;
            }
        }
        if (command == null) {
            context.response = textResponse("Unknown command: " + cmd);
            return;
        }

        // Log command before execution (write-ahead)
        long txnId = transactionManager.getCurrentTransactionId();
        if (txnId == 0) {
            txnId = transactionManager.beginTransaction();
        }
        long logId = commandLogger.logCommand(txnId, ApiCommand.valueOf(command.name()), null);

        try {
            StringBuilder output = new StringBuilder();
            
            switch (command) {
                case HELP -> {
                    output.append("Available commands:\n");
                    output.append("  help - show this message\n");
                    output.append("  info - show collection info\n");
                    output.append("  show - show all dragons\n");
                    output.append("  add - add a new dragon (input via 'add' prompt)\n");
                    output.append("  update_by_id <id> - update dragon by id\n");
                    output.append("  remove_by_id <id> - remove dragon by id\n");
                    output.append("  clear - clear the collection\n");
                    output.append("  save - save the collection\n");
                    output.append("  exit - exit the application\n");
                    output.append("  remove_greater - remove dragons greater than given\n");
                    output.append("  remove_lower - remove dragons lower than given\n");
                    output.append("  count_by_type <type> - count dragons by type\n");
                    output.append("  filter_starts_with_name <prefix> - filter by name prefix\n");
                    output.append("  execute_script <file> - execute commands from file\n");
                }
                case INFO -> {
                    output.append("Collection type: HashSet<Dragon>\n");
                    output.append("Date created: ");
                    var date = storage.getDateCreated();
                    output.append(date != null ? date.toString() : "unknown");
                    output.append("\nDate modified: ");
                    var dateModified = storage.getDateModified();
                    output.append(dateModified != null ? dateModified.toString() : "unknown");
                    output.append("\nNumber of elements: ");
                    output.append(collection.countIf(e -> true));
                    output.append("\n");
                }
                case SHOW -> {
                    String collectionView = viewToString(outView.toView(collection.getStream()));
                    output.append(collectionView);
                }
                case CLEAR -> {
                    collection.clear();
                    saveCollection();
                    output.append("Collection cleared.");
                }
                case SAVE -> {
                    saveCollection();
                    output.append("Collection saved.");
                }
                case EXIT -> {
                    output.append("Goodbye!");
                }
                case FILTER_STARTS_WITH_NAME -> {
                    if (args.isEmpty()) {
                        output.append("Error: You must provide a name prefix.\n");
                        break;
                    }
                    String nameStart = args;
                    String filtered = viewToString(
                        outView.toView(
                            collection.getStream()
                                .filter(e -> e.getName().startsWith(nameStart))
                        )
                    );
                    output.append(filtered);
                }
                case COUNT_BY_TYPE -> {
                    if (args.isEmpty()) {
                        output.append("Error: You must provide a dragon type.\n");
                        break;
                    }
                    try {
                        dragon.DragonType dragonType = dragon.DragonType.valueOf(args.toUpperCase());
                        int count = collection.countIf(d -> d.getType() == dragonType);
                        output.append(count);
                    } catch (IllegalArgumentException e) {
                        output.append("Error: Invalid dragon type '" + args + "'");
                    }
                }
                case REMOVE_GREATER -> {
                    if (args.isEmpty()) {
                        output.append("INPUT: remove_greater");
                        break;
                    }
                    try {
                        Dragon sample = parseDragonFromCsv(args);
                        int before = collection.countIf(d -> true);
                        collection.removeIf(d -> d.compareTo(sample) > 0);
                        int after = collection.countIf(d -> true);
                        saveCollection();
                        output.append("Removed greater dragons: ").append(before - after);
                    } catch (Exception e) {
                        output.append("Error: " + e.getMessage());
                    }
                }
                case REMOVE_LOWER -> {
                    if (args.isEmpty()) {
                        output.append("INPUT: remove_lower");
                        break;
                    }
                    try {
                        Dragon sample = parseDragonFromCsv(args);
                        int before = collection.countIf(d -> true);
                        collection.removeIf(d -> d.compareTo(sample) < 0);
                        int after = collection.countIf(d -> true);
                        saveCollection();
                        output.append("Removed lower dragons: ").append(before - after);
                    } catch (Exception e) {
                        output.append("Error: " + e.getMessage());
                    }
                }
                case COUNT_GREATER_THAN_TYPE -> {
                    if (args.isEmpty()) {
                        output.append("INPUT: count_greater_than_type");
                        break;
                    }
                    try {
                        dragon.DragonType dragonType = dragon.DragonType.valueOf(args.toUpperCase());
                        int count = collection.countIf(d -> d.getType() != null && d.getType().compareTo(dragonType) > 0);
                        output.append(count);
                    } catch (Exception e) {
                        output.append("Error: " + e.getMessage());
                    }
                }
                case UPDATE_BY_ID -> {
                    if (args.isEmpty()) {
                        output.append("INPUT: update_by_id");
                        break;
                    }
                    // Expect args as: id,name,x,y,age,weight,type,headSize,headToothCount
                    String[] updateArgs = args.split("\\s*,\\s*");
                    if (updateArgs.length != 9) {
                        output.append("Error: update_by_id requires 9 values (id,name,x,y,age,weight,type,headSize,headToothCount)");
                        break;
                    }
                    try {
                        long id = Long.parseLong(updateArgs[0]);
                        String name = updateArgs[1];
                        float x = Float.parseFloat(updateArgs[2]);
                        float y = Float.parseFloat(updateArgs[3]);
                        int age = Integer.parseInt(updateArgs[4]);
                        long weight = Long.parseLong(updateArgs[5]);
                        String typeName = updateArgs[6];
                        float headSize = Float.parseFloat(updateArgs[7]);
                        int headToothCount = Integer.parseInt(updateArgs[8]);

                        var coordinates = new core.Coordinates(x, y);
                        dragon.DragonType dragonType = null;
                        if (!typeName.isBlank()) {
                            dragonType = dragon.DragonType.valueOf(typeName.toUpperCase());
                        }
                        var head = new dragon.DragonHead(headSize, headToothCount);
                        var dragon = new dragon.Dragon(name, coordinates, age, weight, dragonType, head);
                        // force id is not available; we update by removing existing and adding new one using provided id
                        collection.removeIf(d -> d.getId() == id);
                        collection.add(dragon);
                        saveCollection();
                        output.append("Dragon updated by ID: ").append(id);
                    } catch (Exception e) {
                        output.append("Error: Could not update dragon: ").append(e.getMessage());
                    }
                }
                case REMOVE_BY_ID -> {
                    if (args.isEmpty()) {
                        output.append("Error: You must provide a dragon ID.\n");
                        break;
                    }
                    try {
                        long id = Long.parseLong(args);
                        int before = collection.countIf(d -> true);
                        collection.removeIf(d -> d.getId() == id);
                        int after = collection.countIf(d -> true);
                        if (before == after) {
                            output.append("Dragon with ID ").append(id).append(" not found.");
                        } else {
                            saveCollection();
                            output.append("Dragon with ID ").append(id).append(" removed.");
                        }
                    } catch (NumberFormatException e) {
                        output.append("Error: Invalid ID format.");
                    }
                }
                case ADD -> {
                    if (args.isEmpty()) {
                        output.append("INPUT: add");
                        break;
                    }

                    // Expect args as comma-separated values: name,x,y,age,weight,type,headSize,headToothCount
                    String[] addArgs = args.split("\\s*,\\s*");
                    if (addArgs.length != 8) {
                        output.append("Error: add requires 8 values (name,x,y,age,weight,type,headSize,headToothCount)\n");
                        break;
                    }

                    try {
                        String name = addArgs[0];
                        float x = Float.parseFloat(addArgs[1]);
                        float y = Float.parseFloat(addArgs[2]);
                        int age = Integer.parseInt(addArgs[3]);
                        long weight = Long.parseLong(addArgs[4]);
                        String typeName = addArgs[5];
                        float headSize = Float.parseFloat(addArgs[6]);
                        int headToothCount = Integer.parseInt(addArgs[7]);

                        var coordinates = new core.Coordinates(x, y);
                        dragon.DragonType dragonType = null;
                        if (!typeName.isBlank()) {
                            dragonType = dragon.DragonType.valueOf(typeName.toUpperCase());
                        }
                        var head = new dragon.DragonHead(headSize, headToothCount);
                        var dragon = new dragon.Dragon(name, coordinates, age, weight, dragonType, head);

                        collection.add(dragon);
                        saveCollection();
                        output.append("Dragon added: ").append(name);
                    } catch (Exception e) {
                        output.append("Error: Could not add dragon: ").append(e.getMessage());
                    }
                }
                case EXECUTE_SCRIPT -> {
                    if (context.scriptContent == null || context.scriptContent.isBlank()) {
                        output.append("Error: No script content provided.");
                        break;
                    }

                    output.append("Executing script:\n");
                    String[] lines = context.scriptContent.split("\\r?\\n");
                    int i = 0;
                    while (i < lines.length) {
                        String line = lines[i].trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            i++;
                            continue;
                        }

                        output.append("> ").append(line).append("\n");

                        if (line.equalsIgnoreCase("add")) {
                            int neededLines = 8;
                            if (i + neededLines >= lines.length) {
                                output.append("  Error: Not enough data for add command in script.\n");
                                break;
                            }
                            try {
                                String name = lines[++i].trim();
                                float x = Float.parseFloat(lines[++i].trim());
                                double y = Double.parseDouble(lines[++i].trim());
                                int age = Integer.parseInt(lines[++i].trim());
                                long weight = Long.parseLong(lines[++i].trim());
                                String typeName = lines[++i].trim();
                                float headSize = Float.parseFloat(lines[++i].trim());
                                int headToothCount = Integer.parseInt(lines[++i].trim());

                                var coordinates = new core.Coordinates(x, (float) y);
                                dragon.DragonType dragonType = null;
                                if (!typeName.isBlank()) {
                                    dragonType = dragon.DragonType.valueOf(typeName.toUpperCase());
                                }
                                var head = new dragon.DragonHead(headSize, headToothCount);
                                var dragon = new dragon.Dragon(name, coordinates, age, weight, dragonType, head);

                                collection.add(dragon);
                                saveCollection();
                                output.append("  Dragon added: ").append(name).append("\n");
                            } catch (IllegalArgumentException | NullPointerException e) {
                                output.append("  Error parsing add block: ").append(e.getMessage()).append("\n");
                            }
                            i++;
                            continue;
                        }

                        String[] cmdParts = line.split("\\s+", 2);
                        String lineCmd = cmdParts[0].toLowerCase();
                        String lineArgs = cmdParts.length > 1 ? cmdParts[1].trim() : "";
                        executeScriptCommand(lineCmd, lineArgs, output);
                        i++;
                    }
                }
                case HISTORY -> {
                    output.append("Error: History is not available in remote mode.");
                }
            }

            context.response = textResponse(output.toString());
            commandLogger.commitCommand(logId);
            logger.debug("Command executed successfully: " + cmd);
            
        } catch (Exception e) {
            logger.error("Error executing command '" + cmd + "': " + e.getMessage(), e);
            commandLogger.rollbackCommand(logId);
            context.response = textResponse("Error executing command: " + e.getMessage());
        }
    }

    /**
     * Executes a single command from a script file.
     */
    private void executeScriptCommand(String cmd, String args, StringBuilder output) {
        Commands command = null;
        for (Commands c : Commands.values()) {
            if (c.getText().equals(cmd)) {
                command = c;
                break;
            }
        }
        
        if (command == null) {
            output.append("  Unknown command: ").append(cmd).append("\n");
            return;
        }

        try {
            switch (command) {
                case SHOW -> {
                    String collectionView = viewToString(outView.toView(collection.getStream()));
                    output.append(collectionView);
                }
                case CLEAR -> {
                    collection.clear();
                    saveCollection();
                    output.append("  Collection cleared.\n");
                }
                case SAVE -> {
                    saveCollection();
                    output.append("  Collection saved.\n");
                }
                case INFO -> {
                    output.append("  Elements: ").append(collection.countIf(e -> true)).append("\n");
                }
                case REMOVE_BY_ID -> {
                    if (args.isEmpty()) {
                        output.append("  Error: You must provide a dragon ID.\n");
                        break;
                    }
                    try {
                        long id = Long.parseLong(args);
                        int before = collection.countIf(d -> true);
                        collection.removeIf(d -> d.getId() == id);
                        int after = collection.countIf(d -> true);
                        if (before == after) {
                            output.append("  Dragon with ID ").append(id).append(" not found.\n");
                        } else {
                            saveCollection();
                            output.append("  Dragon removed.\n");
                        }
                    } catch (NumberFormatException e) {
                        output.append("  Error: Invalid ID format.\n");
                    }
                }
                case FILTER_STARTS_WITH_NAME -> {
                    if (args.isEmpty()) {
                        output.append("  Error: You must provide a name prefix.\n");
                        break;
                    }
                    String filtered = viewToString(
                        outView.toView(
                            collection.getStream()
                                .filter(e -> e.getName().startsWith(args))
                        )
                    );
                    output.append(filtered);
                }
                default -> output.append("  Command '").append(cmd).append("' not supported in script.\n");
            }
        } catch (Exception e) {
            output.append("  Error executing '").append(cmd).append("': ").append(e.getMessage()).append("\n");
        }
    }

    private ByteBuffer textResponse(String value) {
        return ByteBuffer.wrap(value.getBytes(core.Defaults.CHARSET));
    }

    private String viewToString(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }

        try (var reader = new InputStreamReader(inputStream, core.Defaults.CHARSET)) {
            var builder = new StringBuilder();
            var buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException e) {
            logger.error("Failed to convert view InputStream to String: " + e.getMessage(), e);
            return "";
        }
    }

    private Dragon parseDragonFromCsv(String csv) {
        String[] parts = csv.split("\\s*,\\s*");
        if (parts.length < 6) {
            throw new IllegalArgumentException("Dragon CSV should include at least 6 fields: name,x,y,age,weight,type,headSize,headToothCount");
        }
        try {
            String name = parts[0];
            float x = Float.parseFloat(parts[1]);
            float y = Float.parseFloat(parts[2]);
            int age = Integer.parseInt(parts[3]);
            long weight = Long.parseLong(parts[4]);
            dragon.DragonType dragonType = null;
            if (parts.length > 5 && !parts[5].isBlank()) {
                dragonType = dragon.DragonType.valueOf(parts[5].toUpperCase());
            }
            float headSize = parts.length > 6 ? Float.parseFloat(parts[6]) : 0f;
            int headToothCount = parts.length > 7 ? Integer.parseInt(parts[7]) : 0;

            var coordinates = new core.Coordinates(x, y);
            var head = new dragon.DragonHead(headSize, headToothCount);
            return new dragon.Dragon(name, coordinates, age, weight, dragonType, head);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid dragon data: " + e.getMessage(), e);
        }
    }

    private void saveCollection() {
        storage.save(storageView.toView(collection.getStream()));
    }

    private final API collection;
    private final Storage storage;
    private final CommandLogger commandLogger;
    private final TransactionManager transactionManager;
    private final View storageView;
    private final View outView;
    private final Logger logger;
}
