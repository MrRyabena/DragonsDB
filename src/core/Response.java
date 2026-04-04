package core;

import java.io.Serializable;
import java.util.List;

import dragon.Dragon;
import server.ParameterRequest;

/**
 * Server response to client requests.
 * Can contain result data, error message, or parameter request for interactive commands.
 */
public class Response implements Serializable {
    public enum Status {
        SUCCESS,           // Command executed successfully
        ERROR,             // Command failed
        NEED_PARAMETER     // Server is requesting a parameter from client
    }

    public Status status;
    public List<Dragon> data;  // null if not applicable
    public String message;  // null if not applicable
    public ParameterRequest parameterRequest;  // null if not NEED_PARAMETER response
    public long sessionId;  // Session ID for tracking dialog state

    public Response() {
        this.status = Status.SUCCESS;
        this.data = null;
        this.message = null;
        this.parameterRequest = null;
        this.sessionId = 0;
    }

    public static Response success(List<Dragon> dragons, String message) {
        Response resp = new Response();
        resp.status = Status.SUCCESS;
        resp.data = dragons;
        resp.message = message;
        return resp;
    }

    public static Response error(String errorMessage) {
        Response resp = new Response();
        resp.status = Status.ERROR;
        resp.message = errorMessage;
        return resp;
    }

    public static Response needParameter(long sessionId, ParameterRequest param) {
        Response resp = new Response();
        resp.status = Status.NEED_PARAMETER;
        resp.sessionId = sessionId;
        resp.parameterRequest = param;
        return resp;
    }
}
