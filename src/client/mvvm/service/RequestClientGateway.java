package client.mvvm.service;

import org.apache.log4j.Logger;

import client.RequestClient;
import client.mvvm.model.Credentials;
import core.Request;
import core.Response;

/**
 * Implementation of ClientGateway using UDP-based RequestClient.
 *
 * <p>Sends commands to server via socket communication and handles server responses,
 * including interactive parameter requests. Bridges the UDP protocol layer with
 * the high-level MVVM service interface.
 */
public class RequestClientGateway implements ClientGateway {
    private static final Logger logger = Logger.getLogger(RequestClientGateway.class);

    private final RequestClient requestClient;

    public RequestClientGateway(RequestClient requestClient) {
        this.requestClient = requestClient;
    }

    @Override
    public GatewayResult sendCommand(
            String command, Credentials credentials, ParameterValueProvider provider) {
        try {
            Response response =
                    requestClient.sendRequest(
                            Request.command(command, credentials.login(), credentials.password()));

            while (response.status == Response.Status.NEED_PARAMETER
                    || response.status == Response.Status.NEED_PASSWORD) {
                if (response.parameterRequest == null) {
                    return GatewayResult.error(
                            "Server requested parameter but did not provide specification");
                }

                if (provider == null) {
                    return GatewayResult.error(
                            "Interactive parameter is required: "
                                    + response.parameterRequest.parameterName);
                }

                boolean passwordInput = response.status == Response.Status.NEED_PASSWORD;
                String value = provider.provide(response.parameterRequest, passwordInput);
                if (value == null) {
                    value = "";
                }

                response =
                        requestClient.sendRequest(
                                Request.parameterResponse(
                                        response.sessionId,
                                        value,
                                        credentials.login(),
                                        credentials.password()));
            }

            return GatewayResult.fromResponse(response);
        } catch (Exception e) {
            logger.error("Failed to execute command: " + command, e);
            return GatewayResult.error(e.getMessage());
        }
    }
}
