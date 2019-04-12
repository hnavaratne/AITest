package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface TCIMyTask extends Entity {

    int getEntityId();

    void setEntityId(int entityId);

    int getExecutionEntityTestPlanId();

    void setExecutionEntityTestPlanId(int executionEntityTestPlanId);

    int getExecEntityTPTCI();

    void setExecEntityTPTCI(int execEntityTPTCI);

    String getProjectKey();

    void setProjectKey(String projectKey);

    TCaseDataIteration getTCaseDataIteration();

    void setTCaseDataIteration(TCaseDataIteration tCaseDataIteration);

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getTestCaseName();

    void setTestCaseName(String testCaseName);

    String getTestCaseManual();

    void setTestCaseManual(String testCaseManual);

    String getTestCaseUser();

    void setTestCaseUser(String testCaseUser);

    String getOverallExpectedResult();

    void setOverallExpectedResult(String overallExpectedResult);

    int getCycle();

    void setCycle(int cycle);

    String getTestCaseBrowser();

    void setTestCaseBrowser(String testCaseBrowser);

    String getOverallStatus();

    void setOverallStatus(String overallStatus);

    String getActualResult();

    void setActualResult(String actualResult);

    boolean isExecuted();

    void setExecuted(boolean executed);

}
