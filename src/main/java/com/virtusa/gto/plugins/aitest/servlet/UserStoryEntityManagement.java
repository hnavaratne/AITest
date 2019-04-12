package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.DefectsEntity;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanInstance;
import com.virtusa.gto.plugins.aitest.db.UserStoryEntityInstance;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
import com.virtusa.gto.plugins.aitest.entity.UserStoryEntityI;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class UserStoryEntityManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(UserStoryEntityManagement.class);
    private static final long serialVersionUID = 4384728454746859322L;

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
    public UserStoryEntityManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
                                     LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // try {
        //User login authentication
        String redirectUrl = "";
        // JSONObject jsonObject;

        String payLoad = IOUtils.toString(req.getReader());
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        String action = req.getParameter("action");

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

//            jsonObject = new JSONObject(data);
//            if (jsonObject.has("redirectView")) {
//                redirectUrl = jsonObject.get("redirectView").toString();
//            }
        if (!Objects.nonNull(currentUser)) {
            resp.setStatus(HttpServletResponse.SC_FOUND);
            if (redirectUrl.equals("/secure/TestCaseDefectsView.jspa")) {
                resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
            }
        } else {
            if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                handleAction(action, data, user, resp);
            }
        }
//        } catch (JSONException e) {
//            log.error(e.getMessage(), e);
//        }
    }

    private void handleAction(String action, String data, String user, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get-user-stories":
                    resp.getWriter().write(getAllUserStories(user));
                    break;
                case "get-test-cases":
                    resp.getWriter().write(getAllTestCases(data, user));
                    break;
                case "get-story-by-version":
                    resp.getWriter().write(getUserStoryByVersion(data, user));
                    break;
                case "get-plan-user-story":
                    resp.getWriter().write(String.valueOf(getPlanUserStoryList(data)));
                    break;
                case "get-versions":
                    resp.getWriter().write(getVersions());
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getAllUserStories(String user) {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        UserStoryEntityInstance userStoryEntityInstance = new UserStoryEntityInstance(activeObjects);
        List<UserStoryEntityI> userStories = userStoryEntityInstance.getUserStories(selectedProjectKey, user);
        JsonArray userStoriesJsonArray = new JsonArray();
        if (userStories.size() > 0) {
            userStories.forEach(story -> {
                if (story.getUserStorySummary() != null) {
                    JsonObject jStory = new JsonObject();
                    jStory.addProperty("storyid", story.getUserStoryId());
                    jStory.addProperty("summary", story.getUserStorySummary());
                    userStoriesJsonArray.add(jStory);
                }
            });
            return userStoriesJsonArray.toString();
        }
        return "";
    }

    public String getAllTestCases(String data, String user) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONArray jsonTestcaseArray = new JSONArray();
        String userStoryId = jsonData.get("userStoryId").getAsString();
        String baseUrl = jsonData.get("baseUrl").getAsString();
        UserStoryEntityInstance userStoryEntityInstance = new UserStoryEntityInstance(activeObjects);
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
//        List<ExecEntityTPTCI> testCases = userStoryEntityInstance.getTestCases(userStoryId);
//        ArrayList<String> duplicateList = new ArrayList<>();
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(user);
        Project project = new GetProjectKey(context).getSelectedProject();
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        builder.where().issueType("TestCase").and().project(project.getId());
        SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);
        DefectEntityManagement dem = new DefectEntityManagement(authenticationContext, activeObjects, userManager, loginUriProvider);
        try {
            SearchResults results = searchProvider.searchOverrideSecurity(applicationUser, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                Collection<Issue> issueLinks = ComponentAccessor.getIssueLinkManager().getLinkCollection(issue, applicationUser).getAllIssues();
                for (Issue issueF : issueLinks) {
                    if (Objects.equals(Objects.requireNonNull(issueF.getIssueType()).getName().toLowerCase(Locale.ENGLISH), "story") && issueF.getId().equals(Long.valueOf(userStoryId))) {
                        JSONObject testCaseJson = new JSONObject();
                        int defectCount = defectsEntity.getTestCaseRelatedDefectCount(String.valueOf(issue.getId()));
                        try {
                            testCaseJson.put("key",issue.getKey());
                            testCaseJson.put("status",issue.getStatus().getSimpleStatus().getName());
                            testCaseJson.put("id", issue.getId());
                            testCaseJson.put("Name", issue.getSummary());
                            testCaseJson.put("instanceid", issue.getId());
                            testCaseJson.put("defectCount", defectCount);
                            testCaseJson.put("time", issue.getCreated().toString());

                            JSONObject tempObject = new JSONObject();
                            tempObject.put("testcaseid", issue.getId());
                            tempObject.put("baseUrl", baseUrl);

                            String defects = dem.getDefectsForTestCase(tempObject.toString());
                            JsonElement defectElement = new JsonParser().parse(defects);
                            JsonArray defectsArray = defectElement.getAsJsonArray();
                            JSONArray returnDefectArray = new JSONArray();
                            for (int i = 0; i < defectsArray.size(); i++) {
                                JSONObject defectJsonObj = new JSONObject();
                                defectJsonObj.put("id", defectsArray.get(i).getAsJsonObject().get("id").getAsLong());
                                defectJsonObj.put("key", defectsArray.get(i).getAsJsonObject().get("key").getAsString());
                                defectJsonObj.put("name", defectsArray.get(i).getAsJsonObject().get("name").getAsString());
                                defectJsonObj.put("priority", defectsArray.get(i).getAsJsonObject().get("priority").getAsString());
                                defectJsonObj.put("priorityurl", defectsArray.get(i).getAsJsonObject().get("priorityurl").getAsString());
                                defectJsonObj.put("siverity", defectsArray.get(i).getAsJsonObject().get("siverity").getAsString());
                                defectJsonObj.put("status", defectsArray.get(i).getAsJsonObject().get("status").getAsString());
                                defectJsonObj.put("createddate", defectsArray.get(i).getAsJsonObject().get("createddate").getAsString());
                                defectJsonObj.put("assigned", defectsArray.get(i).getAsJsonObject().get("assigned").getAsString());
                                returnDefectArray.put(defectJsonObj);
                            }
                            testCaseJson.put("defects", returnDefectArray);

                            //testCaseJson.put("defects", defectsArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        jsonTestcaseArray.put(testCaseJson);

                    }
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
        return jsonTestcaseArray.toString();
    }

    private String getUserStoryByVersion(String data, String user) {

        final int[] totalTestCaseCount = {0};
        final int[] totalDefectCount = {0};

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JsonObject returnObj = new JsonObject();
        JsonArray userStoriesJsonArray = new JsonArray();
        String version = jsonData.get("version").getAsString();
        Project project = new GetProjectKey(context).getSelectedProject();
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);

        if (!version.equals("All")) {
            builder.where().project(project.getId()).and().fixVersion(version).and().issueType("Story");
            try {
                SearchResults results = searchProvider.searchOverrideSecurity(userName, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                issues.forEach(issue -> {
                    if (!(issue.getIssueType() == null) && issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story")) {
                        JsonObject jStory = new JsonObject();
                        jStory.addProperty("storyid", issue.getId());
                        JSONObject storyObj = new JSONObject();
                        try {
                            storyObj.put("userStoryId", issue.getId());
                            storyObj.put("baseUrl", "");
                            String testcaseString = getAllTestCases(storyObj.toString(), user);
                            JsonElement defectElement = new JsonParser().parse(testcaseString);
                            JsonArray testcaseArray = defectElement.getAsJsonArray();
                            totalTestCaseCount[0] += testcaseArray.size();
                            for (int k = 0; k < testcaseArray.size(); k++) {
                                JsonObject testcase = testcaseArray.get(k).getAsJsonObject();
                                JsonArray defectArray = testcase.getAsJsonArray("defects");
                                totalDefectCount[0] += defectArray.size();
                            }
                            jStory.addProperty("testcaseCount", testcaseArray.size());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        jStory.addProperty("issuekey", issue.getKey());
                        jStory.addProperty("summary", issue.getSummary());
                        userStoriesJsonArray.add(jStory);
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            builder.where().project(project.getId()).and().issueType("Story");
            try {
                SearchResults results = searchProvider.searchOverrideSecurity(userName, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                issues.forEach(issue -> {
                    if (!(issue.getIssueType() == null) && issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story")) {
                        issue.getKey();
                        JsonObject jStory = new JsonObject();
                        jStory.addProperty("storyid", issue.getId());
                        JSONObject storyObj = new JSONObject();
                        try {
                            storyObj.put("userStoryId", issue.getId());
                            storyObj.put("baseUrl", "");
                            String testcaseString = getAllTestCases(storyObj.toString(), user);
                            JsonElement defectElement = new JsonParser().parse(testcaseString);
                            JsonArray testcaseArray = defectElement.getAsJsonArray();
                            totalTestCaseCount[0] += testcaseArray.size();
                            for (int k = 0; k < testcaseArray.size(); k++) {
                                JsonObject testcase = testcaseArray.get(k).getAsJsonObject();
                                JsonArray defectArray = testcase.getAsJsonArray("defects");
                                totalDefectCount[0] += defectArray.size();
                            }
                            jStory.addProperty("testcaseCount", testcaseArray.size());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        jStory.addProperty("issuekey", issue.getKey());
                        jStory.addProperty("summary", issue.getSummary());
                        userStoriesJsonArray.add(jStory);
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        returnObj.addProperty("totaltestcases", totalTestCaseCount[0]);
        returnObj.addProperty("totaldefectcount", totalDefectCount[0]);
        returnObj.add("stories", userStoriesJsonArray);
        return returnObj.toString();
    }

    private String getPlanUserStoryList(String data) {
        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnEntityUserStoryObject = new JSONObject();

        JSONArray returnEntityUserStoryArray = new JSONArray();
        try {
            List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanInstance.get(Integer.parseInt(jsonData.get("execEntityTPInstanceId").getAsString()));
            ArrayList<String> duplicateList = new ArrayList<>();
            for (ExecEntityTPTCI execEntityTPTCInstance : execEntityTPTCIS) {
                JSONObject returnUserStoryObject = new JSONObject();
                if (!execEntityTPTCInstance.getUserStoryId().toLowerCase(Locale.ENGLISH).equals("") && !duplicateList.contains(execEntityTPTCInstance.getUserStoryId())) {
                    duplicateList.add(execEntityTPTCInstance.getUserStoryId());
                    returnUserStoryObject.put("userStoryId", execEntityTPTCInstance.getUserStoryId());
                    returnUserStoryObject.put("userStorySummary", execEntityTPTCInstance.getUserStorySummary());
                    returnEntityUserStoryArray.put(returnUserStoryObject);
                }
            }
            returnEntityUserStoryObject.put("stories", returnEntityUserStoryArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnEntityUserStoryObject.toString();
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
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("id", "");
                returnObject.put("name", "All");
                returnObject.put("description", "0");
                returnObject.put("selected", false);
                returnArray.put(returnObject);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }

        }
        return returnArray.toString();
    }
}
