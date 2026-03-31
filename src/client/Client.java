package client;

import java.io.OutputStreamWriter;
import java.nio.file.InvalidPathException;

import org.apache.log4j.Logger;

import collection.RemoteCollection;
import storage.RemoteStorage;
import ui.TextUIHandler;
import ui.UI;

public class Client {
    public static void main(String[] args) {
        String serverHost = null;
        String serverPort = null;
        int port = core.Defaults.SERVER_PORT;

        try {
            serverHost = System.getenv("SERVER_HOST");
            serverPort = System.getenv("SERVER_PORT");
        } catch (InvalidPathException e) {
            e.printStackTrace();
        }

        if (serverHost == null) {
            serverHost = core.Defaults.SERVER_HOST;
            logger.warn(
                    "Environment variable SERVER_HOST not found! Using default: \""
                            + serverHost
                            + "\"");
        }
        if (serverPort == null) {
            logger.warn(
                    "Environment variable SERVER_PORT not found! Using default: \""
                            + core.Defaults.SERVER_PORT
                            + "\"");
        } else {

            try {
                port = Integer.parseInt(serverPort);
            } catch (NumberFormatException e) {
                logger.warn("Invalid SERVER_PORT, using default: " + port);
            }
        }

        // Create a single RequestClient instance to be shared across all components
        RequestClient requestClient;
        try {
            requestClient = new RequestClient(serverHost, port);
            logger.info("Connected to server at " + serverHost + ":" + port);
        } catch (Exception e) {
            logger.warn("Failed to initialize request client: " + e.getMessage());
            requestClient = null;
        }

        // Pass the shared RequestClient to RemoteCollection and RemoteStorage
        RemoteCollection collection;
        RemoteStorage storage;

        if (requestClient != null) {
            collection = new RemoteCollection(requestClient);
            storage = new RemoteStorage(requestClient);
        } else {
            // Fallback to default constructors if RequestClient initialization failed
            collection = new RemoteCollection();
            storage = new RemoteStorage();
        }

        var ui = new UI(collection, storage, new OutputStreamWriter(System.out));
        var textUiHandler = new TextUIHandler(ui);

        // Pass the same RequestClient to TextUIHandler for buffering support
        if (requestClient != null) {
            textUiHandler.setRequestClient(requestClient);
        }

        textUiHandler.run();
    }

    private static final Logger logger = Logger.getLogger(Client.class);
}
