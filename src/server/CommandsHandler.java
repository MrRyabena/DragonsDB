package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import collection.API;
import collection.ApiCommand;
import core.ParameterRequest;
import core.Request;
import core.Response;
import dragon.Dragon;
import dragon.DragonHead;
import dragon.DragonType;
import dragon.view.JsonView;
import dragon.view.View;
import storage.CommandLogger;
import storage.Storage;
import storage.TransactionManager;
import ui.Commands;

/**
 * Processes commands from clients using a stateful session-based dialog model.
 *
 * <p>When a client sends a command that requires parameters, the server responds with a
 * NEED_PARAMETER status and a ParameterRequest. The client collects the input and sends it back.
 * The server continues this dialog until all parameters are collected, then executes the command
 * and returns the result.
 */
public class CommandsHandler {
    public CommandsHandler(
            API collection,
            Storage storage,
            CommandLogger commandLogger,
            TransactionManager transactionManager) {
        this.collection = collection;
        this.storage = storage;
        this.commandLogger = commandLogger;
        this.transactionManager = transactionManager;
        this.sessionManager = new SessionManager();
        this.storageView = new JsonView();
    }

    /** Processes a server context containing a deserialized Request. */
    public void handleRequest(ServerContext context) {
        if (context.request == null) {
            context.response = Response.error("Invalid request: null");
            return;
        }

        try {
            ClientSession session = sessionManager.getOrCreateSession(context.clientAddress);
            session.updateActivity();
            context.sessionId = session.getSessionId();

            if (context.request.status == Request.Status.COMMAND) {
                handleNewCommand(session, context);
            } else if (context.request.status == Request.Status.PARAMETER_RESPONSE) {
                handleParameterResponse(session, context);
            } else {
                context.response =
                        Response.error("Unknown request status: " + context.request.status);
            }
        } catch (Exception e) {
            logger.error("Error processing request", e);
            context.response = Response.error("Internal server error: " + e.getMessage());
        }
    }

    /** Handles a new command from the client. */
    private void handleNewCommand(ClientSession session, ServerContext context) {
        String commandLine = context.request.command.trim();
        if (commandLine.isEmpty()) {
            context.response = Response.error("Empty command");
            return;
        }

        String[] parts = commandLine.split("\\s+", 2);
        String cmdStr = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";

        Commands command = null;
        for (Commands c : Commands.values()) {
            if (c.getText().equals(cmdStr)) {
                command = c;
                break;
            }
        }

        if (command == null) {
            context.response = Response.error("Unknown command: " + cmdStr);
            return;
        }

        session.setCurrentCommand(command);

        // Log command before execution (write-ahead)
        long txnId = transactionManager.getCurrentTransactionId();
        if (txnId == 0) {
            txnId = transactionManager.beginTransaction();
        }
        ApiCommand apiCommand = commandsToApiCommand(command);
        long logId = commandLogger.logCommand(txnId, apiCommand, null);

        try {
            // Get required parameters for this command
            List<ParameterRequest> requiredParams = getRequiredParameters(command, args);

            if (requiredParams.isEmpty()) {
                // No parameters needed, execute immediately
                executeCommand(session, context, args);
                commandLogger.commitCommand(logId);
            } else {
                // Add parameter requests to session
                for (ParameterRequest param : requiredParams) {
                    session.addParameterRequest(param);
                }
                // Ask for first parameter
                ParameterRequest firstParam = session.peekNextParameter();
                context.response = Response.needParameter(session.getSessionId(), firstParam);
            }
        } catch (Exception e) {
            logger.error("Error handling command: " + command, e);
            commandLogger.rollbackCommand(logId);
            context.response = Response.error("Error: " + e.getMessage());
            session.clearDialog();
        }
    }

