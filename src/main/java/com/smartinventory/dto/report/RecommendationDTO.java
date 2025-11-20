package com.smartinventory.dto.report;

/**
 * DTO for smart recommendations
 */
public class RecommendationDTO {
    private String type;        // "success", "warning", "info", "error"
    private String icon;        // emoji icon
    private String title;       // recommendation title
    private String message;     // recommendation message
    private String action;      // action button text

    public RecommendationDTO() {
    }

    public RecommendationDTO(String type, String icon, String title, String message, String action) {
        this.type = type;
        this.icon = icon;
        this.title = title;
        this.message = message;
        this.action = action;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
