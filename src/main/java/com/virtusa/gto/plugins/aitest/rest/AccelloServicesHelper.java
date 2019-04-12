package com.virtusa.gto.plugins.aitest.rest;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.virtusa.gto.plugins.aitest.db.EEntityInstance;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanInstance;
import com.virtusa.gto.plugins.aitest.db.TSteps;
import com.virtusa.gto.plugins.aitest.db.TStepsInstance;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.util.CommonsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.*;

public class AccelloServicesHelper {

    private static final Logger log = LoggerFactory.getLogger(AccelloServicesHelper.class);

    @JiraImport
    private ActiveObjects activeObjects;

    @Inject
    public AccelloServicesHelper(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public AccelloServicesHelper() {
    }

    private void getChildrensData(List<TestCaseFolder> testCaseFolders1, String parentId, List<Object> jsonArray, List<Issue> issueList) {
        testCaseFolders1.forEach(caseFolder -> {
            try {
                if (caseFolder.getParent().equals(parentId)) {
                    Map<String, Object> jsonObject = new HashMap<>();
                    jsonObject.put("name", caseFolder.getName());
                    jsonObject.put("id", caseFolder.getID());
                    jsonObject.put("type", "TESTSUITE");
                    List<Issue> issueByParent = getIssueByParent(issueList, String.valueOf(parentId));
                    issueByParent.forEach(issue -> {
                        Map<String, Object> jsonObjectF = new HashMap<>();
                        jsonObjectF.put("name", issue.getSummary());
                        log.info(issue.getSummary());
                        jsonObjectF.put("id", issue.getId());
                        List<Map<String, Object>> attachedScriptObject = getAttachedScriptObject(issue);
                        jsonObjectF.put("scripts", attachedScriptObject);
                        String testCaseId = String.valueOf(issue.getId());
                        List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
                        List<Map<String, Object>> testSteps = getTestSteps(testCaseId);
                        jsonObjectF.put("steps", testSteps);
                        jsonObjectF.put("params", mappedVariables);
                        jsonObjectF.put("type", "TESTCASE");

                        jsonArray.add(jsonObjectF);
                    });

                    List<Object> jsonElements = new ArrayList<>();
                    getChildren(testCaseFolders1, String.valueOf(caseFolder.getID()), jsonElements, issueList);
                    //handle leaf nodes.
                    if (jsonElements.size() == 0) {
                        getLeafNodes(issueList, String.valueOf(caseFolder.getID()), jsonElements);
                    }
                    jsonObject.put("entity", jsonElements);
                    jsonArray.add(jsonObject);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            }
        });
    }

    List<Issue> getIssueByParent(List<Issue> issueList, String parentId) {
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        List<Issue> issues = new ArrayList<>();
        issueList.forEach(issue -> {
            if (Objects.nonNull(issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("ParentFolder").iterator().next())) && issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("ParentFolder").iterator().next()).equals(parentId)) {
                issues.add(issue);
            }
        });
        return issues;
    }