    /** Handles a parameter response from the client. */
    private void handleParameterResponse(ClientSession session, ServerContext context) {
        String parameterValue = context.request.parameterValue;

        if (session.getCurrentCommand() == null) {
            context.response = Response.error("No active command in session");
            return;
        }

        ParameterRequest currentParam = session.peekNextParameter();
        if (currentParam == null) {
            context.response = Response.error("Unexpected parameter response");
            return;
        }

        try {
            // Validate and store the parameter
            Object value = parseParameter(currentParam.type, parameterValue);
            session.addParameter(currentParam.parameterName, value);
            session.pollNextParameter();

            if (session.hasMoreParameters()) {
                // Ask for next parameter
                ParameterRequest nextParam = session.peekNextParameter();
                context.response = Response.needParameter(session.getSessionId(), nextParam);
            } else {
                // All parameters collected, execute command
                String args = context.request.command != null ? context.request.command : "";

                // Log command execution
                long txnId = transactionManager.getCurrentTransactionId();
                if (txnId == 0) {
                    txnId = transactionManager.beginTransaction();
                }
                ApiCommand apiCommand = commandsToApiCommand(session.getCurrentCommand());
                long logId = commandLogger.logCommand(txnId, apiCommand, null);

                try {
                    executeCommand(session, context, args);
                    commandLogger.commitCommand(logId);
                } catch (Exception e) {
                    commandLogger.rollbackCommand(logId);
                    throw e;
                }
                session.clearDialog();
            }
        } catch (IllegalArgumentException e) {
            context.response = Response.error("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing parameter response", e);
            context.response = Response.error("Error: " + e.getMessage());
            session.clearDialog();
        }
    }

