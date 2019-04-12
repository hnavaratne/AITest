package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

public interface UserStoryEntityI extends Entity {

    String getProjectKey();

    void setProjectKey(String projectKey);

    String getUserStoryId();

    void setUserStoryId(String userStoryId);

    String getUser();

    void setUser(String user);

    void setUserStorySummary(String summary);

    String getUserStorySummary();

    @OneToMany
    ExecEntityTPTCI[] getExecEntityTPTCIList();

}
