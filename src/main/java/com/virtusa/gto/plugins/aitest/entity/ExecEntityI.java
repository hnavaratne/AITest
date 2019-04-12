package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

import java.util.Date;

public interface ExecEntityI extends Entity {

    int getExecEntity();

    void setExecEntity(int execEntity);

    String getRelease();

    void setRelease(String release);

    String getDescription();

    void setDescription(String description);

    Date getExecutionDate();

    void setExecutionDate(Date executionDate);

    String getStatus();

    void setStatus(String status);

    String getProjectKey();

    void setProjectKey(String projectKey);

}
