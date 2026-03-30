package client;

import collection.RemoteCollection;
import java.io.OutputStreamWriter;
import java.nio.file.InvalidPathException;

import storage.RemoteStorage;
import ui.TextUIHandler;
import ui.UI;

public class Client {
    public static void main(String[] args) {
        String serverHost = null;
        String serverPort = null;
        try {
            serverHost = System.getenv("SERVER_HOST");
            serverPort = System.getenv("serverPort");
        } catch (InvalidPathException e) {
            e.printStackTrace();
        }

        if (serverHost == null) {
            serverHost = core.Defaults.SERVER_HOST;
            System.err.printf(
                    "Environment variable STORAGE_PATH not found! Using default path: \"%s\"\n",
                    serverHost);
        }
        if (serverPort == null) {
            serverPort = core.Defaults.SERVER_HOST;
            System.err.printf(
                    "Environment variable STORAGE_PATH not found! Using default path: \"%s\"\n",
                    serverPort);
        }

        var collection = new RemoteCollection();
        var storage = new RemoteStorage();
        var ui = new UI(collection, storage, new OutputStreamWriter(System.out));
        var textUiHandler = new TextUIHandler(ui);
        textUiHandler.run();
    }
}
