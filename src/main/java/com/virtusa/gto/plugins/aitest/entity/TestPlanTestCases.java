package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface TestPlanTestCases extends Entity {

    String getTestPlanId();

    void setTestPlanId(String testPlanId);

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getAgent();

    void setAgent(String agent);

    boolean isPoolAgent();

    void setPoolAgent(boolean poolAgent);

    String getUser();

    void setUser(String user);
}
