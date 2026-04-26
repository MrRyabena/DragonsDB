package client.mvvm.state;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import client.mvvm.model.Credentials;

/** Shared authenticated session state for all view models. */
public class ClientSessionState {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String currentUser = "";
    private Credentials credentials = new Credentials("", "");

    public String getCurrentUser() {
        return currentUser;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public boolean isAuthenticated() {
        return !currentUser.isBlank() && !credentials.isEmpty();
    }

    public void setAuthenticatedUser(Credentials credentials) {
        String oldUser = this.currentUser;
        this.credentials = credentials;
        this.currentUser = credentials.login();
        pcs.firePropertyChange("currentUser", oldUser, this.currentUser);
    }

    public void clear() {
        String oldUser = this.currentUser;
        this.credentials = new Credentials("", "");
        this.currentUser = "";
        pcs.firePropertyChange("currentUser", oldUser, this.currentUser);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
