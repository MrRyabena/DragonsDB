package client.gui.controller;

import java.util.Locale;

import client.gui.GuiClientContext;
import client.gui.SceneManager;
import client.gui.view.LoginView;
import client.mvvm.service.GatewayResult;
import client.mvvm.vm.AuthViewModel;
import javafx.application.Platform;
import javafx.concurrent.Task;

/** Wires login/register view actions to the MVVM auth layer. */
public class LoginController {
    private final LoginView root = new LoginView();
    private final GuiClientContext context;
    private final SceneManager sceneManager;
    private final AuthViewModel viewModel;

    public LoginController(SceneManager sceneManager, GuiClientContext context) {
        this.context = context;
        this.sceneManager = sceneManager;
        this.viewModel = context.getAuthViewModel();

        bindLocaleSwitching();
        bindActions();
        applyLocale();
    }

    public LoginView getRoot() {
        return root;
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

    private void bindActions() {
        root.getLoginButton().setOnAction(event -> runAuth(false));
        root.getRegisterButton().setOnAction(event -> runAuth(true));
    }

    private void runAuth(boolean register) {
        root.getMessageLabel().setText("");
        String login = root.getLoginField().getText();
        String password = root.getPasswordField().getText();

        Task<GatewayResult> task =
                new Task<>() {
                    @Override
                    protected GatewayResult call() {
                        return register ? viewModel.register(login, password) : viewModel.login(login, password);
                    }
                };

        task.setOnSucceeded(
                event -> {
                    GatewayResult result = task.getValue();
                    if (result != null && result.isSuccess()) {
                        sceneManager.showMain();
                    } else if (result != null) {
                        root.getMessageLabel().setText(result.message == null ? "" : result.message);
                    }
                });
        task.setOnFailed(
                event ->
                        root.getMessageLabel()
                                .setText(
                                        task.getException() == null
                                                ? "Authentication failed"
                                                : task.getException().getMessage()));

        Thread thread = new Thread(task, "auth-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyLocale() {
        var localization = context.getLocalizationService();
        root.getTitleLabel().setText(localization.text("app.title"));
        root.getLoginButton().setText(localization.text("auth.login"));
        root.getRegisterButton().setText(localization.text("auth.register"));
        root.getLoginField().setPromptText(localization.text("auth.user"));
        root.getPasswordField().setPromptText("Password");
    }
}
