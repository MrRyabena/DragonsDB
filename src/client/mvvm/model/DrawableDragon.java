package client.mvvm.model;

import dragon.Dragon;

/**
 * Precomputed visual data for rendering a dragon on canvas.
 * 
 * <p>Contains transformed dragon position/size and rendering properties precomputed
 * from VisualizationViewModel for efficient canvas rendering.
 */
public record DrawableDragon(
        /** Source dragon entity */
        Dragon dragon,
        /** Transformed x coordinate for rendering */
        double x,
        /** Transformed y coordinate for rendering */
        double y,
        /** Computed visual size derived from weight */
        double size,
        /** Hex color string (RGB format "#RRGGBB") determined by owner identity */
        String colorHex,
        /** True if dragon is owned by current authenticated user */
        boolean ownedByCurrentUser) {}
