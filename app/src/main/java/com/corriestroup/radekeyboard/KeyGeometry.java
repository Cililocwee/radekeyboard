package com.corriestroup.radekeyboard;

/**
 * Pure (Android-free) hit-testing math for the keyboard. Keys are drawn with small
 * margins between them; treating those gaps as dead zones drops fast taps, so touch
 * resolution falls back to the nearest key by clamped distance.
 */
final class KeyGeometry {

    private KeyGeometry() {
        // Utility class — no instances.
    }

    /**
     * Squared distance from point ({@code px},{@code py}) to the axis-aligned rect
     * {@code [left,top,right,bottom]}. Zero when the point is inside the rect.
     */
    static float distanceSquaredToRect(float px, float py,
                                       float left, float top, float right, float bottom) {
        float clampedX = Math.max(left, Math.min(px, right));
        float clampedY = Math.max(top, Math.min(py, bottom));
        float dx = px - clampedX;
        float dy = py - clampedY;
        return dx * dx + dy * dy;
    }

    /**
     * Index of the rect nearest to ({@code px},{@code py}), where rect {@code i} is
     * {@code [xs[i], ys[i], xs[i]+widths[i], ys[i]+heights[i]]}. Exact hits win with
     * distance zero. Returns -1 for an empty list; ties go to the lowest index.
     */
    static int nearestKeyIndex(float px, float py,
                               float[] xs, float[] ys, float[] widths, float[] heights) {
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            float d = distanceSquaredToRect(px, py,
                    xs[i], ys[i], xs[i] + widths[i], ys[i] + heights[i]);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }
}
