package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface DefectsEntityI extends Entity {

    int getExecutionEntityId();

    void setExecutionEntityId(int executionEntityInstanceId);

    int getExecutionEntityTestPlanId();

    void setExecutionEntityTestPlanId(int executionEntityTestPlanInstanceId);

    int getExecEntityTPTC();

    void setExecEntityTPTC(int execEntityTPTCI);

    String getDefectId();

    void setDefectId(String defectId);

    String getDefectKey();

    void setDefectKey(String defectKey);

    String getDefectName();

    void setDefectName(String defectName);

    String getCycle();

    void setCycle(String cycle);

    String getTestCaseName();

    void setTestCaseName(String testCaseName);

    String getTPTCIMyTaskId();

    void setTPTCIMyTaskId(String tptciMyTaskId);

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

}
