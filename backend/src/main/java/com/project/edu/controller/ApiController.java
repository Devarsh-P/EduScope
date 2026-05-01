package com.project.edu.controller;

import com.project.edu.model.AnalyticsResponse;
import com.project.edu.model.Course;
import com.project.edu.model.RecommendationRequest;
import com.project.edu.model.SearchRequest;
import com.project.edu.model.SearchResult;
import com.project.edu.model.ValidationResponse;
import com.project.edu.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API Controller
 * Exposes all backend features as HTTP endpoints.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final AnalysisService analysisService;

    public ApiController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // Health check
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // Crawl all platforms again and overwrite stored CSV/data
    @GetMapping("/crawl")
    public List<Course> crawl() {
        return analysisService.refreshData();
    }

    // Get cached courses without re-crawling
    @GetMapping("/courses")
    public List<Course> courses() {
        return analysisService.getCourses();
    }

    // Search in existing cached data
    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest request) {
        String keyword = request == null ? "" : request.getKeyword();
        return analysisService.search(keyword);
    }

    // Re-crawl first, then search
    @PostMapping("/crawl-search")
    public List<SearchResult> crawlSearch(@RequestBody SearchRequest request) {
        String keyword = request == null ? "" : request.getKeyword();
        return analysisService.crawlAndSearch(keyword);
    }

    // Word completion
    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam String prefix) {
        return analysisService.autocomplete(prefix);
    }

    // Spell checking
    @GetMapping("/spellcheck")
    public List<String> spellcheck(@RequestParam String word) {
        return analysisService.spellSuggestions(word);
    }

    // Analytics
    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analysisService.analytics();
    }

    // Recommendation system
    @PostMapping("/recommend")
    public Map<String, Object> recommend(@RequestBody RecommendationRequest request) {
        return analysisService.recommend(request);
    }

    // Regex validation
    @GetMapping("/validate")
    public ValidationResponse validate(
            @RequestParam String url,
            @RequestParam String priceText) {
        return analysisService.validate(url, priceText);
    }

    // Inverted index preview
    @GetMapping("/index-preview")
    public Map<String, Set<String>> getIndexPreview() {
        return analysisService.getIndexPreview();
    }

    // Frequency count for a specific course + word
    @GetMapping("/frequency")
    public Map<String, Object> getFrequency(
            @RequestParam String courseId,
            @RequestParam String word) {
        return analysisService.getWordFrequencyForCourse(courseId, word);
    }

    // Crawl summary for frontend
    @GetMapping("/crawl-summary")
    public Map<String, Object> getCrawlSummary() {
        return analysisService.getCrawlSummary();
    }
}