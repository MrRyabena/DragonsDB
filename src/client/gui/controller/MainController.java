package client.gui.controller;

import java.util.Locale;

import client.gui.GuiClientContext;
import client.gui.SceneManager;
import client.gui.view.MainView;
import client.mvvm.model.DrawableDragon;
import client.mvvm.service.GatewayResult;
import client.mvvm.service.ParameterValueProvider;
import client.mvvm.vm.MainViewModel;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;

/** Wires the main screen to the MVVM main view model. */
public class MainController {
    private final MainView root = new MainView();
    private final GuiClientContext context;
    private final SceneManager sceneManager;
    private final MainViewModel viewModel;

    public MainController(SceneManager sceneManager, GuiClientContext context) {
        this.context = context;
        this.sceneManager = sceneManager;
        this.viewModel = context.getMainViewModel();

        bindView();
        bindActions();
        bindLocaleSwitching();
        applyLocale();
        refreshStatus();
    }

    public MainView getRoot() {
        return root;
    }

    public void refreshAsync() {
        executeCommandAsync("show");
    }

    private void bindView() {
        root.getCurrentUserLabel().setText("User: " + viewModel.getCurrentUser());
        root.getTableView().bind(viewModel.getTableViewModel());
        root.getCanvasView().setDrawables(viewModel.getVisualizationViewModel().getDrawableDragons());

        viewModel.getVisualizationViewModel()
                .addPropertyChangeListener(
                        evt -> {
                            if ("drawableDragons".equals(evt.getPropertyName())) {
                                Platform.runLater(
                                        () -> {
                                            root.getCanvasView()
                                                    .setDrawables(viewModel.getVisualizationViewModel().getDrawableDragons());
                                            root.getCanvasView().animateRefresh();
                                        });
                            }
                        });

        viewModel.getTableViewModel()
                .addPropertyChangeListener(
                        evt -> {
                            if ("visibleRows".equals(evt.getPropertyName())) {
                                Platform.runLater(() -> root.getCanvasView().redraw());
                            }
                        });

        root.getFilterField()
                .textProperty()
                .addListener((obs, oldValue, newValue) -> viewModel.getTableViewModel().setQuery(newValue));

        root.getTableView()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldDragon, newDragon) -> {
                            viewModel.getTableViewModel().select(newDragon);
                            refreshStatus();
                        });

        root.getCanvasView()
                .setOnMouseClicked(
                        event -> {
                            DrawableDragon hit =
                                    viewModel.getVisualizationViewModel().getDrawableDragons().stream()
                                            .filter(
                                                    drawable -> {
                                                        double dx = event.getX() - drawable.x();
                                                        double dy = event.getY() - drawable.y();
                                                        return Math.hypot(dx, dy) <= drawable.size() / 2.0;
                                                    })
                                            .findFirst()
                                            .orElse(null);
                            if (hit != null) {
                                viewModel.getVisualizationViewModel().selectById(hit.dragon().getId());
                                root.getTableView().getSelectionModel().select(hit.dragon());
                                showDragonInfo(hit);
                            }
                        });
    }

    private void bindActions() {
        root.getRefreshButton().setOnAction(event -> refreshAsync());
        root.getAddButton().setOnAction(event -> executeCommandAsync(root.getCommandField().getText().isBlank() ? "add" : root.getCommandField().getText()));
        root.getEditButton().setOnAction(event -> executeCommandAsync(root.getCommandField().getText().isBlank() ? "update_by_id" : root.getCommandField().getText()));
        root.getDeleteButton().setOnAction(event -> executeCommandAsync(root.getCommandField().getText().isBlank() ? "remove_by_id" : root.getCommandField().getText()));
        root.getLogoutButton().setOnAction(event -> Platform.exit());
        root.getCommandField().setOnAction(event -> executeCommandAsync(root.getCommandField().getText()));
    }

    private void bindLocaleSwitching() {
        root.getLocaleBox()
                .getSelectionModel()
                .selectedIndexProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {
                            switch (newValue == null ? 0 : newValue.intValue()) {
                                case 1 -> context.getLocalizationService().setCurrentLocale(Locale.forLanguageTag("ru"));
                                case 2 -> context.getLocalizationService().setCurrentLocale(Locale.forLanguageTag("et"));
                                case 3 -> context.getLocalizationService().setCurrentLocale(Locale.forLanguageTag("bg"));
                                default -> context.getLocalizationService().setCurrentLocale(Locale.forLanguageTag("en-CA"));
                            }
                            applyLocale();
                        });
    }

    private void executeCommandAsync(String command) {
        String text = command == null ? "" : command.trim();
        if (text.isEmpty()) {
            return;
        }

        Task<GatewayResult> task =
                new Task<>() {
                    @Override
                    protected GatewayResult call() {
                        return viewModel.executeCommand(text, createParameterProvider());
                    }
                };

        task.setOnSucceeded(event -> executeAsync(task.getValue()));
        task.setOnFailed(
                event ->
                        root.getErrorLabel()
                                .setText(task.getException() == null ? "Command failed" : task.getException().getMessage()));

        Thread thread = new Thread(task, "command-task");
        thread.setDaemon(true);
        thread.start();
    }

    private ParameterValueProvider createParameterProvider() {
        return (parameterRequest, passwordInput) -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("DragonsDB");
            dialog.setHeaderText(parameterRequest == null ? "Enter value" : parameterRequest.prompt);
            dialog.setContentText(parameterRequest == null ? "Value" : parameterRequest.parameterName);
            return dialog.showAndWait().orElse("");
        };
    }

    private void executeAsync(GatewayResult result) {
        if (result == null) {
            return;
        }
        Platform.runLater(
                () -> {
                    if (result.isSuccess()) {
                        root.getStatusLabel().setText("Last action: success");
                        root.getErrorLabel().setText("");
                    } else {
                        root.getErrorLabel().setText(result.message == null ? "" : result.message);
                    }
                    root.getCurrentUserLabel().setText("User: " + viewModel.getCurrentUser());
                    root.getCanvasView().redraw();
                });
    }

    private void refreshStatus() {
        root.getCurrentUserLabel().setText(context.getLocalizationService().text("main.user") + ": " + viewModel.getCurrentUser());
    }

    private void applyLocale() {
        var localization = context.getLocalizationService();
        root.getRefreshButton().setText(localization.text("main.refresh"));
        root.getAddButton().setText(localization.text("main.add"));
        root.getEditButton().setText(localization.text("main.edit"));
        root.getDeleteButton().setText(localization.text("main.delete"));
        root.getLogoutButton().setText(localization.text("main.logout"));
        root.getFilterField().setPromptText(localization.text("main.filter"));
        root.getCommandField().setPromptText(localization.text("main.command"));
        refreshStatus();
    }

    private void showDragonInfo(DrawableDragon dragon) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Dragon");
        alert.setHeaderText(dragon.dragon().getName());
        alert.setContentText(
                "id="
                        + dragon.dragon().getId()
                        + "\ncoords=("
                        + dragon.dragon().getCoordinates().getX()
                        + ", "
                        + dragon.dragon().getCoordinates().getY()
                        + ")\nage="
                        + dragon.dragon().getAge()
                        + "\nweight="
                        + dragon.dragon().getWeight());
        alert.showAndWait();
    }
}
