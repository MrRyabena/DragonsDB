package client.mvvm.vm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import client.mvvm.model.DragonsFilter;
import client.mvvm.service.DragonsQueryService;
import client.mvvm.state.DragonStore;
import dragon.Dragon;

/**
 * Manages client-side filtering, sorting, and projection of dragons for table display.
 *
 * <p>Subscribes to DragonStore changes and applies query/sort filters to produce a filtered
 * list of visible rows. Supports dynamic filter updates with PropertyChange notification.
 */
public class DragonsTableViewModel extends BaseViewModel implements PropertyChangeListener {
    private final DragonStore store;
    private final DragonsQueryService queryService;

    private final DragonsFilter filter = new DragonsFilter();
    private List<Dragon> visibleRows = List.of();

    /**
     * Constructs table view model.
     *
     * @param store dragon collection store
     * @param queryService filtering/sorting service
     */
    public DragonsTableViewModel(DragonStore store, DragonsQueryService queryService) {
        this.store = store;
        this.queryService = queryService;
        recompute();
    }

    /**
     * Subscribes to store changes.
     */
    public void bind() {
        store.addPropertyChangeListener(this);
    }

    /**
     * Unsubscribes from store changes.
     */
    public void unbind() {
        store.removePropertyChangeListener(this);
    }

    /**
     * Gets the filtered and sorted rows for display.
     *
     * @return visible rows list
     */
    public List<Dragon> getVisibleRows() {
        return visibleRows;
    }

    /**
     * Gets the active filter configuration.
     *
     * @return filter object
     */
    public DragonsFilter getFilter() {
        return filter;
    }

    /**
     * Updates search query and recomputes visible rows.
     *
     * @param query search query (searches name, ID, age, weight, coordinates)
     */
    public void setQuery(String query) {
        filter.query = query;
        recompute();
    }

    /**
     * Updates sort field and recomputes visible rows.
     *
     * @param field sort field (NAME, AGE, WEIGHT, ID)
     * @param ascending sort order
     */
    public void setSortField(DragonsFilter.SortField field, boolean ascending) {
        filter.sortField = field;
        filter.ascending = ascending;
        recompute();
    }

    public void select(Dragon dragon) {
        store.setSelectedDragon(dragon);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("dragons".equals(evt.getPropertyName())) {
            recompute();
        }
    }

    private void recompute() {
        List<Dragon> old = this.visibleRows;
        this.visibleRows = queryService.apply(store.getDragons(), filter);
        notifyViewChanged("visibleRows", old, this.visibleRows);
    }
}
