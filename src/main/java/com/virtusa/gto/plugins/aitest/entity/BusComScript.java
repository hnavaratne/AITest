package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

/**
 * @author RPSPERERA on 9/20/2018
 */
public interface BusComScript extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

    String getParent();

    void setParent(String parent);

    String getName();

    void setName(String name);

    @OneToMany
    BusComSteps[] getBusComSteps();


//    void setBusComSteps(BusComSteps[] busComSteps);
}
