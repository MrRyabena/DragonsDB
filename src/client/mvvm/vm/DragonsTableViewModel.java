package client.mvvm.vm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import client.mvvm.model.DragonsFilter;
import client.mvvm.service.DragonsQueryService;
import client.mvvm.state.DragonStore;
import dragon.Dragon;

/** Client-side filtered/sorted projection of dragons for table view. */
public class DragonsTableViewModel extends BaseViewModel implements PropertyChangeListener {
    private final DragonStore store;
    private final DragonsQueryService queryService;

    private final DragonsFilter filter = new DragonsFilter();
    private List<Dragon> visibleRows = List.of();

    public DragonsTableViewModel(DragonStore store, DragonsQueryService queryService) {
        this.store = store;
        this.queryService = queryService;
        recompute();
    }

    public void bind() {
        store.addPropertyChangeListener(this);
    }

    public void unbind() {
        store.removePropertyChangeListener(this);
    }

    public List<Dragon> getVisibleRows() {
        return visibleRows;
    }

    public DragonsFilter getFilter() {
        return filter;
    }

    public void setQuery(String query) {
        filter.query = query;
        recompute();
    }

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
