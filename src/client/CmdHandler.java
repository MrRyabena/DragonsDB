package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
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

public class CmdHandler implements Runnable {

    public CmdHandler(RequestClient requestClient) {
        this.requestClient = requestClient;
        readers.push(new BufferedReader(new InputStreamReader(System.in)));
    }

    @Override
    public void run() {
        sendInitRequest();

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

        if (readers.size() == 1) {
            consoleWriter =
                    new OutputStreamWriter(
                            System.out); // Restore console output when returning to main input
        }

        try {
            consoleWriter.write("Finished executing script.\n");
            consoleWriter.flush();
        } catch (IOException e) {
            logger.error("Failed to write completion message: " + e.getMessage(), e);
        }

        return true;
    }

    private void sendInput(String line) {
        try {
            String stagedLogin = null;
            String stagedPassword = null;
            boolean authCommand = isAuthCommand(line);

            String[] parsedCredentials = parseCredentialsFromAuthCommand(line);
            String requestLogin = login;
            String requestPassword = password;
            if (parsedCredentials != null) {
                stagedLogin = parsedCredentials[0];
                stagedPassword = parsedCredentials[1];
                requestLogin = stagedLogin;
                requestPassword = stagedPassword;
            }

            // Start command execution
            Request request = Request.command(line, requestLogin, requestPassword);
            Response response = requestClient.sendRequest(request);

            // Interactive dialog loop for parameters
            long sessionId = 0; // Will be set by server
            while (response.status == Response.Status.NEED_PARAMETER) {
                if (response.parameterRequest != null) {
                    Object paramReqObj = response.parameterRequest;
                    sessionId = response.sessionId; // Track session

                    // Extract prompt and required flag using reflection
                    String prompt = "Enter value:";
                    boolean required = true;
                    try {
                        prompt =
                                (String) paramReqObj.getClass().getField("prompt").get(paramReqObj);
                        required =
                                (boolean)
                                        paramReqObj
                                                .getClass()
                                                .getField("required")
                                                .get(paramReqObj);
                    } catch (Exception e) {
                        logger.warn("Failed to get parameter details: " + e.getMessage());
                    }

                    // Display prompt and read user input
                    consoleWriter.write(prompt + " ");
                    consoleWriter.flush();

                    String paramValue = readers.peek().readLine();

                    if (paramValue == null || paramValue.trim().isEmpty()) {
                        if (required) {
                            System.err.println("This parameter is required.");
                            continue;
                        }
                        paramValue = "";
                    }

                    if (authCommand) {
                        String normalizedPrompt = prompt.toLowerCase();
                        if (normalizedPrompt.contains("login") || normalizedPrompt.contains("user")) {
                            stagedLogin = paramValue.trim();
                            requestLogin = stagedLogin;
                        } else if (normalizedPrompt.contains("password") || normalizedPrompt.contains("парол")) {
                            stagedPassword = paramValue;
                            requestPassword = stagedPassword;
                        }
                    }

                    // Send parameter response back to server
                    Request paramResponse =
                            Request.parameterResponse(
                                    sessionId, paramValue.trim(), requestLogin, requestPassword);
                    response = requestClient.sendRequest(paramResponse);
                } else {
                    System.err.println(
                            "Server requested parameter but didn't provide specification.");
                    break;
                }
            }

            if (response.status == Response.Status.SUCCESS && authCommand) {
                if (stagedLogin != null && !stagedLogin.isBlank()) {
                    login = stagedLogin;
                }
                if (stagedPassword != null && !stagedPassword.isBlank()) {
                    password = stagedPassword;
                }
            }

            // Handle final response
            if (response.status == Response.Status.SUCCESS) {
                // Display message
                if (response.message != null && !response.message.isEmpty()) {
                    consoleWriter.write(response.message + "\n");
                    consoleWriter.flush();
                }
                // Display dragons if present
                if (response.data != null && !response.data.isEmpty()) {
                    StringView view = new StringView();
                    try (Reader reader =
                            new InputStreamReader(
                                    new StringView().toView(response.data.stream()))) {
                        Writer writer = new OutputStreamWriter(System.out);
                        reader.transferTo(writer);
                        writer.flush();
                    } catch (IOException e) {
                        logger.error("Failed to display response: " + e.getMessage(), e);
                    }
                }
            } else if (response.status == Response.Status.ERROR) {
                if (response.message != null && !response.message.isEmpty()) {
                    System.err.println("Server error: " + response.message);
                } else {
                    System.err.println("Server error: Unknown error");
                }
            }
        } catch (IOException e) {
            logger.error("Failed to send input: " + e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void sendInitRequest() {
        try {
            Request initRequest = new Request();
            initRequest.status = Request.Status.INIT;
            initRequest.login = login;
            initRequest.password = password;

            Response response = requestClient.sendRequest(initRequest);
            if (response != null && response.message != null && !response.message.isEmpty()) {
                consoleWriter.write(response.message + "\n");
                consoleWriter.flush();
            }
        } catch (Exception e) {
            logger.warn("Failed to perform init handshake: " + e.getMessage());
        }
    }

    private boolean isAuthCommand(String line) {
        String normalized = line.trim().toLowerCase();
        return normalized.equals("login")
                || normalized.startsWith("login ")
                || normalized.equals("register")
                || normalized.startsWith("register ");
    }

    private String[] parseCredentialsFromAuthCommand(String line) {
        if (!isAuthCommand(line)) {
            return null;
        }

        String[] parts = line.trim().split("\\s+", 3);
        if (parts.length < 3) {
            return null;
        }

        String parsedLogin = parts[1].trim();
        String parsedPassword = parts[2];
        if (parsedLogin.isEmpty() || parsedPassword.isBlank()) {
            return null;
        }

        return new String[] {parsedLogin, parsedPassword};
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
            consoleWriter.write("Executing script: " + path + "\n");
            consoleWriter.flush();
            readers.push(Files.newBufferedReader(path));
            consoleWriter = OutputStreamWriter.nullWriter(); // Suppress output from scripts
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
    private Writer consoleWriter = new OutputStreamWriter(System.out);
    private String login;
    private String password;
}
