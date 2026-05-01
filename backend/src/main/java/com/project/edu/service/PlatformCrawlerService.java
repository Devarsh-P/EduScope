package com.project.edu.service;

import com.project.edu.model.Course;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PlatformCrawlerService {

    private static final int MAX_PER_PLATFORM = 30;

    private static final List<String> REQUIRED_TOPICS = List.of(
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

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    public List<Course> crawlAll() {
        List<Course> all = new ArrayList<>();

        all.addAll(crawlKhanAcademy());      // manual valid seed list
        all.addAll(crawlEdx());              // topic-based search
        all.addAll(crawlFutureLearn());      // topic-based search
        all.addAll(crawlClassCentral());     // topic-based search
        all.addAll(crawlSaylor());           // topic-based search

        return all;
    }

    public List<Course> crawlAll(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return crawlAll();
        }

        String normalized = keyword.trim().toLowerCase();
        List<Course> all = new ArrayList<>();

        all.addAll(crawlKhanAcademyForKeyword(normalized));
        all.addAll(crawlPlatformForSingleTopic("edX", "https://www.edx.org/search?q=", normalized));
        all.addAll(crawlPlatformForSingleTopic("FutureLearn", "https://www.futurelearn.com/search?q=", normalized));
        all.addAll(crawlPlatformForSingleTopic("Class Central", "https://www.classcentral.com/search?q=", normalized));
        all.addAll(crawlPlatformForSingleTopic("Saylor Academy", "https://learn.saylor.org/course/search.php?search=", normalized));

        return all;
    }

    public List<Course> crawlKhanAcademy() {
        List<Course> results = new ArrayList<>();

        // Real, valid Khan Academy pages. Do not try to invent Java/AWS/Salesforce pages on Khan.
        addKhanCourse(results, "python",
                "Intro to computer science - Python",
                "https://www.khanacademy.org/computing/intro-to-python-fundamentals");

        addKhanCourse(results, "python",
                "Getting Started with Python",
                "https://www.khanacademy.org/computing/in-class-11-computer-science/xab724c3e878917b8%3Agetting-started-with-python");

        addKhanCourse(results, "web development",
                "Computer programming - JavaScript and the web",
                "https://www.khanacademy.org/computing/computer-programming");

        addKhanCourse(results, "web development",
                "Intro to JS: Drawing & Animation",
                "https://www.khanacademy.org/computing/computer-programming/programming");

        addKhanCourse(results, "data science",
                "Data analysis & Big data | AP CSP",
                "https://www.khanacademy.org/computing/ap-computer-science-principles/data-analysis-101");

        addKhanCourse(results, "machine learning",
                "Machine learning algorithms",
                "https://www.khanacademy.org/computing/ap-computer-science-principles/data-analysis-101/x2d2f703b37b450a3%3Amachine-learning-and-bias/a/machine-learning-algorithms");

        addKhanCourse(results, "machine learning",
                "Bias in machine learning",
                "https://www.khanacademy.org/computing/ap-computer-science-principles/data-analysis-101/x2d2f703b37b450a3%3Amachine-learning-and-bias/e/machine-learning-and-bias");

        addKhanCourse(results, "ai",
                "AI for education",
                "https://www.khanacademy.org/college-careers-more/ai-for-education");

        addKhanCourse(results, "ai",
                "Getting started with generative AI",
                "https://www.khanacademy.org/college-careers-more/ai-for-education/x68ea37461197a514%3Aai-for-education-unit-1");

        addKhanCourse(results, "ai",
                "How AI works | Code.org | Computing",
                "https://www.khanacademy.org/computing/code-org/x06130d92%3Ahow-ai-works");

        addKhanCourse(results, "data science",
                "Intro to SQL: Querying and managing data",
                "https://www.khanacademy.org/computing/computer-programming/sql");

        addKhanCourse(results, "web development",
                "Hour of Code on Khan Academy",
                "https://www.khanacademy.org/hourofcode");

        addKhanCourse(results, "web development",
                "Programming | AP CSP",
                "https://www.khanacademy.org/computing/ap-computer-science-principles/programming-101");

        return results;
    }

    public List<Course> crawlKhanAcademyForKeyword(String keyword) {
        List<Course> seed = crawlKhanAcademy();
        List<Course> filtered = new ArrayList<>();

        for (Course c : seed) {
            String text = (c.getTitle() + " " + c.getDescription() + " " + c.getUrl()).toLowerCase();
            if (matchesTopic(text, keyword) || text.contains(keyword.toLowerCase())) {
                filtered.add(c);
            }
        }

        return filtered;
    }

    public List<Course> crawlEdx() {
        return crawlPlatform("edX", "https://www.edx.org/search?q=");
    }

    public List<Course> crawlFutureLearn() {
        return crawlPlatform("FutureLearn", "https://www.futurelearn.com/search?q=");
    }

    public List<Course> crawlClassCentral() {
        return crawlPlatform("Class Central", "https://www.classcentral.com/search?q=");
    }

    public List<Course> crawlSaylor() {
        return crawlPlatform("Saylor Academy", "https://learn.saylor.org/course/search.php?search=");
    }

    private void addKhanCourse(List<Course> results, String topic, String title, String url) {
        results.add(buildCourse("Khan Academy", topic, title, url));
    }

    private List<Course> crawlPlatform(String platform, String searchBaseUrl) {
        List<Course> results = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (String topic : REQUIRED_TOPICS) {
            if (results.size() >= MAX_PER_PLATFORM) {
                break;
            }

            List<Course> topicResults = crawlTopic(platform, searchBaseUrl, topic, seenUrls);

            for (Course c : topicResults) {
                if (results.size() >= MAX_PER_PLATFORM) {
                    break;
                }
                results.add(c);
            }
        }

        return results;
    }

    private List<Course> crawlPlatformForSingleTopic(String platform, String searchBaseUrl, String topic) {
        return crawlTopic(platform, searchBaseUrl, topic, new HashSet<>());
    }

    private List<Course> crawlTopic(String platform, String searchBaseUrl, String topic, Set<String> seenUrls) {
        List<Course> results = new ArrayList<>();

        try {
            String encodedTopic = URLEncoder.encode(topic, StandardCharsets.UTF_8);
            String url = searchBaseUrl + encodedTopic;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout((int) Duration.ofSeconds(15).toMillis())
                    .followRedirects(true)
                    .get();

            Elements links = doc.select("a[href]");

            for (Element link : links) {
                if (results.size() >= 6) {
                    break;
                }

                String href = normalizeUrl(link.absUrl("href"), link.attr("href"));
                String title = cleanText(link.text());

                if (href.isBlank() || title.isBlank()) continue;
                if (!isAllowedPlatformUrl(platform, href)) continue;
                if (!looksLikeCourseLink(platform, href, title)) continue;

                String textBlob = (title + " " + href).toLowerCase();
                if (!matchesTopic(textBlob, topic)) continue;

                if (!seenUrls.add(href)) continue;

                results.add(buildCourse(platform, topic, title, href));
            }

        } catch (Exception ignored) {
            // no fake fallback data
        }

        return results;
    }

    private Course buildCourse(String platform, String topic, String title, String url) {
        String id = platform.substring(0, Math.min(platform.length(), 2)).toLowerCase() + ID_COUNTER.getAndIncrement();

        String category = topic;
        String certificate = guessCertificate(platform);
        String price = guessPrice(platform);
        String format = "Online";
        String details = "Crawled using topic-based search";
        String rating = "N/A";
        String learners = "N/A";

        String description = "Course related to " + topic + " found from " + platform + ".";

        return new Course(
                id,
                platform,
                title,
                category,
                description,
                certificate,
                price,
                format,
                details,
                rating,
                learners,
                url
        );
    }

    private String guessCertificate(String platform) {
        return switch (platform) {
            case "Khan Academy" -> "No certificate";
            case "Saylor Academy" -> "Free Certificate";
            default -> "Certificate";
        };
    }

    private String guessPrice(String platform) {
        return switch (platform) {
            case "Khan Academy" -> "Free";
            case "Saylor Academy" -> "Free";
            case "Class Central" -> "Free / Mixed";
            default -> "Free / Paid";
        };
    }

    private String normalizeUrl(String absUrl, String rawHref) {
        String url = absUrl != null && !absUrl.isBlank() ? absUrl.trim() : rawHref.trim();

        if (url.startsWith("//")) {
            url = "https:" + url;
        }

        if (!url.startsWith("http")) {
            return "";
        }

        int hash = url.indexOf('#');
        if (hash >= 0) {
            url = url.substring(0, hash);
        }

        return url;
    }

    private boolean isAllowedPlatformUrl(String platform, String url) {
        return switch (platform) {
            case "edX" -> url.contains("edx.org");
            case "FutureLearn" -> url.contains("futurelearn.com");
            case "Class Central" -> url.contains("classcentral.com");
            case "Saylor Academy" -> url.contains("learn.saylor.org") || url.contains("saylor.org");
            case "Khan Academy" -> url.contains("khanacademy.org");
            default -> false;
        };
    }

    private boolean looksLikeCourseLink(String platform, String url, String title) {
        String u = url.toLowerCase();
        String t = title.toLowerCase();

        if (t.length() < 4) return false;
        if (u.contains("/login") || u.contains("/signup") || u.contains("/register")) return false;
        if (u.contains("/donate") || u.contains("/about") || u.contains("/privacy") || u.contains("/terms")) return false;
        if (u.contains("/search")) return false;

        return switch (platform) {
            case "edX" -> u.contains("/learn/") || u.contains("/course/");
            case "FutureLearn" -> u.contains("/courses/");
            case "Class Central" -> u.contains("/course/");
            case "Saylor Academy" -> u.contains("course/view.php") || u.contains("course/search.php");
            case "Khan Academy" -> u.contains("/computing/") || u.contains("/college-careers-more/");
            default -> false;
        };
    }

    private boolean matchesTopic(String text, String topic) {
        String t = text.toLowerCase();
        String q = topic.toLowerCase();

        if (t.contains(q)) {
            return true;
        }

        return switch (q) {
            case "ai" -> t.contains("artificial intelligence") || t.matches(".*\\bai\\b.*");
            case "c++" -> t.contains("c++") || t.contains("cpp");
            case "web development" ->
                    t.contains("web development") ||
                    t.contains("web developer") ||
                    t.contains("frontend") ||
                    t.contains("backend") ||
                    t.contains("full stack") ||
                    t.contains("html") ||
                    t.contains("css") ||
                    t.contains("javascript") ||
                    t.contains("web");
            case "business analyst" ->
                    t.contains("business analyst") ||
                    t.contains("business analysis") ||
                    t.contains("business analytics");
            case "machine learning" ->
                    t.contains("machine learning") ||
                    t.contains("deep learning");
            case "data science" ->
                    t.contains("data science") ||
                    t.contains("data analysis") ||
                    t.contains("data analytics") ||
                    t.contains("sql") ||
                    t.contains("big data");
            case "aws" ->
                    t.contains("aws") ||
                    t.contains("amazon web services") ||
                    t.contains("cloud computing");
            case "java" ->
                    t.contains(" java ") ||
                    t.startsWith("java ") ||
                    t.endsWith(" java") ||
                    t.contains("object-oriented");
            default -> false;
        };
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    public List<String> getRequiredTopics() {
        return REQUIRED_TOPICS;
    }

    public int getRequiredTopicsCount() {
        return REQUIRED_TOPICS.size();
    }
}