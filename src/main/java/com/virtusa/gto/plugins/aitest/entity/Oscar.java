package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;

public interface Oscar extends Entity {

    void setTestCaseId(String testCaseId);

    String getTestCaseId();

    @StringLength(StringLength.UNLIMITED)
    void setMindMap(String mindMap);

    String getMindMap();

}
