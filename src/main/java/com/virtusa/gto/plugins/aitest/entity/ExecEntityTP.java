package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface ExecEntityTP extends Entity {

    int getEntityId();

    void setEntityId(int entityId);

    String getTestPlanId();

    void setTestPlanId(String testPlanId);

    String getTestPlanName();

    void setTestPlanName(String testPlanName);

    String getProjectKey();

    void setProjectKey(String projectKey);

}
