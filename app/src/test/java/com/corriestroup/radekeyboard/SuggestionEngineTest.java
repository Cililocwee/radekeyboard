package com.corriestroup.radekeyboard;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SuggestionEngineTest {

    private SuggestionEngine engine;

    @Before
    public void setUp() throws IOException {
        String dict = "# attribution comment line\n"
                + "người 1000\n"
                + "ngươi 5\n"
                + "nguy 300\n"
                + "việt 900\n"
                + "viện 400\n"
                + "vien 2\n"
                + "đây 800\n"
                + "day 10\n";
        WordDictionary dictionary = WordDictionary.load(new BufferedReader(new StringReader(dict)));
        assertEquals(8, dictionary.size());
        engine = new SuggestionEngine(dictionary);
    }

    @Test
    public void bareAsciiPrefixRestoresDiacritics() {
        List<String> s = engine.suggest("nguo", 3);
        assertEquals("người", s.get(0)); // highest frequency folded match
    }

    @Test
    public void exactDiacriticMatchesOutrankFoldedOnes() {
        // Typing "viện"'s prefix with marks: "việ" matches việt exactly (prefix with
        // diacritics) but viện only foldedly? Both fold to "vie"...
        List<String> s = engine.suggest("việ", 3);
        assertEquals("việt", s.get(0)); // exact-prefix beats folded despite ranking below by any tie
        assertTrue(s.contains("viện"));
    }

    @Test
    public void dStrokeWordsMatchPlainDPrefix() {
        List<String> s = engine.suggest("da", 3);
        assertEquals("đây", s.get(0)); // freq 800 folded match beats day (10)
    }

    @Test
    public void typedWordItselfIsExcluded() {
        List<String> s = engine.suggest("day", 3);
        assertEquals("đây", s.get(0));
        for (String w : s) {
            assertTrue(!w.equals("day"));
        }
    }

    @Test
    public void emptyInputYieldsNoSuggestions() {
        assertTrue(engine.suggest("", 3).isEmpty());
        assertTrue(engine.suggest(null, 3).isEmpty());
    }

    @Test
    public void maxIsRespected() {
        assertTrue(engine.suggest("v", 2).size() <= 2);
    }
}
