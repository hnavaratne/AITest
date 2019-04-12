package com.virtusa.gto.plugins.aitest.custompanel;

import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel3;
import com.atlassian.jira.plugin.issuetabpanel.GetActionsRequest;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugin.issuetabpanel.ShowPanelRequest;
import com.virtusa.gto.plugins.aitest.issueaction.QuestionAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class IssueQuestionsPanel extends AbstractIssueTabPanel3 {

    @Override
    public boolean showPanel(ShowPanelRequest request) {
        return Objects.requireNonNull(request.issue().getIssueType()).getName().toLowerCase(Locale.ENGLISH).equalsIgnoreCase("story");
    }

    public List<IssueAction> getActions(GetActionsRequest request) {
        List<IssueAction> issueActions = new ArrayList<>();
        issueActions.add(new QuestionAction(descriptor, request.issue(), request.remoteUser()));
        return issueActions;
    }


}
