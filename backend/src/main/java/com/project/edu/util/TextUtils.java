package com.project.edu.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * FEATURE 3: Edit Distance (Levenshtein algorithm) for spell checking
 * FEATURE 4: Tokenizer for building vocabulary and inverted index
 */
public class TextUtils {

    // Tokenize text into lowercase words (removes punctuation and numbers)
    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z]+"))
                .filter(s -> !s.isBlank() && s.length() > 1)
                .collect(Collectors.toList());
    }

    /**
     * FEATURE 3: Levenshtein Edit Distance
     * Calculates the minimum number of single-character edits (insertions,
     * deletions, substitutions) to transform string a into string b.
     * Used for spell checking suggestions.
     */
    public static int editDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";

        int m = a.length();
        int n = b.length();
        int[][] dp = new int[m + 1][n + 1];

        // Base cases: transform empty string to b (insertions)
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        // Base cases: transform a to empty string (deletions)
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        // Fill DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1,      // deletion
                                 dp[i][j - 1] + 1),      // insertion
                        dp[i - 1][j - 1] + cost          // substitution
                );
            }
        }

        return dp[m][n];
    }
}
