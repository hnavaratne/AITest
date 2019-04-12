package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;


public interface ExecEntityTPI extends Entity {

    int getExecEntityTP();

    void setExecEntityTP(int execEntityTP);

    int getEntityId();

    void setEntityId(int entityId);

    String getTestPlanId();

    void setTestPlanId(String testPlanId);

    String getTestPlanName();

    void setTestPlanName(String testPlanName);

    String getStatus();

    void setStatus(String status);

    String getProjectKey();

    void setProjectKey(String projectKey);
}
