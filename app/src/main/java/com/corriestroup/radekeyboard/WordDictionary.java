package com.corriestroup.radekeyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Pure (Android-free) frequency dictionary with diacritic-folded prefix lookup.
 * Immutable once built; a prefix query is two binary searches plus a bounded scan,
 * so lookups are safe on the UI thread. Loading (~15K lines) is the slow part —
 * the service does it once on a background thread.
 *
 * <p>Asset format: gzipped UTF-8 lines of {@code word<space>count}; {@code #} lines
 * are comments (attribution headers). The assets use a {@code .dict} extension
 * because aapt gunzips and renames {@code .gz} assets inside the APK.
 */
final class WordDictionary {

    private final String[] foldedKeys; // sorted
    private final String[] words;      // parallel to foldedKeys
    private final int[] frequencies;   // parallel to foldedKeys

    private WordDictionary(String[] foldedKeys, String[] words, int[] frequencies) {
        this.foldedKeys = foldedKeys;
        this.words = words;
        this.frequencies = frequencies;
    }

    static WordDictionary loadGzip(InputStream gzipped) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(gzipped), StandardCharsets.UTF_8))) {
            return load(reader);
        }
    }

    static WordDictionary load(BufferedReader reader) throws IOException {
        List<String[]> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            int space = line.indexOf(' ');
            if (space <= 0) continue;
            rows.add(new String[]{line.substring(0, space), line.substring(space + 1)});
        }

        int n = rows.size();
        String[][] entries = new String[n][3];
        for (int i = 0; i < n; i++) {
            String word = Normalizer.normalize(rows.get(i)[0], Normalizer.Form.NFC);
            entries[i][0] = DiacriticFolder.fold(word);
            entries[i][1] = word;
            entries[i][2] = rows.get(i)[1];
        }
        // Explicit Comparator: Comparator.comparing needs API 24 (minSdk 21).
        Arrays.sort(entries, new Comparator<String[]>() {
            @Override
            public int compare(String[] a, String[] b) {
                return a[0].compareTo(b[0]);
            }
        });

        String[] keys = new String[n];
        String[] words = new String[n];
        int[] freqs = new int[n];
        for (int i = 0; i < n; i++) {
            keys[i] = entries[i][0];
            words[i] = entries[i][1];
            try {
                freqs[i] = Integer.parseInt(entries[i][2].trim());
            } catch (NumberFormatException e) {
                freqs[i] = 1;
            }
        }
        return new WordDictionary(keys, words, freqs);
    }

    int size() {
        return foldedKeys.length;
    }

    /** One completion candidate. */
    static final class Candidate {
        final String word;
        final int frequency;

        Candidate(String word, int frequency) {
            this.word = word;
            this.frequency = frequency;
        }
    }

    /**
     * All words whose folded form starts with {@code foldedPrefix}, unranked.
     * Callers rank/trim (see {@code SuggestionEngine}).
     */
    List<Candidate> prefixMatches(String foldedPrefix) {
        List<Candidate> out = new ArrayList<>();
        if (foldedPrefix == null || foldedPrefix.isEmpty()) return out;

        int start = lowerBound(foldedPrefix);
        for (int i = start; i < foldedKeys.length && foldedKeys[i].startsWith(foldedPrefix); i++) {
            out.add(new Candidate(words[i], frequencies[i]));
        }
        return out;
    }

    /** Index of the first key >= {@code prefix}. */
    private int lowerBound(String prefix) {
        int lo = 0;
        int hi = foldedKeys.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (foldedKeys[mid].compareTo(prefix) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}
