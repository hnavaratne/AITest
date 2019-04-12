package com.virtusa.gto.plugins.aitest.entity;

public enum EEntityStatus {
    IN_PROGRESS("In Progress"),
    NOT_STARTED("Not Started"),
    COMPLETED("Completed");

    private String status;

    EEntityStatus(String status) {
        this.status = status;
    }

    public String value() {
        return this.status;
    }
}
