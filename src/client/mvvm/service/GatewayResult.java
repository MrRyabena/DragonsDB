package client.mvvm.service;

import java.util.List;

import core.Response;
import dragon.Dragon;

/** Unified response model for view models. */
public class GatewayResult {
    public final Response.Status status;
    public final String message;
    public final List<Dragon> dragons;

    public GatewayResult(Response.Status status, String message, List<Dragon> dragons) {
        this.status = status;
        this.message = message;
        this.dragons = dragons;
    }

    public boolean isSuccess() {
        return status == Response.Status.SUCCESS;
    }

    public static GatewayResult fromResponse(Response response) {
        return new GatewayResult(response.status, response.message, response.data);
    }

    public static GatewayResult error(String message) {
        return new GatewayResult(Response.Status.ERROR, message, List.of());
    }
}
