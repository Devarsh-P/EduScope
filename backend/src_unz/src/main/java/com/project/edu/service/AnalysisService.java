package com.project.edu.service;

import com.project.edu.model.*;
import com.project.edu.util.TextUtils;
import com.project.edu.util.Trie;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AnalysisService {
    private final PlatformCrawlerService crawlerService;
    private final CsvStorageService csvStorageService;
    private final List<Course> courses = new ArrayList<>();
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();
    private final Map<String, Integer> searchFrequency = new HashMap<>();
    private final Set<String> vocabulary = new TreeSet<>();
    private Trie trie = new Trie();

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\$\\d+(?:\\.\\d{2})?|free|subscription|paid)", Pattern.CASE_INSENSITIVE);

    public AnalysisService(PlatformCrawlerService crawlerService, CsvStorageService csvStorageService) {
        this.crawlerService = crawlerService;
        this.csvStorageService = csvStorageService;
        refreshData();
    }

    public List<Course> refreshData() {
        courses.clear();
        courses.addAll(crawlerService.crawlAll());
        rebuildIndexes();
        csvStorageService.saveCourses(courses);
        return courses;
    }

    public List<SearchResult> crawlAndSearch(String keyword) {
        courses.clear();
        courses.addAll(crawlerService.crawlAll(keyword));
        rebuildIndexes();
        csvStorageService.saveCourses(courses);
        return search(keyword);
    }

    private void rebuildIndexes() {
        invertedIndex.clear();
        vocabulary.clear();
        trie = new Trie();

        for (Course course : courses) {
            List<String> tokens = TextUtils.tokenize(course.combinedText());
            course.setTokens(tokens);

            for (String token : tokens) {
                invertedIndex.computeIfAbsent(token, k -> new TreeSet<>()).add(course.getId());
                vocabulary.add(token);
            }
        }

        for (String word : vocabulary) {
            trie.insert(word);
        }
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<SearchResult> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();

        String normalized = keyword.toLowerCase().trim();
        List<String> queryTokens = TextUtils.tokenize(normalized);
        searchFrequency.merge(normalized, 1, Integer::sum);

        return courses.stream()
                .map(course -> {
                    int score = rankingScore(course, normalized, queryTokens);
                    int occurrences = frequencyCount(course, normalized);
                    return new SearchResult(course, score, occurrences);
                })
                .filter(result -> result.getScore() > 0)
                .sorted(Comparator.comparingInt(SearchResult::getScore).reversed())
                .collect(Collectors.toList());
    }

    public int frequencyCount(Course course, String keyword) {
        if (course == null || keyword == null || keyword.isBlank()) return 0;

        String fullText = course.combinedText().toLowerCase();
        String normalizedKeyword = keyword.toLowerCase().trim();

        int count = countOccurrences(fullText, normalizedKeyword);

        if (count == 0) {
            for (String token : TextUtils.tokenize(normalizedKeyword)) {
                count += countOccurrences(fullText, token);
            }
        }

        return count;
    }

    private int countOccurrences(String text, String phrase) {
        if (phrase == null || phrase.isBlank()) return 0;

        int count = 0;
        int index = 0;

        while ((index = text.indexOf(phrase, index)) != -1) {
            count++;
            index += phrase.length();
        }

        return count;
    }

    private int rankingScore(Course course, String fullQuery, List<String> queryTokens) {
    String title = safe(course.getTitle()).toLowerCase();
    String category = safe(course.getCategory()).toLowerCase();
    String description = safe(course.getDescription()).toLowerCase();
    String platform = safe(course.getPlatform()).toLowerCase();

    int score = 0;

    // exact phrase match gets highest priority
    if (title.equals(fullQuery)) score += 200;
    if (title.contains(fullQuery)) score += 120;
    if (category.contains(fullQuery)) score += 60;
    if (description.contains(fullQuery)) score += 40;

    int matchedTokens = 0;

    for (String token : queryTokens) {
        if (title.contains(token)) {
            score += 25;
            matchedTokens++;
        }
        if (category.contains(token)) {
            score += 10;
        }
        if (description.contains(token)) {
            score += 8;
        }
        if (platform.contains(token)) {
            score += 4;
        }
    }

    // if nothing matches at all, reject
    if (matchedTokens == 0) {
        return 0;
    }

    // more token matches = better ranking
    score += matchedTokens * 15;

    // frequency count bonus
    score += frequencyCount(course, fullQuery) * 12;

    return score;
}

    public List<String> autocomplete(String prefix) {
        return trie.startsWith(prefix == null ? "" : prefix.toLowerCase(), 8);
    }

    public List<String> spellSuggestions(String word) {
        if (word == null || word.isBlank()) return List.of();

        String normalized = word.toLowerCase().trim();
        if (vocabulary.contains(normalized)) return List.of(normalized);

        return vocabulary.stream()
                .map(candidate -> Map.entry(candidate, TextUtils.editDistance(normalized, candidate)))
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public AnalyticsResponse analytics() {
        Map<String, Long> platformCounts = courses.stream()
                .collect(Collectors.groupingBy(Course::getPlatform, TreeMap::new, Collectors.counting()));

        List<String> sample = vocabulary.stream().limit(20).toList();
        return new AnalyticsResponse(courses.size(), platformCounts, new TreeMap<>(searchFrequency), sample);
    }

    public Map<String, Object> recommend(RecommendationRequest request) {
        String interest = optional(request.getInterest());
        String budget = optional(request.getBudget());
        boolean certRequired = request.isCertificationRequired();

        List<SearchResult> ranked = courses.stream().map(course -> {
            int score = 0;

            String combined = course.combinedText().toLowerCase();
            String category = optional(course.getCategory());
            String platform = optional(course.getPlatform());
            String pricing = optional(course.getPricingModel());
            String certification = optional(course.getCertification());

            if (!interest.isBlank() && (combined.contains(interest) || category.contains(interest))) score += 20;
            if (budget.equals("free") && pricing.contains("free")) score += 18;
            if (budget.equals("low") && (pricing.contains("free") || pricing.contains("paid"))) score += 12;
            if (budget.equals("subscription") && pricing.contains("subscription")) score += 14;
            if (certRequired && certification.contains("certificate")) score += 18;
            if (!certRequired) score += 4;

            if (platform.contains("khan academy") && budget.equals("free")) score += 10;
            if (platform.contains("edx") && certRequired) score += 10;
            if (platform.contains("saylor academy") && budget.equals("free")) score += 8;
            if (platform.contains("futurelearn") && budget.equals("subscription")) score += 8;
            if (platform.contains("class central") && !interest.isBlank()) score += 5;

            return new SearchResult(course, score, 0);
        }).sorted(Comparator.comparingInt(SearchResult::getScore).reversed()).limit(5).toList();

        String platform = ranked.isEmpty() ? "No match" : ranked.get(0).getCourse().getPlatform();

        String reason = switch (platform) {
            case "Khan Academy" -> "Best for fully free and self-paced learning resources.";
            case "edX" -> "Strong for university-backed content and certificate options.";
            case "Saylor Academy" -> "Good for budget-friendly learning with free course access.";
            case "FutureLearn" -> "Useful for guided learning and subscription-based study.";
            case "Class Central" -> "Best for discovering courses from multiple providers in one place.";
            default -> "No strong recommendation could be formed.";
        };

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("recommendedPlatform", platform);
        response.put("reason", reason);
        response.put("courses", ranked);
        return response;
    }

    public ValidationResponse validate(String url, String priceText) {
        boolean validUrl = url != null && URL_PATTERN.matcher(url).matches();
        boolean validPrice = priceText != null && PRICE_PATTERN.matcher(priceText).find();

        List<String> prices = new ArrayList<>();
        if (priceText != null) {
            Matcher matcher = PRICE_PATTERN.matcher(priceText);
            while (matcher.find()) {
                prices.add(matcher.group());
            }
        }

        return new ValidationResponse(validUrl, validPrice, prices);
    }

    public Map<String, Object> getWordFrequencyForCourse(String courseId, String word) {
        Course course = courses.stream()
                .filter(c -> Objects.equals(c.getId(), courseId))
                .findFirst()
                .orElse(null);

        if (course == null) {
            return Map.of(
                    "courseId", courseId,
                    "word", word,
                    "count", 0,
                    "url", "Not found",
                    "title", "Not found"
            );
        }

        int count = frequencyCount(course, word);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courseId", course.getId());
        response.put("title", course.getTitle());
        response.put("url", course.getUrl());
        response.put("word", word);
        response.put("count", count);
        return response;
    }

    public Map<String, Set<String>> getIndexPreview() {
        return invertedIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(12)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Object> getCrawlSummary() {
        Map<String, Long> platformCounts = courses.stream()
                .collect(Collectors.groupingBy(Course::getPlatform, TreeMap::new, Collectors.counting()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCourses", courses.size());
        summary.put("platformCounts", platformCounts);
        summary.put("csvPath", csvStorageService.getOutputPath());
        summary.put("configuredTopics", PlatformCrawlerService.DEFAULT_TOPICS);
        summary.put("fieldsExtracted", List.of(
                "title", "platform", "category", "description", "certification",
                "pricingModel", "format", "specialFeatures", "rating", "reviewCount", "url"
        ));
        return summary;
    }

    private String optional(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}