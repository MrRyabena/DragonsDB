package client;

import org.apache.log4j.Logger;

import client.gui.JavaFxClientApp;

public class Client {
    public static void main(String[] args) {
        for (String arg : args) {
            if ("--gui".equalsIgnoreCase(arg) || "--fx".equalsIgnoreCase(arg)) {
                launchGui(args);
                return;
            }
        }

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

    private static void launchGui(String[] args) {
        try {
            Class<?> applicationClass = Class.forName("javafx.application.Application");
            java.lang.reflect.Method launchMethod =
                    applicationClass.getMethod("launch", Class.class, String[].class);
            launchMethod.invoke(null, JavaFxClientApp.class, args);
        } catch (ReflectiveOperationException e) {
            logger.error("Failed to launch JavaFX client", e);
        }
    }
}
