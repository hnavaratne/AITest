package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface ExecEntityTPTC extends Entity {

    int getExecutionEntityTestPlanId();

    void setExecutionEntityTestPlanId(int executionEntityTestPlanId);

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getTestCaseName();

    boolean isPoolAgent();

    void setPoolAgent(boolean poolAgent);

    void setTestCaseName(String testCaseName);

    String getTestCaseAutomated();

    void setTestCaseAutomated(String testCaseAutomated);

    String getTestCaseManual();

    void setTestCaseManual(String testCaseManual);

    String getTestCaseAgent();

    void setTestCaseAgent(String testCaseAgent);

    String getTestCaseUser();

    void setTestCaseUser(String testCaseUser);

    String getOverallExpectedResult();

    void setOverallExpectedResult(String overallExpectedResult);

    String getTestCaseBrowser();

    void setTestCaseBrowser(String testCaseBrowser);

}
