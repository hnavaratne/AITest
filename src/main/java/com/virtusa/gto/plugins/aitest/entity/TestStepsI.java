package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;

public interface TestStepsI extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

    String getExecEntityTPTCI();

    void setExecEntityTPTCI(String execEntityTPTCI);

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getExpectedResult();

    @StringLength(StringLength.UNLIMITED)
    void setExpectedResult(String expectedResult);

    String getStep();

    @StringLength(StringLength.UNLIMITED)
    void setStep(String step);

    String getData();

    @StringLength(StringLength.UNLIMITED)
    void setData(String data);

    String getActualResult();

    void  setActualResult(String actualResult);

    String getActualStatus();

    void  setActualStatus(String actualStatus);

    String getDataIterationId();

    void setDataIterationId(String dataIterationId);

}
