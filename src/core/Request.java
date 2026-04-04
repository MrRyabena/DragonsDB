package core;

import java.io.Serializable;

/** Client request to server. Can contain a command, parameter response, or other actions. */
public class Request implements Serializable {
    public enum Status {
        INIT, // Initial connection (unused for now)
        COMMAND, // Client is sending a command to execute
        PARAMETER_RESPONSE, // Client is responding to a parameter request
        GET_SCRIPT, // Client is requesting a script file (for execute_script)
    }

    public Status status;
    public String command; // For COMMAND: the command text
    public String parameterValue; // For PARAMETER_RESPONSE: the parameter value entered by user
    public long sessionId; // Session ID (for server to track dialog state)

    public Request() {
        this.status = Status.COMMAND;
        this.sessionId = 0;
    }

    public static Request command(String commandText) {
        Request req = new Request();
        req.status = Status.COMMAND;
        req.command = commandText;
        return req;
    }

    public static Request parameterResponse(long sessionId, String parameterValue) {
        Request req = new Request();
        req.status = Status.PARAMETER_RESPONSE;
        req.sessionId = sessionId;
        req.parameterValue = parameterValue;
        return req;
    }
}
