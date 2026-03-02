package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dragon.Dragon;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/**
 * Хранилище коллекции Dragon в JSON-файле.
 * Использует библиотеку Gson; объем кода заметно меньше
 * по сравнению с ручным парсингом, при этом формат
 * остается стандартным и расширяемым.
 * Метаданные коллекции (время создания/изменения) не
 * сериализуются, поскольку легко читаются из файловой
 * системы через Files.getLastModifiedTime(...).
 */
public class FileStorage implements Storage {
    private final Path filename;
    private HashSet<Dragon> collection;

    /* Gson и тип для десериализации */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type collectionType = new TypeToken<HashSet<Dragon>>() {
    }.getType();

    public FileStorage(Path filename, HashSet<Dragon> collection) {
        this.filename = filename;
        this.collection = collection;
    }

    @Override
    public void save(HashSet<Dragon> collection) throws IOException {
        this.collection = collection;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename.toFile()))) {
            writer.write(gson.toJson(collection, collectionType));
        }
    }

    @Override
    public HashSet<Dragon> load() throws IOException {
        if (!Files.exists(filename)) {
            return new HashSet<>();
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filename.toFile()))) {
            this.collection = gson.fromJson(reader, collectionType);
            return collection;
        }
    }

    @Override
    public void update(HashSet<Dragon> collection) throws IOException {
        save(collection);
    }

    /**
     * Десериализует JSON строку в коллекцию Dragon
     */
    private HashSet<Dragon> deserializeFromJson(String jsonContent) throws IOException {
        HashSet<Dragon> dragons = new HashSet<>();
        jsonContent = jsonContent.trim();

        if (jsonContent.isEmpty() || jsonContent.equals("[]")) {
            return dragons;
        }

        // Простой JSON парсер
        jsonContent = jsonContent.substring(1, jsonContent.length() - 1); // Убираем [ и ]
        String[] dragonJsons = splitJsonObjects(jsonContent);

        for (String dragonJson : dragonJsons) {
            Dragon dragon = parseJsonToDragon(dragonJson);
            if (dragon != null) {
                dragons.add(dragon);
            }
        }

        return dragons;
    }

    /**
     * Парсит JSON объект Dragon
     */
    private Dragon parseJsonToDragon(String json) throws IOException {
        try {
            String name = extractStringValue(json, "\"name\"");
            String coordsJson = extractObjectValue(json, "\"coordinates\"");
            Coordinates coordinates = parseCoordinates(coordsJson);
            int age = extractIntValue(json, "\"age\"");
            long weight = extractLongValue(json, "\"weight\"");
            String typeStr = extractStringValue(json, "\"type\"");
            DragonType type = typeStr != null && !typeStr.isEmpty() && !typeStr.equals("null")
                    ? DragonType.valueOf(typeStr)
                    : null;
            String headJson = extractObjectValue(json, "\"head\"");
            DragonHead head = parseHead(headJson);

            Dragon dragon = new Dragon(name, coordinates, age, weight, type, head);
            return dragon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Парсит Coordinates из JSON
     */
    private Coordinates parseCoordinates(String json) throws Exception {
        Float x = extractFloatValue(json, "\"x\"");
        Float y = extractFloatValue(json, "\"y\"");
        return new Coordinates(x, y);
    }

    /**
     * Парсит DragonHead из JSON
     */
    private DragonHead parseHead(String json) {
        try {
            float size = extractFloatValue(json, "\"size\"");
            float toothCount = extractFloatValue(json, "\"toothCount\"");
            return new DragonHead(size, toothCount);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Серилизует Coordinates в JSON формат
     */
    private String serializeCoordinates(Dragon dragon) {
        return String.format("{\"x\": %s, \"y\": %s}",
                dragon.getCoordinates().getX(),
                dragon.getCoordinates().getY());
    }

    /**
     * Сериализует DragonHead в JSON формат
     */
    private String serializeDragonHead(DragonHead head) {
        if (head == null) {
            return "null";
        }
        return String.format("{\"size\": %s, \"toothCount\": %s}",
                head.getSize(),
                head.getToothCount());
    }

    /**
     * Разбивает JSON объекты в массиве
     */
    private String[] splitJsonObjects(String json) {
        java.util.List<String> objects = new java.util.ArrayList<>();
        int braceCount = 0;
        StringBuilder current = new StringBuilder();

        for (char c : json.toCharArray()) {
            if (c == '{') {
                braceCount++;
                current.append(c);
            } else if (c == '}') {
                current.append(c);
                braceCount--;
                if (braceCount == 0) {
                    objects.add(current.toString());
                    current = new StringBuilder();
                }
            } else if (braceCount > 0) {
                current.append(c);
            }
        }

        return objects.toArray(new String[0]);
    }

    /**
     * Извлекает строковое значение из JSON
     */
    private String extractStringValue(String json, String key) {
        int startIdx = json.indexOf(key);
        if (startIdx == -1)
            return null;

        startIdx = json.indexOf("\"", startIdx + key.length());
        if (startIdx == -1)
            return null;

        int endIdx = json.indexOf("\"", startIdx + 1);
        if (endIdx == -1)
            return null;

        return json.substring(startIdx + 1, endIdx);
    }

    /**
     * Извлекает целое число из JSON
     */
    private int extractIntValue(String json, String key) {
        int startIdx = json.indexOf(key);
        if (startIdx == -1)
            return 0;

        startIdx = json.indexOf(":", startIdx) + 1;
        int endIdx = json.indexOf(",", startIdx);
        if (endIdx == -1) {
            endIdx = json.indexOf("}", startIdx);
        }

        return Integer.parseInt(json.substring(startIdx, endIdx).trim());
    }

    /**
     * Извлекает длинное целое число из JSON
     */
    private long extractLongValue(String json, String key) {
        int startIdx = json.indexOf(key);
        if (startIdx == -1)
            return 0L;

        startIdx = json.indexOf(":", startIdx) + 1;
        int endIdx = json.indexOf(",", startIdx);
        if (endIdx == -1) {
            endIdx = json.indexOf("}", startIdx);
        }

        return Long.parseLong(json.substring(startIdx, endIdx).trim());
    }

    /**
     * Извлекает float значение из JSON
     */
    private float extractFloatValue(String json, String key) {
        int startIdx = json.indexOf(key);
        if (startIdx == -1)
            return 0f;

        startIdx = json.indexOf(":", startIdx) + 1;
        int endIdx = json.indexOf(",", startIdx);
        if (endIdx == -1) {
            endIdx = json.indexOf("}", startIdx);
        }

        return Float.parseFloat(json.substring(startIdx, endIdx).trim());
    }

    /**
     * Извлекает объект из JSON (например координаты или голову)
     */
    private String extractObjectValue(String json, String key) {
        int startIdx = json.indexOf(key);
        if (startIdx == -1)
            return "{}";

        startIdx = json.indexOf("{", startIdx);
        if (startIdx == -1)
            return "{}";

        int braceCount = 1;
        int endIdx = startIdx + 1;

        while (endIdx < json.length() && braceCount > 0) {
            if (json.charAt(endIdx) == '{') {
                braceCount++;
            } else if (json.charAt(endIdx) == '}') {
                braceCount--;
            }
            endIdx++;
        }

        return json.substring(startIdx, endIdx);
    }

    /**
     * Экранирует специальные символы в JSON строках
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
