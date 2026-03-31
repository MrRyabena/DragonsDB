package ui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Interactive text UI handler.
 *
 * <p>
 * Reads commands from stdin (or from script files via {@code execute_script})
 * and dispatches them
 * to {@link UI}. Supports a bounded script recursion depth and basic input
 * validation for building
 * {@link dragon.Dragon} objects.
 */
public class TextUIHandler implements Runnable {
    /**
     * Creates the handler bound to a {@link UI} instance.
     *
     * @param ui UI facade used to execute commands
     * @throws NullPointerException if ui is null
     */
    public TextUIHandler(UI ui) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.readers = new ArrayDeque<>();
        this.openScripts = new ArrayDeque<>();
        this.in = new BufferedReader(new InputStreamReader(System.in));
        this.out = new OutputStreamWriter(System.out);

        try {
            out.write("Welcome to the Dragon database!\n");
            out.write("You can manage dragons using the following commands (type 'help' to list them):\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        readers.push(in);

        commandHandlers = new HashMap<>();
        registerHandlers();
    }

    /**
     * Main interactive loop.
     *
     * <p>
     * Reads lines from the current input source, supports returning from scripts,
     * and executes
     * parsed commands until input is exhausted.
     */
    @Override
    public void run() {
        try {
            while (true) {
                printPromptIfInteractive();
                var line = readLineFromCurrentInput();
                if (line == null) {
                    if (!closeCurrentScriptIfAny()) {
                        return;
                    }
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                dispatchLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** UI facade used to execute parsed commands. */
    private UI ui;

    /** Output destination for user-visible messages. */
    private Writer out;

    /** Primary reader for interactive stdin. */
    private BufferedReader in;

    /** Client for server communication with buffering support. */
    private client.RequestClient requestClient;

    /** Stack of active readers (stdin + nested script readers). */
    private final Deque<BufferedReader> readers;

    /** Stack of script paths currently being executed (for recursion detection). */
    private final Deque<Path> openScripts;

    /** Command dispatcher mapping from command text to handler. */
    private final HashMap<String, Consumer<String>> commandHandlers;

    /** Maximum allowed nested script execution depth. */
    private static final int MAX_SCRIPT_DEPTH = 16;

    /**
     * Sets the output writer for user-visible messages.
     *
     * @param out output writer
     */
    public void setOutputStream(OutputStreamWriter out) {
        this.out = out;
    }

    /**
     * Sets the request client for server communication.
     *
     * @param requestClient the client instance
     */
    public void setRequestClient(client.RequestClient requestClient) {
        this.requestClient = requestClient;
        System.out.println("[HANDLER] RequestClient set successfully");
    }

    /**
     * Prints the prompt only when running in interactive mode (stdin is the only
     * source).
     */
    private void printPromptIfInteractive() {
        if (readers.size() == 1) {
            try {
                out.write("> ");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads a single line from the current input source.
     *
     * @return next line, or null on EOF
     * @throws IOException if reading fails
     */
    private String readLineFromCurrentInput() throws IOException {
        return readers.peek().readLine();
    }

    /**
     * Closes current script reader (if any) and returns to the previous input
     * source.
     *
     * @return true if a script reader was closed, false if currently in interactive
     *         mode
     */
    private boolean closeCurrentScriptIfAny() {
        if (readers.size() <= 1) {
            return false;
        }
        if (readers.size() <= 2) {
            ui.setOutputWriter(new OutputStreamWriter(System.out));
            this.out = new OutputStreamWriter(System.out);
            
            // Try to flush buffered commands when returning to interactive mode
            if (requestClient != null) {
                System.out.println("[CLIENT] Flushing buffered commands after script...");
                try {
                    int flushed = requestClient.flushBufferedCommands();
                    if (flushed > 0) {
                        out.write(String.format("[CLIENT] Successfully flushed %d buffered commands after script execution.%n", flushed));
                        out.flush();
                    } else {
                        System.out.println("[CLIENT] No buffered commands to flush.");
                    }
                } catch (Exception e) {
                    System.err.println("[CLIENT] Exception during flush: " + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    try {
                        out.write("[CLIENT] Failed to flush buffered commands: " + e.getMessage() + "\n");
                        out.flush();
                    } catch (IOException ignored) {
                    }
                }
            } else {
                System.out.println("[CLIENT] RequestClient is null, cannot flush.");
            }
        }

        var reader = readers.pop();
        try {
            reader.close();
        } catch (IOException ignored) {
        }

        var path = openScripts.pollFirst();
        if (path != null) {
            try {
                out.write(String.format("Returning to previous input source (script finished): %s%n", path));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Parses and dispatches a command line to its handler.
     *
     * @param line raw input line
     */
    private void dispatchLine(String line) {
        var parts = line.split("\\s+", 2);
        var cmd = parts[0].toLowerCase(Locale.ROOT);
        var args = parts.length > 1 ? parts[1].trim() : "";

        var handler = commandHandlers.get(cmd);
        if (handler == null) {
            try {
                out.write(String.format("Unknown command: %s%n", cmd));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            handler.accept(args);
        } catch (RuntimeException e) {
            try {
                out.write(String.format("Error while executing command '%s': %s%n", cmd, e.getMessage()));
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Registers all supported command handlers.
     */
    private void registerHandlers() {
        commandHandlers.put(Commands.HELP.getText(), args -> printHelp());
        commandHandlers.put(Commands.EXIT.getText(), args -> System.exit(0));
        commandHandlers.put(Commands.EXECUTE_SCRIPT.getText(), this::handleExecuteScript);

        commandHandlers.put(Commands.INFO.getText(), args -> ui.execute(new Command(Commands.INFO, null, null, null)));
        commandHandlers.put(Commands.SHOW.getText(), args -> ui.execute(new Command(Commands.SHOW, null, null, null)));

        commandHandlers.put(Commands.CLEAR.getText(), args -> {
            ui.execute(new Command(Commands.CLEAR, null, null, null));
            try {
                out.write("Collection cleared successfully.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.SAVE.getText(), args -> {
            ui.execute(new Command(Commands.SAVE, null, null, null));
            try {
                out.write("Collection saved successfully.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.HISTORY.getText(),
                args -> ui.execute(new Command(Commands.HISTORY, null, null, null)));

        commandHandlers.put(Commands.ADD.getText(), args -> {
            ui.execute(new Command(Commands.ADD, readDragon(), null, null));
            try {
                out.write("Dragon added successfully.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.REMOVE_GREATER.getText(), args -> {
            ui.execute(new Command(Commands.REMOVE_GREATER, readDragon(), null, null));
            try {
                out.write("All dragons greater than the given one were removed.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.REMOVE_LOWER.getText(), args -> {
            ui.execute(new Command(Commands.REMOVE_LOWER, readDragon(), null, null));
            try {
                out.write("All dragons lower than the given one were removed.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.REMOVE_BY_ID.getText(), args -> {
            var id = parseLongRequired(args, "id");
            ui.execute(new Command(Commands.REMOVE_BY_ID, dragonWithIdOnly(id), null, null));
            try {
                out.write("Dragon removed by id.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.UPDATE_BY_ID.getText(), args -> {
            var id = parseLongRequired(args, "id");
            var d = readDragon();
            forceSetDragonId(d, id);
            ui.execute(new Command(Commands.UPDATE_BY_ID, d, null, null));
            try {
                out.write("Dragon updated successfully.\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        commandHandlers.put(Commands.COUNT_BY_TYPE.getText(), args -> {
            var type = parseDragonTypeRequired(args);
            ui.execute(new Command(Commands.COUNT_BY_TYPE, null, type, null));
        });

        commandHandlers.put(Commands.COUNT_GREATER_THAN_TYPE.getText(), args -> {
            var type = parseDragonTypeRequired(args);
            ui.execute(new Command(Commands.COUNT_GREATER_THAN_TYPE, null, type, null));
        });

        commandHandlers.put(Commands.FILTER_STARTS_WITH_NAME.getText(), args -> {
            if (args == null || args.isBlank()) {
                throw new IllegalArgumentException("You must provide a name substring.");
            }
            ui.execute(new Command(Commands.FILTER_STARTS_WITH_NAME, null, null, args));
        });
    }

    /**
     * Handles {@code execute_script} command by pushing a new reader onto the
     * stack.
     *
     * @param args path to the script file (may be quoted)
     * @throws IllegalArgumentException if the path is invalid, unreadable, or
     *                                  recursion is detected
     */
    private void handleExecuteScript(String args) {
        if (args == null || args.isBlank()) {
            throw new IllegalArgumentException("You must provide a script file name.");
        }

        if (readers.size() - 1 >= MAX_SCRIPT_DEPTH) {
            try {
                out.write(String.format("Maximum script recursion depth reached: %d%n", MAX_SCRIPT_DEPTH));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        var raw = args.trim();
        raw = stripMatchingQuotes(raw);

        Path path;
        try {
            var p = Paths.get(raw);
            if (!p.isAbsolute()) {
                p = getScriptBaseDir().resolve(p);
            }
            path = p.toAbsolutePath().normalize();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid script file path.");
        }

        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Script file not found or not readable: " + path);
        }

        if (openScripts.contains(path)) {
            throw new IllegalArgumentException("Recursion detected: script is already being executed: " + path);
        }

        try {
            System.out.println("Executing script: " + path);
            var br = new BufferedReader(new FileReader(path.toFile()));
            readers.push(br);
            openScripts.push(path);
            ui.setOutputWriter(OutputStreamWriter.nullWriter());
            this.out = OutputStreamWriter.nullWriter();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to open script: " + path);
        }
    }

    /**
     * Returns a base directory for resolving relative script paths.
     *
     * <p>
     * If scripts are nested, relative paths are resolved against the directory of
     * the current script.
     *
     * @return base directory path
     */
    private Path getScriptBaseDir() {
        if (!openScripts.isEmpty()) {
            var current = openScripts.peek();
            var parent = current != null ? current.getParent() : null;
            if (parent != null) {
                return parent;
            }
        }
        return Paths.get("").toAbsolutePath();
    }

    /**
     * Removes matching leading and trailing quotes (single or double) from a
     * string.
     *
     * @param s input string
     * @return unquoted string
     */
    private String stripMatchingQuotes(String s) {
        if (s == null)
            return null;
        if (s.length() >= 2) {
            var first = s.charAt(0);
            var last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }

    /**
     * Prints available command names.
     */
    private void printHelp() {
        try {
            out.write("Available commands:\n");
            for (var c : Commands.values()) {
                out.write("- " + c.getText() + "\n");
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a required long value.
     *
     * @param s       raw string
     * @param argName argument name for error messages
     * @return parsed long
     * @throws IllegalArgumentException if missing or invalid
     */
    private long parseLongRequired(String s, String argName) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("You must provide " + argName + ".");
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + s.trim());
        }
    }

    /**
     * Parses a required {@link dragon.DragonType} value.
     *
     * @param s raw string
     * @return parsed type
     * @throws IllegalArgumentException if missing or invalid
     */
    private dragon.DragonType parseDragonTypeRequired(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("You must provide type (one of: WATER, UNDERGROUND, AIR, FIRE).");
        }
        try {
            return dragon.DragonType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid type. Allowed: WATER, UNDERGROUND, AIR, FIRE.");
        }
    }

    /**
     * Reads an optional {@link dragon.DragonType} from input. Empty line is treated
     * as null.
     *
     * @return parsed type or null
     * @throws IOException on input error
     */
    private dragon.DragonType readDragonTypeNullable() throws IOException {
        while (true) {
            out.write("Enter type (WATER/UNDERGROUND/AIR/FIRE) or empty line for null: ");
            out.flush();
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input while reading type.");
            }
            line = line.trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                return dragon.DragonType.valueOf(line.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                out.write("Error: invalid enum value. Please try again.\n");
                out.flush();
            }
        }
    }

    /**
     * Reads a non-empty string from input.
     *
     * @param prompt prompt to display
     * @return non-empty user input
     * @throws IOException on input error
     */
    private String readNonEmptyString(String prompt) throws IOException {
        while (true) {
            out.write(prompt);
            out.flush();
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
            out.write("Error: value cannot be empty. Please try again.\n");
            out.flush();
        }
    }

    /**
     * Reads a float from input.
     *
     * @param prompt prompt to display
     * @return parsed float value
     * @throws IOException on input error
     */
    private float readFloat(String prompt) throws IOException {
        while (true) {
            out.write(prompt);
            out.flush();
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            try {
                return Float.parseFloat(line);
            } catch (NumberFormatException e) {
                out.write("Error: you must enter a float number. Please try again.\n");
                out.flush();
            }
        }
    }

    /**
     * Reads a positive integer from input.
     *
     * @param prompt prompt to display
     * @return parsed integer greater than 0
     * @throws IOException on input error
     */
    private int readIntPositive(String prompt) throws IOException {
        while (true) {
            out.write(prompt);
            out.flush();
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            try {
                var v = Integer.parseInt(line);
                if (v <= 0) {
                    out.write("Error: number must be > 0. Please try again.\n");
                    out.flush();
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                out.write("Error: you must enter an integer number. Please try again.\n");
                out.flush();
            }
        }
    }

    /**
     * Reads a positive long from input.
     *
     * @param prompt prompt to display
     * @return parsed long greater than 0
     * @throws IOException on input error
     */
    private long readLongPositive(String prompt) throws IOException {
        while (true) {
            out.write(prompt);
            out.flush();
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            try {
                var v = Long.parseLong(line);
                if (v <= 0) {
                    out.write("Error: number must be > 0. Please try again.\n");
                    out.flush();
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                out.write("Error: you must enter a long number. Please try again.\n");
                out.flush();
            }
        }
    }

    /**
     * Reads a {@link dragon.Dragon} from input by prompting for its fields.
     *
     * @return constructed dragon
     * @throws IllegalStateException on I/O error
     */
    private dragon.Dragon readDragon() {
        try {
            var name = readNonEmptyString("Enter name: ");

            core.Coordinates coordinates;
            while (true) {
                try {
                    var x = readFloat("Enter coordinates.x (float, > -359): ");
                    var y = readFloat("Enter coordinates.y (float, <= 603): ");
                    coordinates = new core.Coordinates(x, y);
                    break;
                } catch (core.BadDataException | NullPointerException e) {
                    out.write(String.format("Coordinates error: %s%n", e.getMessage()));
                    out.flush();
                }
            }

            var age = readIntPositive("Enter age (int, > 0): ");
            var weight = readLongPositive("Enter weight (long, > 0): ");
            var type = readDragonTypeNullable();

            var headSize = readFloat("Enter head.size (float): ");
            var headTeeth = readFloat("Enter head.toothCount (float): ");
            var head = new dragon.DragonHead(headSize, headTeeth);

            return new dragon.Dragon(name, coordinates, age, weight, type, head);
        } catch (IOException e) {
            throw new IllegalStateException("Input error: " + e.getMessage());
        }
    }

    /**
     * Creates a temporary dragon and overwrites its id using reflection.
     *
     * <p>
     * Used for commands that operate by id without requiring full entity input.
     *
     * @param id target id
     * @return dragon instance with forced id
     */
    private dragon.Dragon dragonWithIdOnly(long id) {
        try {
            var d = new dragon.Dragon(
                    "stub",
                    new core.Coordinates(-358f, 0f),
                    1,
                    1L,
                    null,
                    new dragon.DragonHead(1f, 1f));
            forceSetDragonId(d, id);
            return d;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать временный объект для id");
        }
    }

    /**
     * Sets the {@code id} field of a dragon instance via reflection.
     *
     * @param d  dragon instance
     * @param id id to assign
     * @throws IllegalStateException if reflection fails
     */
    private void forceSetDragonId(dragon.Dragon d, long id) {
        try {
            var f = dragon.Dragon.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(d, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Невозможно установить id у Dragon (reflection): " + e.getMessage());
        }
    }
}
