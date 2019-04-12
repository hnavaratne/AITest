package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.browse.BrowseContext;
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
import com.virtusa.gto.plugins.aitest.entity.DefectsEntityI;
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
import java.util.*;

public class DefectEntityManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(DefectEntityManagement.class);
    private static final long serialVersionUID = -4210645582779315192L;
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    //Project Key
    BrowseContext context;
    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    @Inject
    public DefectEntityManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
                                  LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            //User login authentication
            String redirectUrl = "";
            JSONObject jsonObject;
            String payLoad = IOUtils.toString(req.getReader());
            String user = req.getParameter("user");
            String data = req.getParameter("data");
            String action = req.getParameter("action");

            if (Objects.nonNull(payLoad) && payLoad.contains("user") && payLoad.contains("action") && payLoad.contains("data")) {
                JsonObject parse = new JsonParser().parse(payLoad).getAsJsonObject();
                user = parse.get("user").getAsString();
                data = parse.get("data").getAsString();
                action = parse.get("action").getAsString();
            }
            jsonObject = new JSONObject(data);
            if (jsonObject.has("redirectView")) {
                redirectUrl = jsonObject.get("redirectView").toString();
            }
            UserProfile currentUser = userManager.getRemoteUser(req);
            if (!Objects.nonNull(currentUser)) {
                resp.setStatus(HttpServletResponse.SC_FOUND);
                if (redirectUrl.equals("/secure/TestCaseDefectsView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                } else if (redirectUrl.equals("/secure/TestReportsView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    handleAction(action, data, user, resp);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String data, String user, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get-entity-defect-count":
                    resp.getWriter().write(getEntityDefectCount(data));
                    break;
                case "get-plan-defect-count":
                    resp.getWriter().write(getPlanDefectCount(data));
                    break;
                case "get-test-case-defect-count":
                    resp.getWriter().write(getTestCaseDefectCount(data));
                    break;
                case "get-plan-defect-list":
                    resp.getWriter().write(String.valueOf(getPlanDefectList(data)));
                    break;
                default:
                    handleRemainingActions1(action, data, user, resp);
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleRemainingActions1(String action, String data, String user, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get-test-case-defect-list":
                    resp.getWriter().write(String.valueOf(getTestCaseDefectList(data)));
                    break;
                case "get-all-defects":
                    resp.getWriter().write(getAllDefects(user, data));
                    break;
                case "save-defects":
                    resp.getWriter().write(saveDefectsAttachedManually(data));
                    break;
                case "get-saved-defects":
                    resp.getWriter().write(getSavedDefects(data));
                    break;
                default:
                    handleRemainingActions2(action, data, user, resp);
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleRemainingActions2(String action, String data, String user, HttpServletResponse resp) {
        try {
            switch (action) {
                case "append-selected-defect":
                    resp.getWriter().write(appendSelectedDefect(user));
                    break;
                case "get-defects-for-testcase":
                    resp.getWriter().write(getDefectsForTestCase(data));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getEntityDefectCount(String data) {
        JsonObject jsonObj = new JsonParser().parse(data).getAsJsonObject();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        int defectCount = defectsEntity.getEntityDefectCount(jsonObj.get("execEntityInstanceId").getAsString());
        return String.valueOf(defectCount);
    }

    private String getPlanDefectCount(String data) {
        JsonObject jsonObj = new JsonParser().parse(data).getAsJsonObject();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        int defectCount = defectsEntity.getPlanDefectCount(jsonObj.get("execEntityPlanInstanceId").getAsString());
        return String.valueOf(defectCount);
    }

    private String getTestCaseDefectCount(String data) {
        JsonObject jsonObj = new JsonParser().parse(data).getAsJsonObject();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        int defectCount = defectsEntity.getTestCaseDefectCount(jsonObj.get("execEntityTPTCInstanceId").getAsString());
        return String.valueOf(defectCount);
    }

    private String getPlanDefectList(String data) {
        JsonObject jsonObj = new JsonParser().parse(data).getAsJsonObject();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        JSONArray returnObject = new JSONArray();
        List<DefectsEntityI> execEntityPlanInstanceList = defectsEntity.getPlanDefectList(jsonObj.get("execEntityPlanInstanceId").getAsString());
        try {
            for (DefectsEntityI execEntityPlanInstance : execEntityPlanInstanceList) {
                JSONObject jsonObject = new JSONObject();
                MutableIssue userStory = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(execEntityPlanInstance.getDefectId()));
                jsonObject.put("defectId", execEntityPlanInstance.getDefectId());
                jsonObject.put("defectName", userStory.getSummary());
                jsonObject.put("defectKey", execEntityPlanInstance.getDefectKey());
                jsonObject.put("defectStatus", userStory.getStatus().getName());
                returnObject.put(jsonObject);
            }

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }

    private String getTestCaseDefectList(String data) {
        JsonObject jsonObj = new JsonParser().parse(data).getAsJsonObject();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        JSONArray returnObject = new JSONArray();
        List<DefectsEntityI> execEntityTestCaseInstanceList = defectsEntity.getTestCaseDefectList(jsonObj.get("execEntityTPTCInstanceId").getAsString());
        try {
            for (DefectsEntityI defectsEntityI : execEntityTestCaseInstanceList) {
                JSONObject jsonObject = new JSONObject();
                MutableIssue userStory = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(defectsEntityI.getDefectId()));
                jsonObject.put("defectId", defectsEntityI.getDefectId());
                jsonObject.put("defectName", userStory.getSummary());
                jsonObject.put("defectKey", defectsEntityI.getDefectKey());
                jsonObject.put("defectStatus", userStory.getStatus().getName());
                returnObject.put(jsonObject);
            }

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }

    private String getAllDefects(String user, String data) {
        JSONArray defectList = new JSONArray();
        Project project = new GetProjectKey(context).getSelectedProject();
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        String savedDefectListStr = getSavedDefects(data);
        JsonElement jsonElement = new JsonParser().parse(savedDefectListStr);
        JsonArray savedDefectList = jsonElement.getAsJsonArray();
        JqlQueryBuilder builderTCase = JqlQueryBuilder.newBuilder();
        SearchService searchProviderTestCases = ComponentAccessor.getComponentOfType(SearchService.class);
        builderTCase.where().project(project.getId()).and().issueType("Bug");
        List<Issue> tempIssueArray = new ArrayList<>();

        try {
            SearchResults results = searchProviderTestCases.searchOverrideSecurity(userName, builderTCase.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                if (savedDefectList.size() > 0) {
                    for (int i = 0; savedDefectList.size() > i; i++) {
                        JsonObject savedDefectObj = savedDefectList.get(i).getAsJsonObject();
                        if (Objects.nonNull(savedDefectObj) && !issue.getId().equals(Long.valueOf(savedDefectObj.get("defectId").getAsString()))) {
                            tempIssueArray.add(issue);
                        }
                    }
                } else {
                    tempIssueArray.add(issue);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
        Set<Issue> issueSet = new HashSet<>(tempIssueArray);
        tempIssueArray.clear();
        tempIssueArray.addAll(issueSet);

        tempIssueArray.forEach(issue -> {
            try {
                JSONObject defectObj = new JSONObject();
                defectObj.put(issue.getId().toString() + "-" + issue.getKey(), issue.getSummary());
                defectList.put(defectObj);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        });
        return defectList.toString();
    }

    private String appendSelectedDefect(String user) {
        JSONArray defectList = new JSONArray();
        Project project = new GetProjectKey(context).getSelectedProject();
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        JqlQueryBuilder builderTCase = JqlQueryBuilder.newBuilder();
        SearchService searchProviderTestCases = ComponentAccessor.getComponentOfType(SearchService.class);
        builderTCase.where().project(project.getId()).and().issueType("Bug");
        try {
            SearchResults results = searchProviderTestCases.searchOverrideSecurity(userName, builderTCase.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                try {
                    JSONObject defectObj = new JSONObject();
                    defectObj.put(issue.getId().toString() + "-" + issue.getKey(), issue.getSummary());
                    defectList.put(defectObj);
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
        return defectList.toString();
    }

    private String saveDefectsAttachedManually(String data) {
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        JsonElement jsonElement = new JsonParser().parse(data);
        JsonObject parsedData = jsonElement.getAsJsonObject();
        JsonArray attachedDefectList = parsedData.get("attached_Defect_List").getAsJsonArray();
        boolean isDeleted = defectsEntity.delete(parsedData.get("tCIMyTask_Id").getAsString());
        if (isDeleted) {
            for (int i = 0; i < attachedDefectList.size(); i++) {
                JsonObject attachedDefectObj = attachedDefectList.get(i).getAsJsonObject();
                defectsEntity.update(attachedDefectObj.get("defectId").getAsString(), attachedDefectObj.get("defectKey").getAsString(), attachedDefectObj.get("defectName").getAsString(), parsedData.get("tCIMyTask_Id").getAsString());
            }
        }
        return "true";
    }

    private String getSavedDefects(String data) {
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        JSONArray savedDefectsArray = new JSONArray();
        JsonElement jsonElement = new JsonParser().parse(data);
        JsonObject parsedData = jsonElement.getAsJsonObject();
        try {
            List<DefectsEntityI> defectsSavedList = defectsEntity.getDefectsSaved(parsedData.get("tCIMyTask_Id").getAsString());
            for (DefectsEntityI defectsEntityI : defectsSavedList) {
                JSONObject defectObj = new JSONObject();
                defectObj.put("defectId", defectsEntityI.getDefectId());
                defectObj.put("defectKey", defectsEntityI.getDefectKey());
                defectObj.put("defectName", defectsEntityI.getDefectName());
                savedDefectsArray.put(defectObj);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return savedDefectsArray.toString();
    }

    public String getDefectsForTestCase(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JsonArray defectJsonArray = new JsonArray();
        String testCaseId = jsonData.get("testcaseid").getAsString();
        String baseUrl = jsonData.get("baseUrl").getAsString();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        List<DefectsEntityI> defectList = defectsEntity.getTestCaseAllDefectList(testCaseId);
        CustomField severity = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Severity").iterator().next();
        defectList.forEach(defect -> {
            MutableIssue userStory = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(defect.getDefectId()));
            JsonObject defectJsonObj = new JsonObject();
            defectJsonObj.addProperty("id", defect.getDefectId());
            defectJsonObj.addProperty("key", defect.getDefectKey());
            defectJsonObj.addProperty("name", defect.getDefectName());
            if (userStory.getPriority() != null) {
                defectJsonObj.addProperty("priority", userStory.getPriority().getName());
                defectJsonObj.addProperty("priorityurl", baseUrl + userStory.getPriority().getIconUrl());
            } else {
                defectJsonObj.addProperty("priority", "N/A");
                defectJsonObj.addProperty("priorityurl", "N/A");
            }
            if (userStory.getCustomFieldValue(severity) != null) {
                defectJsonObj.addProperty("siverity", userStory.getCustomFieldValue(severity).toString());
            } else {
                defectJsonObj.addProperty("siverity", "N/A");
            }

            defectJsonObj.addProperty("status", userStory.getStatus().getSimpleStatus().getName());
            defectJsonObj.addProperty("createddate", userStory.getCreated().toString());
            if (userStory.getAssignee() != null) {
                defectJsonObj.addProperty("assigned", userStory.getAssignee().getUsername());
            } else {
                defectJsonObj.addProperty("assigned", "N/A");
            }
            defectJsonArray.add(defectJsonObj);
        });

        return defectJsonArray.toString();
    }
}
