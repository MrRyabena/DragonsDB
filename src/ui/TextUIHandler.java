package ui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/*
help : вывести справку по доступным командам
info : вывести в стандартный поток вывода информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)
show : вывести в стандартный поток вывода все элементы коллекции в строковом представлении
add {element} : добавить новый элемент в коллекцию
update id {element} : обновить значение элемента коллекции, id которого равен заданному
remove_by_id id : удалить элемент из коллекции по его id
clear : очистить коллекцию
save : сохранить коллекцию в файл
execute_script file_name : считать и исполнить скрипт из указанного файла. В скрипте содержатся команды в таком же виде, в котором их вводит пользователь в интерактивном режиме.
exit : завершить программу (без сохранения в файл)
remove_greater {element} : удалить из коллекции все элементы, превышающие заданный
remove_lower {element} : удалить из коллекции все элементы, меньшие, чем заданный
history : вывести последние 8 команд (без их аргументов)
count_by_type type : вывести количество элементов, значение поля type которых равно заданному
count_greater_than_type type : вывести количество элементов, значение поля type которых больше заданного
filter_starts_with_name name : вывести элементы, значение поля name которых начинается с заданной подстроки

Формат ввода команд:

Все аргументы команды, являющиеся стандартными типами данных (примитивные типы, классы-оболочки, String, классы для хранения дат), должны вводиться в той же строке, что и имя команды.
Все составные типы данных (объекты классов, хранящиеся в коллекции) должны вводиться по одному полю в строку.
При вводе составных типов данных пользователю должно показываться приглашение к вводу, содержащее имя поля (например, "Введите дату рождения:")
Если поле является enum'ом, то вводится имя одной из его констант (при этом список констант должен быть предварительно выведен).
При некорректном пользовательском вводе (введена строка, не являющаяся именем константы в enum'е; введена строка вместо числа; введённое число не входит в указанные границы и т.п.) должно быть показано сообщение об ошибке и предложено повторить ввод поля.
Для ввода значений null использовать пустую строку.
Поля с комментарием "Значение этого поля должно генерироваться автоматически" не должны вводиться пользователем вручную при добавлении.
 */

public class TextUIHandler implements Runnable {
    public TextUIHandler(UI ui) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.readers = new ArrayDeque<>();
        this.openScripts = new ArrayDeque<>();
        this.in = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Welcome to the Dragon database!");
        System.out.println("You can manage dragons using the following commands (type 'help' to list them):");

        readers.push(in);

