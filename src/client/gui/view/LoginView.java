package client.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Compact authorization pane for login and registration.
 *
 * <p>Contains input fields for username/password, locale selector, and action buttons.
 * Uses dark theme (dark blue gradient). Message label displays errors or status.
 * All bindings to AuthViewModel are done by LoginController.
 */
public class LoginView extends VBox {
    private final TextField loginField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button();
    private final Button registerButton = new Button();
    private final ComboBox<String> localeBox = new ComboBox<>();
    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();

    public LoginView() {
        setSpacing(12);
        setPadding(new Insets(24));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: linear-gradient(to bottom right, #111827, #1f2937); -fx-border-color: #334155; -fx-border-width: 1;");

        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        messageLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");

        loginField.setPromptText("login");
        passwordField.setPromptText("password");
        loginField.setMaxWidth(280);
        passwordField.setMaxWidth(280);

        localeBox.getItems().addAll("English (Canada)", "Русский", "Eesti", "Български");
        localeBox.getSelectionModel().selectFirst();

        loginButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setMaxWidth(Double.MAX_VALUE);

        getChildren()
                .addAll(
                        titleLabel,
                        loginField,
                        passwordField,
                        localeBox,
                        loginButton,
                        registerButton,
                        messageLabel);
    }

    public TextField getLoginField() { return loginField; }
    public PasswordField getPasswordField() { return passwordField; }
    public Button getLoginButton() { return loginButton; }
    public Button getRegisterButton() { return registerButton; }
    public ComboBox<String> getLocaleBox() { return localeBox; }
    public Label getTitleLabel() { return titleLabel; }
    public Label getMessageLabel() { return messageLabel; }
}
