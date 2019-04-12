package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 7/2/2018
 */
public interface TestCaseLink extends Entity {

    String getIssueID();

    void setIssueID(String id);

    String getName();

    void setName(String name);

    String getType();

    void setType(String type);
}
