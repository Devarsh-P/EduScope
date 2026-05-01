package com.project.edu.model;

public class RecommendationRequest {
    private String interest;
    private String budget;
    private boolean certificationRequired;

    public String getInterest() { return interest; }
    public void setInterest(String interest) { this.interest = interest; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public boolean isCertificationRequired() { return certificationRequired; }
    public void setCertificationRequired(boolean certificationRequired) { this.certificationRequired = certificationRequired; }
}
