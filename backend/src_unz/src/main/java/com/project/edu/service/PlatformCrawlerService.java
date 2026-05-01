package com.project.edu.service;

import com.project.edu.model.Course;
import com.project.edu.util.TextUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class PlatformCrawlerService {

    public static final List<String> DEFAULT_TOPICS = List.of(
            "python",
            "java",
            "machine learning",
            "data science",
            "ai",
            "aws",
            "c++",
            "web development",
            "salesforce",
            "business analyst"
    );

    private static final int MAX_RESULTS_PER_PLATFORM = 30;
    private static final int MAX_RESULTS_PER_TOPIC_PER_PLATFORM = 5;

    public List<Course> crawlAll() {
        List<Course> allCourses = new ArrayList<>();
        allCourses.addAll(crawlConfiguredTopics());
        return deduplicate(allCourses);
    }

    public List<Course> crawlAll(String query) {
        if (query == null || query.isBlank()) {
            return crawlAll();
        }

        List<Course> allCourses = new ArrayList<>();
        allCourses.addAll(crawlKhanAcademy(query, MAX_RESULTS_PER_PLATFORM));
        allCourses.addAll(crawlEdx(query, MAX_RESULTS_PER_PLATFORM));
        allCourses.addAll(crawlFutureLearn(query, MAX_RESULTS_PER_PLATFORM));
        allCourses.addAll(crawlClassCentral(query, MAX_RESULTS_PER_PLATFORM));
        allCourses.addAll(crawlSaylor(query, MAX_RESULTS_PER_PLATFORM));
        return deduplicate(allCourses);
    }

    public List<Course> crawlConfiguredTopics() {
        Map<String, Integer> platformCounter = new HashMap<>();
        List<Course> collected = new ArrayList<>();

        for (String topic : DEFAULT_TOPICS) {
            collected.addAll(limitByPlatform(crawlKhanAcademy(topic, MAX_RESULTS_PER_TOPIC_PER_PLATFORM), platformCounter));
            collected.addAll(limitByPlatform(crawlEdx(topic, MAX_RESULTS_PER_TOPIC_PER_PLATFORM), platformCounter));
            collected.addAll(limitByPlatform(crawlFutureLearn(topic, MAX_RESULTS_PER_TOPIC_PER_PLATFORM), platformCounter));
            collected.addAll(limitByPlatform(crawlClassCentral(topic, MAX_RESULTS_PER_TOPIC_PER_PLATFORM), platformCounter));
            collected.addAll(limitByPlatform(crawlSaylor(topic, MAX_RESULTS_PER_TOPIC_PER_PLATFORM), platformCounter));
        }

        return deduplicate(collected);
    }

    public List<Course> crawlKhanAcademy(String query, int limit) {
        String url = "https://www.khanacademy.org/search?page_search_query=" + encode(query);
        return crawlPlatform(
                "Khan Academy",
                query,
                url,
                Set.of("khanacademy.org"),
                List.of("/computing/", "/science/", "/college-careers-more/", "/math/", "/search?"),
                List.of("a[href]"),
                limit,
                "No formal certificate",
                "Free",
                "Video + Practice",
                "Self-paced learning"
        );
    }

    public List<Course> crawlEdx(String query, int limit) {
        String url = "https://www.edx.org/courses?q=" + encode(query);
        return crawlPlatform(
                "edX",
                query,
                url,
                Set.of("edx.org"),
                List.of("/learn/", "/course/", "/certificate/", "/courses?q="),
                List.of("a[href]"),
                limit,
                "Verified certificate available",
                "Free / Paid Certificate",
                "Video + Assignments",
                "University-backed content"
        );
    }

    public List<Course> crawlFutureLearn(String query, int limit) {
        String url = "https://www.futurelearn.com/search?q=" + encode(query);
        return crawlPlatform(
                "FutureLearn",
                query,
                url,
                Set.of("futurelearn.com"),
                List.of("/courses/"),
                List.of("a[href]"),
                limit,
                "Certificate available",
                "Subscription / Paid Upgrade",
                "Video + Activities",
                "Guided learning experience"
        );
    }

    public List<Course> crawlClassCentral(String query, int limit) {
        String url = "https://www.classcentral.com/search?q=" + encode(query);
        return crawlPlatform(
                "Class Central",
                query,
                url,
                Set.of("classcentral.com"),
                List.of("/course/", "/subject/", "/learn/"),
                List.of("a[href]"),
                limit,
                "Certificate depends on provider",
                "Free / Paid",
                "Aggregated Course Links",
                "Course discovery platform"
        );
    }

    public List<Course> crawlSaylor(String query, int limit) {
        String url = "https://learn.saylor.org/course/search.php?search=" + encode(query);
        return crawlPlatform(
                "Saylor Academy",
                query,
                url,
                Set.of("learn.saylor.org"),
                List.of("/course/view.php", "/enrol/index.php", "/mod/page/view.php"),
                List.of("a[href]"),
                limit,
                "Certificate of completion",
                "Free",
                "Reading + Exams",
                "Low-cost certification pathway"
        );
    }

    private List<Course> limitByPlatform(List<Course> fresh, Map<String, Integer> platformCounter) {
        List<Course> accepted = new ArrayList<>();
        for (Course course : fresh) {
            int current = platformCounter.getOrDefault(course.getPlatform(), 0);
            if (current >= MAX_RESULTS_PER_PLATFORM) {
                continue;
            }
            accepted.add(course);
            platformCounter.put(course.getPlatform(), current + 1);
        }
        return accepted;
    }

    private List<Course> deduplicate(List<Course> courses) {
        Map<String, Course> unique = new LinkedHashMap<>();
        for (Course course : courses) {
            String key = normalizeUrl(course.getUrl());
            if (!key.isBlank()) {
                unique.putIfAbsent(key, course);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<Course> crawlPlatform(
            String platform,
            String query,
            String url,
            Set<String> allowedHosts,
            List<String> pathHints,
            List<String> selectors,
            int limit,
            String certification,
            String pricing,
            String format,
            String features
    ) {
        try {
            Document doc = connect(url);
            List<String> queryTokens = TextUtils.tokenize(query);
            LinkedHashMap<String, Course> results = new LinkedHashMap<>();

            for (String selector : selectors) {
                Elements links = doc.select(selector);
                for (Element link : links) {
                    String absUrl = normalizeUrl(link.absUrl("href"));
                    String title = extractTitle(link);

                    if (!isUsableCourseLink(absUrl, title, allowedHosts, pathHints, queryTokens)) {
                        continue;
                    }

                    String id = buildId(platform, absUrl);
                    results.putIfAbsent(absUrl, new Course(
                            id,
                            platform,
                            title,
                            query,
                            "Live crawled result for \"" + query + "\" from " + platform + ".",
                            certification,
                            pricing,
                            format,
                            features,
                            "N/A",
                            "N/A",
                            absUrl
                    ));

                    if (results.size() >= limit) {
                        return new ArrayList<>(results.values());
                    }
                }
            }

            return new ArrayList<>(results.values());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Document connect(String url) throws Exception {
        Connection.Response response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36")
                .timeout((int) Duration.ofSeconds(20).toMillis())
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .execute();

        return response.parse();
    }

    private String extractTitle(Element link) {
        String title = clean(link.text());
        if (!title.isBlank()) {
            return title;
        }

        title = clean(link.attr("title"));
        if (!title.isBlank()) {
            return title;
        }

        Element heading = link.selectFirst("h1, h2, h3, h4, span, div");
        return heading == null ? "" : clean(heading.text());
    }

    private boolean isUsableCourseLink(
            String url,
            String title,
            Set<String> allowedHosts,
            List<String> pathHints,
            List<String> queryTokens
    ) {
        if (url.isBlank() || title.isBlank()) {
            return false;
        }
        if (title.length() < 4) {
            return false;
        }
        if (title.equalsIgnoreCase("learn more") || title.equalsIgnoreCase("read more") || title.equalsIgnoreCase("sign in")) {
            return false;
        }
        if (!hostAllowed(url, allowedHosts)) {
            return false;
        }
        if (!matchesAnyHint(url, pathHints)) {
            return false;
        }
        if (containsBlockedFragment(url)) {
            return false;
        }
        return matchesQuery(title, url, queryTokens);
    }

    private boolean hostAllowed(String url, Set<String> allowedHosts) {
        try {
            String host = Optional.ofNullable(URI.create(url).getHost()).orElse("").toLowerCase(Locale.ROOT);
            return allowedHosts.stream().anyMatch(host::contains);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean containsBlockedFragment(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("/about")
                || lower.contains("/help")
                || lower.contains("/blog")
                || lower.contains("/privacy")
                || lower.contains("/terms")
                || lower.contains("/signup")
                || lower.contains("/sign-up")
                || lower.contains("/register")
                || lower.contains("/login")
                || lower.contains("/log-in")
                || lower.contains("/donate")
                || lower.contains("facebook.com")
                || lower.contains("linkedin.com")
                || lower.contains("twitter.com")
                || lower.contains("youtube.com")
                || lower.contains("mailto:");
    }

    private boolean matchesAnyHint(String url, List<String> hints) {
        for (String hint : hints) {
            if (url.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesQuery(String title, String url, List<String> queryTokens) {
        if (queryTokens.isEmpty()) {
            return true;
        }

        String full = (title + " " + url).toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String token : queryTokens) {
            if (full.contains(token)) {
                matched++;
            }
        }

        if (queryTokens.size() == 1) {
            return matched >= 1;
        }
        return matched >= Math.max(1, queryTokens.size() - 1);
    }

    private String encode(String text) {
        return URLEncoder.encode(text == null ? "" : text, StandardCharsets.UTF_8);
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String buildId(String platform, String url) {
        String prefix = platform.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (prefix.length() > 3) {
            prefix = prefix.substring(0, 3);
        }
        return prefix + "_" + Math.abs(url.hashCode());
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String normalized = url.trim();
        int hashIndex = normalized.indexOf('#');
        if (hashIndex >= 0) {
            normalized = normalized.substring(0, hashIndex);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
