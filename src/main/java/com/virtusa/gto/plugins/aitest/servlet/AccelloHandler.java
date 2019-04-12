package com.virtusa.gto.plugins.aitest.servlet;

//import com.atlassian.activeobjects.external.ActiveObjects;
//import com.atlassian.jira.bc.issue.IssueService;
//import com.atlassian.jira.component.ComponentAccessor;
//import com.atlassian.jira.issue.*;
//import com.atlassian.jira.issue.fields.CustomField;
//import com.atlassian.jira.project.Project;
//import com.atlassian.jira.project.browse.BrowseContext;
//import com.atlassian.jira.user.ApplicationUser;
//import com.atlassian.jira.util.json.JSONException;
//import com.atlassian.jira.util.json.JSONObject;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import javax.servlet.http.HttpServlet;

//import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
//import com.google.gson.*;
//import com.virtusa.gto.plugins.aitest.db.*;
//import com.virtusa.gto.plugins.aitest.entity.*;
//import com.virtusa.gto.plugins.aitest.util.CommonsUtil;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.text.StringEscapeUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.charset.Charset;
//import java.util.*;

@Scanned
public class AccelloHandler extends HttpServlet {
//    private static final Logger log = LoggerFactory.getLogger(AccelloHandler.class);
//
//    BrowseContext context;
//
//    @JiraImport
//    private ActiveObjects activeObjects;
//
//    public AccelloHandler(ActiveObjects activeObjects) {
//        this.activeObjects = activeObjects;
//    }
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        JsonElement jsonElement = new JsonParser().parse(req.getHeader("data"));
//        JsonObject data = jsonElement.getAsJsonObject();
//        String parameter = data.get("api").getAsString();
//
//        InputStream inputStream = req.getInputStream();
//        String contentType = req.getContentType();
//        if (contentType != null && !contentType.toLowerCase(Locale.ENGLISH).startsWith(MediaType.APPLICATION_JSON)) {
//            log.info(IOUtils.toString(inputStream, "UTF-8"));
//        }
//
//        switch (parameter) {
//            case "test-case-hierarchy":
//                Long projectKey = data.get("project_key").getAsLong();
//                resp.getWriter().write(getTestCaseHierarchy(projectKey, resp));
//                break;
//            case "projects":
//                getProjectList(resp);
//                break;
//            case "test-scripts-add":
//                update(req, resp);
//                break;
//            case "test-scripts-remove":
//                remove(req, resp);
//                break;
//            case "test-case_result":
//                updateClonedInstances(data);
//                break;
//            case "ai-rci":
//                resp.getWriter().write(getAIRCI(req));
//                break;
//            default:
//                break;
//        }
//    }
//
//    private List<Issue> getIssues(Long projectId) {
//        try {
//            IssueManager issueMgr = ComponentAccessor.getIssueManager();
//            List<Issue> searchResults = new ArrayList<>();
//            issueMgr.getIssueObjects(issueMgr.getIssueIdsForProject(projectId)).forEach(issue -> {
//                if (Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ENGLISH).equals("testcase".toLowerCase(Locale.ENGLISH))) {
//                    searchResults.add(issue);
//                }
//            });
//            return searchResults;
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//        return null;
//    }
//
//    private String getTestCaseHierarchy(Long projectKey, HttpServletResponse resp) {
//        List<Issue> issueList = getIssues(projectKey);
//        TestCaseFolder[] testCaseFolders = activeObjects.find(TestCaseFolder.class);
//        List<TestCaseFolder> testCaseFolders1 = Arrays.asList(testCaseFolders);
//        Map<Integer, TestCaseFolder> parent = new HashMap<>();
//        for (TestCaseFolder testCaseFolder : testCaseFolders1) {
//            String testCaseFolderParent = testCaseFolder.getParent();
//            if (testCaseFolderParent.equals("null")) {
//                parent.put(testCaseFolder.getID(), testCaseFolder);
//
//            }
//        }
//
//        List<Object> jsonElements = new ArrayList<>();
//        //iterate parents where parentId id null
//        parent.forEach((parentId, parentFolder) -> {
//            Map<String, Object> parentFolders = new HashMap<>();
//            parentFolders.put("name", parentFolder.getName());
//            parentFolders.put("id", parentId);
//            parentFolders.put("type", "TESTSUITE");
//
//            List<Object> childFolder = new ArrayList<>();
//            //get the children entities
//            getChildren(testCaseFolders1, String.valueOf(parentFolder.getID()), childFolder, issueList, resp);
//            if (childFolder.size() == 0) {
//                getLeafNodes(issueList, String.valueOf(parentFolder.getID()), childFolder);
//            }
//            parentFolders.put("entity", childFolder);
//            jsonElements.add(parentFolders);
//        });
//        //add root testcases
//        List<Issue> issueByParent = getIssueByParent(Objects.requireNonNull(issueList), "0");
//        issueByParent.forEach(issue -> {
//            Map<String, Object> jsonObjectF = new HashMap<>();
//            jsonObjectF.put("name", issue.getSummary());
//            jsonObjectF.put("id", issue.getId());
//            jsonObjectF.put("type", "TESTCASE");
//            List<Map<String, Object>> attachedScriptObject = getAttachedScriptObject(issue);
//            jsonObjectF.put("scripts", attachedScriptObject);
//            String testCaseId = String.valueOf(issue.getId());
//            List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
//            List<Map<String, Object>> testSteps = getTestSteps(testCaseId);
//            jsonObjectF.put("steps", testSteps);
//            jsonObjectF.put("params", mappedVariables);
//            jsonElements.add(jsonObjectF);
//        });
//        resp.setStatus(Response.Status.OK.getStatusCode());
//        Gson gson = new Gson();
//        return gson.toJson(jsonElements);
//
//    }
//
//    private void getChildren(List<TestCaseFolder> testCaseFolders1, String parentId, List<Object> jsonArray, List<Issue> issueList, HttpServletResponse resp) {
//        testCaseFolders1.forEach(caseFolder -> {
//            try {
//                if (caseFolder.getParent().equals(parentId)) {
//                    Map<String, Object> jsonObject = new HashMap<>();
//                    jsonObject.put("name", caseFolder.getName());
//                    jsonObject.put("id", caseFolder.getID());
//                    jsonObject.put("type", "TESTSUITE");
//                    List<Issue> issueByParent = getIssueByParent(issueList, String.valueOf(parentId));
//                    issueByParent.forEach(issue -> {
//                        Map<String, Object> jsonObjectF = new HashMap<>();
//                        jsonObjectF.put("name", issue.getSummary());
//                        System.out.println(issue.getSummary());
//                        jsonObjectF.put("id", issue.getId());
//                        List<Map<String, Object>> attachedScriptObject = getAttachedScriptObject(issue);
//                        jsonObjectF.put("scripts", attachedScriptObject);
//                        String testCaseId = String.valueOf(issue.getId());
//                        List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
//                        List<Map<String, Object>> testSteps = getTestSteps(testCaseId);
//                        jsonObjectF.put("steps", testSteps);
//                        jsonObjectF.put("params", mappedVariables);
//                        jsonObjectF.put("type", "TESTCASE");
//
//                        jsonArray.add(jsonObjectF);
//                    });
//
//                    List<Object> jsonElements = new ArrayList<>();
//                    getChildren(testCaseFolders1, String.valueOf(caseFolder.getID()), jsonElements, issueList, resp);
//                    //handle leaf nodes.
//                    if (jsonElements.size() == 0) {
//                        getLeafNodes(issueList, String.valueOf(caseFolder.getID()), jsonElements);
//                    }
//                    jsonObject.put("entity", jsonElements);
//                    jsonArray.add(jsonObject);
//                }
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//                resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//            }
//        });
//    }
//
//
//    private void getLeafNodes(List<Issue> issueList, String parentId, List<Object> jsonArray) {
//        List<Issue> issueByParent = getIssueByParent(issueList, String.valueOf(parentId));
//        issueByParent.forEach(issue -> {
//            Map<String, Object> jsonObjectF = new HashMap<>();
//            jsonObjectF.put("name", issue.getSummary());
//            jsonObjectF.put("id", issue.getId());
//            jsonObjectF.put("type", "TESTCASE");
//            List<Map<String, Object>> attachedScriptObject = getAttachedScriptObject(issue);
//            jsonObjectF.put("scripts", attachedScriptObject);
//            String testCaseId = String.valueOf(issue.getId());
//            List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
//            List<Map<String, Object>> testSteps = getTestSteps(testCaseId);
//            jsonObjectF.put("steps", testSteps);
//            jsonObjectF.put("params", mappedVariables);
//            jsonArray.add(jsonObjectF);
//        });
//    }
//
//    private List<Issue> getIssueByParent(List<Issue> issueList, String parentId) {
//        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
//        List<Issue> issues = new ArrayList<>();
//        issueList.forEach(issue -> {
//            if (Objects.nonNull(issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("ParentFolder").iterator().next())) && issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("ParentFolder").iterator().next()).equals(parentId)) {
//                issues.add(issue);
//            }
//        });
//        return issues;
//    }
//
//    private void getProjectList(HttpServletResponse resp) {
//        try {
//            List<Project> projects = ComponentAccessor.getProjectManager().getProjects();
//            List<JsonObject> projectList = new ArrayList<>();
//            projects.forEach(project -> {
//                        JsonObject projectObject = new JsonObject();
//                        projectObject.addProperty("name", project.getName());
//                        projectObject.addProperty("id", project.getId());
//                        projectList.add(projectObject);
//                    }
//            );
//            resp.getWriter().write(projectList.toString());
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//        }
//        resp.setStatus(Response.Status.OK.getStatusCode());
//    }
//
//    private List<Map<String, Object>> getAttachedScriptObject(Issue issue) {
//        List<Map<String, Object>> mapList = new ArrayList<>();
//        try {
//            CustomField scripts = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Scripts").iterator().next();
//            JsonArray customFieldValue = (JsonArray) issue.getCustomFieldValue(scripts);
//            if (Objects.nonNull(customFieldValue)) {
//                for (JsonElement element : customFieldValue) {
//                    JsonObject asJsonObject = element.getAsJsonObject();
//                    Map<String, Object> stringObjectMap = new HashMap<>();
//                    stringObjectMap.put("script-id", asJsonObject.get("script-id").getAsString());
//                    stringObjectMap.put("script-name", asJsonObject.get("script-name").getAsString());
//                    Map<String, Object> mappedVariables = new HashMap<>();
//                    if (asJsonObject.has("parameters")) {
//                        JsonArray asJsonArray = asJsonObject.get("parameters").getAsJsonArray();
//                        for (JsonElement jsonElement : asJsonArray) {
//                            jsonElement.getAsJsonObject().entrySet().forEach(stringJsonElementEntry -> mappedVariables.put(stringJsonElementEntry.getValue().getAsString(), stringJsonElementEntry.getKey()));
//                        }
//                    }
//                    stringObjectMap.put("mappedParams", mappedVariables);
//                    mapList.add(stringObjectMap);
//                }
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//        return mapList;
//    }
//
//    private void update(HttpServletRequest request, HttpServletResponse resp) {
//        String data = request.getHeader("data");
//        JsonElement jsonElement = new JsonParser().parse(data);
//        JsonArray jsonScriptElement = jsonElement.getAsJsonObject().get("scripts").getAsJsonArray();
//        IssueManager issueManager = ComponentAccessor.getIssueManager();
//        IssueService issueService = ComponentAccessor.getIssueService();
//        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
//        CustomField scripts = customFieldManager.getCustomFieldObjectsByName("Scripts").iterator().next();
//        //get the testcase objects from the object
//        for (JsonElement testCase : jsonScriptElement) {
//            JsonArray diffArray = new JsonArray();
//            //each object
//            JsonObject tCase = testCase.getAsJsonObject();
//            //get the testcase id from from the testcase object
//            MutableIssue issueObject = issueManager.getIssueObject(tCase.get("test-case-id").getAsLong());
//            JsonArray customFieldValue = (JsonArray) issueObject.getCustomFieldValue(scripts);
//            JsonArray jsonElementF = new JsonArray();
//            if (!(customFieldValue == null)) {
//                //parse the contents of the custom field.
//                jsonElementF = customFieldValue;
//                JsonArray customFieldValues = jsonElementF.getAsJsonArray();
//                JsonArray scriptsArray = tCase.getAsJsonArray("scripts");
//                for (JsonElement jsonElement1 : scriptsArray) {
//                    boolean[] containsScript = {false};
//                    customFieldValues.forEach(jsonElement2 -> {
//                        if (jsonElement2.equals(jsonElement1)) {
//                            containsScript[0] = true;
//                        }
//                    });
//                    if (!containsScript[0]) {
//                        jsonElementF.add(jsonElement1);
//                    }
//                }
//                diffArray.forEach(jsonElementF::add);
//            } else {
//                tCase.get("scripts").getAsJsonArray().forEach(jsonElementF::add);
//            }
//            List<JsonElement> jsonElements = new ArrayList<>();
//            jsonElementF.forEach(jsonElements::add);
//
//            ApplicationUser creator = issueObject.getCreator();
//            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
//            issueInputParameters.addCustomFieldValue(scripts.getId(), jsonElements.toString());
//            IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(creator, issueObject.getId(), issueInputParameters);
//            if (updateValidationResult.isValid()) {
//                IssueService.IssueResult updateResult = issueService.update(creator, updateValidationResult);
//                if (!updateResult.isValid()) {
//                    log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
//                    resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//                }
//            } else {
//                log.error(updateValidationResult.getErrorCollection().getErrorMessages().toString());
//                resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//            }
//        }
//        resp.setStatus(Response.Status.OK.getStatusCode());
//    }
//
//    private void remove(HttpServletRequest request, HttpServletResponse resp) {
//        String data = request.getHeader("data");
//        JsonElement jsonElement = new JsonParser().parse(data);
//        JsonArray jsonScriptElement = jsonElement.getAsJsonObject().get("scripts").getAsJsonArray();
//        IssueManager issueManager = ComponentAccessor.getIssueManager();
//        IssueService issueService = ComponentAccessor.getIssueService();
//        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
//
//        CustomField scripts = customFieldManager.getCustomFieldObjectsByName("Scripts").iterator().next();
//        //get the testcase objects from the object
//        for (JsonElement testCase : jsonScriptElement) {
//
//            JsonArray diffArray = new JsonArray();
//            //each object
//            JsonObject tCase = testCase.getAsJsonObject();
//            //get the testcase id from from the testcase object
//            MutableIssue issueObject = issueManager.getIssueObject(tCase.get("test-case-id").getAsLong());
//
//            JsonArray customFieldValue = (JsonArray) issueObject.getCustomFieldValue(scripts);
//            JsonArray jsonElementF;
//            if (customFieldValue.size() > 0) {
//                //parse the contents of the custom field.
//                jsonElementF = customFieldValue;
//                JsonArray customFieldValues = jsonElementF.getAsJsonArray();
//                JsonArray scriptsArray = tCase.getAsJsonArray("scripts");
//                for (JsonElement jsonElement1 : scriptsArray) {
//                    customFieldValues.forEach(jsonElement2 -> {
//                        if (jsonElement2.getAsJsonObject().get("script-id").getAsString().equals(jsonElement1.getAsJsonObject().get("script-id").getAsString())) {
//                            diffArray.add(jsonElement2);
//                        }
//                    });
//
//                }
//                List<JsonElement> jsonElements = new ArrayList<>();
//                jsonElementF.forEach(jsonElements::add);
//                diffArray.forEach(jsonElements::remove);
//
//                ApplicationUser creator = issueObject.getCreator();
//                IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
//                issueInputParameters.addCustomFieldValue(scripts.getId(), jsonElements.toString());
//
//                IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(creator, issueObject.getId(), issueInputParameters);
//
//                if (updateValidationResult.isValid()) {
//                    IssueService.IssueResult updateResult = issueService.update(creator, updateValidationResult);
//                    if (!updateResult.isValid()) {
//                        log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
//                        resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//                    }
//                } else {
//                    log.error(updateValidationResult.getErrorCollection().getErrorMessages().toString());
//                    resp.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//                }
//            }
//        }
//        resp.setStatus(Response.Status.OK.getStatusCode());
//    }
//
//
//    private List<Map<String, Object>> getTestSteps(String testCaseId) {
//        List<Map<String, Object>> dataList = new ArrayList<>();
//        try {
//            TSteps tSteps = new TSteps(activeObjects);
//            List<TestSteps> testSteps = tSteps.get(testCaseId);
//            testSteps.forEach(testStep -> {
//                Map<String, Object> stepInfo = new HashMap<>();
//                stepInfo.put("step_description", testStep.getStep());
//                stepInfo.put("expected_result", testStep.getExpectedResult());
//                stepInfo.put("data", testStep.getData());
//                dataList.add(stepInfo);
//            });
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//
//        return dataList;
//    }
//
//    private void updateClonedInstances(JsonObject data) {
//        JsonObject jsonElement = data.get("results").getAsJsonObject();
//        String entityid = jsonElement.get("entityid").getAsString();
//        String planinstanceid = jsonElement.get("planinstanceid").getAsString();
//        Integer tcinstanceid = jsonElement.get("tcinstanceid").getAsInt();
//        String state = jsonElement.get("state").getAsString();
//        String iterationId = null;
//        if (!jsonElement.get("IterationId").isJsonNull()) {
//            iterationId = jsonElement.get("IterationId").getAsString();
//        }
//
//        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
//        List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanTestCaseInstance.getTCaseInstance(tcinstanceid);
//        EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);
//        TCaseDataIteration[] testCaseDataIterations = eEnityPlanTestCaseDataIteration.getTestCaseDataIterations(String.valueOf(tcinstanceid));
//
//        TStepsInstance tStepsInstance = new TStepsInstance(activeObjects);
//
//        if (iterationId != null) {
//            boolean hasIterationPending = false;
//            boolean hasIterationFailed = false;
//            updateTCaseDataIteration(state, iterationId, testCaseDataIterations);
//
//            for (TCaseDataIteration testCaseDataIteration : testCaseDataIterations) {
//                String overallStatus = testCaseDataIteration.getStatus();
//                if (overallStatus.toLowerCase(Locale.ENGLISH).equals("in progress")) {
//                    hasIterationPending = true;
//                } else if (overallStatus.toLowerCase(Locale.ENGLISH).equals("failed")) {
//                    hasIterationFailed = true;
//                }
//            }
//
//            iterationExecEntityTPTCIUpdateStatus(execEntityTPTCIS, tStepsInstance, hasIterationPending, hasIterationFailed);
//
//        } else {
//            execEntityTPTCIStatusUpdate(state, execEntityTPTCIS, tStepsInstance);
//        }
//
//        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstanceNew = new EEntityPlanTestCaseInstance(activeObjects);
//        List<ExecEntityTPTCI> testCasesForTPlan = eEntityPlanTestCaseInstanceNew.getTestCasesForTPlan(planinstanceid);
//        boolean haspending = false;
//        boolean hasFailed = false;
//        for (ExecEntityTPTCI execEntityTPTCI : testCasesForTPlan) {
//            boolean executed = execEntityTPTCI.isExecuted();
//            if (executed) {
//                String overallStatus = execEntityTPTCI.getOverallStatus();
//                if (overallStatus.toLowerCase(Locale.ENGLISH).equals("in progress")) {
//                    haspending = true;
//                } else if (overallStatus.toLowerCase(Locale.ENGLISH).equals("failed")) {
//                    hasFailed = true;
//                }
//            } else {
//                haspending = true;
//            }
//        }
//
//        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
//        List<ExecEntityTPI> execEntityTPIS = eEntityPlanInstance.getEEntityTPI(planinstanceid);
//        updateExecEntityTPIStatus(haspending, hasFailed, execEntityTPIS);
//
//        boolean entityInstanceHasPending = getExecEntityInstanceStatus(entityid, eEntityPlanInstance);
//
//        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
//        List<ExecEntityI> byInsatanceId = eEntityInstance.getByInsatanceId(entityid);
//
//        updateExecEntityInstanceStatus(entityInstanceHasPending, byInsatanceId);
//    }
//
//    private void updateExecEntityTPIStatus(boolean haspending, boolean hasFailed, List<ExecEntityTPI> execEntityTPIS) {
//        if (!hasFailed && !haspending) {
//            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
//                execEntityTPI.setStatus(EEntityTPStatus.COMPLETED.value());
//                execEntityTPI.save();
//            }
//
//        } else if (hasFailed && !haspending) {
//            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
//                execEntityTPI.setStatus(EEntityTPStatus.COMPLETED.value());
//                execEntityTPI.save();
//            }
//        }
//    }
//
//    private void updateExecEntityInstanceStatus(boolean entityInstanceHasPending, List<ExecEntityI> byInsatanceId) {
//        if (!entityInstanceHasPending) {
//            for (ExecEntityI execEntityI : byInsatanceId) {
//                execEntityI.setStatus(EEntityStatus.COMPLETED.value());
//                execEntityI.save();
//            }
//        } else {
//            for (ExecEntityI execEntityI : byInsatanceId) {
//                execEntityI.setStatus(EEntityStatus.IN_PROGRESS.value());
//                execEntityI.save();
//            }
//        }
//    }
//
//    private boolean getExecEntityInstanceStatus(String entityid, EEntityPlanInstance eEntityPlanInstance) {
//        boolean entityInstanceHasPending = false;
//        List<ExecEntityTPI> eEntityTPIByEntityInstance = eEntityPlanInstance.getEEntityTPIByEntityInstance(entityid);
//        for (ExecEntityTPI execEntityI : eEntityTPIByEntityInstance) {
//            String status = execEntityI.getStatus();
//            if (status.toLowerCase(Locale.ENGLISH).equals("in progress")) {
//                entityInstanceHasPending = true;
//            }
//        }
//        return entityInstanceHasPending;
//    }
//
//    private void updateTCaseDataIteration(String state, String iterationId, TCaseDataIteration[] testCaseDataIterations) {
//        for (TCaseDataIteration testCaseDataIteration : testCaseDataIterations) {
//            if (Objects.equals(String.valueOf(testCaseDataIteration.getID()), iterationId)) {
//                testCaseDataIteration.setStatus(state);
//                testCaseDataIteration.save();
//            }
//        }
//    }
//
//    private void iterationExecEntityTPTCIUpdateStatus(List<ExecEntityTPTCI> execEntityTPTCIS, TStepsInstance tStepsInstance, boolean hasIterationPending, boolean hasIterationfailed) {
//        for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
//            if (!hasIterationPending && !hasIterationfailed) {
//                execEntityTPTCI.setExecuted(true);
//                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.PASS.value());
//                execEntityTPTCI.save();
//                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
//                for (TestStepsI testStepsI : testStepsIS) {
//                    testStepsI.setActualStatus(EEntityTPTCDIStatus.PASS.value());
//                    testStepsI.save();
//                }
//            } else if (!hasIterationPending) {
//                execEntityTPTCI.setExecuted(true);
//                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.FAILED.value());
//                execEntityTPTCI.save();
//                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
//                for (TestStepsI testStepsI : testStepsIS) {
//                    testStepsI.setActualStatus(EEntityTPTCDIStatus.FAILED.value());
//                    testStepsI.save();
//                }
//            }
//        }
//    }
//
//    private void execEntityTPTCIStatusUpdate(String state, List<ExecEntityTPTCI> execEntityTPTCIS, TStepsInstance tStepsInstance) {
//        for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
//            if (state.toLowerCase(Locale.ENGLISH).equals("failed")) {
//                execEntityTPTCI.setExecuted(true);
//                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.FAILED.value());
//                execEntityTPTCI.save();
//                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
//                for (TestStepsI testStepsI : testStepsIS) {
//                    testStepsI.setActualStatus(EEntityTPTCDIStatus.PASS.value());
//                    testStepsI.save();
//                }
//            } else {
//                execEntityTPTCI.setExecuted(true);
//                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.PASS.value());
//                execEntityTPTCI.save();
//                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
//                for (TestStepsI testStepsI : testStepsIS) {
//                    testStepsI.setActualStatus(EEntityTPTCDIStatus.FAILED.value());
//                    testStepsI.save();
//                }
//            }
//        }
//    }
//
//    private String getAIRCI(HttpServletRequest request) {
//        String result;
//        try {
//            JSONObject data = new JSONObject();
//            data.put("key", "requirement");
//            data.put("content", StringEscapeUtils.escapeJava(request.getParameter("description")));
//
//            URL url = new URL("http://localhost:5000/api/v1/clarity-index");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setDoOutput(true);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json");
//
//            OutputStream os = conn.getOutputStream();
//            os.write(data.toString().getBytes(Charset.forName("UTF-8")));
//            os.flush();
//
//            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
//                throw new RuntimeException("Failed : HTTP error code : "
//                        + conn.getResponseCode());
//            }
//            InputStream inputStream = conn.getInputStream();
//            log.info("Output from Server .... \n");
//            result = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
//            IOUtils.closeQuietly(inputStream);
//            os.close();
//            conn.disconnect();
//            JsonObject parsedData = new JsonParser().parse(result).getAsJsonObject();
//            if (Objects.nonNull(parsedData)) {
//                return parsedData.get("clarity-index").getAsJsonArray().get(0).toString();
//            }
//        } catch (IOException | JSONException e) {
//            log.error(e.getMessage(), e);
//        }
//        return "1";
//
//    }
}

