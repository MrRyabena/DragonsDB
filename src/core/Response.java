package core;

import java.io.Serializable;
import java.util.List;

import dragon.Dragon;

/**
 * Server response to client requests with support for result data and owner information.
 * 
 * <p>Responses can contain dragon collection data with associated owner logins (index-aligned),
 * error messages for failed commands, parameter requests for interactive prompts, or password
 * requests. Supports various response statuses to guide client behavior.
 */
public class Response implements Serializable {
    /**
     * Response status codes.
     */
    public enum Status {
        /** Command executed successfully */
        SUCCESS,
        /** Command failed */
        ERROR,
        /** Server is requesting a parameter from client */
        NEED_PARAMETER,
        /** Server is requesting a password (should be hidden in console) */
        NEED_PASSWORD
    }

    /** Response status indicating success, error, or parameter request */
    public Status status;
    /** Collection of dragons returned by the command (null if not applicable) */
    public List<Dragon> data;
    /** Owner logins for dragons, parallel array aligned with data by index (null if not applicable) */
    public List<String> ownerLogins;
    /** Message describing the result or error (null if not applicable) */
    public String message;
    /** Parameter request for interactive commands (null if not NEED_PARAMETER response) */
    public ParameterRequest parameterRequest;
    /** Session ID for tracking dialog state across request-response cycles */
    public long sessionId;

    public Response() {
        this.status = Status.SUCCESS;
        this.data = null;
        this.ownerLogins = null;
        this.message = null;
        this.parameterRequest = null;
        this.sessionId = 0;
    }

    /**
     * Creates a successful response with dragon data.
     *
     * @param dragons list of dragons returned by the command (may be empty)
     * @param message descriptive message about the result
     * @return success response
     */
    public static Response success(List<Dragon> dragons, String message) {
        Response resp = new Response();
        resp.status = Status.SUCCESS;
        resp.data = dragons;
        resp.message = message;
        return resp;
    }

    /**
     * Creates a successful response with dragon data and owner information.
     *
     * <p>The ownerLogins list must be index-aligned with the dragons list:
     * ownerLogins.get(i) corresponds to dragons.get(i).getId().
     *
     * @param dragons list of dragons returned by the command (may be empty)
     * @param ownerLogins list of owner logins corresponding to each dragon (index-aligned)
     * @param message descriptive message about the result
     * @return success response with owner information
     */
    public static Response success(List<Dragon> dragons, List<String> ownerLogins, String message) {
        Response resp = new Response();
        resp.status = Status.SUCCESS;
        resp.data = dragons;
        resp.ownerLogins = ownerLogins;
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

    public static Response needPassword(long sessionId, ParameterRequest param) {
        Response resp = new Response();
        resp.status = Status.NEED_PASSWORD;
        resp.sessionId = sessionId;
        resp.parameterRequest = param;
        return resp;
    }
}
