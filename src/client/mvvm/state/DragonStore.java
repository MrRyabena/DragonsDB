package client.mvvm.state;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dragon.Dragon;

/** Source of truth for collection data used by table and visualization view models. */
public class DragonStore {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private List<Dragon> dragons = List.of();
    private Map<Long, String> ownerByDragonId = Map.of();
    private Dragon selectedDragon;

    public List<Dragon> getDragons() {
        return Collections.unmodifiableList(dragons);
    }

    public Dragon getSelectedDragon() {
        return selectedDragon;
    }

    /**
     * Retrieves the owner login for a specific dragon.
     *
     * @param dragonId the ID of the dragon
     * @return owner login string (null if no owner information available)
     */
    public String getOwnerLogin(long dragonId) {
        return ownerByDragonId.get(dragonId);
    }

    /**
     * Replaces all dragons without updating owner information.
     *
     * @param newDragons new collection of dragons
     */
    public void replaceAll(List<Dragon> newDragons) {
        replaceAll(newDragons, null);
    }

    /**
     * Replaces all dragons and updates owner information.
     *
     * <p>The ownerLogins list must be index-aligned with newDragons:
     * ownerLogins.get(i) corresponds to newDragons.get(i).getId().
     *
     * @param newDragons new collection of dragons
     * @param ownerLogins owner logins corresponding to each dragon (index-aligned), or null if not available
     */
    public void replaceAll(List<Dragon> newDragons, List<String> ownerLogins) {
        List<Dragon> old = this.dragons;
        this.dragons = new ArrayList<>(newDragons);
        Map<Long, String> owners = new HashMap<>();
        if (ownerLogins != null) {
            int count = Math.min(this.dragons.size(), ownerLogins.size());
            for (int i = 0; i < count; i++) {
                owners.put(this.dragons.get(i).getId(), ownerLogins.get(i));
            }
        }
        this.ownerByDragonId = owners;
        pcs.firePropertyChange("dragons", old, Collections.unmodifiableList(this.dragons));
    }

    public void setSelectedDragon(Dragon dragon) {
        Dragon old = this.selectedDragon;
        this.selectedDragon = dragon;
        pcs.firePropertyChange("selectedDragon", old, this.selectedDragon);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
