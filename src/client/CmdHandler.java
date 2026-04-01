package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.log4j.Logger;

import client.RequestClient;

public class CmdHandler implements Runnable {

    public CmdHandler(RequestClient requestClient) {
        this.requestClient = requestClient;
    }

    @Override
    public void run() {
        System.out.println("Welcome to the Dragon database client!");
        System.out.println("Type commands or 'exit' to quit.");

        while (true) {
            try {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("exit")) break;

                if (line.startsWith("execute_script")) {
                    handleExecuteScript(line);
                } else {
                    sendCommand(line);
                }
            } catch (IOException e) {
                logger.error(e.getStackTrace());
            }
        }
    }

    private void sendCommand(String commandLine) {
        try {
            byte[] response = requestClient.send(commandLine);
            String responseStr = new String(response, core.Defaults.CHARSET);
            System.out.println(responseStr);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleExecuteScript(String line) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2) {
            System.err.println("You must provide a script file name.");
            return;
        }
        String raw = parts[1].trim();
        String pathStr = stripQuotes(raw);

        Path path;
        try {
            path = Paths.get(pathStr);
            if (!path.isAbsolute()) {
                path = Paths.get("").toAbsolutePath().resolve(path);
            }
            path = path.normalize();
        } catch (Exception e) {
            System.err.println("Invalid script file path.");
            return;
        }

        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            System.err.println("Script file not found or not readable: " + path);
            return;
        }

        if (openScripts.size() >= MAX_SCRIPT_DEPTH) {
            System.err.println("Maximum script recursion depth reached: " + MAX_SCRIPT_DEPTH);
            return;
        }

        if (openScripts.contains(path)) {
            System.err.println("Recursion detected: script is already being executed: " + path);
            return;
        }

        try {
            String content = Files.readString(path);
            openScripts.push(path);
            System.out.println("Executing script: " + path);
            byte[] response = requestClient.sendScript(content);
            String responseStr = new String(response, core.Defaults.CHARSET);
            System.out.println(responseStr);
            openScripts.pop();
        } catch (Exception e) {
            System.err.println("Failed to execute script: " + e.getMessage());
        }
    }

    private String stripQuotes(String s) {
        if (s.length() >= 2
                && ((s.startsWith("\"") && s.endsWith("\""))
                        || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static final Logger logger = Logger.getLogger(CmdHandler.class);
    private static final int MAX_SCRIPT_DEPTH = 5;
    private RequestClient requestClient;
    private final Deque<Path> openScripts = new ArrayDeque<>();
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
}
