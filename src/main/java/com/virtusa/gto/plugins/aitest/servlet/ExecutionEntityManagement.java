package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Scanned
public class ExecutionEntityManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ExecutionEntityManagement.class);
    private static final long serialVersionUID = -154715243026434395L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;
    //Project Key
    BrowseContext context;

    @Inject
    public ExecutionEntityManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            String action = req.getParameter("action");
            switch (action) {
                case "insert":
                    resp.getWriter().write(insert(data));
                    break;
                case "get":
                    resp.getWriter().write(get(data));
                    break;
                case "clone":
                    resp.getWriter().write(clone(data));
                    break;
                case "validate":
                    resp.getWriter().write(validateBeforeRun(data));
                    break;
                case "clone-manual-test-case":
                    JsonObject obj = new JsonParser().parse(data).getAsJsonObject();
                    String test_case_id = obj.get("testCaseId").getAsString();
                    resp.getWriter().write(createRowDataIterations(test_case_id));
                    break;
                default:
                    break;

            }
        }
    }

    private String get(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnData = new JSONObject();
        EEntity eEntity = new EEntity(activeObjects);
        EEntityPlan eEntityPlan = new EEntityPlan(activeObjects);
        EEntityPlanTestCase eEntityPlanTestCase = new EEntityPlanTestCase(activeObjects);
        List<ExecEntity> execEntities = eEntity.get(jsonData.get("id").getAsString());
        ExecEntity execEntity = execEntities.get(0);
        if (Objects.nonNull(execEntity)) {
            try {
                JSONArray returnDataPlanArray = new JSONArray();
                returnData.put("id", execEntity.getID());
                returnData.put("release", execEntity.getRelease());
                returnData.put("description", execEntity.getDescription());
                returnData.put("projectKey", execEntity.getProjectKey());

                List<ExecEntityTP> execEntityTPList = eEntityPlan.get(execEntity.getID());
                for (ExecEntityTP execEntityTP : execEntityTPList) {
                    JSONObject execEntityTPObject = new JSONObject();
                    execEntityTPObject.put("id", execEntityTP.getID());
                    execEntityTPObject.put("entityId", execEntityTP.getEntityId());
                    execEntityTPObject.put("planId", execEntityTP.getTestPlanId());
                    execEntityTPObject.put("planName", execEntityTP.getTestPlanName());

                    JSONArray returnDataPlanTCArray = new JSONArray();
                    List<ExecEntityTPTC> execEntityTPTCList = eEntityPlanTestCase.get(execEntityTP.getID());
                    for (ExecEntityTPTC execEntityTPTC : execEntityTPTCList) {
                        JSONObject execEntityTPTCObject = new JSONObject();
                        execEntityTPTCObject.put("id", execEntityTPTC.getID());
                        execEntityTPTCObject.put("testCaseId", execEntityTPTC.getTestCaseId());
                        execEntityTPTCObject.put("testCaseName", execEntityTPTC.getTestCaseName());
                        execEntityTPTCObject.put("automated", execEntityTPTC.getTestCaseAutomated());
                        execEntityTPTCObject.put("manual", execEntityTPTC.getTestCaseManual());
                        execEntityTPTCObject.put("agent", execEntityTPTC.getTestCaseAgent());
                        execEntityTPTCObject.put("isPoolAgent", execEntityTPTC.isPoolAgent());
                        execEntityTPTCObject.put("user", execEntityTPTC.getTestCaseUser());
                        execEntityTPTCObject.put("overallExpectedResult", execEntityTPTC.getOverallExpectedResult());
                        execEntityTPTCObject.put("testCaseBrowser", execEntityTPTC.getTestCaseBrowser());
                        returnDataPlanTCArray.put(execEntityTPTCObject);
                    }
                    execEntityTPObject.put("testCases", returnDataPlanTCArray);
                    returnDataPlanArray.put(execEntityTPObject);
                }
                returnData.put("plans", returnDataPlanArray);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnData.toString();
    }

    public String insert(String data) {
        String projectKey;
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (jsonData.has("projectKey")) {
            projectKey = jsonData.get("projectKey").getAsString();
        } else {
            projectKey = new GetProjectKey(context).getSelectedProjectKey();
        }
        JSONObject resultObject = new JSONObject();
        EEntity eEntity = new EEntity(activeObjects);
        EEntityPlan eEntityPlan = new EEntityPlan(activeObjects);
        EEntityPlanTestCase eEntityPlanTestCase = new EEntityPlanTestCase(activeObjects);
        ExecEntity execEntityInserted = eEntity.insert(projectKey, jsonData.get("release").getAsString(), jsonData.get("description").getAsString());
        try {
            resultObject.put("id", execEntityInserted.getID());
            resultObject.put("release", execEntityInserted.getRelease());
            resultObject.put("description", execEntityInserted.getDescription());
            JsonArray plansToInsert;
            if (jsonData.get("isFromSchedule").getAsBoolean()) {
                JsonElement jsonElement = new JsonParser().parse(jsonData.get("plans").getAsString());
                plansToInsert = jsonElement.getAsJsonArray();
            } else {
                plansToInsert = jsonData.get("plans").getAsJsonArray();
            }
            for (JsonElement planToInsert : plansToInsert) {
                JsonObject planToInsertObject = planToInsert.getAsJsonObject();
                ExecEntityTP executionEntityPlanInserted = eEntityPlan.insert(execEntityInserted.getID(), planToInsertObject.get("planId").getAsString(), planToInsertObject.get("planName").getAsString(), projectKey);
                JsonArray testCases = planToInsertObject.get("testCases").getAsJsonArray();
                for (JsonElement testCase : testCases) {
                    JsonObject testCaseObject = testCase.getAsJsonObject();
                    String testCaseAgent = "";
                    boolean isPoolAgent = false;
                    if (testCaseObject.get("agent") != null && !testCaseObject.get("agent").getAsString().equals("Select")) {
                        testCaseAgent = testCaseObject.get("agent").getAsString();
                        isPoolAgent = testCaseObject.get("testAgentStatus").getAsBoolean();
                    }
                    String testCaseUser = "";
                    if (testCaseObject.get("user") != null && !testCaseObject.get("user").getAsString().equals("Select")) {
                        testCaseUser = testCaseObject.get("user").getAsString();
                    }
                    String testCaseOverallExpectedResult = testCaseObject.get("overallExpectedResult").getAsString();
                    eEntityPlanTestCase.insert(executionEntityPlanInserted.getID(), testCaseObject.get("testCaseId").getAsString(), testCaseObject.get("testCaseName").getAsString(), testCaseObject.get("automated").getAsString(), testCaseObject.get("manual").getAsString(), isPoolAgent, testCaseAgent, testCaseUser, testCaseOverallExpectedResult, testCaseObject.get("testCaseBrowser").getAsString());
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return resultObject.toString();
    }

    public String clone(String data) {
        String dataString = get(data);
        JsonElement element = new JsonParser().parse(dataString);
        JsonObject jsonData = element.getAsJsonObject();
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
        JSONObject resultObject = new JSONObject();
        JSONArray resultPlansArray = new JSONArray();
        String projectKey;
        if (jsonData.has("projectKey")) {
            projectKey = jsonData.get("projectKey").getAsString();
        } else {
            projectKey = new GetProjectKey(context).getSelectedProjectKey();
        }
        int eEntityId = jsonData.get("id").getAsInt();
        String release = jsonData.get("release").getAsString();
        String description = jsonData.get("description").getAsString();

        ExecEntityI execEntityInserted = eEntityInstance.insert(eEntityId, release, description, projectKey);
        try {
            resultObject.put("id", execEntityInserted.getID());
            resultObject.put("execEntity", execEntityInserted.getExecEntity());
            resultObject.put("release", execEntityInserted.getRelease());
            resultObject.put("description", execEntityInserted.getDescription());
            resultObject.put("status", execEntityInserted.getStatus());
            resultObject.put("projectKey", execEntityInserted.getProjectKey());
            JsonArray plansToInsert = jsonData.get("plans").getAsJsonArray();
            cloneTestPlan(projectKey, resultPlansArray, eEntityInstance, eEntityPlanInstance, eEntityPlanTestCaseInstance, execEntityInserted.getID(), release, execEntityInserted, plansToInsert);
            resultObject.put("plans", resultPlansArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return resultObject.toString();
    }

    private void cloneTestPlan(String projectKey, JSONArray resultPlansArray, EEntityInstance eEntityInstance, EEntityPlanInstance eEntityPlanInstance, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, int eEntityId, String release, ExecEntityI execEntityInserted, JsonArray plansToInsert) throws JSONException {
        for (JsonElement planToInsert : plansToInsert) {
            JSONObject resultPlanObject = new JSONObject();
            JSONArray resultTestCaseArray = new JSONArray();
            JsonObject planToInsertObject = planToInsert.getAsJsonObject();
            int execEntityTPId = planToInsertObject.get("id").getAsInt();
            String planId = planToInsertObject.get("planId").getAsString();
            String planName = planToInsertObject.get("planName").getAsString();
            ExecEntityTPI executionEntityPlanInserted = eEntityPlanInstance.insert(execEntityTPId, execEntityInserted.getID(), planId, planName, projectKey);
            resultPlanObject.put("id", executionEntityPlanInserted.getID());
            resultPlanObject.put("execEntityTP", executionEntityPlanInserted.getExecEntityTP());
            resultPlanObject.put("entityId", executionEntityPlanInserted.getEntityId());
            resultPlanObject.put("planId", executionEntityPlanInserted.getTestPlanId());
            resultPlanObject.put("planName", executionEntityPlanInserted.getTestPlanName());
            resultPlanObject.put("status", executionEntityPlanInserted.getStatus());
            resultPlanObject.put("projectKey", executionEntityPlanInserted.getProjectKey());
            JsonArray testCases = planToInsertObject.get("testCases").getAsJsonArray();
            cloneTestCase(projectKey, eEntityInstance, eEntityPlanTestCaseInstance, eEntityId, release, resultTestCaseArray, executionEntityPlanInserted.getID(), executionEntityPlanInserted, testCases);
            resultPlanObject.put("testCases", resultTestCaseArray);
            resultPlansArray.put(resultPlanObject);
        }
    }

    private void cloneTestCase(String projectKey, EEntityInstance eEntityInstance, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, int eEntityId, String release, JSONArray resultTestCaseArray, int execEntityTPId, ExecEntityTPI executionEntityPlanInserted, JsonArray testCases) throws JSONException {
        for (JsonElement testCase : testCases) {
            UserStoryEntityInstance userStoryEntityInstance = new UserStoryEntityInstance(activeObjects);
            JSONObject resultTestCaseObject = new JSONObject();
            JsonObject testCaseObject = testCase.getAsJsonObject();
            int execEntityTPTC = testCaseObject.get("id").getAsInt();
            String testCaseId = testCaseObject.get("testCaseId").getAsString();
            String testCaseName = testCaseObject.get("testCaseName").getAsString();
            String testCaseAutomated = testCaseObject.get("automated").getAsString();
            String testCaseManual = testCaseObject.get("manual").getAsString();
            String testCaseBrowser = testCaseObject.get("testCaseBrowser").getAsString();
            String testCaseAgent = "";
            boolean isPoolAgent = false;
            if (testCaseObject.get("agent") != null && !testCaseObject.get("agent").getAsString().equals("Select")) {
                testCaseAgent = testCaseObject.get("agent").getAsString();
                isPoolAgent = testCaseObject.get("isPoolAgent").getAsBoolean();
            }
            String testCaseUser = "";
            if (testCaseObject.get("user") != null && !testCaseObject.get("user").getAsString().equals("Select")) {
                testCaseUser = testCaseObject.get("user").getAsString();
            }
            String expectedResult = testCaseObject.get("overallExpectedResult").getAsString();

            IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
            MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(testCaseId));
            UserManager userManager = ComponentAccessor.getUserManager();
            cloneTestCaseExtended(projectKey, eEntityInstance, eEntityPlanTestCaseInstance, eEntityId, release, resultTestCaseArray, execEntityTPId, executionEntityPlanInserted, userStoryEntityInstance, resultTestCaseObject, execEntityTPTC, testCaseId, testCaseName, testCaseAutomated, testCaseManual, testCaseBrowser, testCaseAgent, isPoolAgent, testCaseUser, expectedResult, issueLinkManager, issueObject, userManager);
        }
    }

    private void cloneTestCaseExtended(String projectKey, EEntityInstance eEntityInstance, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, int eEntityId, String release, JSONArray resultTestCaseArray, int execEntityTPId, ExecEntityTPI executionEntityPlanInserted, UserStoryEntityInstance userStoryEntityInstance, JSONObject resultTestCaseObject, int execEntityTPTC, String testCaseId, String testCaseName, String testCaseAutomated, String testCaseManual, String testCaseBrowser, String testCaseAgent, boolean isPoolAgent, String testCaseUser, String expectedResult, IssueLinkManager issueLinkManager, MutableIssue issueObject, UserManager userManager) throws JSONException {
        ApplicationUser applicationUser;
        Collection<Issue> issues;
        if (Objects.nonNull(testCaseUser) && !testCaseUser.isEmpty()) {
            applicationUser = userManager.getUserByName(testCaseUser);
            issues = issueLinkManager.getLinkCollection(issueObject, applicationUser).getAllIssues();
        } else {
            issues = issueLinkManager.getLinkCollection(issueObject, issueObject.getCreator()).getAllIssues();
        }
        Issue issueObj = null;
        for (Issue issue : issues) {
            if (Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ENGLISH).equals("story")) {
                issueObj = issue;
            }
        }
        if (!Objects.nonNull(issueObj)) {
            userStoryEntityInstance.insert("", projectKey, "", "");
        } else {
            userStoryEntityInstance.insert(String.valueOf(issueObj.getId()), projectKey, issueObj.getCreator().getName(), issueObj.getSummary());
        }
        if (testCaseManual.toLowerCase(Locale.ENGLISH).equals("yes")) {
            cloneManualTestCase(eEntityInstance, eEntityPlanTestCaseInstance, eEntityId, release, execEntityTPId, executionEntityPlanInserted, execEntityTPTC, testCaseId, testCaseName, testCaseManual, testCaseBrowser, testCaseAgent, isPoolAgent, testCaseUser, expectedResult, issueObj, projectKey);
        }
        if (testCaseAutomated.toLowerCase(Locale.ENGLISH).equals("yes")) {
            new ExecutionEntityDataManagement().cloneAutomatedTestCase(eEntityInstance, eEntityPlanTestCaseInstance, eEntityId, release, resultTestCaseArray, executionEntityPlanInserted, resultTestCaseObject, execEntityTPTC, testCaseId, testCaseName, testCaseAutomated, testCaseBrowser, testCaseAgent, isPoolAgent, testCaseUser, expectedResult, issueObj, activeObjects);
        }
    }

    private void cloneManualTestCase(EEntityInstance eEntityInstance, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, int eEntityId, String release, int execEntityTPId, ExecEntityTPI executionEntityPlanInserted, int execEntityTPTC, String testCaseId, String testCaseName, String testCaseManual, String testCaseBrowser, String testCaseAgent, boolean isPoolAgent, String testCaseUser, String expectedResult, Issue issueObj, String projectKey) {
        String manualRowDataIteration = createRowDataIterations(testCaseId);
        JsonArray rowDataArray = new JsonParser().parse(manualRowDataIteration).getAsJsonArray();
        boolean hasIterationData = rowDataArray.size() > 0;
        EEntityPlanTestCaseDataInstance eEntityPlanTestCaseDataInstance = new EEntityPlanTestCaseDataInstance(activeObjects);
        if (!Objects.nonNull(issueObj)) {
            ExecEntityTPTCI execEntityTPTCInserted = eEntityPlanTestCaseInstance.insert(eEntityId, execEntityTPTC, executionEntityPlanInserted.getID(), testCaseId, testCaseName, "", testCaseManual, isPoolAgent, testCaseAgent, testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser, hasIterationData, "", "");
            if (!hasIterationData) {
                String selectedProjectKey;
                if (Objects.nonNull(projectKey)) {
                    selectedProjectKey = projectKey;
                } else {
                    selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
                }
                new ExecutionEntityDataManagement().cloneTestSteps(testCaseId, execEntityTPTCInserted.getID(), true, null, null, activeObjects);
                TCInstanceMyTask tcInstanceMyTask = new TCInstanceMyTask(activeObjects);
                tcInstanceMyTask.insert(eEntityId, execEntityTPId, execEntityTPTCInserted.getID(), selectedProjectKey, null, testCaseId, testCaseName, String.valueOf(true), testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser);
            } else {
                TCaseDataInstance testCaseDataInstance = eEntityPlanTestCaseDataInstance.insert(execEntityTPTCInserted.getID());
                createManualIterations(eEntityId, execEntityTPId, rowDataArray, testCaseDataInstance, testCaseId, execEntityTPTCInserted.getID(), testCaseName, testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser, projectKey);
            }
        } else {
            ExecEntityTPTCI execEntityTPTCInserted = eEntityPlanTestCaseInstance.insert(eEntityId, execEntityTPTC, executionEntityPlanInserted.getID(), testCaseId, testCaseName, "", testCaseManual, isPoolAgent, testCaseAgent, testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser, hasIterationData, String.valueOf(issueObj.getId()), issueObj.getSummary());
            if (!hasIterationData) {
                String selectedProjectKey;
                if (Objects.nonNull(projectKey)) {
                    selectedProjectKey = projectKey;
                } else {
                    selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
                }
                new ExecutionEntityDataManagement().cloneTestSteps(testCaseId, execEntityTPTCInserted.getID(), true, null, null, activeObjects);
                TCInstanceMyTask tcInstanceMyTask = new TCInstanceMyTask(activeObjects);
                tcInstanceMyTask.insert(eEntityId, execEntityTPId, execEntityTPTCInserted.getID(), selectedProjectKey, null, testCaseId, testCaseName, String.valueOf(true), testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser);
            } else {
                TCaseDataInstance testCaseDataInstance = eEntityPlanTestCaseDataInstance.insert(execEntityTPTCInserted.getID());
                createManualIterations(eEntityId, execEntityTPId, rowDataArray, testCaseDataInstance, testCaseId, execEntityTPTCInserted.getID(), testCaseName, testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser, projectKey);
            }
        }
    }

    private void createManualIterations(int eEntityId, int execEntityTPId, JsonArray rowDataArray, TCaseDataInstance tCaseDataInstance, String testCaseId, int execEntityTPTCInserted, String tCaseName, String user, String expectedResult, int cycle, String testCaseBrowser, String projectKey) {
        EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);
        TCInstanceMyTask tcInstanceMyTask = new TCInstanceMyTask(activeObjects);
        for (int i = 0; i < rowDataArray.size(); i++) {
            String selectedProjectKey;
            if (Objects.nonNull(projectKey)) {
                selectedProjectKey = projectKey;
            } else {
                selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
            }
            TCaseDataIteration tCaseDataIteration = eEnityPlanTestCaseDataIteration.insertTestCaseDataIteration(tCaseDataInstance, rowDataArray.get(i).getAsJsonObject().get("data").getAsJsonObject().toString());
            new ExecutionEntityDataManagement().cloneTestSteps(testCaseId, execEntityTPTCInserted, true, String.valueOf(tCaseDataIteration.getID()), rowDataArray.get(i).getAsJsonObject(), activeObjects);
            tcInstanceMyTask.insert(eEntityId, execEntityTPId, execEntityTPTCInserted, selectedProjectKey, tCaseDataIteration, testCaseId, tCaseName, String.valueOf(true), user, expectedResult, cycle, testCaseBrowser);
        }
    }

    private String validateBeforeRun(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        CustomField scriptsCustomField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Scripts").iterator().next();
        JSONArray scriptLessTestCases = new JSONArray();
        try {
            JsonArray plansToInsert = jsonData.get("plans").getAsJsonArray();
            for (JsonElement planToInsert : plansToInsert) {
                JsonObject planToInsertObject = planToInsert.getAsJsonObject();
                JsonArray testCases = planToInsertObject.get("testCases").getAsJsonArray();
                for (JsonElement testCase : testCases) {
                    JsonObject testCaseObject = testCase.getAsJsonObject();
                    String testCaseAutomated = testCaseObject.get("automated").getAsString().toLowerCase(Locale.ENGLISH);
                    long testCaseId = testCaseObject.get("testCaseId").getAsLong();
                    String testCaseName = testCaseObject.get("testCaseName").getAsString();
                    if (Objects.equals(testCaseAutomated, "yes")) {
                        JsonArray customFieldValue = (JsonArray) issueManager.getIssueObject(testCaseId).getCustomFieldValue(scriptsCustomField);
                        boolean hasAttachedScripts = false;
                        for (JsonElement customFieldElement : customFieldValue) {
                            JsonObject customFieldElementAsJsonObject = customFieldElement.getAsJsonObject();
                            if (customFieldElementAsJsonObject.has("script-id") && customFieldElementAsJsonObject.has("script-name")) {
                                hasAttachedScripts = true;
                            }
                        }
                        if (!hasAttachedScripts) {
                            scriptLessTestCases.put(testCaseName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return scriptLessTestCases.toString();
    }

    private String createRowDataIterations(String testCaseId) {
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        JSONArray finalJsonArray = new JSONArray();
        JSONObject paramObject = new JSONObject();
        int maxLength = 0;
        int paramCount;
        try {
            TestStepParams[] testStepParams = testStepParam.get(testCaseId);
            paramCount = testStepParams.length;
            for (TestStepParams stepParam : testStepParams) {
                String paramName = stepParam.getParamName();
                TestParamV[] testStepParamValues = stepParam.getTestStepParamValues();
                if (testStepParamValues.length > maxLength) {
                    maxLength = testStepParamValues.length;
                }
                JSONArray testParamVJsonArray = new JSONArray();
                for (TestParamV testStepParamValue : testStepParamValues) {
                    testParamVJsonArray.put(testStepParamValue.getValue());
                }
                paramObject.put(paramName, testParamVJsonArray);
            }
            finalJsonArray = new ExecutionEntityDataManagement().createManualTestCaseVariables(paramCount, maxLength, paramObject);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return finalJsonArray.toString();
    }
}
