package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.TStepsInstance;
import com.virtusa.gto.plugins.aitest.entity.TestStepsI;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TestStepInstanceManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestStepInstanceManagement.class);
    private static final long serialVersionUID = 784350774693334513L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    @Inject
    public TestStepInstanceManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
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
                case "get":
                    resp.getWriter().write(get(data));
                    break;
                case "update":
                    resp.getWriter().write(update(data));
                    break;
                case "update-iteration":
                    resp.getWriter().write(updateIteration(data));
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

        TStepsInstance tSteps = new TStepsInstance(activeObjects);

        if (!jsonData.get("iteration_Id").getAsString().toLowerCase(Locale.ROOT).equals("empty") && Objects.nonNull(jsonData.get("iteration_Id").getAsString()) && !jsonData.get("iteration_Id").getAsString().isEmpty()) {
            List<TestStepsI> steps = tSteps.get(jsonData.get("execEntityTPTCI").getAsString(), jsonData.get("iteration_Id").getAsString());
            for (TestStepsI step : steps) {
                JSONObject returnObject = new JSONObject();
                try {
                    returnObject.put("step", step.getStep());
                    returnObject.put("stepId", step.getID());
                    returnObject.put("data", step.getData());
                    returnObject.put("expectedResult", step.getExpectedResult());
                    returnObject.put("actualResult", step.getActualResult());
                    returnObject.put("actualStatus", step.getActualStatus());
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
                stepsArray.put(returnObject);
            }
        } else {
            List<TestStepsI> steps = tSteps.get(jsonData.get("execEntityTPTCI").getAsString());
            for (TestStepsI step : steps) {
                JSONObject returnObject = new JSONObject();
                try {
                    returnObject.put("step", step.getStep());
                    returnObject.put("stepId", step.getID());
                    returnObject.put("data", step.getData());
                    returnObject.put("expectedResult", step.getExpectedResult());
                    returnObject.put("actualResult", step.getActualResult());
                    returnObject.put("actualStatus", step.getActualStatus());
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
                stepsArray.put(returnObject);
            }
        }
        try {
            returnData.put("execEntityTPTCI", jsonData.get("execEntityTPTCI").getAsString());
            returnData.put("steps", stepsArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return returnData.toString();
    }

    private String update(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnObject = new JSONObject();
        TStepsInstance tSteps = new TStepsInstance(activeObjects);
        JsonArray stepsArray = jsonData.get("steps").getAsJsonArray();

        for (JsonElement step : stepsArray) {
            JsonObject stepObject = step.getAsJsonObject();
            String stepId = stepObject.get("stepInstanceId").getAsString();
            String actualResult = stepObject.get("actualResult").getAsString();
            String actualStatus = stepObject.get("actualStatus").getAsString();
            boolean result = tSteps.upsert(stepId, actualResult, actualStatus);
            if (!result) {
                try {
                    returnObject.put("result", false);
                } catch (JSONException e) {
                    log.error(e.getMessage());
                }
                return returnObject.toString();
            } else {
                try {
                    returnObject.put("result", true);
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return returnObject.toString();
    }

    private String updateIteration(String data) {
        boolean upsertSucess = false;
        JsonElement jsonElement = new JsonParser().parse(data);
        String dataString = jsonElement.getAsJsonObject().get("data").getAsString();
        JsonElement parse = new JsonParser().parse(dataString);
        JsonObject parsedData = parse.getAsJsonObject();
        TStepsInstance tSteps = new TStepsInstance(activeObjects);

        if (!parsedData.get("iterationId").getAsString().equals("empty") && Objects.nonNull(parsedData.get("iterationId").getAsString())) {
            List<TestStepsI> steps = tSteps.get(parsedData.get("execEntityTPTCI").getAsString(), parsedData.get("iterationId").getAsString());
            for (TestStepsI step : steps) {
                upsertSucess = tSteps.upsert(String.valueOf(step.getID()), parsedData.get("curActVal").getAsString(), parsedData.get("curOveralStat").getAsString());
            }
        } else {
            List<TestStepsI> steps = tSteps.get(parsedData.get("execEntityTPTCI").getAsString());
            if(steps.size() > 0){
                for (TestStepsI step : steps) {
                    upsertSucess = tSteps.upsert(String.valueOf(step.getID()), parsedData.get("curActVal").getAsString(), parsedData.get("curOveralStat").getAsString());
                }
            }else {
                upsertSucess = true;
            }
        }
        if (upsertSucess) {
            return "true";
        } else {
            return "false";
        }
    }
}
