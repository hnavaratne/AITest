package com.virtusa.gto.plugins.aitest.servlet;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.BComponentStep;
import com.virtusa.gto.plugins.aitest.db.TSteps;
import com.virtusa.gto.plugins.aitest.db.TestStepParam;
import com.virtusa.gto.plugins.aitest.db.TestStepParamValue;
import com.virtusa.gto.plugins.aitest.entity.BusComSteps;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
import com.virtusa.gto.plugins.aitest.entity.TestSteps;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Scanned
public class TestStepManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestStepManagement.class);
    private static final long serialVersionUID = 8780348209545092898L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    //Project Key
    BrowseContext context;

    @Inject
    public TestStepManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
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
        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            switch (action) {
                case "insert":
                    resp.getWriter().write(upsert(data));
                    break;
                case "insert-data-iteration":
                    break;
                case "get":
                    resp.getWriter().write(get(data));
                    break;
                case "validate":
                    resp.getWriter().write(validate(data));
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

        JSONArray stepsArray = new JSONArray();

        TSteps tSteps = new TSteps(activeObjects);
        List<TestSteps> steps = tSteps.get(jsonData.get("testCaseId").getAsString());
        for (TestSteps step : steps) {
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("step", step.getStep());
                returnObject.put("expectedResult", step.getExpectedResult());
                returnObject.put("data", step.getData());
                returnObject.put("function", step.getScriptId());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            stepsArray.put(returnObject);
        }
        try {
            returnData.put("testCaseId", jsonData.get("testCaseId").getAsString());
            returnData.put("steps", stepsArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return returnData.toString();
    }

    private String upsert(String data) {

        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();

        JSONArray resultArray = new JSONArray();

        TSteps tSteps = new TSteps(activeObjects);
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
        BComponentStep bComponentStep = new BComponentStep(activeObjects);
        String testCaseId = jsonData.get("testCaseId").getAsString();
        tSteps.delete(testCaseId);

        JsonArray steps = jsonData.get("steps").getAsJsonArray();
        TestStepParams[] testStepParams = testStepParam.get(testCaseId);
        List<String> oldParamList = new ArrayList<>();
        for (TestStepParams stepParam : testStepParams) {
            String paramName = stepParam.getParamName();
            oldParamList.add(paramName);
        }
        List<String> newParamList = new ArrayList<>();
        JSONArray stepParamArray = new JSONArray();
        for (JsonElement step : steps) {
            JsonObject stepObject = step.getAsJsonObject();
            String stepString = getValue("step", stepObject);
            String expectedString = getValue("expectedResult", stepObject);
            String dataString = getValue("data", stepObject);
            String scriptIdString = getValue("function", stepObject);
            //if value inserted
            TestSteps stepUpserted = tSteps.insert(selectedProjectKey, testCaseId, stepString, expectedString, dataString, scriptIdString);
            if (Objects.nonNull(stepUpserted)) {
                getUpsertedSteps(resultArray, bComponentStep, newParamList, stepParamArray, dataString, scriptIdString, stepUpserted);
            }
        }
        removeDuplicates(selectedProjectKey, testStepParam, testStepParamValue, testCaseId, oldParamList, newParamList);

        return resultArray.toString();
    }

    private String getValue(String key, JsonObject stepObject) {
        return stepObject.has(key) ? stepObject.get(key).getAsString() : "";
    }

    private void removeDuplicates(String selectedProjectKey, TestStepParam testStepParam, TestStepParamValue testStepParamValue, String testCaseId, List<String> oldParamList, List<String> newParamList) {
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

    private void getUpsertedSteps(JSONArray resultArray, BComponentStep bComponentStep, List<String> newParamList, JSONArray stepParamArray, String dataString, String scriptIdString, TestSteps stepUpserted) {
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
            returnObject.put("function", stepUpserted.getScriptId());
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

    private String validate(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONArray jsonArray = new JSONArray();
        List<String> paramList = new ArrayList<>();
        BComponentStep bComponentStep = new BComponentStep(activeObjects);
        try {
            JsonArray steps = jsonData.get("steps").getAsJsonArray();
            for (JsonElement step : steps) {
                JsonObject stepObject = step.getAsJsonObject();
                if (!stepObject.isJsonNull() && stepObject.has("function")) {
                    String businessFunction = stepObject.get("function").getAsString();
                    if (businessFunction.length() > 0) {
                        BusComSteps[] steps1 = bComponentStep.getSteps(businessFunction);
                        for (BusComSteps busComSteps : steps1) {
                            String dataString = busComSteps.getDataParams();
                            validateDuplicates(dataString, paramList, jsonArray);
                        }
                    } else {
                        String dataString = stepObject.get("data").getAsString();
                        validateDuplicates(dataString, paramList, jsonArray);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jsonArray.toString();
    }

    private void validateDuplicates(String dataString, List<String> paramList, JSONArray jsonArray) {
        String regex = "\\{\\{[a-zA-Z0-9]*}}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dataString);
        while (matcher.find()) {
            String paramName = matcher.group(0).replace("{{", "").replace("}}", "");
            if (paramList.contains(paramName)) {
                jsonArray.put(paramName);
            } else {
                paramList.add(paramName);
            }
        }
    }
}
