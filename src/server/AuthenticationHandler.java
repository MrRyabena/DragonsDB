package server;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import core.ParameterRequest;
import core.Request;
import core.Response;

/**
 * Pre-command authentication handler.
 *
 * <p>Runs before {@link CommandsHandler}. In case of auth-only requests or auth failures, it
 * prepares {@link ServerContext#response} and sets {@link ServerContext#skipCommandHandling}.
 */
public class AuthenticationHandler implements Consumer<ServerContext> {

    public AuthenticationHandler(SessionManager sessionManager, AuthService authService) {
        this.sessionManager = sessionManager;
        this.authService = authService;
    }

    @Override
    public void accept(ServerContext context) {
        if (context.request == null) {
            context.response = Response.error("Invalid request: null");
            context.skipCommandHandling = true;
            return;
        }

        ClientSession session = sessionManager.getOrCreateSession(context.clientAddress);
        session.updateActivity();
        context.sessionId = session.getSessionId();

        switch (context.request.status) {
            case INIT -> {
                handleInit(session, context);
                context.skipCommandHandling = true;
            }
            case COMMAND -> handleCommand(session, context);
            case PARAMETER_RESPONSE -> handleParameterResponse(session, context);
            default -> {
                context.response = Response.error("Unknown request status: " + context.request.status);
                context.skipCommandHandling = true;
            }
        }
    }

    private void handleInit(ClientSession session, ServerContext context) {
        if (isRequestAuthenticated(context.request)) {
            session.setAuthenticatedLogin(context.request.login);
            context.response =
                    Response.success(List.of(), "Authorized as: " + session.getAuthenticatedLogin());
            return;
        }

        context.response =
                Response.success(
                        List.of(),
                        "User is not authorized. Use login or register command to continue.");
    }

    private void handleCommand(ClientSession session, ServerContext context) {
        if (context.request.command == null || context.request.command.isBlank()) {
            context.response = Response.error("Empty command");
            context.skipCommandHandling = true;
            return;
        }

        String commandLine = context.request.command.trim();
        String[] parts = commandLine.split("\\s+", 2);
        String cmdStr = parts[0].toLowerCase(Locale.ROOT);
        String args = parts.length > 1 ? parts[1].trim() : "";

        if ("login".equals(cmdStr) || "register".equals(cmdStr)) {
            handleAuthCommand(session, context, cmdStr, args);
            context.skipCommandHandling = true;
            return;
        }

        if (!isRequestAuthenticated(context.request)) {
            context.response = Response.error("User is not authorized. Use register/login first.");
            context.skipCommandHandling = true;
            return;
        }

        session.setAuthenticatedLogin(context.request.login);
        context.skipCommandHandling = false;
    }

    private void handleParameterResponse(ClientSession session, ServerContext context) {
        if (session.isAuthDialogActive()) {
            handleAuthParameterResponse(session, context);
            context.skipCommandHandling = true;
            return;
        }

        if (!isRequestAuthenticated(context.request)) {
            context.response = Response.error("User is not authorized. Use register/login first.");
            session.clearDialog();
            context.skipCommandHandling = true;
            return;
        }

        session.setAuthenticatedLogin(context.request.login);
        context.skipCommandHandling = false;
    }

    private void handleAuthCommand(
            ClientSession session, ServerContext context, String command, String args) {
        String[] parts = args.isBlank() ? new String[0] : args.split("\\s+", 2);
        if (parts.length == 2) {
            executeAuthCommand(session, context, command, parts[0], parts[1]);
            return;
        }

        session.startAuthDialog(
                "register".equals(command)
                        ? ClientSession.AuthAction.REGISTER
                        : ClientSession.AuthAction.LOGIN);
        session.addParameterRequest(
                new ParameterRequest(
                        "auth_login",
                        "Enter login:",
                        ParameterRequest.ParameterType.STRING,
                        true));
        session.addParameterRequest(
                new ParameterRequest(
                        "auth_password",
                        "Enter password:",
                        ParameterRequest.ParameterType.STRING,
                        true));
        context.response = Response.needParameter(session.getSessionId(), session.peekNextParameter());
    }

    private void handleAuthParameterResponse(ClientSession session, ServerContext context) {
        ParameterRequest currentParam = session.peekNextParameter();
        if (currentParam == null) {
            session.clearAuthDialog();
            context.response = Response.error("Unexpected authentication response");
            return;
        }

        String value = context.request.parameterValue;
        if (value == null || value.isBlank()) {
            context.response = Response.error("Invalid input: Parameter cannot be empty");
            return;
        }

        session.addParameter(currentParam.parameterName, value.trim());
        session.pollNextParameter();

        if (session.hasMoreParameters()) {
            context.response = Response.needParameter(session.getSessionId(), session.peekNextParameter());
            return;
        }

        String loginValue = (String) session.getParameter("auth_login");
        String passwordValue = (String) session.getParameter("auth_password");
        ClientSession.AuthAction action = session.getAuthAction();
        session.clearAuthDialog();

        executeAuthCommand(
                session,
                context,
                action == ClientSession.AuthAction.REGISTER ? "register" : "login",
                loginValue,
                passwordValue);
    }

    private void executeAuthCommand(
            ClientSession session,
            ServerContext context,
            String command,
            String login,
            String password) {
        try {
            authService.validateCredentials(login, password);
        } catch (IllegalArgumentException e) {
            context.response = Response.error("Invalid input: " + e.getMessage());
            return;
        }

        String normalizedCommand = command.toLowerCase(Locale.ROOT);
        if ("register".equals(normalizedCommand)) {
            boolean registered = authService.register(login, password);
            if (!registered) {
                context.response = Response.error("User already exists");
                return;
            }
            session.setAuthenticatedLogin(login);
            context.response = Response.success(List.of(), "Registration successful");
            return;
        }

        if (!authService.authenticate(login, password)) {
            context.response = Response.error("Invalid login or password");
            return;
        }

        session.setAuthenticatedLogin(login);
        context.response = Response.success(List.of(), "Login successful");
    }

    private boolean isRequestAuthenticated(Request request) {
        if (request == null) {
            return false;
        }
        return authService.authenticate(request.login, request.password);
    }

    private static final Logger logger = Logger.getLogger(AuthenticationHandler.class);
    private final SessionManager sessionManager;
    private final AuthService authService;
}
