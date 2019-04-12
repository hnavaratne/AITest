package com.virtusa.gto.plugins.aitest.entity;

public enum QuestionStatus {

    OPEN("Open"),
    RESOLVED("Resolved");

    private String status;

    QuestionStatus(String status) {
        this.status = status;
    }

    public String value() {
        return this.status;
    }
}
