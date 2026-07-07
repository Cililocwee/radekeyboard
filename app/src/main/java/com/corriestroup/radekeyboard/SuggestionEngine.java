package com.corriestroup.radekeyboard;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
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

        // Bounded top-K selection: a 1-char prefix can match thousands of entries,
        // and this runs per keystroke — never sort the whole match list.
        List<WordDictionary.Candidate> matches = dictionary.prefixMatches(folded);
        ScoredWord[] top = new ScoredWord[max];
        for (WordDictionary.Candidate c : matches) {
            if (c.word.equals(typedNfc)) continue;
            boolean exactPrefix = typedHasMarks && c.word.startsWith(typedNfc);
            insertRanked(top, new ScoredWord(c.word, c.frequency, exactPrefix));
        }

        List<String> out = new ArrayList<>(max);
        for (ScoredWord s : top) {
            if (s != null) out.add(s.word);
        }
        return out;
    }

    /** Insert into the fixed-size ranked array (best first), dropping the last. */
    private static void insertRanked(ScoredWord[] top, ScoredWord candidate) {
        for (int i = 0; i < top.length; i++) {
            if (top[i] == null || candidate.ranksAbove(top[i])) {
                System.arraycopy(top, i, top, i + 1, top.length - i - 1);
                top[i] = candidate;
                return;
            }
        }
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

        boolean ranksAbove(ScoredWord other) {
            if (exactPrefix != other.exactPrefix) return exactPrefix;
            return frequency > other.frequency;
        }
    }
}
