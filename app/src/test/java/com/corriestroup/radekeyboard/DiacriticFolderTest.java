package com.corriestroup.radekeyboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiacriticFolderTest {

    @Test
    public void stripsToneAndQualityMarks() {
        assertEquals("nguoi", DiacriticFolder.fold("người"));
        assertEquals("viet", DiacriticFolder.fold("viết"));
        assertEquals("an", DiacriticFolder.fold("ăn"));
    }

    @Test
    public void dWithStrokeFoldsToD() {
        // đ does NOT decompose to d + combining mark — the explicit special case.
        assertEquals("day", DiacriticFolder.fold("đây"));
        assertEquals("dong", DiacriticFolder.fold("Đông"));
    }

    @Test
    public void handlesDecomposedInputFromTheRadeLayer() {
        // Base char + combining marks (how this keyboard commits Rade text).
        assertEquals("e", DiacriticFolder.fold("ế")); // ê + sắc, decomposed
        assertEquals("cho", DiacriticFolder.fold("čho"));   // č (decomposed) + ho
    }

    @Test
    public void lowercasesAndPassesAsciiThrough() {
        assertEquals("hello", DiacriticFolder.fold("Hello"));
        assertEquals("", DiacriticFolder.fold(""));
        assertEquals("", DiacriticFolder.fold(null));
    }
}
