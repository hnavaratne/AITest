package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.DefectsEntity;
import com.virtusa.gto.plugins.aitest.db.UserStoryEntityInstance;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


public class TestCase extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestCase.class);
    private static final long serialVersionUID = 384413060585375876L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    //Project Key
    BrowseContext context;

    @Inject
    public TestCase(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
        switch (action) {
            case "insert":
                resp.getWriter().append(createIssue(data, user, null));
                break;
            case "delete":
                resp.getWriter().append(delete(data, user));
                break;
            case "rename":
                resp.getWriter().append(rename(data, user));
                break;
            case "update":
                resp.getWriter().append(update(data, user));
                break;
            case "get_issues":
                resp.getWriter().append(getIssues(data));
                break;
            case "get":
                resp.getWriter().append(getIssue(data).toString());
                break;
            default:
                break;
        }
    }

    public String createIssue(String data, String user, String projectId) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        int testCaseFolderId = jsonData.get("parent").getAsInt();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        String projectID;
        if (!(projectId == null)) {
            projectID = projectId;
        } else {
            projectID = new GetProjectKey(context).getSelectedProjectKey();
        }
        String name = jsonData.get("name").getAsString();
        String description = jsonData.get("description").getAsString();
        String overallExpectedResult = jsonData.get("overall_expected_result").getAsString();
        String automated = jsonData.get("automated").getAsString();
        String manual = jsonData.get("manual").getAsString();
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        Project projectObjByKey = projectManager.getProjectObj(Long.valueOf(projectID));
        Collection<IssueType> issueTypesForProject = ComponentAccessor.getIssueTypeSchemeManager().getIssueTypesForProject(projectObjByKey);
        IssueType issueTypeF = null;
        for (IssueType issueType : issueTypesForProject) {
            if (issueType.getName().toLowerCase(Locale.ENGLISH).equals("testcase")) {
                issueTypeF = issueType;
                break;
            }
        }
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setSummary(name);
        issueInputParameters.setDescription(description);
        issueInputParameters.setReporterId(user);
        issueInputParameters.setIssueTypeId(Objects.requireNonNull(issueTypeF).getId());
        issueInputParameters.setProjectId(Long.valueOf(projectID));
        CustomField automated1 = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Automated");
        Options options = ComponentAccessor.getOptionsManager().getOptions(automated1.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        Option optionAutomated = null;
        Option optionManual = null;
        for (Option option1 : options) {
            if (option1.getValue().toLowerCase(Locale.ENGLISH).equals(automated.toLowerCase(Locale.ENGLISH))) {
                optionAutomated = option1;
                break;
            }
        }
        CustomField manual1 = customFieldManager.getCustomFieldObjectByName("Manual");
        Options optionsManual = ComponentAccessor.getOptionsManager().getOptions(manual1.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        for (Option option : optionsManual) {
            if (option.getValue().toLowerCase(Locale.ENGLISH).equals(manual.toLowerCase(Locale.ENGLISH))) {
                optionManual = option;
                break;
            }
        }
        issueInputParameters
                .addCustomFieldValue(automated1.getId(), Objects.requireNonNull(Objects.requireNonNull(optionAutomated).getOptionId()).toString())
                .addCustomFieldValue(manual1.getId(), Objects.requireNonNull(Objects.requireNonNull(optionManual).getOptionId()).toString())
                .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallExpectedResult").getId(), overallExpectedResult)
                .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder").getId(), String.valueOf(testCaseFolderId));

        IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(userByKey, issueInputParameters);
        JSONObject returnObject = new JSONObject();
        if (createValidationResult.isValid()) {
            IssueService.IssueResult createResult = issueService.create(userByKey, createValidationResult);
            if (!createResult.isValid()) {
                log.error(createResult.getErrorCollection().getFlushedErrorMessages().toString());
            }
            try {
                returnObject.put("id", createResult.getIssue().getId().toString());
                returnObject.put("name", createResult.getIssue().getSummary());
                returnObject.put("automated", createResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Automated")));
                returnObject.put("manual", createResult.getIssue().getCustomFieldValue(manual1));
                returnObject.put("parent", createResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder")));
                returnObject.put("overallExpectedResult", createResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallExpectedResult")));
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnObject.toString();
    }

    public String update(String data, String user) {
        JSONObject returnObject = new JSONObject();

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        Long id = jsonData.get("id").getAsLong();
        String name = jsonData.get("name").getAsString();
        String description = jsonData.get("description").getAsString();
        String overallExpectedResult = jsonData.get("overall_expected_result").getAsString();
        String automated = jsonData.get("automated").getAsString();
        String manual = jsonData.get("manual").getAsString();
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueObject(id);
        IssueService issueService = ComponentAccessor.getIssueService();
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setSummary(name);
        issueInputParameters.setDescription(description);
        CustomField automated1 = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Automated");
        Options options = ComponentAccessor.getOptionsManager().getOptions(automated1.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        Option optionAutomated = null;
        Option optionManual = null;
        for (Option option1 : options) {
            if (option1.getValue().toLowerCase(Locale.ENGLISH).equals(automated.toLowerCase(Locale.ENGLISH))) {
                optionAutomated = option1;
                break;
            }
        }
        CustomField manual1 = customFieldManager.getCustomFieldObjectByName("Manual");
        Options optionsManual = ComponentAccessor.getOptionsManager().getOptions(manual1.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        for (Option option : optionsManual) {
            if (option.getValue().toLowerCase(Locale.ENGLISH).equals(manual.toLowerCase(Locale.ENGLISH))) {
                optionManual = option;
                break;
            }
        }
        issueInputParameters
                .addCustomFieldValue(automated1.getId(), Objects.requireNonNull(Objects.requireNonNull(optionAutomated).getOptionId()).toString())
                .addCustomFieldValue(manual1.getId(), Objects.requireNonNull(Objects.requireNonNull(optionManual).getOptionId()).toString())
                .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallExpectedResult").getId(), overallExpectedResult);
//                    .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallStatus").getId(), overallStatus);
        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(userByKey, issueByCurrentKey.getId(), issueInputParameters);
        createReturnObject(updateValidationResult, issueService, customFieldManager, userByKey, returnObject);
        return returnObject.toString();
    }

    private void createReturnObject(IssueService.UpdateValidationResult updateValidationResult, IssueService issueService, CustomFieldManager customFieldManager, ApplicationUser userByKey, JSONObject returnObject) {
        try {
            if (updateValidationResult.isValid()) {
                IssueService.IssueResult updateResult = issueService.update(userByKey, updateValidationResult);
                if (!updateResult.isValid()) {
                    log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
                }
                returnObject.put("id", updateResult.getIssue().getId().toString());
                returnObject.put("name", updateResult.getIssue().getSummary());
                returnObject.put("description", updateResult.getIssue().getDescription());
                returnObject.put("automated", updateResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Automated")));
                returnObject.put("manual", updateResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Manual")));
                returnObject.put("parent", updateResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder")));
                returnObject.put("overallExpectedResult", updateResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallExpectedResult")));
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String delete(String data, String user) {
        try {
            JsonElement element = new JsonParser().parse(data);
            JsonObject jsonData = element.getAsJsonObject();
            Long testCaseId = jsonData.get("id").getAsLong();
            MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueObject(testCaseId);
            ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
            IssueService issueService = ComponentAccessor.getIssueService();
            TestCaseManagementViewProjectIssues tcmvpi=new TestCaseManagementViewProjectIssues(authenticationContext);
            UserStoryEntityInstance usei=new UserStoryEntityInstance(activeObjects);
            DefectsEntity defectEntity=new DefectsEntity(activeObjects);
            defectEntity.deleteDefectsForTestCase(String.valueOf(testCaseId));
            tcmvpi.deleteIssueLinkForTestCase(String.valueOf(testCaseId),user);
            usei.deleteTestCase(String.valueOf(testCaseId));
            IssueService.DeleteValidationResult deleteValidationResult = issueService.validateDelete(userByKey, issueByCurrentKey.getId());
            if (deleteValidationResult.isValid()) {
                ErrorCollection deleteResult = issueService.delete(userByKey, deleteValidationResult);
                if (deleteResult.hasAnyErrors()) {
                    log.error(deleteResult.getFlushedErrorMessages().toString());
                }
                return "true";
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "false";
    }

    private String rename(String data, String user) {
        try {
            JsonElement element = new JsonParser().parse(data);
            JsonObject jsonData = element.getAsJsonObject();
            Long testCaseId = jsonData.get("id").getAsLong();
            String name = jsonData.get("newname").getAsString();
            MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueObject(testCaseId);
            IssueService issueService = ComponentAccessor.getIssueService();
            UserStoryEntityInstance userStoryEntityInstance=new UserStoryEntityInstance(activeObjects);
            userStoryEntityInstance.updateTestCase(String.valueOf(testCaseId),name);
            ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            issueInputParameters.setSummary(name);
            IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(userByKey, issueByCurrentKey.getId(), issueInputParameters);
            if (updateValidationResult.isValid()) {
                IssueService.IssueResult updateResult = issueService.update(userByKey, updateValidationResult);
                if (!updateResult.isValid()) {
                    log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
                    return "false";
                }
                return "true";
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "false";
    }

    public JSONObject getIssue(String data) {
        JSONObject testcaseObject = new JSONObject();
        try {
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            JsonElement element = new JsonParser().parse(data);
            JsonObject jsonData = element.getAsJsonObject();
            Long id = jsonData.get("id").getAsLong();
            IssueManager issueMgr = ComponentAccessor.getIssueManager();
            Issue issue = issueMgr.getIssueObject(id);
            testcaseObject.put("id", issue.getId());
            testcaseObject.put("name", issue.getSummary());
            testcaseObject.put("description", issue.getDescription());
            testcaseObject.put("overallExpectedResult", issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallExpectedResult")));
            testcaseObject.put("automated", issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Automated")));
            testcaseObject.put("manual", issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Manual")));
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return testcaseObject;
    }

    public String getIssues(String data) {
        JSONArray testcaseArray = new JSONArray();
        String selectedProjectKey;
        try {
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            JsonElement element = new JsonParser().parse(data);
            JsonObject jsonData = element.getAsJsonObject();
            String parent = jsonData.get("parent").getAsString();
            if(jsonData.has("projectKey")){
                selectedProjectKey = jsonData.get("projectKey").getAsString();
            }else{
                selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
            }
            String parentValue;
            if (parent.equals("null")) {
                parentValue = "0";
            } else {
                parentValue = parent;
            }
            IssueManager issueMgr = ComponentAccessor.getIssueManager();
            List<Issue> searchResults = new ArrayList<>();
            String finalParentValue = parentValue;
            issueMgr.getIssueObjects(issueMgr.getIssueIdsForProject(Long.valueOf(selectedProjectKey))).forEach(issue -> {
                if (Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ENGLISH).equals("testcase")) {
                    Object customField = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder"));

                    if (Objects.nonNull(customField) && customField.equals(finalParentValue)) {
                        searchResults.add(issue);
                    }
                }
            });
            for (Issue testcase : searchResults) {
                JSONObject returnObject = new JSONObject();
                returnObject.put("id", testcase.getId());
                returnObject.put("name", testcase.getSummary());
                returnObject.put("parent", testcase.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder")));
                returnObject.put("automated", testcase.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Automated")));
                returnObject.put("manual", testcase.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Manual")));
                testcaseArray.put(returnObject);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return testcaseArray.toString();
    }

    public void removeIssue(String data, String user) {
        JsonElement element = new JsonParser().parse(getIssues(data));
        JsonArray testcaseArray = element.getAsJsonArray();
        TestPlanTestCaseManagement tptc = new TestPlanTestCaseManagement(authenticationContext, activeObjects);
        TestCaseManagementViewProjectIssues tcmvpi=new TestCaseManagementViewProjectIssues(authenticationContext);
        UserStoryEntityInstance usei=new UserStoryEntityInstance(activeObjects);
        DefectsEntity defectEntity=new DefectsEntity(activeObjects);
        for (int i = 0; i < testcaseArray.size(); i++) {
            JsonObject testCase = testcaseArray.get(i).getAsJsonObject();
            String testPlanId = testCase.get("id").getAsString();
            JsonObject tempTestCase = new JsonObject();
            tempTestCase.addProperty("testPlanId", testPlanId);
            defectEntity.deleteDefectsForTestCase(testPlanId);
            tcmvpi.deleteIssueLinkForTestCase(testPlanId,user);
            usei.deleteTestCase(testPlanId);
            tptc.removeTestCaseFromTestPlan(tempTestCase.toString());
            delete(testCase.toString(), user);
        }
    }

    public String getAllIssues() {
        JSONArray testcaseArray = new JSONArray();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        try {
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            IssueManager issueMgr = ComponentAccessor.getIssueManager();
            List<Issue> searchResults = new ArrayList<>();
            issueMgr.getIssueObjects(issueMgr.getIssueIdsForProject(Long.valueOf(selectedProjectKey))).forEach(issue -> {
                if (Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ENGLISH).equals("testcase")) {
                    Object customField = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder"));
                    if (Objects.nonNull(customField)) {
                        searchResults.add(issue);
                    }
                }
            });
            for (Issue testcase : searchResults) {
                JSONObject returnObject = new JSONObject();
                returnObject.put("name", testcase.getSummary());
                testcaseArray.put(returnObject);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return testcaseArray.toString();
    }
}