package client.gui.controller;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import client.gui.GuiClientContext;
import client.gui.SceneManager;
import client.gui.view.MainView;
import client.mvvm.model.DrawableDragon;
import client.mvvm.service.GatewayResult;
import client.mvvm.service.ParameterValueProvider;
import client.mvvm.vm.MainViewModel;
import dragon.Dragon;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import dragon.DragonType;

/**
 * Wires the main application screen to the MVVM layer.
 *
 * <p>Binds MainView controls to MainViewModel, handles user actions (commands, filters,
 * CRUD operations), manages async task execution, and displays results/errors.
 * Also handles interactive parameter requests and locale switching.
 */
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

    /**
     * Asynchronously refreshes dragon collection with full data including owner information.
     *
     * <p>Fetches fresh collection using "show_with_owners" command which returns dragons
     * and their corresponding owner logins for visualization and ownership tracking.
     */
    public void refreshAsync() {
        executeCommandAsync("show_with_owners", createParameterProvider(), false);
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
                        root.getCanvasView().findAt(event.getX(), event.getY()).orElse(null);
                            if (hit != null) {
                                viewModel.getVisualizationViewModel().selectById(hit.dragon().getId());
                                root.getTableView().getSelectionModel().select(hit.dragon());
                                showDragonInfo(hit);
                            }
                        });
    }

    private void bindActions() {
        root.getRefreshButton().setOnAction(event -> refreshAsync());
        root.getAddButton().setOnAction(event -> handleAddAction());
        root.getEditButton().setOnAction(event -> handleEditAction());
        root.getDeleteButton()
            .setOnAction(
                event ->
                    executeCommandAsync(
                        root.getCommandField().getText().isBlank()
                            ? "remove_by_id"
                            : root.getCommandField().getText(),
                        createParameterProvider(),
                        true));
        root.getLogoutButton().setOnAction(event -> Platform.exit());
        root.getCommandField().setOnAction(event -> executeCommandAsync(root.getCommandField().getText()));
    }

    private void handleAddAction() {
        DragonFormData form = askDragonForm(null);
        if (form == null) {
            return;
        }

        executeCommandAsync("add", createDragonFormProvider(form, null), true);
    }

    private void handleEditAction() {
        Dragon selected = root.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError(text("error.edit.header"), text("error.edit.selectDragon"));
            return;
        }

        DragonFormData form = askDragonForm(selected);
        if (form == null) {
            return;
        }

        executeCommandAsync("update_by_id", createDragonFormProvider(form, selected.getId()), true);
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
        executeCommandAsync(command, createParameterProvider(), false);
    }

    private void executeCommandAsync(String command, ParameterValueProvider provider) {
        executeCommandAsync(command, provider, false);
    }

    private void executeCommandAsync(
            String command, ParameterValueProvider provider, boolean refreshAfterSuccess) {
        String text = command == null ? "" : command.trim();
        if (text.isEmpty()) {
            return;
        }
        if ("show".equalsIgnoreCase(text)) {
            text = "show_with_owners";
        }
        final String commandToRun = text;

        Task<GatewayResult> task =
                new Task<>() {
                    @Override
                    protected GatewayResult call() {
                        return viewModel.executeCommand(commandToRun, provider);
                    }
                };

        task.setOnSucceeded(
                event -> {
                    GatewayResult result = task.getValue();
                    executeAsync(result);
                    if (refreshAfterSuccess && result != null && result.isSuccess()) {
                        refreshAsync();
                    }
                });
        task.setOnFailed(
            event ->
                showError(
                    text("error.command.header"),
                    task.getException() == null ? text("error.unknown") : task.getException().getMessage()));

        Thread thread = new Thread(task, "command-task");
        thread.setDaemon(true);
        thread.start();
    }

    private ParameterValueProvider createParameterProvider() {
        return (parameterRequest, passwordInput) ->
                askTextOnFx(
                        parameterRequest == null ? "Enter value" : parameterRequest.prompt,
                        parameterRequest == null ? "Value" : parameterRequest.parameterName,
                        "");
    }

    private ParameterValueProvider createDragonFormProvider(DragonFormData form, Long updateId) {
        return (parameterRequest, passwordInput) -> {
            if (parameterRequest == null || parameterRequest.parameterName == null) {
                return "";
            }

            return switch (parameterRequest.parameterName) {
                case "update_id" -> updateId == null ? "" : String.valueOf(updateId);
                case "dragon_name" -> form.name;
                case "dragon_x" -> form.x;
                case "dragon_y" -> form.y;
                case "dragon_age" -> form.age;
                case "dragon_weight" -> form.weight;
                case "dragon_type" -> form.type;
                case "head_size" -> form.headSize;
                case "head_teeth" -> form.headTeeth;
                default -> "";
            };
        };
    }

    private DragonFormData askDragonForm(Dragon selected) {
        FutureTask<DragonFormData> task =
                new FutureTask<>(
                        () -> {
                            Dialog<DragonFormData> dialog = new Dialog<>();
                            dialog.setTitle(selected == null ? "Add dragon" : "Edit dragon");
                            dialog.setHeaderText(selected == null ? "Enter dragon fields" : "Update dragon fields");

                            DialogPane pane = dialog.getDialogPane();
                            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                            GridPane grid = new GridPane();
                            grid.setHgap(8);
                            grid.setVgap(8);

                            TextField name = new TextField(selected == null ? "" : selected.getName());
                            TextField x =
                                    new TextField(
                                            selected == null
                                                    ? ""
                                                    : String.valueOf(selected.getCoordinates().getX()));
                            TextField y =
                                    new TextField(
                                            selected == null
                                                    ? ""
                                                    : String.valueOf(selected.getCoordinates().getY()));
                            TextField age =
                                    new TextField(selected == null ? "" : String.valueOf(selected.getAge()));
                            TextField weight =
                                    new TextField(selected == null ? "" : String.valueOf(selected.getWeight()));
                            
                            ComboBox<DragonType> type = new ComboBox<>();
                            type.getItems().addAll(DragonType.values());
                            if (selected != null && selected.getType() != null) {
                                type.setValue(selected.getType());
                            }
                            
                            TextField headSize =
                                    new TextField(
                                            selected == null
                                                    ? ""
                                                    : String.valueOf(selected.getHead().getSize()));
                            TextField headTeeth =
                                    new TextField(
                                            selected == null
                                                    ? ""
                                                    : String.valueOf(selected.getHead().getToothCount()));

                            grid.addRow(0, new Label("Name"), name);
                            grid.addRow(1, new Label("X (> -359)"), x);
                            grid.addRow(2, new Label("Y (≤ 603)"), y);
                            grid.addRow(3, new Label("Age (> 0)"), age);
                            grid.addRow(4, new Label("Weight (> 0)"), weight);
                            grid.addRow(5, new Label("Type"), type);
                            grid.addRow(6, new Label("Head size (> 0)"), headSize);
                            grid.addRow(7, new Label("Head teeth (> 0)"), headTeeth);

                            pane.setContent(grid);

                            dialog.setResultConverter(
                                    btn -> {
                                        if (btn != ButtonType.OK) {
                                            return null;
                                        }
                                        return new DragonFormData(
                                                name.getText().trim(),
                                                x.getText().trim(),
                                                y.getText().trim(),
                                                age.getText().trim(),
                                                weight.getText().trim(),
                                                type.getValue() == null ? "" : type.getValue().name(),
                                                headSize.getText().trim(),
                                                headTeeth.getText().trim());
                                    });

                            return dialog.showAndWait().orElse(null);
                        });

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }

        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            showError(
                    text("error.dialog.header"),
                    e.getCause() == null ? text("error.dialog.openFailed") : e.getCause().getMessage());
            return null;
        }
    }

    private String askTextOnFx(String header, String content, String initialValue) {
        FutureTask<String> task =
                new FutureTask<>(
                        () -> {
                            TextInputDialog dialog = new TextInputDialog(initialValue == null ? "" : initialValue);
                            dialog.setTitle("DragonsDB");
                            dialog.setHeaderText(header);
                            dialog.setContentText(content);
                            return dialog.showAndWait().orElse("");
                        });

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }

        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException e) {
            return "";
        }
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
                        showError(
                                text("error.operation.header"),
                                result.message == null ? text("error.unknown") : result.message);
                        root.getErrorLabel().setText("");
                    }
                    root.getCurrentUserLabel().setText("User: " + viewModel.getCurrentUser());
                    root.getCanvasView().redraw();
                });
    }

    private String text(String key) {
        return context.getLocalizationService().text(key);
    }

    private void showError(String header, String message) {
        Runnable action =
                () -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle(text("error.title"));
                    alert.setHeaderText(header == null || header.isBlank() ? text("error.title") : header);
                    alert.setContentText(message == null || message.isBlank() ? text("error.unknown") : message);
                    alert.showAndWait();
                };

        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
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

    private record DragonFormData(
            String name,
            String x,
            String y,
            String age,
            String weight,
            String type,
            String headSize,
            String headTeeth) {}
}
