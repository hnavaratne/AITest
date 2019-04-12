package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 7/31/2018
 */
public interface TCaseDataIteration extends Entity {

    TCaseDataInstance getTestCaseDataInstance();

    void setTestCaseDataInstance(TCaseDataInstance tCaseDataInstance);

    void setIterationData(String iterationData);

    String getIterationData();

    void setStatus(String status);

    String getStatus();

    String getActualResult();

    void setActualResult(String actualResult);


}
