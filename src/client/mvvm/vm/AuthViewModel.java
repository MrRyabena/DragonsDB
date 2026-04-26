package client.mvvm.vm;

import client.mvvm.model.Credentials;
import client.mvvm.service.ClientGateway;
import client.mvvm.service.GatewayResult;
import client.mvvm.state.ClientSessionState;

/** Handles login/register flows and updates shared session state. */
public class AuthViewModel extends BaseViewModel {
    private final ClientGateway gateway;
    private final ClientSessionState sessionState;

    public AuthViewModel(ClientGateway gateway, ClientSessionState sessionState) {
        this.gateway = gateway;
        this.sessionState = sessionState;
    }

    public GatewayResult login(String login, String password) {
        return authenticate("login", login, password);
    }

    public GatewayResult register(String login, String password) {
        return authenticate("register", login, password);
    }

    private GatewayResult authenticate(String commandPrefix, String login, String password) {
        clearError();
        setBusy(true);
        try {
            Credentials credentials = new Credentials(login, password);
            String command = commandPrefix + " " + login + " " + password;
            GatewayResult result = gateway.sendCommand(command, credentials, null);
            if (result.isSuccess()) {
                sessionState.setAuthenticatedUser(credentials);
            } else {
                setErrorMessage(result.message);
            }
            return result;
        } finally {
            setBusy(false);
        }
    }

    public void logout() {
        sessionState.clear();
    }
}
