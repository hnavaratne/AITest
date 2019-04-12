package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface Question extends Entity {

    void setTitle(String title);

    String getTitle();

    void setDescription(String description);

    String getDescription();

    void setIssue(Long id);

    Long getIssue();

    void setAssignTo(Long user);

    Long getAssignTo();

    void setStatus(String status);

    String getStatus();
}
