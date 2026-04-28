package client.mvvm.service;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Manages runtime locale switching and message translation.
 *
 * <p>Provides access to localized strings from resource bundles (Messages_*.java).
 * Supports switching locales at runtime with PropertyChange notifications for UI updates.
 */
public class LocalizationService {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private Locale currentLocale = Locale.ENGLISH;
    private ResourceBundle bundle = ResourceBundle.getBundle("client.mvvm.i18n.Messages", currentLocale);

    /**
     * Gets the currently active locale.
     *
     * @return current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Sets the active locale and reloads message bundles.
     *
     * @param locale new locale for message translation
     */
    public void setCurrentLocale(Locale locale) {
        Locale old = this.currentLocale;
        this.currentLocale = locale;
        this.bundle = ResourceBundle.getBundle("client.mvvm.i18n.Messages", locale);
        pcs.firePropertyChange("locale", old, this.currentLocale);
    }

    /**
     * Gets localized text for a key.
     *
     * @param key message key
     * @return localized text, or key itself if not found
     */
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
