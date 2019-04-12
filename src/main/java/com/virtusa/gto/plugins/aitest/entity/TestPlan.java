package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface TestPlan extends Entity {

    String getName();

    void setName(String name);

    String getProjectKey();

    void setProjectKey(String projectKey);
}
