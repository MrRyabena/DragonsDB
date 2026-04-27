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

    public String getOwnerLogin(long dragonId) {
        return ownerByDragonId.get(dragonId);
    }

    public void replaceAll(List<Dragon> newDragons) {
        replaceAll(newDragons, null);
    }

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