    List<Map<String, Object>> getAttachedScriptObject(Issue issue) {
        List<Map<String, Object>> mapList = new ArrayList<>();

        try {
            CustomField scripts = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Scripts").iterator().next();
            JsonArray customFieldValue = (JsonArray) issue.getCustomFieldValue(scripts);

            if (Objects.nonNull(customFieldValue)) {

                for (JsonElement element : customFieldValue) {
                    JsonObject asJsonObject = element.getAsJsonObject();
                    Map<String, Object> stringObjectMap = new HashMap<>();

                    stringObjectMap.put("script-id", asJsonObject.get("script-id").getAsString());
                    stringObjectMap.put("script-name", asJsonObject.get("script-name").getAsString());
                    Map<String, Object> mappedVariables = new HashMap<>();
                    if (asJsonObject.has("parameters")) {
                        JsonArray asJsonArray = asJsonObject.get("parameters").getAsJsonArray();
                        for (JsonElement jsonElement : asJsonArray) {
                            jsonElement.getAsJsonObject().entrySet().forEach(stringJsonElementEntry -> mappedVariables.put(stringJsonElementEntry.getValue().getAsString(), stringJsonElementEntry.getKey()));
                        }
                    }
                    stringObjectMap.put("mappedParams", mappedVariables);
                    mapList.add(stringObjectMap);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return mapList;
    }


    List<Map<String, Object>> getTestSteps(String testCaseId) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        try {
            TSteps tSteps = new TSteps(activeObjects);
            List<TestSteps> testSteps = tSteps.get(testCaseId);
            testSteps.forEach(testStep -> {
                Map<String, Object> stepInfo = new HashMap<>();
                stepInfo.put("step_description", testStep.getStep());
                stepInfo.put("expected_result", testStep.getExpectedResult());
                stepInfo.put("data", testStep.getData());
                dataList.add(stepInfo);
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return dataList;
    }

    void getChildren(List<TestCaseFolder> testCaseFolders1, String parentId, List<Object> jsonArray, List<Issue> issueList) {
        getChildrensData(testCaseFolders1, parentId, jsonArray, issueList);
    }

    void getLeafNodes(List<Issue> issueList, String parentId, List<Object> jsonArray) {
        List<Issue> issueByParent = getIssueByParent(issueList, String.valueOf(parentId));
        issueByParent.forEach(issue -> {
            Map<String, Object> jsonObjectF = new HashMap<>();
            jsonObjectF.put("name", issue.getSummary());
            jsonObjectF.put("id", issue.getId());
            jsonObjectF.put("type", "TESTCASE");
            List<Map<String, Object>> attachedScriptObject = getAttachedScriptObject(issue);
            jsonObjectF.put("scripts", attachedScriptObject);
            String testCaseId = String.valueOf(issue.getId());
            List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
            List<Map<String, Object>> testSteps = getTestSteps(testCaseId);
            jsonObjectF.put("steps", testSteps);
            jsonObjectF.put("params", mappedVariables);
            jsonArray.add(jsonObjectF);
        });
    }

    private void handleIterationNonNullExecEntityTPTCI(List<ExecEntityTPTCI> execEntityTPTCIS, TStepsInstance tStepsInstance, boolean hasIterationPending, boolean hasIterationFailed) {
        for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
            if (!hasIterationPending && !hasIterationFailed) {
                execEntityTPTCI.setExecuted(true);
                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.PASS.value());
                execEntityTPTCI.save();
                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
                for (TestStepsI testStepsI : testStepsIS) {
                    testStepsI.setActualStatus(EEntityTPTCDIStatus.PASS.value());
                    testStepsI.save();
                }
            } else if (!hasIterationPending) {
                execEntityTPTCI.setExecuted(true);
                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.FAILED.value());
                execEntityTPTCI.save();
                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
                for (TestStepsI testStepsI : testStepsIS) {
                    testStepsI.setActualStatus(EEntityTPTCDIStatus.FAILED.value());
                    testStepsI.save();
                }
            }
        }
    }

    List<Issue> getIssues(Long projectId) {
        try {
            IssueManager issueMgr = ComponentAccessor.getIssueManager();
            List<Issue> searchResults = new ArrayList<>();
            issueMgr.getIssueObjects(issueMgr.getIssueIdsForProject(projectId)).forEach(issue -> {
                if (Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ROOT).equals("testcase".toLowerCase(Locale.ROOT))) {
                    searchResults.add(issue);
                }
            });
            return searchResults;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    void updateInstance(EEntityInstance eEntityInstance, String entityId, boolean entityInstanceHasPending) {
        List<ExecEntityI> byInstanceId = eEntityInstance.getByInsatanceId(entityId);
        if (!entityInstanceHasPending) {
            for (ExecEntityI execEntityI : byInstanceId) {
                execEntityI.setStatus(EEntityStatus.COMPLETED.value());
                execEntityI.save();
            }
        } else {
            for (ExecEntityI execEntityI : byInstanceId) {
                execEntityI.setStatus(EEntityStatus.IN_PROGRESS.value());
                execEntityI.save();
            }
        }
    }

    void handleTestPlanInstance(EEntityPlanInstance eEntityPlanInstance, String planInstanceId, boolean hasFailed, boolean hasPending) {
        List<ExecEntityTPI> execEntityTPIS = eEntityPlanInstance.getEEntityTPI(planInstanceId);
        if (!hasFailed && !hasPending) {
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                execEntityTPI.setStatus(EEntityTPStatus.COMPLETED.value());
                execEntityTPI.save();
            }
        } else if (hasFailed && !hasPending) {
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                execEntityTPI.setStatus(EEntityTPStatus.COMPLETED.value());
                execEntityTPI.save();
            }
        } else if (!hasFailed){
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                execEntityTPI.setStatus(EEntityTPStatus.IN_PROGRESS.value());
                execEntityTPI.save();
            }
        }
    }

