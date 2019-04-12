package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutionEntityDataManagement {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEntityDataManagement.class);

    public JSONArray createManualTestCaseVariables(int paramCount, int maxLength, JSONObject paramObject) {
        JSONArray finalJsonArray = new JSONArray();
        try {
            if (paramCount > 0) {
                int[] index = {0};
                for (int j = 0; j < maxLength; j++) {
                    JSONObject dataRow = new JSONObject();
                    index[0] = j;
                    for (int i = 0; i < paramCount; i++) {
                        paramObject.keys().forEachRemaining(key -> {
                            try {
                                JSONArray jsonArray = paramObject.getJSONArray(key);
                                if (jsonArray.length() > 0) {
                                    dataRow.put(key, jsonArray.getString(index[0]));
                                }
                            } catch (JSONException e) {
                                log.error(e.getMessage(), e);
                            }
                        });
                    }
                    if (dataRow.length() > 0) {
                        JSONObject object = new JSONObject();
                        object.put("data", dataRow);
                        finalJsonArray.put(object);
                    }
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return finalJsonArray;
    }

    public String getStepDataList(TestSteps step, BusComSteps busComSteps, String data, JsonObject jsonObject) {
        JsonObject object = jsonObject.get("data").getAsJsonObject();
        String stepData = Objects.nonNull(step) ? step.getData() : busComSteps.getDataParams();
        if (Objects.nonNull(stepData)) {
            String regex = "\\{\\{[a-zA-Z0-9]*}}";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(stepData);
            StringBuilder dataBuilder = new StringBuilder(data);
            while (matcher.find()) {
                String paramName = matcher.group(0).replace("{{", "").replace("}}", "");
                if (object.has(paramName)) {
                    dataBuilder.append(" ").append(" ").append(object.get(paramName).getAsString());
                } else {
                    dataBuilder.append(" ");
                }
            }
            data = dataBuilder.toString();
        }
        return data;
    }

    public JSONArray getMappedParameters(int execEntityTPTCI, String testCaseId, ActiveObjects activeObjects) {
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

            CustomField scripts = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Scripts").iterator().next();
            MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(testCaseId));
            JsonArray customFieldValue = (JsonArray) issueObject.getCustomFieldValue(scripts);

            if (Objects.nonNull(customFieldValue)) {
                for (JsonElement jsonElement : customFieldValue) {
                    JsonObject jsonElementAsJsonObject = jsonElement.getAsJsonObject();
                    JSONObject testData = new JSONObject();
                    if (jsonElementAsJsonObject.has("script-id") && jsonElementAsJsonObject.has("script-name")) {
                        testData.put("scriptId", jsonElementAsJsonObject.get("script-id").getAsString());
                        testData.put("scriptName", jsonElementAsJsonObject.get("script-name").getAsString());
                        JSONArray finalDataArray = new JSONArray();
                        EEntityPlanTestCaseDataInstance eEntityPlanTestCaseDataInstance = new EEntityPlanTestCaseDataInstance(activeObjects);
                        TCaseDataInstance testCaseDataInstance = eEntityPlanTestCaseDataInstance.insert(execEntityTPTCI);
                        createMappedVariable(testCaseDataInstance, finalDataArray, paramObject, maxLength, paramCount, activeObjects);
                        testData.put("dataRows", finalDataArray);
                    }
                    finalJsonArray.put(testData);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return finalJsonArray;
    }

    public void createMappedVariable(TCaseDataInstance testCaseDataInstance, JSONArray finalArray, JSONObject data, int maxLength, int paramCount, ActiveObjects activeObjects) {
        try {
            if (paramCount > 0) {
                int[] index = {0};
                for (int j = 0; j < maxLength; j++) {
                    JSONObject dataRow = new JSONObject();
                    index[0] = j;
                    for (int i = 0; i < paramCount; i++) {
                        data.keys().forEachRemaining(key -> {
                            try {
                                //value array
                                JSONArray jsonArray = data.getJSONArray(key);
                                //add it to the new object
                                if (jsonArray.length() > 0) {
                                    dataRow.put(key, jsonArray.getString(index[0]));
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage());
                            }
                        });
                    }

                    if (dataRow.length() > 0) {
                        EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);
                        TCaseDataIteration tCaseDataIteration = eEnityPlanTestCaseDataIteration.insertTestCaseDataIteration(testCaseDataInstance, dataRow.toString());

                        JSONObject object = new JSONObject();
                        object.put("dataIterationId", tCaseDataIteration.getID());
                        object.put("data", dataRow);
                        finalArray.put(object);
                    }
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void addToObject(TestStepsI testStepsI, JSONArray stepsArray) {

        JSONObject stepObject = new JSONObject();
        try {
            stepObject.put("id", testStepsI.getID());
            stepObject.put("execEntityTPTCI", testStepsI.getExecEntityTPTCI());
            stepObject.put("testCaseId", testStepsI.getTestCaseId());
            stepObject.put("expectedResult", testStepsI.getExpectedResult());
            stepObject.put("step", testStepsI.getStep());
            stepObject.put("data", testStepsI.getData());
            stepObject.put("iterationId", testStepsI.getDataIterationId());
            stepObject.put("actualResult", testStepsI.getActualResult());
            stepObject.put("actualStatus", testStepsI.getActualStatus());
            stepsArray.put(stepObject);

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    public JSONArray cloneTestSteps(String testCaseId, int execEntityTPTCI, boolean isManual, String iterationId, JsonObject rowDataObj, ActiveObjects activeObjects) {
        JSONArray stepsArray = new JSONArray();
        TSteps tSteps = new TSteps(activeObjects);
        TStepsInstance tStepsInstance = new TStepsInstance(activeObjects);
        BComponentStep bComponentStep = new BComponentStep(activeObjects);
        List<TestSteps> steps = tSteps.get(testCaseId);
        for (TestSteps step : steps) {
            TestStepsI testStepsI;
            String bcId = step.getScriptId();
            if (Objects.nonNull(bcId) && !bcId.isEmpty()) {
                BusComSteps[] steps1 = bComponentStep.getSteps(bcId);
                for (BusComSteps aSteps1 : steps1) {
                    if (isManual && Objects.nonNull(iterationId)) {
                        String data = "";
                        String dataIteration = getStepDataList(null, aSteps1, data, rowDataObj);
                        aSteps1.setDataParams(dataIteration);
                        testStepsI = tStepsInstance.insertBCSteps(aSteps1, step.getProjectKey(), step.getTestCaseId(), String.valueOf(execEntityTPTCI), iterationId);
                    } else {
                        testStepsI = tStepsInstance.insertBCSteps(aSteps1, step.getProjectKey(), step.getTestCaseId(), String.valueOf(execEntityTPTCI), String.valueOf(execEntityTPTCI));
                    }
                    addToObject(testStepsI, stepsArray);
                }
            } else {
                if (isManual && Objects.nonNull(iterationId)) {
                    String data = "";
                    String dataIteration = getStepDataList(step, null, data, rowDataObj);
                    step.setData(dataIteration);
                    testStepsI = tStepsInstance.insert(step, String.valueOf(execEntityTPTCI), iterationId);
                } else {
                    testStepsI = tStepsInstance.insert(step, String.valueOf(execEntityTPTCI));
                }
                addToObject(testStepsI, stepsArray);
            }
        }
        return stepsArray;
    }

    public void cloneAutomatedTestCase(EEntityInstance eEntityInstance, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, int eEntityId, String release, JSONArray resultTestCaseArray, ExecEntityTPI executionEntityPlanInserted, JSONObject resultTestCaseObject, int execEntityTPTC, String testCaseId, String testCaseName, String testCaseAutomated, String testCaseBrowser, String testCaseAgent, boolean isPoolAgent, String testCaseUser, String expectedResult, Issue issueObj, ActiveObjects activeObjects) throws JSONException {
        if (!Objects.nonNull(issueObj)) {
            ExecEntityTPTCI execEntityTPTCInserted = eEntityPlanTestCaseInstance.insert(eEntityId, execEntityTPTC, executionEntityPlanInserted.getID(), testCaseId, testCaseName, testCaseAutomated, "", isPoolAgent, testCaseAgent, testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser, false, "", "");
            resultTestCaseObject.put("id", execEntityTPTCInserted.getID());
            resultTestCaseObject.put("execEntityTPTC", execEntityTPTCInserted.getExecEntityTPTC());
            resultTestCaseObject.put("executionEntityTestPlanId", execEntityTPTCInserted.getExecutionEntityTestPlanId());
            resultTestCaseObject.put("testCaseId", execEntityTPTCInserted.getTestCaseId());
            resultTestCaseObject.put("testCaseName", execEntityTPTCInserted.getTestCaseName());
            resultTestCaseObject.put("automated", execEntityTPTCInserted.getTestCaseAutomated());
            resultTestCaseObject.put("manual", execEntityTPTCInserted.getTestCaseManual());
            resultTestCaseObject.put("agent", execEntityTPTCInserted.getTestCaseAgent());
            resultTestCaseObject.put("user", execEntityTPTCInserted.getTestCaseUser());
            resultTestCaseObject.put("isPoolAgent", execEntityTPTCInserted.isPoolAgent());
            resultTestCaseObject.put("overallExpectedResult", execEntityTPTCInserted.getOverallExpectedResult());
            resultTestCaseObject.put("overallStatus", execEntityTPTCInserted.getOverallStatus());
            resultTestCaseObject.put("testData", getMappedParameters(execEntityTPTCInserted.getID(), testCaseId, activeObjects));
            resultTestCaseObject.put("steps", cloneTestSteps(testCaseId, execEntityTPTCInserted.getID(), false, null, null, activeObjects));
            resultTestCaseObject.put("testCaseBrowser", execEntityTPTCInserted.getTestCaseBrowser());
            resultTestCaseArray.put(resultTestCaseObject);
        } else {
            ExecEntityTPTCI execEntityTPTCInserted = eEntityPlanTestCaseInstance.insert(eEntityId, execEntityTPTC, executionEntityPlanInserted.getID(), testCaseId, testCaseName, testCaseAutomated, "", isPoolAgent, testCaseAgent, testCaseUser, expectedResult, eEntityInstance.getCycle(release), testCaseBrowser, false, String.valueOf(issueObj.getId()), issueObj.getSummary());
            resultTestCaseObject.put("id", execEntityTPTCInserted.getID());
            resultTestCaseObject.put("execEntityTPTC", execEntityTPTCInserted.getExecEntityTPTC());
            resultTestCaseObject.put("executionEntityTestPlanId", execEntityTPTCInserted.getExecutionEntityTestPlanId());
            resultTestCaseObject.put("testCaseId", execEntityTPTCInserted.getTestCaseId());
            resultTestCaseObject.put("testCaseName", execEntityTPTCInserted.getTestCaseName());
            resultTestCaseObject.put("automated", execEntityTPTCInserted.getTestCaseAutomated());
            resultTestCaseObject.put("manual", execEntityTPTCInserted.getTestCaseManual());
            resultTestCaseObject.put("agent", execEntityTPTCInserted.getTestCaseAgent());
            resultTestCaseObject.put("user", execEntityTPTCInserted.getTestCaseUser());
            resultTestCaseObject.put("isPoolAgent", execEntityTPTCInserted.isPoolAgent());
            resultTestCaseObject.put("overallExpectedResult", execEntityTPTCInserted.getOverallExpectedResult());
            resultTestCaseObject.put("overallStatus", execEntityTPTCInserted.getOverallStatus());
            resultTestCaseObject.put("testData", getMappedParameters(execEntityTPTCInserted.getID(), testCaseId, activeObjects));
            resultTestCaseObject.put("steps", cloneTestSteps(testCaseId, execEntityTPTCInserted.getID(), false, null, null, activeObjects));
            resultTestCaseObject.put("testCaseBrowser", execEntityTPTCInserted.getTestCaseBrowser());
            resultTestCaseArray.put(resultTestCaseObject);
        }
    }
}
