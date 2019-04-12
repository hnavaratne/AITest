package com.virtusa.gto.plugins.aitest.webwork.testmanagement;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Scanned
public class TestCaseDesigning extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(TestCaseDesigning.class);

    @JiraImport
    private TemplateRenderer templateRenderer;

    @Inject
    public TestCaseDesigning(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String execute() throws Exception {
        return "test-case-designing-view";
    }
}