    void handleIterationNull(List<ExecEntityTPTCI> execEntityTPTCIS, String state, TStepsInstance tStepsInstance) {
        for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
            if (state.toLowerCase(Locale.ROOT).equals("failed")) {
                execEntityTPTCI.setExecuted(true);
                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.FAILED.value());
                execEntityTPTCI.save();
                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
                for (TestStepsI testStepsI : testStepsIS) {
                    testStepsI.setActualStatus(EEntityTPTCDIStatus.PASS.value());
                    testStepsI.save();
                }
            } else {
                execEntityTPTCI.setExecuted(true);
                execEntityTPTCI.setOverallStatus(EEntityTPTCDIStatus.PASS.value());
                execEntityTPTCI.save();
                List<TestStepsI> testStepsIS = tStepsInstance.get(String.valueOf(execEntityTPTCI.getID()));
                for (TestStepsI testStepsI : testStepsIS) {
                    testStepsI.setActualStatus(EEntityTPTCDIStatus.FAILED.value());
                    testStepsI.save();
                }
            }
        }
    }

    void handleIterationNonNull(TCaseDataIteration[] testCaseDataIterations, String iterationId, String state, List<ExecEntityTPTCI> execEntityTPTCIS, TStepsInstance tStepsInstance) {
        boolean hasIterationPending = false;
        boolean hasIterationFailed = false;
        for (TCaseDataIteration testCaseDataIteration : testCaseDataIterations) {
            if (Objects.equals(String.valueOf(testCaseDataIteration.getID()), iterationId)) {
                if (state.equalsIgnoreCase("Passed") || state.toLowerCase(Locale.ENGLISH).contains("pass")) {
                    testCaseDataIteration.setStatus("Pass");
                } else {
                    testCaseDataIteration.setStatus(state);
                }
                testCaseDataIteration.save();
            }
        }
        for (TCaseDataIteration testCaseDataIteration : testCaseDataIterations) {
            String overallStatus = testCaseDataIteration.getStatus();
            if (overallStatus.toLowerCase(Locale.ROOT).equals("in progress")) {
                hasIterationPending = true;
            } else if (overallStatus.toLowerCase(Locale.ROOT).equals("failed")) {
                hasIterationFailed = true;
            }
        }
        new AccelloServicesHelper(activeObjects).handleIterationNonNullExecEntityTPTCI(execEntityTPTCIS, tStepsInstance, hasIterationPending, hasIterationFailed);
    }

    Response handleUpdateValidation(IssueService.UpdateValidationResult updateValidationResult, IssueService issueService, ApplicationUser creator) {
        if (updateValidationResult.isValid()) {
            IssueService.IssueResult updateResult = issueService.update(creator, updateValidationResult);
            if (!updateResult.isValid()) {
                log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
            }
        } else {
            log.error(updateValidationResult.getErrorCollection().getErrorMessages().toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
        }
        return null;
    }

}
