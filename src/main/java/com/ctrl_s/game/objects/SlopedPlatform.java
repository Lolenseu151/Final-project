package com.ctrl_s.game.objects;

/**
 * Simple sloped platform representation as a line segment between two points.
 */
public class SlopedPlatform {
    public final float x1, y1, x2, y2;
    public final float minX, maxX;
    public final float angleDeg;

    public SlopedPlatform(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.angleDeg = (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    public boolean containsX(float x) {
        return x >= minX && x <= maxX;
    }

    /**
     * Returns the Y coordinate on the slope for the given X (linear interpolation).
     */
    public float getYAt(float x) {
        if (x1 == x2)
            return Math.max(y1, y2);
        float t = (x - x1) / (x2 - x1);
        return y1 + t * (y2 - y1);
    }
}
