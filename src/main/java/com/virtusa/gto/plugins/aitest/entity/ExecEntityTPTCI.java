package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface ExecEntityTPTCI extends Entity {

    int getEntityId();

    void setEntityId(int entityId);

    int getExecEntityTPTC();

    void setExecEntityTPTC(int execEntityTPTC);

    int getExecutionEntityTestPlanId();

    void setExecutionEntityTestPlanId(int executionEntityTestPlanId);

    String getTestCaseId();

    boolean isPoolAgent();

    void setPoolAgent(boolean poolAgent);

    void setTestCaseId(String testCaseId);

    String getTestCaseName();

    void setTestCaseName(String testCaseName);

    String getTestCaseAutomated();

    void setTestCaseAutomated(String testCaseAutomated);

    String getTestCaseManual();

    void setTestCaseManual(String testCaseManual);

    String getTestCaseAgent();

    void setTestCaseAgent(String testCaseAgent);

    String getTestCaseUser();

    void setTestCaseUser(String testCaseUser);

    String getActualResult();

    void setActualResult(String actualResult);

    String getOverallStatus();

    void setOverallStatus(String overallStatus);

    boolean isExecuted();

    void setExecuted(boolean executed);

    String getOverallExpectedResult();

    void setOverallExpectedResult(String overallExpectedResult);

    //edited
    int getCycle();

    void setCycle(int cycle);

    String getTestCaseBrowser();

    void setTestCaseBrowser(String testCaseBrowser);

    boolean hasIterationData();

    void setIterationData(boolean iterationData);

    String getUserStoryId();

    void setUserStoryId(String userStoryId);

    String getUserStorySummary();

    void setUserStorySummary(String userStorySummary);

    String getDefectId();

    void setDefectId(String defectId);

    String getDefectName();

    void setDefectName(String defectName);
}
