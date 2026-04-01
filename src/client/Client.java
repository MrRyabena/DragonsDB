package client;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

public class Client {
    public static void main(String[] args) {
        String serverHost = System.getenv("SERVER_HOST");
        if (serverHost == null) {
            serverHost = core.Defaults.SERVER_HOST;
        }
        int serverPort = core.Defaults.SERVER_PORT;
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null) {
            try {
                serverPort = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                logger.warn("Invalid SERVER_PORT, using default: " + serverPort);
            }
        }

        try (RequestClient requestClient = new RequestClient(serverHost, serverPort)) {
            CmdHandler cmdHandler = new CmdHandler(requestClient);
            cmdHandler.run();
        } catch (Exception e) {
            logger.error("Client error", e);
        }
    }

    private static final Logger logger = Logger.getLogger(Client.class);
    private static final int MAX_SCRIPT_DEPTH = 5;
    private final Deque<Path> openScripts = new ArrayDeque<>();
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
}
