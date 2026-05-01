package com.project.edu.service;

import com.project.edu.model.*;
import com.project.edu.util.TextUtils;
import com.project.edu.util.Trie;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private final PlatformCrawlerService crawlerService;

    private final List<Course> courses = new ArrayList<>();
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();
    private final Map<String, Integer> searchFrequency = new HashMap<>();
    private final Set<String> vocabulary = new TreeSet<>();
    private Trie trie = new Trie();

    private static final Pattern URL_PATTERN =
            Pattern.compile("^(https?://)([\\w\\-]+\\.)+[\\w]{2,}(/.*)?$");
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(\\$\\d+(?:\\.\\d{2})?|free|subscription|paid)", Pattern.CASE_INSENSITIVE);

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path COURSES_CSV = DATA_DIR.resolve("courses.csv");
    private static final Path SEARCH_HISTORY_CSV = DATA_DIR.resolve("search_history.csv");
    private static final Path FREQUENCY_LOG_CSV = DATA_DIR.resolve("frequency_log.csv");
    private static final Path CRAWL_SUMMARY_CSV = DATA_DIR.resolve("crawl_summary.csv");

    public AnalysisService(PlatformCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
        initializeData();
    }

    private void initializeData() {
        try {
            Files.createDirectories(DATA_DIR);

            if (Files.exists(COURSES_CSV) && Files.size(COURSES_CSV) > 0) {
                courses.clear();
                courses.addAll(loadCoursesFromCsv());
            } else {
                courses.clear();
                courses.addAll(crawlerService.crawlAll());
                saveCoursesToCsv(courses);
                saveCrawlSummaryCsv();
            }

            if (Files.exists(SEARCH_HISTORY_CSV) && Files.size(SEARCH_HISTORY_CSV) > 0) {
                loadSearchHistoryFromCsv();
            }

            rebuildIndexes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Course> refreshData() {
        courses.clear();
        courses.addAll(crawlerService.crawlAll());
        saveCoursesToCsv(courses);
        saveCrawlSummaryCsv();
        rebuildIndexes();
        return courses;
    }

    public List<SearchResult> crawlAndSearch(String keyword) {
        courses.clear();
        courses.addAll(crawlerService.crawlAll(keyword));
        saveCoursesToCsv(courses);
        saveCrawlSummaryCsv();
        rebuildIndexes();
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
        saveSearchHistoryCsv();

        Set<String> candidateIds = new HashSet<>();
        for (String token : queryTokens) {
            candidateIds.addAll(invertedIndex.getOrDefault(token, Set.of()));
        }

        for (Course course : courses) {
            if (course.combinedText().toLowerCase().contains(normalized)) {
                candidateIds.add(course.getId());
            }
        }

        PriorityQueue<SearchResult> heap = new PriorityQueue<>(
                Comparator.comparingInt(SearchResult::getScore).reversed()
        );

        for (Course course : courses) {
            if (!candidateIds.contains(course.getId())) continue;

            int occurrences = frequencyCount(course, normalized);
            int score = rankingScore(course, normalized, queryTokens, occurrences);

            if (score > 0) {
                heap.offer(new SearchResult(course, score, occurrences));
            }
        }

        List<SearchResult> results = new ArrayList<>();
        while (!heap.isEmpty()) {
            results.add(heap.poll());
        }
        return results;
    }

    public int frequencyCount(Course course, String keyword) {
        if (course == null || keyword == null || keyword.isBlank()) return 0;

        String fullText = course.combinedText().toLowerCase();
        String normalized = keyword.toLowerCase().trim();

        int count = countOccurrences(fullText, normalized);

        if (count == 0) {
            for (String token : TextUtils.tokenize(normalized)) {
                count += countOccurrences(fullText, token);
            }
        }

        logFrequencyCheck(course, keyword, count);
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

    private int rankingScore(Course course, String fullQuery,
                             List<String> queryTokens, int occurrences) {
        String title = safe(course.getTitle()).toLowerCase();
        String category = safe(course.getCategory()).toLowerCase();
        String desc = safe(course.getDescription()).toLowerCase();
        String platform = safe(course.getPlatform()).toLowerCase();

        int score = 0;

        if (title.equals(fullQuery)) score += 200;
        if (title.contains(fullQuery)) score += 120;
        if (category.contains(fullQuery)) score += 60;
        if (desc.contains(fullQuery)) score += 40;

        int matchedTokens = 0;
        for (String token : queryTokens) {
            boolean matched = false;

            if (title.contains(token)) {
                score += 25;
                matched = true;
            }
            if (category.contains(token)) {
                score += 10;
                matched = true;
            }
            if (desc.contains(token)) {
                score += 8;
                matched = true;
            }
            if (platform.contains(token)) {
                score += 4;
                matched = true;
            }

            if (matched) matchedTokens++;
        }

        if (matchedTokens == 0) return 0;

        score += matchedTokens * 15;
        score += occurrences * 12;

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
                .collect(Collectors.groupingBy(
                        Course::getPlatform, TreeMap::new, Collectors.counting()));

        List<String> sample = vocabulary.stream().limit(20).toList();
        return new AnalyticsResponse(
                courses.size(),
                platformCounts,
                new TreeMap<>(searchFrequency),
                sample
        );
    }

    public Map<String, Object> recommend(RecommendationRequest request) {
        String interest = optional(request.getInterest());
        String budget = optional(request.getBudget());
        boolean certRequired = request.isCertificationRequired();

        PriorityQueue<SearchResult> heap = new PriorityQueue<>(
                Comparator.comparingInt(SearchResult::getScore).reversed()
        );

        for (Course course : courses) {
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

            if (platform.contains("khan academy") && budget.equals("free")) score += 12;
            if (platform.contains("edx") && certRequired) score += 12;
            if (platform.contains("saylor") && budget.equals("free")) score += 10;
            if (platform.contains("futurelearn") && budget.equals("subscription")) score += 10;
            if (platform.contains("class central") && !interest.isBlank()) score += 6;

            heap.offer(new SearchResult(course, score, 0));
        }

        List<SearchResult> ranked = new ArrayList<>();
        int count = 0;
        while (!heap.isEmpty() && count < 5) {
            ranked.add(heap.poll());
            count++;
        }

        String topPlatform = ranked.isEmpty() ? "No match" : ranked.get(0).getCourse().getPlatform();

        String reason = switch (topPlatform) {
            case "Khan Academy" -> "Best choice for fully free, self-paced learning with mastery-based progress tracking.";
            case "edX" -> "Ideal for university-backed courses with verified certificates from top institutions.";
            case "FutureLearn" -> "Great for guided, social learning with subscription access and mentor support.";
            case "Class Central" -> "Best platform for discovering and comparing free courses from multiple providers worldwide.";
            case "Saylor Academy", "Saylor" -> "Top pick for completely free college-level courses with budget-friendly access.";
            default -> "No strong recommendation could be formed based on your preferences.";
        };

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("recommendedPlatform", topPlatform);
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

    public Map<String, Set<String>> getIndexPreview() {
        return invertedIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(15)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Object> getWordFrequencyForCourse(String courseId, String word) {
        Course course = courses.stream()
                .filter(c -> Objects.equals(c.getId(), courseId))
                .findFirst().orElse(null);

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
        response.put("platform", course.getPlatform());
        response.put("url", course.getUrl());
        response.put("word", word);
        response.put("count", count);
        return response;
    }

    public Map<String, Object> getCrawlSummary() {
        Map<String, Long> platformCounts = courses.stream()
                .collect(Collectors.groupingBy(
                        Course::getPlatform, TreeMap::new, Collectors.counting()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCourses", courses.size());
        summary.put("platformCounts", platformCounts);
        summary.put("fieldsExtracted", List.of(
                "id", "platform", "title", "category", "description",
                "certification", "pricingModel", "format",
                "specialFeatures", "rating", "reviewCount", "url"
        ));
        summary.put("csvPath", COURSES_CSV.toAbsolutePath().toString());

        // this is what your frontend needs so it stops showing Topics: 0
        summary.put("topics", crawlerService.getRequiredTopics());
        summary.put("topicsCount", crawlerService.getRequiredTopicsCount());

        return summary;
    }

    private void saveCoursesToCsv(List<Course> courseList) {
        try {
            Files.createDirectories(DATA_DIR);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    COURSES_CSV,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                writer.write("id,platform,title,category,description,certification,pricingModel,format,specialFeatures,rating,reviewCount,url");
                writer.newLine();

                for (Course c : courseList) {
                    writer.write(csv(c.getId()) + "," +
                            csv(c.getPlatform()) + "," +
                            csv(c.getTitle()) + "," +
                            csv(c.getCategory()) + "," +
                            csv(c.getDescription()) + "," +
                            csv(c.getCertification()) + "," +
                            csv(c.getPricingModel()) + "," +
                            csv(c.getFormat()) + "," +
                            csv(c.getSpecialFeatures()) + "," +
                            csv(c.getRating()) + "," +
                            csv(c.getReviewCount()) + "," +
                            csv(c.getUrl()));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Course> loadCoursesFromCsv() {
        List<Course> loaded = new ArrayList<>();

        if (!Files.exists(COURSES_CSV)) return loaded;

        try (BufferedReader reader = Files.newBufferedReader(COURSES_CSV)) {
            String line = reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 12) continue;

                Course course = new Course(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        fields.get(3),
                        fields.get(4),
                        fields.get(5),
                        fields.get(6),
                        fields.get(7),
                        fields.get(8),
                        fields.get(9),
                        fields.get(10),
                        fields.get(11)
                );

                loaded.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return loaded;
    }

    private void saveSearchHistoryCsv() {
        try {
            Files.createDirectories(DATA_DIR);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    SEARCH_HISTORY_CSV,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                writer.write("searchTerm,count");
                writer.newLine();

                for (Map.Entry<String, Integer> entry : new TreeMap<>(searchFrequency).entrySet()) {
                    writer.write(csv(entry.getKey()) + "," + csv(String.valueOf(entry.getValue())));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSearchHistoryFromCsv() {
        searchFrequency.clear();

        try (BufferedReader reader = Files.newBufferedReader(SEARCH_HISTORY_CSV)) {
            String line = reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 2) continue;

                String term = fields.get(0);
                int count = Integer.parseInt(fields.get(1));
                searchFrequency.put(term, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logFrequencyCheck(Course course, String word, int count) {
        try {
            Files.createDirectories(DATA_DIR);

            boolean writeHeader = !Files.exists(FREQUENCY_LOG_CSV) || Files.size(FREQUENCY_LOG_CSV) == 0;

            try (BufferedWriter writer = Files.newBufferedWriter(
                    FREQUENCY_LOG_CSV,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                if (writeHeader) {
                    writer.write("courseId,title,platform,url,word,count");
                    writer.newLine();
                }

                writer.write(csv(course.getId()) + "," +
                        csv(course.getTitle()) + "," +
                        csv(course.getPlatform()) + "," +
                        csv(course.getUrl()) + "," +
                        csv(word) + "," +
                        csv(String.valueOf(count)));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCrawlSummaryCsv() {
        try {
            Files.createDirectories(DATA_DIR);

            Map<String, Long> platformCounts = courses.stream()
                    .collect(Collectors.groupingBy(
                            Course::getPlatform, TreeMap::new, Collectors.counting()));

            try (BufferedWriter writer = Files.newBufferedWriter(
                    CRAWL_SUMMARY_CSV,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                writer.write("platform,count");
                writer.newLine();

                for (Map.Entry<String, Long> entry : platformCounts.entrySet()) {
                    writer.write(csv(entry.getKey()) + "," + csv(String.valueOf(entry.getValue())));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        values.add(current.toString());
        return values;
    }

    private String optional(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}