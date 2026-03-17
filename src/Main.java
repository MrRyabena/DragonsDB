import java.io.OutputStreamWriter;
import java.nio.file.InvalidPathException;

import dragon.view.JsonView;
import storage.FileStorage;
import ui.TextUIHandler;
import ui.UI;

/**
 * Application entry point.
 *
 * <p>Bootstraps storage, loads the initial collection state, and starts the interactive text UI.
 */
public class Main {

    /**
     * Starts the application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String args[]) {

        String storage_path = null;
        collection.Collection collection = new collection.Collection();

        try {
            storage_path = System.getenv("STORAGE_PATH");
        } catch (InvalidPathException e) {
            e.printStackTrace();
        }

        if (storage_path == null) {
            storage_path = core.Defaults.STORAGE_PATH;
            System.err.printf(
                    "Environment variable STORAGE_PATH not found! Using default path: \"%s\"\n",
                    storage_path);
        }

        var storage = new FileStorage(java.nio.file.FileSystems.getDefault().getPath(storage_path));
        var jsonView = new JsonView();

        collection.add(jsonView.fromView(storage.load()));
      
        var ui = new UI(collection, storage, new OutputStreamWriter(System.out));
        var textUIHandler = new TextUIHandler(ui);

        textUIHandler.run();
    }
}
