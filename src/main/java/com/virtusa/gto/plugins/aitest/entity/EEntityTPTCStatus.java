package com.virtusa.gto.plugins.aitest.entity;

public enum EEntityTPTCStatus {

    IN_PROGRESS("In Progress"),
    PASS("Pass"),
    FAILED("Failed");

    private String status;

    EEntityTPTCStatus(String status) {
        this.status = status;
    }

    public String value() {
        return this.status;
    }
}
