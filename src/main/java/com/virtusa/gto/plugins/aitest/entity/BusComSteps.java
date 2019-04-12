package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 9/20/2018
 */
public interface BusComSteps extends Entity {

    void setBusComScript(BusComScript busComScript);

    BusComScript getBusComScript();

    String getExpectedValue();

    void setExpectedValue(String expectedValue);

    String getStepDescription();

    void setStepDescription(String stepDescription);

    String getDataParams();

    void setDataParams(String dataParams);

}
