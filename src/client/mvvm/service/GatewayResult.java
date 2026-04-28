package client.mvvm.service;

import java.util.List;

import core.Response;
import dragon.Dragon;

/**
 * Unified response model bridging server Response to client view models.
 * 
 * <p>Encapsulates command results including dragons collection and associated owner information
 * in a format optimized for MVVM bindings and state updates.
 */
public class GatewayResult {
    /** Response status from server */
    public final Response.Status status;
    /** Result message or error description */
    public final String message;
    /** Dragons returned by command */
    public final List<Dragon> dragons;
    /** Owner logins for dragons (index-aligned) */
    public final List<String> ownerLogins;

    /**
     * Constructs a gateway result.
     *
     * @param status response status
     * @param message result or error message
     * @param dragons collection of dragons (may be empty)
     * @param ownerLogins owner logins corresponding to dragons
     */
    public GatewayResult(
            Response.Status status, String message, List<Dragon> dragons, List<String> ownerLogins) {
        this.status = status;
        this.message = message;
        this.dragons = dragons;
        this.ownerLogins = ownerLogins;
    }

    /**
     * Checks whether the result represents success.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Response.Status.SUCCESS;
    }

    /**
     * Converts server Response to GatewayResult.
     *
     * @param response server response
     * @return gateway result with status, message, dragons, and owner data
     */
    public static GatewayResult fromResponse(Response response) {
        return new GatewayResult(response.status, response.message, response.data, response.ownerLogins);
    }

    /**
     * Creates an error result.
     *
     * @param message error message
     * @return error gateway result with empty dragon and owner lists
     */
    public static GatewayResult error(String message) {
        return new GatewayResult(Response.Status.ERROR, message, List.of(), List.of());
    }
}
