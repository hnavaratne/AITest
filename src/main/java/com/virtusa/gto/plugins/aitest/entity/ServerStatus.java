package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 9/5/2018
 */
public interface ServerStatus extends Entity {

    boolean getStatus();

    void setStatus(boolean status);

    String getProcessList();

    void setProcessList(String processList);
}
