package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;

import java.util.List;

/**
 * @author RPSPERERA on 7/2/2018
 */
public interface TestSteps extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

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

    void setScriptId(String scriptId);

    String getScriptId();
}
