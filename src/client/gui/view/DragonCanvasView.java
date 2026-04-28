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

/**
 * Canvas-based visualization of dragons with smooth appearance animations.
 * 
 * <p>Renders dragons as circles on a 2D canvas with automatic coordinate scaling to fit viewport.
 * Tracks newly added dragons and displays smooth fade-in + scale-up animations over approximately
 * 420 milliseconds. Dragons are colored by owner identity, with current user's dragons shown with
 * brighter borders.
 * 
 * <p>Animation System:
 * <ul>
 *   <li>New dragons detected via ID comparison between previous and current sets</li>
 *   <li>Appearance time recorded using System.nanoTime() for precise timing</li>
 *   <li>AnimationTimer drives continuous redraw while animations are active</li>
 *   <li>Cubic easing applied for smooth appearance progression (1 - (1-t)³)</li>
 *   <li>Scale: 60% → 100%, Alpha: 20% → 100% over duration</li>
 *   <li>Automatically stops timer when all dragons reach full appearance</li>
 * </ul>
 */
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

    /**
     * Constructs a dragon canvas with specified dimensions.
     *
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     */
    public DragonCanvasView(double width, double height) {
        super(width, height);
        setStyle("-fx-background-color: #0f172a;");
    }

    /**
     * Updates drawable dragons and initiates appearance animations for new dragons.
     *
     * <p>Detects newly added dragons by comparing IDs against previous set. Records appearance
     * start times and begins animation timer if any new dragons detected.
     *
     * @param drawables list of dragons to display (may be null, treated as empty)
     */
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

    /**
     * Plays fade-in and scale-up refresh animations on canvas.
     *
     * <p>Fades from 75% to 100% opacity and scales from 98% to 100% over 180ms.
     * Used to provide visual feedback when collection is refreshed.
     */
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

    /**
     * Redraws all dragons with current appearance progress and coordinate scaling.
     *
     * <p>Applies auto-scaling to fit all dragons in viewport with padding. Calculates
     * appearance progress for each dragon and applies scale + alpha interpolation.
     * Current user's dragons rendered with bright borders (2.4px).
     * Dragon labels shown as white text (ID).
     */
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

    /**
     * Calculates appearance progress for a specific dragon.
     *
     * <p>Returns smooth interpolation from 0.0 (fully hidden) to 1.0 (fully visible)
     * using cubic easing function (1 - (1-t)³). Dragons not in appearance map
     * return 1.0 (fully visible).
     *
     * @param dragonId the dragon to check
     * @return appearance progress value 0.0-1.0 with cubic easing applied
     */
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

    /**
     * Finds drawable dragon at specified screen coordinates.
     *
     * <p>Uses hit detection based on rendered dragon radius. Returns empty Optional
     * if no dragon found at coordinates.
     *
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @return optional containing drawable dragon if found at coordinates
     */
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
