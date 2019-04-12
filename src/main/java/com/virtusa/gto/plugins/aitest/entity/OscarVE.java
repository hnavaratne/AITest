package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

public interface OscarVE extends Entity {

    void setTestCaseId(String testCaseId);

    String getTestCaseId();

    void setParam(String param);

    String getParam();

    void setParamV(String paramV);

    String getParamV();

    void setParamVE(String paramVE);

    String getParamVE();

    @OneToMany
    OscarPVE[] getOscarValueExpansion();

}
