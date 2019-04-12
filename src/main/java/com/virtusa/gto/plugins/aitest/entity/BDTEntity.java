package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface BDTEntity extends Entity {

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getDescription();

    void setDescription(String description);
}
