package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 7/20/2018
 */
public interface TestParamV extends Entity {

    String getValue();

    void setValue(String setValue);

    TestStepParams getTestParam();

    void setTestParam(TestStepParams testParamId);


}
