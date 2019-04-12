package com.virtusa.gto.plugins.aitest.webwork.testmanagement;

import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class Schedule extends JiraWebActionSupport
{
    private static final Logger log = LoggerFactory.getLogger(Schedule.class);

    @JiraImport
    private TemplateRenderer templateRenderer;

    public Schedule(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String execute() throws Exception {

        return "schedule-view"; //returns SUCCESS
    }
}
