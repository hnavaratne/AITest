package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

/**
 * @author RPSPERERA on 7/20/2018
 */
public interface TestStepParams extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getParamName();

    void setParamName(String paramName);

    String getData();

    void setData(String data);


    @OneToMany
    TestParamV[] getTestStepParamValues();

}
