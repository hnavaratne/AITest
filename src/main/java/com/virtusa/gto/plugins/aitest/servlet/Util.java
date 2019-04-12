package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.TConfig;
import com.virtusa.gto.plugins.aitest.entity.Configuration;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Scanned
public class Util extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    private static final long serialVersionUID = -3142869249805201544L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    @JiraImport
    private ActiveObjects activeObjects;

    //Project Key
    BrowseContext context;

    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    @ComponentImport
    private final UserManager userManager;

    @Inject
    public Util(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects,
                UserManager userManager,
                LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        String action = req.getParameter("action");
        String payLoad = IOUtils.toString(req.getReader());
        String redirectUrl;
        UserProfile currentUser = userManager.getRemoteUser(req);
        if (currentUser == null) {
            resp.setStatus(HttpServletResponse.SC_FOUND);
            redirectUrl = "/secure/TestCaseDefectsView.jspa";
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
            handleAction(action, data, resp);
        }
    }

    private void handleAction(String action, String data, HttpServletResponse resp) throws IOException {
        switch (action) {
            case "get-versions":
                resp.getWriter().write(getVersions());
                break;
            case "get-non-released-versions":
                resp.getWriter().write(getNonReleasedVersions());
                break;
            case "get-users":
                resp.getWriter().write(getUsers());
                break;
            case "set-configuration":
                resp.getWriter().write(setConfiguration(data));
                break;
            case "get-configurations":
                resp.getWriter().write(getConfigurations());
                break;
            case "test-configuration":
                resp.getWriter().write(testConfiguration(data));
                break;
            default:
                break;

        }
    }

    private String getVersions() {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        Collection<Version> versions;
        JSONArray returnArray = new JSONArray();
        Project projectObj = ComponentAccessor.getProjectManager().getProjectObj(Long.valueOf(selectedProjectKey));
        if (Objects.nonNull(projectObj)) {
            versions = Objects.requireNonNull(projectObj).getVersions();
            for (Version version : versions) {
                JSONObject returnObject = new JSONObject();
                try {
                    returnObject.put("id", version.getId());
                    returnObject.put("name", version.getName());
                    returnObject.put("description", version.getDescription());
                    returnObject.put("selected", false);
                    if (Objects.nonNull(version.getStartDate())) {
                        returnObject.put("startDate", version.getStartDate().toString());
                    } else {
                        returnObject.put("startDate", "");
                    }
                    if (Objects.nonNull(version.getReleaseDate())) {
                        returnObject.put("releaseDate", version.getReleaseDate().toString());
                    } else {
                        returnObject.put("releaseDate", "");
                    }
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
                returnArray.put(returnObject);
            }
        }
        return returnArray.toString();
    }

    private String getNonReleasedVersions() {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        Collection<Version> versions;
        JSONArray returnArray = new JSONArray();
        Project projectObj = ComponentAccessor.getProjectManager().getProjectObj(Long.valueOf(selectedProjectKey));
        if (Objects.nonNull(projectObj)) {
            versions = Objects.requireNonNull(projectObj.getVersions());
            for (Version version : versions) {
                JSONObject returnObject = new JSONObject();
                boolean released = version.isReleased();
                if (!released) {
                    try {
                        returnObject.put("id", version.getId());
                        returnObject.put("name", version.getName());
                        returnObject.put("description", version.getDescription());
                        if (Objects.nonNull(version.getStartDate())) {
                            returnObject.put("startDate", version.getStartDate().toString());
                        } else {
                            returnObject.put("startDate", "");
                        }
                        if (Objects.nonNull(version.getReleaseDate())) {
                            returnObject.put("releaseDate", version.getReleaseDate().toString());
                        } else {
                            returnObject.put("releaseDate", "");
                        }
                    } catch (JSONException e) {
                        log.error(e.getMessage(), e);
                    }
                    returnArray.put(returnObject);
                }
            }
        }
        return returnArray.toString();
    }

    private String getUsers() {
        JSONArray usersArray = new JSONArray();

        UserSearchService userSearchService = ComponentAccessor.getComponent(UserSearchService.class);
        UserSearchParams userSearchParams = (new UserSearchParams.Builder()).allowEmptyQuery(true).includeActive(true).includeInactive(true).maxResults(100000).build();

        List<ApplicationUser> applicationUsers = userSearchService.findUsers("", userSearchParams);
        for (ApplicationUser applicationUser : applicationUsers) {
            JSONObject userObject = new JSONObject();
            try {
                if (applicationUser.isActive()) {
                    userObject.put("id", applicationUser.getId());
                    userObject.put("username", applicationUser.getUsername());
                    userObject.put("name", applicationUser.getName());
                    userObject.put("displayName", applicationUser.getDisplayName());
                    userObject.put("emailAddress", applicationUser.getEmailAddress());
                }
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            usersArray.put(userObject);
        }
        return usersArray.toString();
    }


    private String setConfiguration(String data) {
        JSONObject returnObject = new JSONObject();
        String testConnection = new Util(authenticationContext, activeObjects, userManager, loginUriProvider).testConfiguration(data);
        JsonElement testConnectionElement = new JsonParser().parse(testConnection);
        JsonObject testConnectionObj = testConnectionElement.getAsJsonObject();
        try {
            if (testConnectionObj.get("testConnection").getAsBoolean()) {
                JsonElement element = new JsonParser().parse(data);
                JsonObject jsonData = element.getAsJsonObject();
                TConfig config = new TConfig(activeObjects);
                String host = jsonData.get("host").getAsString();
                String port = jsonData.get("port").getAsString();
                String baseUrl = jsonData.get("baseUrl").getAsString();
                String pythonHome = jsonData.get("pythonHome").getAsString();
                Configuration configuration = config.upsert(host, port, baseUrl, pythonHome);

                returnObject.put("host", configuration.getHost());
                returnObject.put("port", configuration.getPort());
                returnObject.put("baseurl", configuration.getBaseUrl());
                returnObject.put("pythonHome", configuration.getPythonHome());
                returnObject.put("testConnection", testConnectionObj.get("testConnection").getAsBoolean());
            } else {
                returnObject.put("host", testConnectionObj.get("host").getAsString());
                returnObject.put("port", testConnectionObj.get("port").getAsString());
                returnObject.put("baseurl", testConnectionObj.get("baseurl").getAsString());
                returnObject.put("pythonHome", testConnectionObj.get("pythonHome").getAsString());
                returnObject.put("testConnection", testConnectionObj.get("testConnection").getAsBoolean());
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }

    private String getConfigurations() {

        TConfig config = new TConfig(activeObjects);
        Configuration configuration = config.get();
        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("host", Objects.nonNull(configuration) ? configuration.getHost() : "");
            returnObject.put("port", Objects.nonNull(configuration) ? configuration.getPort() : "");
            returnObject.put("baseurl", Objects.nonNull(configuration) ? configuration.getBaseUrl() : "");
            returnObject.put("pythonHome", Objects.nonNull(configuration) ? configuration.getPythonHome() : "");
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }

    private String testConfiguration(String strData) {
        JSONObject returnObject = new JSONObject();
        JsonElement element = new JsonParser().parse(strData);
        JsonObject jsonData = element.getAsJsonObject();
        String host = jsonData.get("host").getAsString();
        String port = jsonData.get("port").getAsString();
        String baseUrl = jsonData.get("baseUrl").getAsString();
        String pythonHome = jsonData.get("pythonHome").getAsString();
        boolean connectionStatus = false;
        try {
            //boolean agentConnectivity = checkAgentConnectivity(host, port, baseUrl);
            //boolean poolAgentConnectivity = checkPoolAgentConnectivity(host, port, baseUrl);
//            if (agentConnectivity || poolAgentConnectivity) {
//                connectionStatus = true;
//            }
            connectionStatus = true;
            returnObject.put("host", host);
            returnObject.put("port", port);
            returnObject.put("baseurl", baseUrl);
            returnObject.put("pythonHome", pythonHome);
            returnObject.put("testConnection", connectionStatus);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }

    private boolean checkAgentConnectivity(String host, String port, String baseUrl) {
        boolean connectionStatus = false;
        try {
            URL agentConnectionUrl = new URL("http://" + host + ":" + port + baseUrl + "/browseagent");
            HttpURLConnection agentConnection = (HttpURLConnection) agentConnectionUrl.openConnection();
            agentConnection.setDoOutput(true);
            agentConnection.setRequestMethod("GET");
            agentConnection.setRequestProperty("Content-Type", "application/json");

            if (agentConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                connectionStatus = true;
            } else {
                throw new RuntimeException("Failed : HTTP error code : " + agentConnection.getResponseCode());
            }
            agentConnection.disconnect();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return connectionStatus;
    }

    private boolean checkPoolAgentConnectivity(String host, String port, String baseUrl) {
        boolean connectionStatus = false;
        try {
            URL poolConnectionUrl = new URL("http://" + host + ":" + port + baseUrl + "/browseagentpool");
            HttpURLConnection poolConnection = (HttpURLConnection) poolConnectionUrl.openConnection();
            poolConnection.setDoOutput(true);
            poolConnection.setRequestMethod("GET");
            poolConnection.setRequestProperty("Content-Type", "application/json");

            if (poolConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                connectionStatus = true;
            } else {
                throw new RuntimeException("Failed : HTTP error code : " + poolConnection.getResponseCode());
            }
            poolConnection.disconnect();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return connectionStatus;
    }
}
