package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.log4j.Logger;

import core.Request;
import core.Response;
import dragon.view.StringView;
import dragon.view.View;

public class CmdHandler implements Runnable {

    public CmdHandler(RequestClient requestClient) {
        this.requestClient = requestClient;
        readers.push(new BufferedReader(new InputStreamReader(System.in)));
    }

    @Override
    public void run() {
        while (true) {
            try {
                String line = readers.peek().readLine();
                if (line == null) {
                    if (!closeCurrentScriptIfAny()) {
                        return;
                    }
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("exit")) break;

                if (line.startsWith("execute_script")) {
                    handleExecuteScript(line);
                } else {
                    sendInput(line);
                }
            } catch (IOException e) {
                logger.error(e.getStackTrace());
            }
        }
    }

    private boolean closeCurrentScriptIfAny() {
        if (readers.size() <= 1) {
            return false;
        }

        BufferedReader reader = readers.pop();
        try {
            reader.close();
        } catch (IOException e) {
        }
        return true;
    }

    private void sendInput(String line) {
        Request request = new Request();
        request.status = Request.Status.GET;
        request.request = line;
        try {
            Response response = requestClient.sendRequest(request);  
            if (response.data.isPresent()) 
            {
                try (InputStreamReader reader = new InputStreamReader(outView.toView(response.data.get()))) {
                    reader.transferTo(out);
                } catch (IOException e) {
                    logger.error("Failed to display response: " + e.getMessage(), e);
                }             
        
            }
        } catch (IllegalStateException e) {
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

        if (readers.size() >= MAX_SCRIPT_DEPTH) {
            System.err.println("Maximum script recursion depth reached: " + MAX_SCRIPT_DEPTH);
            return;
        }

        if (readers.contains(path)) {
            System.err.println("Recursion detected: script is already being executed: " + path);
            return;
        }

        try {
            System.out.println("Executing script: " + path);
            readers.push(Files.newBufferedReader(path));
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
    private final RequestClient requestClient;
    private final Deque<BufferedReader> readers = new ArrayDeque<>();
    private Writer out;
    private View outView = new StringView();
}
