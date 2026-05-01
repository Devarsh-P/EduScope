package com.project.edu.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FEATURE 4: Word Completion using Trie data structure
 *
 * A Trie (prefix tree) stores all vocabulary words character by character.
 * Given a prefix, it efficiently finds all words that start with that prefix.
 * Time complexity: O(prefix length) for lookup, much faster than scanning all words.
 */
public class Trie {

    private static class Node {
        Map<Character, Node> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private final Node root = new Node();

    // Insert a word into the Trie
    public void insert(String word) {
        if (word == null || word.isBlank()) return;
        Node current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current = current.children.computeIfAbsent(ch, k -> new Node());
        }
        current.isEndOfWord = true;
    }

    // Find all words that start with the given prefix (up to limit results)
    public List<String> startsWith(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        if (prefix == null) return results;

        // Navigate to end of prefix
        Node current = root;
        for (char ch : prefix.toLowerCase().toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return results; // prefix not found
        }

        // Collect all words from this node onwards
        collectWords(current, prefix.toLowerCase(), results, limit);
        return results;
    }

    // DFS to collect all complete words reachable from the given node
    private void collectWords(Node node, String current, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isEndOfWord) results.add(current);
        for (Map.Entry<Character, Node> entry : node.children.entrySet()) {
            collectWords(entry.getValue(), current + entry.getKey(), results, limit);
        }
    }
}
