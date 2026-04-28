package core;

import java.io.Serializable;

/**
 * Client request to server carrying command execution or dialog responses.
 *
 * <p>Requests are serialized and sent over the network. A request can be:
 * <ul>
 *   <li>COMMAND - User-initiated command with login/password for auth
 *   <li>PARAMETER_RESPONSE - Response to server's parameter request (interactive input)
 *   <li>GET_SCRIPT - Request for script file content
 *   <li>INIT - Initial connection marker (unused currently)
 * </ul>
 */
public class Request implements Serializable {
    /**
     * Request type indicator.
     */
    public enum Status {
        /** Initial connection (unused for now) */
        INIT,
        /** Client is sending a command to execute */
        COMMAND,
        /** Client is responding to a parameter request */
        PARAMETER_RESPONSE,
        /** Client is requesting a script file (for execute_script) */
        GET_SCRIPT,
    }

    /** Request type */
    public Status status;
    /** Command text (for COMMAND requests) */
    public String command;
    /** Parameter value entered by user (for PARAMETER_RESPONSE requests) */
    public String parameterValue;
    /** Session ID for tracking dialog state across requests */
    public long sessionId;
    /** Username for authentication */
    public String login;
    /** Password for authentication */
    public String password;

    public Request() {
        this.status = Status.COMMAND;
        this.sessionId = 0;
    }

    /**
     * Creates a command request.
     *
     * @param commandText command to execute
     * @param login username for authentication
     * @param password password for authentication
     * @return request with COMMAND status
     */
    public static Request command(String commandText, String login, String password) {
        Request req = new Request();
        req.status = Status.COMMAND;
        req.command = commandText;
        req.login = login;
        req.password = password;
        return req;
    }

    /**
     * Creates a parameter response request (for interactive commands).
     *
     * @param sessionId session ID from server's parameter request
     * @param parameterValue user-entered parameter value
     * @param login username
     * @param password password
     * @return request with PARAMETER_RESPONSE status
     */
    public static Request parameterResponse(long sessionId, String parameterValue, String login, String password) {
        Request req = new Request();
        req.status = Status.PARAMETER_RESPONSE;
        req.sessionId = sessionId;
        req.parameterValue = parameterValue;
        req.login = login;
        req.password = password;
        return req;
    }
}
