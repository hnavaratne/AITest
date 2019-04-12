package com.virtusa.gto.plugins.aitest.entity;

/**
 * @author RPSPERERA on 8/3/2018
 */
public enum EEntityTPTCDIStatus {

    IN_PROGRESS("In Progress"),
    PASS("Pass"),
    FAILED("Failed");

    private String status;

    EEntityTPTCDIStatus(String status) {
        this.status = status;
    }

    public String value() {
        return this.status;
    }
}