    /** Determines what parameters are required for a command. */
    private List<ParameterRequest> getRequiredParameters(Commands command, String args) {
        List<ParameterRequest> params = new ArrayList<>();

        switch (command) {
            case HELP, INFO, SHOW, CLEAR, SAVE, EXIT, HISTORY:
                // No parameters needed
                break;

            case ADD:
                params.add(
                        new ParameterRequest(
                                "dragon_name",
                                "Enter dragon name:",
                                ParameterRequest.ParameterType.STRING,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_x",
                                "Enter X coordinate:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_y",
                                "Enter Y coordinate:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_age",
                                "Enter dragon age:",
                                ParameterRequest.ParameterType.INTEGER,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_weight",
                                "Enter dragon weight:",
                                ParameterRequest.ParameterType.LONG,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_type",
                                "Enter dragon type (WATER, FIRE, EARTH, AIR, or leave empty):",
                                ParameterRequest.ParameterType.DRAGON_TYPE,
                                false));
                params.add(
                        new ParameterRequest(
                                "head_size",
                                "Enter dragon head size:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "head_teeth",
                                "Enter number of teeth in head:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                break;

            case UPDATE_BY_ID:
                params.add(
                        new ParameterRequest(
                                "update_id",
                                "Enter dragon ID to update:",
                                ParameterRequest.ParameterType.LONG,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_name",
                                "Enter new dragon name:",
                                ParameterRequest.ParameterType.STRING,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_x",
                                "Enter new X coordinate:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_y",
                                "Enter new Y coordinate:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_age",
                                "Enter new dragon age:",
                                ParameterRequest.ParameterType.INTEGER,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_weight",
                                "Enter new dragon weight:",
                                ParameterRequest.ParameterType.LONG,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_type",
                                "Enter new dragon type (WATER, FIRE, EARTH, AIR, or leave empty):",
                                ParameterRequest.ParameterType.DRAGON_TYPE,
                                false));
                params.add(
                        new ParameterRequest(
                                "head_size",
                                "Enter new dragon head size:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "head_teeth",
                                "Enter new number of teeth in head:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                break;

            case REMOVE_BY_ID:
                params.add(
                        new ParameterRequest(
                                "remove_id",
                                "Enter dragon ID to remove:",
                                ParameterRequest.ParameterType.LONG,
                                true));
                break;

            case REMOVE_GREATER:
            case REMOVE_LOWER:
                params.add(
                        new ParameterRequest(
                                "dragon_name",
                                "Enter dragon name:",
                                ParameterRequest.ParameterType.STRING,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_x",
                                "Enter X coordinate:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_y",
                                "Enter Y coordinate:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_age",
                                "Enter dragon age:",
                                ParameterRequest.ParameterType.INTEGER,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_weight",
                                "Enter dragon weight:",
                                ParameterRequest.ParameterType.LONG,
                                true));
                params.add(
                        new ParameterRequest(
                                "dragon_type",
                                "Enter dragon type (WATER, FIRE, EARTH, AIR, or leave empty):",
                                ParameterRequest.ParameterType.DRAGON_TYPE,
                                false));
                params.add(
                        new ParameterRequest(
                                "head_size",
                                "Enter dragon head size:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                params.add(
                        new ParameterRequest(
                                "head_teeth",
                                "Enter number of teeth in head:",
                                ParameterRequest.ParameterType.FLOAT,
                                true));
                break;

            case COUNT_BY_TYPE:
            case COUNT_GREATER_THAN_TYPE:
                params.add(
                        new ParameterRequest(
                                "filter_type",
                                "Enter dragon type (WATER, FIRE, EARTH, AIR):",
                                ParameterRequest.ParameterType.DRAGON_TYPE,
                                true));
                break;

            case FILTER_STARTS_WITH_NAME:
                params.add(
                        new ParameterRequest(
                                "name_prefix",
                                "Enter name prefix to filter:",
                                ParameterRequest.ParameterType.STRING,
                                true));
                break;

            case EXECUTE_SCRIPT:
                params.add(
                        new ParameterRequest(
                                "script_path",
                                "Enter script file path:",
                                ParameterRequest.ParameterType.STRING,
                                true));
                break;
        }

        return params;
    }

    /** Parses a string value into the requested parameter type. */
    private Object parseParameter(ParameterRequest.ParameterType type, String value)
            throws IllegalArgumentException {
        if (value == null || value.trim().isEmpty()) {
            if (type == ParameterRequest.ParameterType.DRAGON_TYPE) {
                return null; // Optional type
            }
            throw new IllegalArgumentException("Parameter cannot be empty");
        }

        try {
            return switch (type) {
                case STRING -> value.trim();
                case INTEGER -> Integer.parseInt(value.trim());
                case LONG -> Long.parseLong(value.trim());
                case FLOAT -> Float.parseFloat(value.trim());
                case DRAGON_TYPE -> parseDragonType(value.trim());
                case ID -> Long.parseLong(value.trim());
                default -> throw new IllegalArgumentException("Unknown type: " + type);
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + e.getMessage());
        }
    }

    /** Parses a dragon type from string. */
    private DragonType parseDragonType(String str) throws IllegalArgumentException {
        if (str.isEmpty()) {
            return null;
        }
        try {
            return DragonType.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid dragon type. Must be one of: WATER, FIRE, EARTH, AIR");
        }
    }

    /** Executes the command with collected parameters. */
    private void executeCommand(ClientSession session, ServerContext context, String args)
            throws Exception {
        Commands command = session.getCurrentCommand();
        Map<String, Object> params = session.getCollectedParameters();

        StringBuilder output = new StringBuilder();
        List<Dragon> resultDragons = new ArrayList<>();

        switch (command) {
            case HELP:
                output.append("Available commands:\n");
                output.append("  help - show this message\n");
                output.append("  info - show collection info\n");
                output.append("  show - show all dragons\n");
                output.append("  add - add a new dragon\n");
                output.append("  update_by_id {id} - update dragon by id\n");
                output.append("  remove_by_id {id} - remove dragon by id\n");
                output.append("  clear - clear the collection\n");
                output.append("  exit - exit the application\n");
                output.append("  remove_greater - remove dragons greater than given\n");
                output.append("  remove_lower - remove dragons lower than given\n");
                output.append("  count_by_type {type} - count dragons by type\n");
                output.append("  filter_starts_with_name {prefix} - filter by name prefix\n");
                output.append("  execute_script {file} - execute commands from file\n");
                break;

            case INFO:
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
                break;

            case SHOW:
                resultDragons.addAll(collection.getStream().collect(Collectors.toList()));
                if (resultDragons.isEmpty()) {
                    output.append("Collection is empty.\n");
                } else {
                    output.append("Dragons in collection:\n");
                }
                break;

            case ADD:
                {
                    String name = (String) params.get("dragon_name");
                    Float x = (Float) params.get("dragon_x");
                    Float y = (Float) params.get("dragon_y");
                    Integer age = (Integer) params.get("dragon_age");
                    Long weight = (Long) params.get("dragon_weight");
                    DragonType type = (DragonType) params.get("dragon_type");
                    Float headSize = (Float) params.get("head_size");
                    Float teeth = (Float) params.get("head_teeth");

                    Dragon dragon = createDragon(name, x, y, age, weight, type, headSize, teeth);
                    collection.add(dragon);
                    saveCollection();
                    output.append("Dragon added successfully.\n");
                    break;
                }

            case UPDATE_BY_ID:
                {
                    Long id = (Long) params.get("update_id");
                    String name = (String) params.get("dragon_name");
                    Float x = (Float) params.get("dragon_x");
                    Float y = (Float) params.get("dragon_y");
                    Integer age = (Integer) params.get("dragon_age");
                    Long weight = (Long) params.get("dragon_weight");
                    DragonType type = (DragonType) params.get("dragon_type");
                    Float headSize = (Float) params.get("head_size");
                    Float teeth = (Float) params.get("head_teeth");

                    Dragon dragon = createDragon(name, x, y, age, weight, type, headSize, teeth);
                    setDragonId(dragon, id);
                    collection.updateById(dragon);
                    saveCollection();
                    resultDragons.add(dragon);
                    output.append("Dragon updated successfully.\n");
                    break;
                }

            case REMOVE_BY_ID:
                {
                    Long id = (Long) params.get("remove_id");
                    collection.removeIf(d -> d.getId() == id);
                    saveCollection();
                    output.append("Dragon removed successfully.\n");
                    break;
                }

            case CLEAR:
                collection.clear();
                saveCollection();
                output.append("Collection cleared.\n");
                break;

            case SAVE:
                saveCollection();
                output.append("Collection saved.\n");
                break;

            case REMOVE_GREATER:
                {
                    String name = (String) params.get("dragon_name");
                    Float x = (Float) params.get("dragon_x");
                    Float y = (Float) params.get("dragon_y");
                    Integer age = (Integer) params.get("dragon_age");
                    Long weight = (Long) params.get("dragon_weight");
                    DragonType type = (DragonType) params.get("dragon_type");
                    Float headSize = (Float) params.get("head_size");
                    Float teeth = (Float) params.get("head_teeth");

                    Dragon sample = createDragon(name, x, y, age, weight, type, headSize, teeth);
                    int before = (int) collection.countIf(d -> true);
                    collection.removeIf(d -> d.compareTo(sample) > 0);
                    int after = (int) collection.countIf(d -> true);
                    int removed = before - after;
                    saveCollection();
                    output.append("Removed ")
                            .append(removed)
                            .append(" dragons greater than given.\n");
                    break;
                }

            case REMOVE_LOWER:
                {
                    String name = (String) params.get("dragon_name");
                    Float x = (Float) params.get("dragon_x");
                    Float y = (Float) params.get("dragon_y");
                    Integer age = (Integer) params.get("dragon_age");
                    Long weight = (Long) params.get("dragon_weight");
                    DragonType type = (DragonType) params.get("dragon_type");
                    Float headSize = (Float) params.get("head_size");
                    Float teeth = (Float) params.get("head_teeth");

                    Dragon sample = createDragon(name, x, y, age, weight, type, headSize, teeth);
                    int before = (int) collection.countIf(d -> true);
                    collection.removeIf(d -> d.compareTo(sample) < 0);
                    int after = (int) collection.countIf(d -> true);
                    int removed = before - after;
                    saveCollection();
                    output.append("Removed ")
                            .append(removed)
                            .append(" dragons lower than given.\n");
                    break;
                }

            case COUNT_BY_TYPE:
                {
                    DragonType type = (DragonType) params.get("filter_type");
                    int count = (int) collection.countIf(d -> d.getType() == type);
                    output.append("Count of dragons with type ")
                            .append(type)
                            .append(": ")
                            .append(count)
                            .append("\n");
                    break;
                }

            case COUNT_GREATER_THAN_TYPE:
                {
                    DragonType type = (DragonType) params.get("filter_type");
                    int count =
                            (int)
                                    collection.countIf(
                                            d ->
                                                    d.getType() != null
                                                            && d.getType().compareTo(type) > 0);
                    output.append("Count of dragons greater than type ")
                            .append(type)
                            .append(": ")
                            .append(count)
                            .append("\n");
                    break;
                }

            case FILTER_STARTS_WITH_NAME:
                {
                    String prefix = (String) params.get("name_prefix");
                    resultDragons.addAll(
                            collection
                                    .getStream()
                                    .filter(d -> d.getName().startsWith(prefix))
                                    .collect(Collectors.toList()));
                    output.append("Dragons with name starting with '")
                            .append(prefix)
                            .append("':\n");
                    for (Dragon d : resultDragons) {
                        output.append(d.toString()).append("\n");
                    }
                    break;
                }

            case EXECUTE_SCRIPT:
                output.append("Script execution is not supported in remote mode.\n");
                break;

            case HISTORY:
                output.append("History is not available in remote mode.\n");
                break;

            case EXIT:
                output.append("Goodbye!\n");
                break;
        }

        context.response = Response.success(resultDragons, output.toString());
    }

    /** Creates a Dragon from collected parameters. */
    private Dragon createDragon(
            String name,
            Float x,
            Float y,
            Integer age,
            Long weight,
            DragonType type,
            Float headSize,
            Float teeth)
            throws Exception {
        core.Coordinates coords = new core.Coordinates(x, y);
        DragonHead head = new DragonHead(headSize, teeth);
        return new Dragon(name, coords, age, weight, type, head);
    }

    /** Sets the ID of a dragon using reflection (for update operations). */
    private void setDragonId(Dragon dragon, Long id) {
        try {
            java.lang.reflect.Field idField = Dragon.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(dragon, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set dragon ID", e);
        }
    }

    private ApiCommand commandsToApiCommand(Commands cmd) {
        switch (cmd) {
            case HELP:
                return ApiCommand.HELP;
            case INFO:
                return ApiCommand.INFO;
            case ADD:
                return ApiCommand.ADD;
            case SHOW:
                return ApiCommand.SHOW;
            case UPDATE_BY_ID:
                return ApiCommand.UPDATE_BY_ID;
            case REMOVE_BY_ID:
                return ApiCommand.REMOVE_BY_ID;
            case CLEAR:
                return ApiCommand.CLEAR;
            case SAVE:
                return ApiCommand.SAVE;
            case EXECUTE_SCRIPT:
                return ApiCommand.EXECUTE_SCRIPT;
            case EXIT:
                return ApiCommand.EXIT;
            case REMOVE_GREATER:
                return ApiCommand.REMOVE_GREATER;
            case REMOVE_LOWER:
                return ApiCommand.REMOVE_LOWER;
            case HISTORY:
                return ApiCommand.HISTORY;
            case COUNT_BY_TYPE:
                return ApiCommand.COUNT_BY_TYPE;
            case COUNT_GREATER_THAN_TYPE:
                return ApiCommand.COUNT_GREATER_THAN_TYPE;
            case FILTER_STARTS_WITH_NAME:
                return ApiCommand.FILTER_STARTS_WITH_NAME;
            default:
                throw new IllegalArgumentException("Unknown command: " + cmd);
        }
    }

    private void saveCollection() {
        try {
            storage.save(storageView.toView(collection.getStream()));
        } catch (Exception e) {
            logger.error("Failed to save collection", e);
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    private static final Logger logger = Logger.getLogger(CommandsHandler.class);

    private final API collection;
    private final Storage storage;
    private final CommandLogger commandLogger;
    private final TransactionManager transactionManager;
    private final SessionManager sessionManager;
    private final View storageView;
}
