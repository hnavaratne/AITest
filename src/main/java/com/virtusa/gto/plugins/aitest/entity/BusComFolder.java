package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 9/20/2018
 */
public interface BusComFolder extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

    String getParent();

    void setParent(String parent);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);
}
