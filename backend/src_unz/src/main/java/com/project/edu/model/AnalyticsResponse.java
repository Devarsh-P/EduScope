package com.project.edu.model;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {
    private int totalCourses;
    private Map<String, Long> platformCounts;
    private Map<String, Integer> searchFrequency;
    private List<String> vocabularySample;

    public AnalyticsResponse(int totalCourses, Map<String, Long> platformCounts,
                             Map<String, Integer> searchFrequency, List<String> vocabularySample) {
        this.totalCourses = totalCourses;
        this.platformCounts = platformCounts;
        this.searchFrequency = searchFrequency;
        this.vocabularySample = vocabularySample;
    }

    public int getTotalCourses() { return totalCourses; }
    public void setTotalCourses(int totalCourses) { this.totalCourses = totalCourses; }
    public Map<String, Long> getPlatformCounts() { return platformCounts; }
    public void setPlatformCounts(Map<String, Long> platformCounts) { this.platformCounts = platformCounts; }
    public Map<String, Integer> getSearchFrequency() { return searchFrequency; }
    public void setSearchFrequency(Map<String, Integer> searchFrequency) { this.searchFrequency = searchFrequency; }
    public List<String> getVocabularySample() { return vocabularySample; }
    public void setVocabularySample(List<String> vocabularySample) { this.vocabularySample = vocabularySample; }
}
