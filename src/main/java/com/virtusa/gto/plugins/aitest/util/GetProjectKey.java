package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class GetProjectKey extends JiraWebActionSupport {

    private BrowseContext context;

    public GetProjectKey(BrowseContext context) {
        this.context = context;
    }

    public String getSelectedProjectKey() {
        return String.valueOf(getSelectedProjectObject().getId());
    }

    public BrowseContext getContext() {
        return context;
    }

    public void setContext(BrowseContext context) {
        this.context = context;
    }
}
