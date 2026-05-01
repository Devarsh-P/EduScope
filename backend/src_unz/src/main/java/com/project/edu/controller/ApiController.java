package com.project.edu.controller;

import com.project.edu.model.*;
import com.project.edu.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final AnalysisService analysisService;

    public ApiController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/crawl")
    public List<Course> crawl() {
        return analysisService.refreshData();
    }

    @GetMapping("/courses")
    public List<Course> courses() {
        return analysisService.getCourses();
    }

    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest request) {
        return analysisService.search(request.getKeyword());
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam String prefix) {
        return analysisService.autocomplete(prefix);
    }

    @GetMapping("/spellcheck")
    public List<String> spellcheck(@RequestParam String word) {
        return analysisService.spellSuggestions(word);
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analysisService.analytics();
    }

    @PostMapping("/recommend")
    public Map<String, Object> recommend(@RequestBody RecommendationRequest request) {
        return analysisService.recommend(request);
    }

    @GetMapping("/validate")
    public ValidationResponse validate(@RequestParam String url, @RequestParam String priceText) {
        return analysisService.validate(url, priceText);
    }

    @GetMapping("/index-preview")
    public Map<String, java.util.Set<String>> getIndexPreview() {
        return analysisService.getIndexPreview();
    }

    @GetMapping("/frequency")
    public Map<String, Object> getFrequency(
            @RequestParam String courseId,
            @RequestParam String word
    ) {
        return analysisService.getWordFrequencyForCourse(courseId, word);
    }

    @GetMapping("/crawl-summary")
    public Map<String, Object> getCrawlSummary() {
        return analysisService.getCrawlSummary();
    }
    @PostMapping("/crawl-search")
public List<SearchResult> crawlSearch(@RequestBody SearchRequest request) {
    return analysisService.crawlAndSearch(request.getKeyword());
}
}