        commandHandlers = new HashMap<>();
        registerHandlers();
    }

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

    private UI ui;
    private BufferedReader in;
    private final Deque<BufferedReader> readers;
    private final Deque<Path> openScripts;
    private final HashMap<String, Consumer<String>> commandHandlers;

    private static final int MAX_SCRIPT_DEPTH = 16;

    private void printPromptIfInteractive() {
        if (readers.size() == 1) {
            System.out.print("> ");
        }
    }

    private String readLineFromCurrentInput() throws IOException {
        return readers.peek().readLine();
    }

    private boolean closeCurrentScriptIfAny() {
        if (readers.size() <= 1) {
            return false;
        }

        var reader = readers.pop();
        try {
            reader.close();
        } catch (IOException ignored) {
        }

        var path = openScripts.pollFirst();
        if (path != null) {
            System.out.printf("Returning to previous input source (script finished): %s%n", path);
        }
        return true;
    }

    private void dispatchLine(String line) {
        var parts = line.split("\\s+", 2);
        var cmd = parts[0].toLowerCase(Locale.ROOT);
        var args = parts.length > 1 ? parts[1].trim() : "";

        var handler = commandHandlers.get(cmd);
        if (handler == null) {
            System.out.printf("Unknown command: %s%n", cmd);
            return;
        }

        try {
            handler.accept(args);
        } catch (RuntimeException e) {
            System.out.printf("Error while executing command '%s': %s%n", cmd, e.getMessage());
        }
    }

    private void registerHandlers() {
        commandHandlers.put(Commands.HELP.getText(), args -> printHelp());
        commandHandlers.put(Commands.EXIT.getText(), args -> System.exit(0));
        commandHandlers.put(Commands.EXECUTE_SCRIPT.getText(), this::handleExecuteScript);

        commandHandlers.put(Commands.INFO.getText(), args -> ui.execute(new Command(Commands.INFO, null, null, null)));
        commandHandlers.put(Commands.SHOW.getText(), args -> ui.execute(new Command(Commands.SHOW, null, null, null)));

        commandHandlers.put(Commands.CLEAR.getText(), args -> {
            ui.execute(new Command(Commands.CLEAR, null, null, null));
            System.out.println("Collection cleared successfully.");
        });

        commandHandlers.put(Commands.SAVE.getText(), args -> {
            ui.execute(new Command(Commands.SAVE, null, null, null));
            System.out.println("Collection saved successfully.");
        });

        commandHandlers.put(Commands.HISTORY.getText(), args -> ui.execute(new Command(Commands.HISTORY, null, null, null)));

        commandHandlers.put(Commands.ADD.getText(), args -> {
            ui.execute(new Command(Commands.ADD, readDragon(), null, null));
            System.out.println("Dragon added successfully.");
        });

        commandHandlers.put(Commands.REMOVE_GREATER.getText(), args -> {
            ui.execute(new Command(Commands.REMOVE_GREATER, readDragon(), null, null));
            System.out.println("All dragons greater than the given one were removed.");
        });

        commandHandlers.put(Commands.REMOVE_LOWER.getText(), args -> {
            ui.execute(new Command(Commands.REMOVE_LOWER, readDragon(), null, null));
            System.out.println("All dragons lower than the given one were removed.");
        });

        commandHandlers.put(Commands.REMOVE_BY_ID.getText(), args -> {
            var id = parseLongRequired(args, "id");
            ui.execute(new Command(Commands.REMOVE_BY_ID, dragonWithIdOnly(id), null, null));
            System.out.println("Dragon removed by id.");
        });

        commandHandlers.put(Commands.UPDATE_BY_ID.getText(), args -> {
            var id = parseLongRequired(args, "id");
            var d = readDragon();
            forceSetDragonId(d, id);
            ui.execute(new Command(Commands.UPDATE_BY_ID, d, null, null));
            System.out.println("Dragon updated successfully.");
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

    private void handleExecuteScript(String args) {
        if (args == null || args.isBlank()) {
            throw new IllegalArgumentException("You must provide a script file name.");
        }

        if (readers.size() - 1 >= MAX_SCRIPT_DEPTH) {
            System.out.printf("Maximum script recursion depth reached: %d%n", MAX_SCRIPT_DEPTH);
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
            var br = new BufferedReader(new FileReader(path.toFile()));
            readers.push(br);
            openScripts.push(path);
            System.out.printf("Executing script: %s%n", path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to open script: " + path);
        }
    }

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

    private String stripMatchingQuotes(String s) {
        if (s == null) return null;
        if (s.length() >= 2) {
            var first = s.charAt(0);
            var last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }

    private void printHelp() {
        System.out.println("Available commands:");
        for (var c : Commands.values()) {
            System.out.println("- " + c.getText());
        }
    }

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

    private dragon.DragonType readDragonTypeNullable() throws IOException {
        while (true) {
            System.out.print("Enter type (WATER/UNDERGROUND/AIR/FIRE) or empty line for null: ");
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
                System.out.println("Error: invalid enum value. Please try again.");
            }
        }
    }

    private String readNonEmptyString(String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("Error: value cannot be empty. Please try again.");
        }
    }

    private float readFloat(String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            try {
                return Float.parseFloat(line);
            } catch (NumberFormatException e) {
                System.out.println("Error: you must enter a float number. Please try again.");
            }
        }
    }

    private int readIntPositive(String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            try {
                var v = Integer.parseInt(line);
                if (v <= 0) {
                    System.out.println("Error: number must be > 0. Please try again.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Error: you must enter an integer number. Please try again.");
            }
        }
    }

    private long readLongPositive(String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            var line = readLineFromCurrentInput();
            if (line == null) {
                throw new IllegalStateException("Unexpected end of input.");
            }
            line = line.trim();
            try {
                var v = Long.parseLong(line);
                if (v <= 0) {
                    System.out.println("Error: number must be > 0. Please try again.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Error: you must enter a long number. Please try again.");
            }
        }
    }

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
                    System.out.printf("Coordinates error: %s%n", e.getMessage());
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
