package com.project.edu.model;

import java.util.List;

public class ValidationResponse {
    private boolean validUrl;
    private boolean validPrice;
    private List<String> pricesFound;

    public ValidationResponse(boolean validUrl, boolean validPrice, List<String> pricesFound) {
        this.validUrl = validUrl;
        this.validPrice = validPrice;
        this.pricesFound = pricesFound;
    }

    public boolean isValidUrl() { return validUrl; }
    public void setValidUrl(boolean validUrl) { this.validUrl = validUrl; }
    public boolean isValidPrice() { return validPrice; }
    public void setValidPrice(boolean validPrice) { this.validPrice = validPrice; }
    public List<String> getPricesFound() { return pricesFound; }
    public void setPricesFound(List<String> pricesFound) { this.pricesFound = pricesFound; }
}
