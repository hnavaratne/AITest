package com.virtusa.gto.plugins.aitest.webwork.testmanagement;

import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class AgentRegistry extends JiraWebActionSupport {
    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    @JiraImport
    private TemplateRenderer templateRenderer;

    public AgentRegistry(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String execute() throws Exception {

        return "agent-registry-view"; //returns SUCCESS
    }
}
