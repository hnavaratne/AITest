package com.virtusa.gto.plugins.aitest.entity;

public enum TestStepStatus {

    IN_PROGRESS("In Progress"),
    PASS("Pass"),
    FAILED("Failed");

    private String status;

    TestStepStatus(String status) {
        this.status = status;
    }

    public String value() {
        return this.status;
    }
}
