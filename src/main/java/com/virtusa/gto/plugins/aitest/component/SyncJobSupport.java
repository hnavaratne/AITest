package com.virtusa.gto.plugins.aitest.component;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.JiraComponent;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;

import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService(SyncJobSupport.class)
@Named("SyncJobSupport")
@JiraComponent
@Scanned
public class SyncJobSupport implements SyncJobInterface {

    @JiraImport
    ActiveObjects activeObjects;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    @Inject
    public SyncJobSupport(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public ActiveObjects getActiveObject() {
        return this.activeObjects;
    }

    @Override
    public JiraAuthenticationContext getJiraAuthenticationContext() {
        return this.authenticationContext;
    }
}
