package client.gui;

import java.util.Locale;

import client.mvvm.service.LocalizationService;
import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX launcher for the graphical client layer. */
public class JavaFxClientApp extends Application {
    private GuiClientContext context;

    @Override
    public void start(Stage stage) {
        String host = System.getenv("SERVER_HOST");
        if (host == null || host.isBlank()) {
            host = core.Defaults.SERVER_HOST;
        }

        int port = core.Defaults.SERVER_PORT;
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
            }
        }

        context = new GuiClientContext(host, port);
        LocalizationService localizationService = context.getLocalizationService();
        localizationService.setCurrentLocale(Locale.forLanguageTag("en-CA"));

        SceneManager sceneManager = new SceneManager(stage, context);
        sceneManager.showLogin();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}
