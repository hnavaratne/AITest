package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 10/5/2018
 */
public interface Scripts extends Entity {

    String getTestCaseId();

    void setTestCaseId(String testCaseId);

    String getScript();

    void setScript(String script);

    boolean isAttached();

    void setAttached(boolean attached);

    String getProject();

    void setProject(String project);

}
