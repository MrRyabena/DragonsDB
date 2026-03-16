package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import dragon.Dragon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Хранилище коллекции Dragon в JSON-файле. Использует библиотеку Gson; объем кода заметно меньше по
 * сравнению с ручным парсингом, при этом формат остается стандартным и расширяемым. Метаданные
 * коллекции (время создания/изменения) не сериализуются, поскольку легко читаются из файловой
 * системы через Files.getLastModifiedTime(...).
 */
public class FileStorage implements Storage {
    private final Path filename;

    /* Gson and type for (de)serialization */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type collectionType = new TypeToken<Set<Dragon>>() {}.getType();

    public FileStorage(Path filename) {
        this.filename = filename;
    }

    // @Override
    // public void save(collection.Collection collection) throws IOException {
    //     try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename.toFile()))) {
    //         writer.write(gson.toJson(collection.getBuffer(), collectionType));
    //     }
    // }

    @Override
    public void save(InputStream stream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename.toFile()))) {
            stream.forEachOrdered();
        }
    }

    @Override
    public OutputStream load() throws IOException {
        if (!Files.exists(filename)) {
            return Stream.empty();
        }

        try (var reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(filename.toFile())))) {

            return reader.lines();
        }
    }
}
