package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

/**
 * @author RPSPERERA on 7/31/2018
 */
public interface TCaseDataInstance extends Entity {

    int getExecEntityTPTCI();

    void setExecEntityTPTCI(int execEntityTPTCI);

    @OneToMany
    TCaseDataIteration[] getTestCaseDataIterations();

}
