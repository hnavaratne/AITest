package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;


public class LinkIssuePanelCondition extends AbstractIssueCondition {
    private final static String ISSUE_TYPE_NAME = "Story";

    @Override
    public boolean shouldDisplay(ApplicationUser applicationUser, Issue issue, JiraHelper jiraHelper) {
        return issueIsType(issue, ISSUE_TYPE_NAME);
    }

    private boolean issueIsType(Issue issue, String issueTypeName) {
        if (issue.getIssueTypeObject().getName().equals(issueTypeName)) {
            return true;
        } else {
            return false;
        }
    }

}
