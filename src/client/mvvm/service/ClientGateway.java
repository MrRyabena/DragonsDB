package client.mvvm.service;

import client.mvvm.model.Credentials;

/** Server communication facade used by view models. */
public interface ClientGateway {
    GatewayResult sendCommand(String command, Credentials credentials, ParameterValueProvider provider);
}
