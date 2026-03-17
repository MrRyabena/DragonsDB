import java.nio.file.InvalidPathException;
import java.io.OutputStreamWriter;

import dragon.view.JsonView;
import storage.FileStorage;
import ui.UI;
import ui.TextUIHandler;

public class Main {

    public static void main(String args[]) {

        String storage_path = null;
        collection.Collection collection = null;

        try {
            storage_path = System.getenv("STORAGE_PATH");
        } catch (InvalidPathException e) {
            e.printStackTrace();

        }

        if (storage_path == null) {
            storage_path = core.Defaults.STORAGE_PATH;
            System.err.printf("Environment variable STORAGE_PATH not found! Using default path: \"%s\"\n", storage_path);
        }

        var storage = new FileStorage(java.nio.file.FileSystems.getDefault().getPath(storage_path));
        var jsonView = new JsonView();

        

        try {
            //storage.save(jsonView.toView(collection.getStream()));
            collection = new collection.Collection(jsonView.fromView(storage.load()));
        } catch (Exception e) {
            e.printStackTrace();
            // TODO
        }

        var ui = new UI(collection, storage, new OutputStreamWriter(System.out));
        var textUIHandler = new TextUIHandler(ui);

        textUIHandler.run();
    }
}
