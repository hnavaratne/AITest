package com.virtusa.gto.plugins.aitest.issueaction;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.template.TemplateSources;
import com.atlassian.jira.template.VelocityTemplatingEngine;
import com.atlassian.jira.user.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class QuestionAction extends AbstractIssueAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionAction.class);
    private static final String PLUGIN_TEMPLATES = "templates/custom-panel/";

    private final Issue issue;
    private final ApplicationUser remoteUser;

    public QuestionAction(IssueTabPanelModuleDescriptor descriptor, Issue issue, ApplicationUser user) {
        super(descriptor);
        this.issue = issue;
        this.remoteUser = user;
    }

    public Date getTimePerformed() {
        return issue.getCreated();
    }

    protected void populateVelocityParams(Map params){
        params.put("issue", this.issue);
        params.put("user", this.remoteUser);
    }

    public String getHtml() {
        VelocityTemplatingEngine templatingEngine = ComponentAccessor.getComponent(VelocityTemplatingEngine.class);

        try {
            Map<String, Object> params = new HashMap<>();
            populateVelocityParams(params);
            return templatingEngine.render(TemplateSources.file(PLUGIN_TEMPLATES + "issue-questions.vm")).applying(params).asHtml();
        } catch (Exception var4) {
            LOGGER.error("Error while rendering velocity template for 'templates/custom-panel/issue-questions.vm'.", var4);
            return "Velocity template generation failed.";
        }
    }


}
