package client.mvvm.service;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;
import java.util.ResourceBundle;

/** Runtime locale switcher with bundles implemented as classes. */
public class LocalizationService {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private Locale currentLocale = Locale.ENGLISH;
    private ResourceBundle bundle = ResourceBundle.getBundle("client.mvvm.i18n.Messages", currentLocale);

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public void setCurrentLocale(Locale locale) {
        Locale old = this.currentLocale;
        this.currentLocale = locale;
        this.bundle = ResourceBundle.getBundle("client.mvvm.i18n.Messages", locale);
        pcs.firePropertyChange("locale", old, this.currentLocale);
    }

    public String text(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
