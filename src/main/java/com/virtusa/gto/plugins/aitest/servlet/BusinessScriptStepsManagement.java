package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.BDTEntity;
import com.virtusa.gto.plugins.aitest.entity.BusComScript;
import com.virtusa.gto.plugins.aitest.entity.BusComSteps;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusinessScriptStepsManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BusinessScriptStepsManagement.class);
    private static final long serialVersionUID = -2710262657570854575L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    BrowseContext context;

    @Inject
    public BusinessScriptStepsManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
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
                case "insert_business_steps":
                    resp.getWriter().write(upsertBusinessSteps(data));
                    break;
                case "get_business_steps":
                    resp.getWriter().write(getBusinessSteps(data));
                    break;
                case "get_bdts":
                    resp.getWriter().write(getBDT(data));
                    break;
                case "validate_business_steps":
                    resp.getWriter().write(validateBusinessSteps(data));
                    break;
                default:
                    break;
            }
        }
    }

    private String validateBusinessSteps(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONArray jsonArray = new JSONArray();
        List<String> paramList = new ArrayList<>();
        try {
            JsonArray steps = jsonData.get("steps").getAsJsonArray();
            for (JsonElement step : steps) {
                JsonObject stepObject = step.getAsJsonObject();
                String dataString = stepObject.get("data").getAsString();
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jsonArray.toString();
    }

    private String upsertBusinessSteps(String data) {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String businessScriptId = jsonData.get("businessScriptId").getAsString();
        JsonArray steps = jsonData.get("steps").getAsJsonArray();
        JsonArray changeSet = jsonData.get("changeSet").getAsJsonArray();
        BComponentScript businessScript = new BComponentScript(activeObjects);
        BComponentStep businessStep = new BComponentStep(activeObjects);
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
        TSteps tSteps = new TSteps(activeObjects);
        BusComScript selectedScript = businessScript.get(selectedProjectKey, businessScriptId);
        List<BusComSteps> businessSteps = Arrays.asList(businessStep.getSteps(businessScriptId));
        if (businessSteps.size() > 0) {
            Map<String, HashMap<String, String>> mappedChangeSet = businessStep.deleteTestStepsById(businessScriptId, changeSet);
            Set<String> testCases = tSteps.getTestSteps(selectedProjectKey, businessScriptId);
            testCases.forEach(testcaseId -> testStepParam.upsertParams(selectedProjectKey, testcaseId, mappedChangeSet, testStepParamValue));
            businessStep.deleteTestStepsById(String.valueOf(businessScriptId));
        }
        for (JsonElement step : steps) {
            JsonObject stepObject = step.getAsJsonObject();
            String stepString = stepObject.has("step") ? stepObject.get("step").getAsString() : "";
            String expectedString = stepObject.has("expectedResult") ? stepObject.get("expectedResult").getAsString() : "";
            String dataString = stepObject.has("data") ? stepObject.get("data").getAsString() : "";
            businessStep.insert(selectedScript, stepString, expectedString, dataString);
        }
        return steps.toString();
    }

    private String getBusinessSteps(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnData = new JSONObject();
        JSONArray stepsArray = new JSONArray();

        BComponentStep businessStep = new BComponentStep(activeObjects);
        BusComSteps[] steps = businessStep.getSteps(jsonData.get("businessScriptId").getAsString());
        for (BusComSteps step : steps) {
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("step", step.getStepDescription());
                returnObject.put("expectedResult", step.getExpectedValue());
                returnObject.put("data", step.getDataParams());
                returnObject.put("id", step.getID());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            stepsArray.put(returnObject);
        }
        try {
            returnData.put("businessScriptId", jsonData.get("businessScriptId").getAsString());
            returnData.put("steps", stepsArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return returnData.toString();
    }

    private String getBDT(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String testCaseId = jsonData.get("testCaseId").getAsString();
        BDTInstance bdtInstance = new BDTInstance(activeObjects);
        BDTEntity[] bdtValues = bdtInstance.get(testCaseId);
        JsonObject bdtValueObj;
        JsonArray bdtValueArray = new JsonArray();
        for (BDTEntity bdtValue : bdtValues) {
            bdtValueObj = new JsonObject();
            bdtValueObj.addProperty("testCaseId", bdtValue.getTestCaseId());
            bdtValueObj.addProperty("description", bdtValue.getDescription());
            bdtValueArray.add(bdtValueObj);
        }
        return bdtValueArray.toString();
    }
}
