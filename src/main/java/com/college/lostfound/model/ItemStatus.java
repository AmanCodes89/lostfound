package com.college.lostfound.model;

public enum ItemStatus {
    OPEN("Open"),
    UNDER_REVIEW("Under Review"),
    VERIFIED("Verified"),
    RESOLVED("Returned / Resolved"),
    REJECTED("Rejected");

    private final String label;

    ItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
