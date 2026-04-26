package client.gui.view;

import java.util.List;

import client.mvvm.model.DrawableDragon;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/** Canvas-based visualization of dragons with simple animations. */
public class DragonCanvasView extends Canvas {
    private List<DrawableDragon> drawables = List.of();

    public DragonCanvasView(double width, double height) {
        super(width, height);
        setStyle("-fx-background-color: #0f172a;");
    }

    public void setDrawables(List<DrawableDragon> drawables) {
        this.drawables = drawables == null ? List.of() : drawables;
        redraw();
    }

    public void animateRefresh() {
        FadeTransition fade = new FadeTransition(Duration.millis(180), this);
        fade.setFromValue(0.75);
        fade.setToValue(1.0);
        fade.play();

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), this);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#0f172a"));
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        for (DrawableDragon drawable : drawables) {
            double size = drawable.size();
            double x = drawable.x();
            double y = drawable.y();

            gc.setFill(Color.web(drawable.colorHex()));
            gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);

            gc.setStroke(Color.web("#e2e8f0"));
            gc.strokeOval(x - size / 2.0, y - size / 2.0, size, size);

            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(drawable.dragon().getId()), x, y);
        }
    }
}
