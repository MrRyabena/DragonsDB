package storage;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * Хранилище коллекции Dragon в JSON-файле. Использует библиотеку Gson; объем
 * кода заметно меньше по
 * сравнению с ручным парсингом, при этом формат остается стандартным и
 * расширяемым. Метаданные
 * коллекции (время создания/изменения) не сериализуются, поскольку легко
 * читаются из файловой
 * системы через Files.getLastModifiedTime(...).
 */
public class FileStorage implements Storage {

    public FileStorage(Path filename) {
        this.filename = filename;
    }

    @Override
    public void save(InputStream stream) {
        try (var reader = new InputStreamReader(stream, core.Defaults.CHARSET);
                var writer = new BufferedWriter(new FileWriter(filename.toFile()))) {
            reader.transferTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public OutputStream load() {

        var baos = new ByteArrayOutputStream();

        try (var reader = new InputStreamReader(new FileInputStream(filename.toFile()));
                var writer = new OutputStreamWriter(baos, core.Defaults.CHARSET)) {
            reader.transferTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos;
    }

    @Override
    public Date getDateCreated() {
        try {
            new Date(((FileTime) (Files.getAttribute(filename, "creationTime"))).toMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Date getDateModified() {
        return new Date(filename.toFile().lastModified());
    }

    private final Path filename;
}
