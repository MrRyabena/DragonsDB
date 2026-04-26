package client.mvvm.model;

import dragon.Dragon;

/** Precomputed visual data for drawing a dragon on a canvas-like area. */
public record DrawableDragon(
        Dragon dragon,
        double x,
        double y,
        double size,
        String colorHex,
        boolean ownedByCurrentUser) {}
