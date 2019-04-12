package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface OscarPVE extends Entity {

    void setValue(String value);

    String getValue();

    void setTestCaseId(String testCaseId);

    String getTestCaseId();

    OscarVE getOscarVe();

    void setOscarVE(OscarVE oscarVE);

}
