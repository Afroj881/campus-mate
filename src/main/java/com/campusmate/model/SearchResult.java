package com.campusmate.model;

public class SearchResult {

    private final String type;
    private final String title;
    private final String description;
    private final String relevantInformation;
    private final String viewUrl;
    private final String openUrl;

    public SearchResult(String type, String title, String description, String relevantInformation,
                        String viewUrl, String openUrl) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.relevantInformation = relevantInformation;
        this.viewUrl = viewUrl;
        this.openUrl = openUrl;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRelevantInformation() {
        return relevantInformation;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public String getOpenUrl() {
        return openUrl;
    }
}