package client.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Main application shell with toolbar, table, canvas and status line. */
public class MainView extends BorderPane {
    private final Label currentUserLabel = new Label();
    private final Label errorLabel = new Label();
    private final Label statusLabel = new Label();
    private final TextField commandField = new TextField();
    private final TextField filterField = new TextField();
    private final ComboBox<String> localeBox = new ComboBox<>();
    private final Button refreshButton = new Button("Refresh");
    private final Button addButton = new Button("Add");
    private final Button editButton = new Button("Edit");
    private final Button deleteButton = new Button("Delete");
    private final Button logoutButton = new Button("Logout");
    private final DragonTableView tableView = new DragonTableView();
    private final DragonCanvasView canvasView = new DragonCanvasView(720, 520);

    public MainView() {
        setPadding(new Insets(12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #0b1020, #111827);");

        Label title = new Label("DragonsDB");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        currentUserLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        errorLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");

        localeBox.getItems().addAll("English (Canada)", "Русский", "Eesti", "Български");
        localeBox.getSelectionModel().selectFirst();

        HBox topBar = new HBox(12, title, currentUserLabel, localeBox, refreshButton, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox leftBar = new VBox(10, new Label("Filter"), filterField, commandField, addButton, editButton, deleteButton);
        leftBar.setPrefWidth(220);
        leftBar.setMinWidth(200);
        leftBar.setMaxWidth(280);
        leftBar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.72); -fx-padding: 12; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #334155;");
        leftBar.getChildren().get(0).setStyle("-fx-text-fill: white;");
        for (var node : leftBar.getChildren()) {
            if (node instanceof Button button) {
                button.setMaxWidth(Double.MAX_VALUE);
            }
        }

        Pane mapPane = new Pane(canvasView);
        mapPane.setStyle("-fx-background-color: rgba(15, 23, 42, 0.8); -fx-border-color: #334155; -fx-border-radius: 8; -fx-background-radius: 8;");
        mapPane.setPadding(new Insets(6));
        mapPane.setMinWidth(240);
        mapPane.setMinHeight(220);

        // Keep canvas out of layout calculations to avoid SplitPane divider feedback loops.
        canvasView.setManaged(false);
        canvasView.relocate(6, 6);

        SplitPane upperArea = new SplitPane(leftBar, mapPane);
        upperArea.setDividerPositions(0.2);
        SplitPane.setResizableWithParent(leftBar, false);
        upperArea.setMinHeight(260);

        SplitPane center = new SplitPane(upperArea, tableView);
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.62);
        tableView.setMinHeight(140);
        VBox.setVgrow(center, Priority.ALWAYS);

        BorderPane bottom = new BorderPane();
        bottom.setLeft(statusLabel);
        bottom.setRight(errorLabel);

        setTop(topBar);
        setCenter(center);
        setBottom(bottom);
    }

    public Label getCurrentUserLabel() { return currentUserLabel; }
    public Label getErrorLabel() { return errorLabel; }
    public Label getStatusLabel() { return statusLabel; }
    public TextField getCommandField() { return commandField; }
    public TextField getFilterField() { return filterField; }
    public ComboBox<String> getLocaleBox() { return localeBox; }
    public Button getRefreshButton() { return refreshButton; }
    public Button getAddButton() { return addButton; }
    public Button getEditButton() { return editButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getLogoutButton() { return logoutButton; }
    public DragonTableView getTableView() { return tableView; }
    public DragonCanvasView getCanvasView() { return canvasView; }
}
