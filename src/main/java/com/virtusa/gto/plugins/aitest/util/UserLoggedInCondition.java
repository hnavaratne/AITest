package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class UserLoggedInCondition extends AbstractWebCondition {

    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    private static final Logger log = LoggerFactory.getLogger(UserLoggedInCondition.class);


    public UserLoggedInCondition(LoginUriProvider loginUriProvider) {
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        return user != null;
    }

    // Login redirect when user is null
    public String redirectToLogin(String url) {
        JSONObject responseObject = new JSONObject();
        try {
            responseObject.put("redirect", loginUriProvider.getLoginUri(getUri(url)).toASCIIString());
        } catch (JSONException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return responseObject.toString();
    }

    private URI getUri(String url) {
        return URI.create(url);
    }
}
