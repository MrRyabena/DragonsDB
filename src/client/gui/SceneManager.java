package client.gui;

import javafx.scene.Scene;
import javafx.stage.Stage;

import client.gui.controller.LoginController;
import client.gui.controller.MainController;

/**
 * Manages scene switching between login and main screens.
 *
 * <p>Responsible for creating controllers and scenes, setting stage properties,
 * and navigating between authentication and application screens.
 */
public class SceneManager {
    private final Stage stage;
    private final GuiClientContext context;

    public SceneManager(Stage stage, GuiClientContext context) {
        this.stage = stage;
        this.context = context;
    }

    public void showLogin() {
        LoginController controller = new LoginController(this, context);
        Scene scene = new Scene(controller.getRoot(), 520, 360);
        stage.setTitle("DragonsDB - Login");
        stage.setScene(scene);
        stage.show();
    }

    public void showMain() {
        MainController controller = new MainController(this, context);
        Scene scene = new Scene(controller.getRoot(), 1280, 840);
        stage.setTitle("DragonsDB");
        stage.setScene(scene);
        stage.show();
        controller.refreshAsync();
    }
}
