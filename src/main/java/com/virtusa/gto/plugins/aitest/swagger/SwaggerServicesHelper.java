package com.virtusa.gto.plugins.aitest.swagger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.util.CommonsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwaggerServicesHelper {

    private static final Logger log = LoggerFactory.getLogger(SwaggerServicesHelper.class);

    @JiraImport
    private ActiveObjects activeObjects;

    public SwaggerServicesHelper(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public SwaggerServicesHelper() {
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

    JsonElement toJsonElement(String data) {
        return new JsonParser().parse(data);
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

    public void removeDuplicates(String selectedProjectKey, TestStepParam testStepParam, TestStepParamValue testStepParamValue, String testCaseId, List<String> oldParamList, List<String> newParamList) {
        List<String> removedParams = new ArrayList<>();
        List<String> newParams = new ArrayList<>();
        oldParamList.forEach(oldparam -> {
            if (!newParamList.contains(oldparam)) {
                removedParams.add(oldparam);
            }
        });

        newParamList.forEach(newparam -> {
            if (!oldParamList.contains(newparam)) {
                newParams.add(newparam);
            }
        });

        removedParams.forEach(oldpramremove -> {
            testStepParamValue.delete(testCaseId, oldpramremove);
            testStepParam.delete(testCaseId, oldpramremove);
        });

        newParams.forEach(newPram -> testStepParam.insertSingleParam(testCaseId, newPram, selectedProjectKey));
    }

    public String getValue(String key, JsonObject stepObject) {
        return stepObject.has(key) ? stepObject.get(key).getAsString() : "";
    }

    public void getUpsertedSteps(JSONArray resultArray, BComponentStep bComponentStep, List<String> newParamList, JSONArray stepParamArray, String dataString, String scriptIdString, TestSteps stepUpserted) {
        String regex = "\\{\\{[a-zA-Z0-9]*}}";
        Pattern pattern = Pattern.compile(regex);
        if (Objects.nonNull(scriptIdString) && scriptIdString.length() > 0) {
            BusComSteps[] steps1 = bComponentStep.getSteps(scriptIdString);
            for (BusComSteps busComSteps : steps1) {
                dataString = busComSteps.getDataParams();
                if (Objects.nonNull(dataString) && dataString.length() > 0) {
                    JsonArray params = getParams(pattern, dataString, newParamList);
                    stepParamArray.put(params);
                }
            }
        } else {
            JsonArray params = getParams(pattern, dataString, newParamList);
            stepParamArray.put(params);
        }
        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("id", stepUpserted.getID());
            returnObject.put("step", stepUpserted.getStep());
            returnObject.put("testCaseId", stepUpserted.getTestCaseId());
            returnObject.put("expectedResult", stepUpserted.getExpectedResult());
            returnObject.put("data", stepUpserted.getData());
            returnObject.put("functionId", stepUpserted.getScriptId());
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
        resultArray.put(returnObject);
    }

    private JsonArray getParams(Pattern pattern, String dataString, List<String> newParamList) {
        Matcher matcher = pattern.matcher(dataString);
        JsonArray jsonElements = new JsonArray();
        while (matcher.find()) {
            JsonObject paramJsonObject = new JsonObject();
            String paramName = matcher.group(0).replace("{{", "").replace("}}", "");
            paramJsonObject.addProperty("data", dataString);
            paramJsonObject.addProperty("name", paramName);
            jsonElements.add(paramJsonObject);
            newParamList.add(paramName);
        }
        return jsonElements;
    }

    public void recursiveSearch(String parent, List<BusComScript> scriptForRemove, List<BusComFolder> folderForRemove, String projectKey) {
        BComponentFolder businessComponentFolder = new BComponentFolder(activeObjects);
        BComponentScript businessScript = new BComponentScript(activeObjects);
        List<BusComFolder> folders = businessComponentFolder.get(projectKey, parent);
        for (BusComFolder busComFolder : folders) {
            folderForRemove.add(busComFolder);
            List<BusComScript> scripts = businessScript.getAll(projectKey, Long.toString(busComFolder.getID()));
            scriptForRemove.addAll(scripts);
            recursiveSearch(Long.toString(busComFolder.getID()), scriptForRemove, folderForRemove, projectKey);
        }
    }

    public String getBusinessFunctions(String parent, String selectedProjectKey) {
        JSONArray businessScriptsArray = new JSONArray();
        BComponentScript businessScript = new BComponentScript(activeObjects);
        try {
            String parentValue;
            if (parent.equals("null")) {
                parentValue = "0";
            } else {
                parentValue = parent;
            }
            List<BusComScript> scripts = businessScript.getAll(selectedProjectKey, parentValue);
            for (BusComScript bcs : scripts) {
                JSONObject returnObject = new JSONObject();
                returnObject.put("id", bcs.getID());
                returnObject.put("name", bcs.getName());
                returnObject.put("parent", bcs.getParent());
                businessScriptsArray.put(returnObject);
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return businessScriptsArray.toString();
    }

    public void readFolderInfo(String folderId, ArrayList<Integer> foldersID, String projectKey) {
        TCFolder tcFolder = new TCFolder(activeObjects);
        List<TestCaseFolder> folders = tcFolder.get(projectKey, folderId);
        for (TestCaseFolder folder : folders) {
            foldersID.add(folder.getID());
            readFolderInfo(Integer.toString(folder.getID()), foldersID, projectKey);
        }
    }

    public boolean delete(String entity, String projectKey, String folderId) {
        if (entity.toLowerCase(Locale.ROOT).equals(TestCaseFolder.class.getSimpleName().toLowerCase(Locale.ROOT))) {
            TCFolder tcFolder = new TCFolder(activeObjects);
            return tcFolder.delete(projectKey, folderId);
        }
        return false;
    }

    public void createUserStoryObj(JSONArray returnEntityPlanTCArray, ExecEntityTPTCI execEntityTPTCI, JSONObject returnPlanTCObject) throws JSONException {
        returnPlanTCObject.put("id", execEntityTPTCI.getID());
        returnPlanTCObject.put("execEntityTPTC", execEntityTPTCI.getExecEntityTPTC());
        returnPlanTCObject.put("executionEntityTestPlanId", execEntityTPTCI.getExecutionEntityTestPlanId());
        returnPlanTCObject.put("testCaseId", execEntityTPTCI.getTestCaseId());
        returnPlanTCObject.put("testCaseName", execEntityTPTCI.getTestCaseName());
        returnPlanTCObject.put("testCaseAutomated", execEntityTPTCI.getTestCaseAutomated());
        returnPlanTCObject.put("testCaseManual", execEntityTPTCI.getTestCaseManual());
        returnPlanTCObject.put("testCaseAgent", execEntityTPTCI.getTestCaseAgent());
        returnPlanTCObject.put("testCaseUser", execEntityTPTCI.getTestCaseUser());
        returnPlanTCObject.put("actualResult", execEntityTPTCI.getActualResult());
        returnPlanTCObject.put("isExecuted", execEntityTPTCI.isExecuted());
        returnPlanTCObject.put("overallExpectedResult", execEntityTPTCI.getOverallExpectedResult());
        returnPlanTCObject.put("cycle", execEntityTPTCI.getCycle());
        returnPlanTCObject.put("userStoryId", execEntityTPTCI.getUserStoryId());
        String overallStatus = Objects.nonNull(execEntityTPTCI.getOverallStatus()) ? execEntityTPTCI.getOverallStatus() : EEntityTPTCStatus.IN_PROGRESS.value();
        returnPlanTCObject.put("overallStatus", overallStatus);
        returnEntityPlanTCArray.put(returnPlanTCObject);
    }

    public String getSavedDefects(String tCIMyTaskId) {
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        JSONArray savedDefectsArray = new JSONArray();
        try {
            List<DefectsEntityI> defectsSavedList = defectsEntity.getDefectsSaved(tCIMyTaskId);
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
}
