package com.project.edu.model;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String id;
    private String platform;
    private String title;
    private String category;
    private String description;
    private String certification;
    private String pricingModel;
    private String format;
    private String specialFeatures;
    private String rating;
    private String reviewCount;
    private String url;
    private List<String> tokens = new ArrayList<>();

    public Course() {}

    public Course(String id, String platform, String title, String category, String description,
                  String certification, String pricingModel, String format, String specialFeatures,
                  String rating, String reviewCount, String url) {
        this.id = id;
        this.platform = platform;
        this.title = title;
        this.category = category;
        this.description = description;
        this.certification = certification;
        this.pricingModel = pricingModel;
        this.format = format;
        this.specialFeatures = specialFeatures;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.url = url;
    }

    public String combinedText() {
        return String.join(" ",
                safe(title), safe(category), safe(description), safe(certification),
                safe(pricingModel), safe(format), safe(specialFeatures), safe(platform));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCertification() { return certification; }
    public void setCertification(String certification) { this.certification = certification; }
    public String getPricingModel() { return pricingModel; }
    public void setPricingModel(String pricingModel) { this.pricingModel = pricingModel; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getSpecialFeatures() { return specialFeatures; }
    public void setSpecialFeatures(String specialFeatures) { this.specialFeatures = specialFeatures; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    public String getReviewCount() { return reviewCount; }
    public void setReviewCount(String reviewCount) { this.reviewCount = reviewCount; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getTokens() { return tokens; }
    public void setTokens(List<String> tokens) { this.tokens = tokens; }
}
