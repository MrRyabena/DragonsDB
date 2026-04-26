package client.gui.view;

import client.mvvm.vm.DragonsTableViewModel;
import dragon.Dragon;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Table with a column per dragon field. */
public class DragonTableView extends TableView<Dragon> {
    private final ObservableList<Dragon> items = FXCollections.observableArrayList();

    public DragonTableView() {
        setItems(items);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        buildColumns();
    }

    public void bind(DragonsTableViewModel viewModel) {
        items.setAll(viewModel.getVisibleRows());
        viewModel.addPropertyChangeListener(
                evt -> {
                    if ("visibleRows".equals(evt.getPropertyName())) {
                        items.setAll(viewModel.getVisibleRows());
                    }
                });
    }

    private void buildColumns() {
        TableColumn<Dragon, Long> id = new TableColumn<>("ID");
        id.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));

        TableColumn<Dragon, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getName()));

        TableColumn<Dragon, Number> x = new TableColumn<>("X");
        x.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCoordinates().getX()));

        TableColumn<Dragon, Number> y = new TableColumn<>("Y");
        y.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCoordinates().getY()));

        TableColumn<Dragon, Number> age = new TableColumn<>("Age");
        age.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAge()));

        TableColumn<Dragon, Number> weight = new TableColumn<>("Weight");
        weight.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getWeight()));

        TableColumn<Dragon, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(String.valueOf(cell.getValue().getType())));

        TableColumn<Dragon, String> created = new TableColumn<>("Created");
        created.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(String.valueOf(cell.getValue().getCreationDate())));

        getColumns().setAll(id, name, x, y, age, weight, type, created);
    }
}
