package client.mvvm.vm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import client.mvvm.model.DrawableDragon;
import client.mvvm.state.ClientSessionState;
import client.mvvm.state.DragonStore;
import dragon.Dragon;

/** Converts dragons into drawable primitives for canvas-based views. */
public class VisualizationViewModel extends BaseViewModel implements PropertyChangeListener {
    private final DragonStore store;
    private final ClientSessionState sessionState;

    private List<DrawableDragon> drawableDragons = List.of();

    public VisualizationViewModel(DragonStore store, ClientSessionState sessionState) {
        this.store = store;
        this.sessionState = sessionState;
        recompute();
    }

    public void bind() {
        this.store.addPropertyChangeListener(this);
        this.sessionState.addPropertyChangeListener(this);
    }

    public void unbind() {
        this.store.removePropertyChangeListener(this);
        this.sessionState.removePropertyChangeListener(this);
    }

    public List<DrawableDragon> getDrawableDragons() {
        return drawableDragons;
    }

    public void selectById(long dragonId) {
        Dragon selected =
                store.getDragons().stream().filter(d -> d.getId() == dragonId).findFirst().orElse(null);
        store.setSelectedDragon(selected);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("dragons".equals(evt.getPropertyName()) || "currentUser".equals(evt.getPropertyName())) {
            recompute();
        }
    }

    private void recompute() {
        String currentUser = normalizeLogin(sessionState.getCurrentUser());
        drawableDragons =
                store.getDragons().stream()
                        .map(
                                dragon -> {
                        String ownerLogin = normalizeLogin(store.getOwnerLogin(dragon.getId()));
                                    double x = dragon.getCoordinates().getX();
                                    double y = dragon.getCoordinates().getY();
                                    double size = Math.max(8.0, Math.min(48.0, 8.0 + Math.log10(dragon.getWeight() + 1) * 10.0));
                                    String colorHex = ownerLogin.isBlank() ? colorById(dragon.getId()) : colorByOwner(ownerLogin);
                                    boolean owned = !currentUser.isBlank() && currentUser.equals(ownerLogin);
                                    return new DrawableDragon(dragon, x, y, size, colorHex, owned);
                                })
                        .collect(Collectors.toList());
        notifyViewChanged("drawableDragons", null, drawableDragons);
    }

    /**
     * Normalizes a login string for consistent comparison.
     *
     * <p>Applies trim and lowercase normalization to enable case-insensitive matching.
     *
     * @param login login string (may be null)
     * @return normalized login (empty string if null)
     */
    private String normalizeLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Generates a stable color based on owner login.
     *
     * <p>Uses hash function to produce consistent RGB values from owner identity,
     * ensuring same owner always gets same hue across sessions.
     *
     * @param ownerLogin owner login string
     * @return hex color string in format "#RRGGBB"
     */
    private String colorByOwner(String ownerLogin) {
        int hash = ownerLogin.hashCode();
        int r = 72 + Math.floorMod(hash, 152);
        int g = 72 + Math.floorMod(hash / 17, 152);
        int b = 72 + Math.floorMod(hash / 31, 152);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private String colorById(long id) {
        int hash = (int) (id ^ (id >>> 32));
        int r = 64 + Math.floorMod(hash, 160);
        int g = 64 + Math.floorMod(hash / 17, 160);
        int b = 64 + Math.floorMod(hash / 31, 160);
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
