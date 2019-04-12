package com.virtusa.gto.plugins.aitest.entity;

public enum EEntityTPStatus {
    IN_PROGRESS("In Progress"),
    NOT_STARTED("Not Started"),
    COMPLETED("Completed");

    private String status;

    EEntityTPStatus(String status) {
        this.status = status;
    }

    public String value() {
        return this.status;
    }
}
