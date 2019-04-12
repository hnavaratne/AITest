package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
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
import com.virtusa.gto.plugins.aitest.db.TestStepParam;
import com.virtusa.gto.plugins.aitest.db.TestStepParamValue;
import com.virtusa.gto.plugins.aitest.entity.TestParamV;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author RPSPERERA on 7/20/2018
 */
public class TestCaseDataManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestCaseManagement.class);
    private static final long serialVersionUID = 2705756286108650079L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    //Project Key
    BrowseContext context;

    @Inject
    public TestCaseDataManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            String action = req.getParameter("action");
            switch (action) {
                case "insert_test_case_param":
                    upsertTestCaseParams(data);
                    break;
                case "get_test_case_params":
                    JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
                    String test_case_id = jsonObject.get("test_case_id").getAsString();
                    resp.getWriter().write(getParamsByTestCaseId(test_case_id));
                    break;
                case "save_test_case_params":
                    resp.getWriter().write(saveTestCaseParams(data, req));
                    break;
                case "save_and_update":
                    resp.setStatus(200);
                    resp.getWriter().write(saveAndUpdate(data, req));
                    break;
                case "oscar_save_and_update":
                    resp.setStatus(200);
                    resp.getWriter().write(convertOscarData(data, req));
                    break;
                case "get_test_case_scripts":
                    resp.getWriter().write(getScriptsByTestCaseId(data));
                    break;
                default:
                    break;
            }
        }
    }


    private void upsertTestCaseParams(String data) {

        JsonElement jsonElement = new JsonParser().parse(data);
        JsonObject dataObject = jsonElement.getAsJsonObject();
        JsonArray params = dataObject.get("params").getAsJsonArray();
        TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
        for (int i = 0; i < params.size(); i++) {
            JsonObject paramsAsJsonObject = params.get(i).getAsJsonObject();
            String test_case_id = paramsAsJsonObject.get("test_case_id").getAsString();
            String param_name = paramsAsJsonObject.get("param_name").getAsString();
            JsonArray values = paramsAsJsonObject.get("values").getAsJsonArray();
            for (JsonElement value : values) {
                testStepParamValue.upsert(test_case_id, param_name, value.getAsString());
            }
        }
    }


    private String getParamsByTestCaseId(String testCaseId) {
        JSONArray jsonElements = new JSONArray();
        try {
            TestStepParam testStepParam = new TestStepParam(activeObjects);
            TestStepParams[] parameters = testStepParam.getParameters(testCaseId);
            for (TestStepParams parameter : parameters) {
                JSONObject paramJsonObject = new JSONObject();
                paramJsonObject.put("paramName", parameter.getParamName());
                List<String> strings = new ArrayList<>();
                for (int i = 0; i < parameter.getTestStepParamValues().length; i++) {
                    strings.add(parameter.getTestStepParamValues()[i].getValue());
                }
                paramJsonObject.put("values", strings);
                jsonElements.put(paramJsonObject);
            }
            jsonElements = reverseParamValueArray(jsonElements);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return jsonElements.toString();
    }

    private String saveTestCaseParams(String data, HttpServletRequest req) {

        String testCaseId = req.getParameter("testCaseId");
        TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
        JSONObject returnObject = new JSONObject();
        try {
            JSONArray paramsJsonArray = new JSONArray(data).getJSONArray(0);
            JSONObject paramsJsonObj;
            if (paramsJsonArray.length() > 0) {
                for (int i = 0; i < paramsJsonArray.length(); i++) {
                    paramsJsonObj = paramsJsonArray.getJSONObject(i);
                    String paramName = paramsJsonObj.getString("name");
                    String value = paramsJsonObj.getString("value");
                    testStepParamValue.upsert(testCaseId, paramName, value);
                }

                returnObject.put("message", "OK");
            }

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return returnObject.toString();

    }

    // updated saveAndUpdate function
    //updated 2018/08/15
    private String saveAndUpdate(String data, HttpServletRequest req) {

        TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
        String testCaseId = req.getParameter("testCaseId");
        String paramName;
        JSONObject dataObject;
        JSONArray testDataArray = null;
        JSONArray valuesJArray;

        try {
            testDataArray = new JSONArray(data);
            if (testDataArray.length() > 0) {
                for (int i = 0; i < testDataArray.length(); i++) {
                    dataObject = (JSONObject) testDataArray.get(i);
                    paramName = dataObject.getString("name");
                    valuesJArray = dataObject.getJSONArray("values");
                    testStepParamValue.deleteParams(testCaseId, paramName);
                    for (int j = 0; j < valuesJArray.length(); j++) {
                        testStepParamValue.upsert(testCaseId, paramName, valuesJArray.getString(j));
                    }
                }
            } else {
                testStepParamValue.deleteAll(testCaseId);
            }
        } catch (JSONException | NullPointerException e) {
            log.error(e.getMessage(), e);
        }
        if (Objects.nonNull(testDataArray)) {
            return testDataArray.toString();
        } else {
            throw new NullPointerException();
        }
    }

    //add 2018/08/15
    private JSONArray reverseParamValueArray(JSONArray testDataArray) {

        JSONObject dataObject;
        ArrayList<String> paramValueArray;
        List<String> newParamValueArray;
        String paramname;
        try {
            for (int i = 0; i < testDataArray.length(); i++) {

                dataObject = (JSONObject) testDataArray.get(i);
                paramname = dataObject.getString("paramName");
                paramValueArray = (ArrayList<String>) dataObject.get("values");

                newParamValueArray = new ArrayList<>(paramValueArray);
                Collections.reverse(newParamValueArray);
                dataObject.put("paramName", paramname);
                dataObject.put("values", newParamValueArray);
                testDataArray.put(i, dataObject);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return testDataArray;
    }

    private String getScriptsByTestCaseId(String data) {

        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        String testCaseId = jsonObject.get("test_case_id").getAsString();
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        JSONArray finalJsonObj = new JSONArray();
        JSONObject paramObject = new JSONObject();

        try {
            TestStepParams[] testStepParams = testStepParam.get(testCaseId);
            for (TestStepParams stepParam : testStepParams) {
                String paramName = stepParam.getParamName();

                TestParamV[] testStepParamValues = stepParam.getTestStepParamValues();
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
                getAttachedScripts(finalJsonObj, paramObject, customFieldValue);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return finalJsonObj.toString();
    }

    private void getAttachedScripts(JSONArray finalJsonObj, JSONObject paramObject, JsonArray customFieldValue) throws JSONException {
        for (JsonElement jsonElement : customFieldValue) {
            JsonObject jsonElementAsJsonObject = jsonElement.getAsJsonObject();
            JSONObject testData = new JSONObject();
            if (jsonElementAsJsonObject.has("script-id") && jsonElementAsJsonObject.has("script-name")) {
                testData.put("scriptId", jsonElementAsJsonObject.get("script-id").getAsString());
                testData.put("scriptName", jsonElementAsJsonObject.get("script-name").getAsString());
                JSONArray mappedVariableJsonArray = new JSONArray();
                if (jsonElementAsJsonObject.has("parameters")) {
                    JsonArray asJsonArray = jsonElementAsJsonObject.get("parameters").getAsJsonArray();

                    for (JsonElement element : asJsonArray) {
                        JsonObject mappedVariableList = element.getAsJsonObject();
                        JSONObject mappedValues = new JSONObject();
                        for (Map.Entry<String, JsonElement> stringJsonElementEntry : mappedVariableList.entrySet()) {
                            try {
                                if (paramObject.has(stringJsonElementEntry.getValue().getAsString())) {
                                    mappedValues.put(stringJsonElementEntry.getKey(), paramObject.get(stringJsonElementEntry.getValue().getAsString()));
                                }

                            } catch (JSONException e) {
                                log.error(e.getMessage());
                            }

                        }
                        if (mappedValues.length() > 0) {
                            mappedVariableJsonArray.put(mappedValues);
                        }
                    }
                }
                testData.put("scriptData", mappedVariableJsonArray);
            }
            finalJsonObj.put(testData);
        }
    }

    private String convertOscarData(String data, HttpServletRequest req) {
        JSONArray finalDataSet = new JSONArray();
        try {
            JSONArray initData = new JSONArray(data);
            int count = 0;
            int maxCount = initData.getJSONArray(0).length();
            do {
                JSONObject paramValueObject = new JSONObject();
                JSONArray valueArray = new JSONArray();
                String name = null;
                for (int i = 0; i < initData.length(); i++) {
                    JSONArray innerArray = initData.getJSONArray(i);
                    name = innerArray.getJSONObject(count).getString("name");
                    valueArray.put(innerArray.getJSONObject(count).getString("value"));
                }
                paramValueObject.put("name", name);
                paramValueObject.put("values", valueArray);
                finalDataSet.put(paramValueObject);
                count += 1;
            } while (count < maxCount);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return saveAndUpdate(finalDataSet.toString(), req);
    }
}

