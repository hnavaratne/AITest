package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public class AgentRegistryManagement extends HttpServlet {
    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    @JiraImport
    private ActiveObjects activeObjects;

    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    //Project Key
    BrowseContext context;

    @Inject
    public AgentRegistryManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager, LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String redirectUrl = "";
        String payLoad = IOUtils.toString(req.getReader());
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        String action = req.getParameter("action");

        UserProfile currentUser = userManager.getRemoteUser(req);
        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_FOUND);
            redirectUrl = "/secure/AgentRegistryView.jspa";
            resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
            return;
        }

        if (Objects.nonNull(payLoad) && payLoad.contains("user") && payLoad.contains("action") && payLoad.contains("data")) {
            JsonObject parse = new JsonParser().parse(payLoad).getAsJsonObject();
            user = parse.get("user").getAsString();
            data = parse.get("data").getAsString();
            action = parse.get("action").getAsString();
        }
        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            switch (action) {
                case "check-login":
                    resp.getWriter().write("work");
                    break;
                default:
                    break;
            }
        }
    }
}
