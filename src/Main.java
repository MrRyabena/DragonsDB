import java.nio.file.InvalidPathException;

import core.Coordinates;
import dragon.Dragon;
import dragon.DragonHead;
import dragon.DragonType;
import storage.FileStorage;

public class Main {

    public static void main(String args[]) {
        var collection = new collection.Collection();

        collection.add(
                new Dragon(
                        "Smaug",
                        new Coordinates(10.0f, 20.0f),
                        500,
                        10000,
                        DragonType.FIRE,
                        new DragonHead(2.0f, 5.0f)));
        collection.add(
                new Dragon(
                        "Charlie",
                        new Coordinates(10.0f, 20.0f),
                        100,
                        10000,
                        DragonType.FIRE,
                        new DragonHead(2.0f, 5.0f)));

        try {
            var env = System.getenv("STORAGE_PATH");
            System.err.println(env);
            var storage =
                    new FileStorage(
                            java.nio.file.FileSystems.getDefault()
                                    .getPath(env));
            try {
                storage.save(collection.getStream());
            } catch (Exception e) {
                e.printStackTrace();
                // TODO
            }
        } catch (InvalidPathException e) {
            e.printStackTrace();

        }
    }
}
