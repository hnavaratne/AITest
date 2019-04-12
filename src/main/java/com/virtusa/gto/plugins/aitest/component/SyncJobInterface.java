package com.virtusa.gto.plugins.aitest.component;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.JiraAuthenticationContext;

public interface SyncJobInterface {

    ActiveObjects getActiveObject();

    JiraAuthenticationContext getJiraAuthenticationContext();
}
