package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface ExecEntity extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

    String getRelease();

    void setRelease(String release);

    String getDescription();

    void setDescription(String description);
}
