package com.corriestroup.radekeyboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeyGeometryTest {

    // Two 100x50 keys side by side with a 10px gap: [0..100] and [110..210].
    private static final float[] XS = {0f, 110f};
    private static final float[] YS = {0f, 0f};
    private static final float[] WIDTHS = {100f, 100f};
    private static final float[] HEIGHTS = {50f, 50f};

    @Test
    public void pointInsideKeyIsExactHit() {
        assertEquals(0, KeyGeometry.nearestKeyIndex(50f, 25f, XS, YS, WIDTHS, HEIGHTS));
        assertEquals(1, KeyGeometry.nearestKeyIndex(150f, 25f, XS, YS, WIDTHS, HEIGHTS));
        assertEquals(0f, KeyGeometry.distanceSquaredToRect(50f, 25f, 0f, 0f, 100f, 50f), 0f);
    }

    @Test
    public void gapBetweenKeysResolvesToNearerKey() {
        assertEquals(0, KeyGeometry.nearestKeyIndex(103f, 25f, XS, YS, WIDTHS, HEIGHTS));
        assertEquals(1, KeyGeometry.nearestKeyIndex(107f, 25f, XS, YS, WIDTHS, HEIGHTS));
    }

    @Test
    public void gapMidpointTiesGoToLowestIndex() {
        assertEquals(0, KeyGeometry.nearestKeyIndex(105f, 25f, XS, YS, WIDTHS, HEIGHTS));
    }

    @Test
    public void pointBelowBottomRowResolvesToBottomKey() {
        assertEquals(0, KeyGeometry.nearestKeyIndex(50f, 80f, XS, YS, WIDTHS, HEIGHTS));
        // Diagonal corner gap: closer to key 1's corner.
        assertEquals(1, KeyGeometry.nearestKeyIndex(109f, 60f, XS, YS, WIDTHS, HEIGHTS));
    }

    @Test
    public void cornerDistanceIsEuclidean() {
        // 3px right of and 4px below key 0's bottom-right corner → 3²+4² = 25.
        assertEquals(25f, KeyGeometry.distanceSquaredToRect(103f, 54f, 0f, 0f, 100f, 50f), 0.001f);
    }

    @Test
    public void emptyListReturnsMinusOne() {
        assertEquals(-1, KeyGeometry.nearestKeyIndex(0f, 0f,
                new float[0], new float[0], new float[0], new float[0]));
    }
}
