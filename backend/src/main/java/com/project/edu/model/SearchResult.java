package com.project.edu.model;

public class SearchResult {
    private Course course;
    private int score;
    private int keywordOccurrences;

    public SearchResult() {}

    public SearchResult(Course course, int score, int keywordOccurrences) {
        this.course = course;
        this.score = score;
        this.keywordOccurrences = keywordOccurrences;
    }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getKeywordOccurrences() { return keywordOccurrences; }
    public void setKeywordOccurrences(int keywordOccurrences) { this.keywordOccurrences = keywordOccurrences; }
}
