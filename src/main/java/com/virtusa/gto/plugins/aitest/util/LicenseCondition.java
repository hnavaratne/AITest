package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;

import javax.inject.Inject;

@Scanned
public class LicenseCondition extends AbstractWebCondition {

    @ComponentImport
    private final PluginLicenseManager pluginLicenseManager;

    @Inject
    public LicenseCondition(PluginLicenseManager pluginLicenseManager) {
        this.pluginLicenseManager = pluginLicenseManager;
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper helper) {
        boolean isValid = false;
        if (pluginLicenseManager.getLicense().isDefined()) {
            PluginLicense license = pluginLicenseManager.getLicense().get();
            if (license.getError().isDefined()) {
                ComponentAccessor.getPluginAccessor().getPlugin(pluginLicenseManager.getLicense().get().getPluginKey()).disable();
            } else {
                ComponentAccessor.getPluginAccessor().getPlugin(pluginLicenseManager.getLicense().get().getPluginKey()).enable();
                isValid = true;
            }
        } else {
            ComponentAccessor.getPluginAccessor().getPlugin(pluginLicenseManager.getLicense().get().getPluginKey()).disable();
        }
        return isValid;
    }

}
