package com.project.edu.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trie {
    private static class Node {
        Map<Character, Node> children = new HashMap<>();
        boolean isWord;
    }

    private final Node root = new Node();

    public void insert(String word) {
        if (word == null || word.isBlank()) return;
        Node current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current = current.children.computeIfAbsent(ch, k -> new Node());
        }
        current.isWord = true;
    }

    public List<String> startsWith(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        if (prefix == null) return results;
        Node current = root;
        for (char ch : prefix.toLowerCase().toCharArray()) {
            current = current.children.get(ch);
            if (current == null) return results;
        }
        collect(current, prefix.toLowerCase(), results, limit);
        return results;
    }

    private void collect(Node node, String current, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isWord) results.add(current);
        for (Map.Entry<Character, Node> entry : node.children.entrySet()) {
            collect(entry.getValue(), current + entry.getKey(), results, limit);
        }
    }
}
