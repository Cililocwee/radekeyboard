package com.corriestroup.radekeyboard;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pure (Android-free) completion ranking over a {@link WordDictionary}. Candidates
 * are prefix matches on the diacritic-folded word; exact typed-prefix matches
 * (diacritics and all) outrank folded-only matches, then frequency decides.
 */
final class SuggestionEngine {

    private final WordDictionary dictionary;

    SuggestionEngine(WordDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Top-{@code max} completions for the word being typed (may be "" → none).
     * The exact typed word itself is excluded — suggesting what's already there
     * helps nobody.
     */
    List<String> suggest(String currentWord, int max) {
        if (currentWord == null || currentWord.isEmpty() || dictionary == null) {
            return Collections.emptyList();
        }
        String typedNfc = Normalizer.normalize(currentWord, Normalizer.Form.NFC)
                .toLowerCase();
        String folded = DiacriticFolder.fold(currentWord);

        // Only boost exact-prefix matches when the user actually typed diacritics —
        // for bare-ASCII input every candidate is a fold match and frequency alone
        // should rank them (typing "da" must still put "đây" first).
        boolean typedHasMarks = !typedNfc.equals(folded);

        List<WordDictionary.Candidate> matches = dictionary.prefixMatches(folded);
        List<ScoredWord> scored = new ArrayList<>(matches.size());
        for (WordDictionary.Candidate c : matches) {
            if (c.word.equals(typedNfc)) continue;
            boolean exactPrefix = typedHasMarks && c.word.startsWith(typedNfc);
            scored.add(new ScoredWord(c.word, c.frequency, exactPrefix));
        }
        scored.sort(Comparator
                .comparing((ScoredWord s) -> !s.exactPrefix)
                .thenComparing(s -> -s.frequency));

        List<String> out = new ArrayList<>(Math.min(max, scored.size()));
        for (int i = 0; i < scored.size() && out.size() < max; i++) {
            out.add(scored.get(i).word);
        }
        return out;
    }

    private static final class ScoredWord {
        final String word;
        final int frequency;
        final boolean exactPrefix;

        ScoredWord(String word, int frequency, boolean exactPrefix) {
            this.word = word;
            this.frequency = frequency;
            this.exactPrefix = exactPrefix;
        }
    }
}
