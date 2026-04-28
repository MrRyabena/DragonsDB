package client.gui.animation;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Reusable animation utilities for the graphical client.
 *
 * <p>Provides predefined animations: fade-in with scale-up, pulse effect, and other
 * common visual transitions for UI elements.
 */
public final class DragonAnimations {
    private DragonAnimations() {}

    public static void playAppear(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();

        ScaleTransition scale = new ScaleTransition(Duration.millis(220), node);
        scale.setFromX(0.85);
        scale.setFromY(0.85);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    public static void playPulse(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.06);
        scale.setToY(1.06);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    public static void playDisappear(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.play();
    }
}
