package client.mvvm.vm;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Minimal base class for propagating view model state changes to view bindings. */
public abstract class BaseViewModel {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean busy;
    private String errorMessage = "";

    public boolean isBusy() {
        return busy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    protected void setBusy(boolean busy) {
        boolean old = this.busy;
        this.busy = busy;
        pcs.firePropertyChange("busy", old, this.busy);
    }

    protected void setErrorMessage(String errorMessage) {
        String old = this.errorMessage;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
        pcs.firePropertyChange("errorMessage", old, this.errorMessage);
    }

    public void clearError() {
        setErrorMessage("");
    }

    protected void notifyViewChanged(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
