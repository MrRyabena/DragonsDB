package client.gui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import client.mvvm.model.DrawableDragon;
import javafx.animation.AnimationTimer;
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
    private static final long APPEAR_DURATION_NANOS = 420_000_000L;

    private List<DrawableDragon> drawables = List.of();
    private List<RenderedDragon> rendered = List.of();
    private final Map<Long, Long> appearingSinceNanos = new HashMap<>();
    private final AnimationTimer appearAnimation =
            new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (appearingSinceNanos.isEmpty()) {
                        stop();
                        return;
                    }
                    redraw();
                    appearingSinceNanos.entrySet().removeIf(e -> now - e.getValue() >= APPEAR_DURATION_NANOS);
                    if (appearingSinceNanos.isEmpty()) {
                        stop();
                    }
                }
            };

    public DragonCanvasView(double width, double height) {
        super(width, height);
        setStyle("-fx-background-color: #0f172a;");
    }

    public void setDrawables(List<DrawableDragon> drawables) {
        List<DrawableDragon> next = drawables == null ? List.of() : drawables;
        Set<Long> previousIds = this.drawables.stream().map(d -> d.dragon().getId()).collect(java.util.stream.Collectors.toSet());
        Set<Long> currentIds = next.stream().map(d -> d.dragon().getId()).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        for (Long id : currentIds) {
            if (!previousIds.contains(id)) {
                appearingSinceNanos.put(id, System.nanoTime());
            }
        }
        appearingSinceNanos.keySet().retainAll(currentIds);

        this.drawables = next;
        if (!appearingSinceNanos.isEmpty()) {
            appearAnimation.start();
        }
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
        double canvasWidth = getWidth();
        double canvasHeight = getHeight();

        gc.setFill(Color.web("#0f172a"));
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        rendered = List.of();
        if (drawables.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) {
            return;
        }

        double padding = 24.0;
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (DrawableDragon drawable : drawables) {
            double radius = Math.max(6.0, drawable.size() / 2.0);
            minX = Math.min(minX, drawable.x() - radius);
            maxX = Math.max(maxX, drawable.x() + radius);
            minY = Math.min(minY, drawable.y() - radius);
            maxY = Math.max(maxY, drawable.y() + radius);
        }

        double worldWidth = Math.max(1.0, maxX - minX);
        double worldHeight = Math.max(1.0, maxY - minY);
        double viewWidth = Math.max(1.0, canvasWidth - 2 * padding);
        double viewHeight = Math.max(1.0, canvasHeight - 2 * padding);
        double scale = Math.min(viewWidth / worldWidth, viewHeight / worldHeight);

        double usedWidth = worldWidth * scale;
        double usedHeight = worldHeight * scale;
        double offsetX = (canvasWidth - usedWidth) / 2.0;
        double offsetY = (canvasHeight - usedHeight) / 2.0;

        List<RenderedDragon> newRendered = new ArrayList<>(drawables.size());

        for (DrawableDragon drawable : drawables) {
            double sx = offsetX + (drawable.x() - minX) * scale;
            double sy = offsetY + (drawable.y() - minY) * scale;
            double radius = Math.max(6.0, drawable.size() / 2.0);
            double appear = appearanceProgress(drawable.dragon().getId());
            double sr = Math.max(6.0, radius * scale) * (0.6 + 0.4 * appear);
            double size = sr * 2.0;
            double alpha = 0.2 + 0.8 * appear;

            gc.setFill(Color.web(drawable.colorHex(), alpha));
            gc.fillOval(sx - sr, sy - sr, size, size);

            gc.setLineWidth(drawable.ownedByCurrentUser() ? 2.4 : 1.4);
            gc.setStroke(drawable.ownedByCurrentUser() ? Color.web("#f8fafc", alpha) : Color.web("#cbd5e1", alpha));
            gc.strokeOval(sx - sr, sy - sr, size, size);

            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(drawable.dragon().getId()), sx, sy);

            newRendered.add(new RenderedDragon(drawable, sx, sy, sr));
        }

        rendered = newRendered;
    }

    private double appearanceProgress(long dragonId) {
        Long startedAt = appearingSinceNanos.get(dragonId);
        if (startedAt == null) {
            return 1.0;
        }

        double t = (System.nanoTime() - startedAt) / (double) APPEAR_DURATION_NANOS;
        if (t >= 1.0) {
            return 1.0;
        }
        if (t <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    public Optional<DrawableDragon> findAt(double x, double y) {
        for (RenderedDragon dragon : rendered) {
            double dx = x - dragon.screenX;
            double dy = y - dragon.screenY;
            if (Math.hypot(dx, dy) <= dragon.screenRadius) {
                return Optional.of(dragon.drawable);
            }
        }
        return Optional.empty();
    }

    private record RenderedDragon(DrawableDragon drawable, double screenX, double screenY, double screenRadius) {}
}
