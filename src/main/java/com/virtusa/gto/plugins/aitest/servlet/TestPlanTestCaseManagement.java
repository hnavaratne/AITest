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
import com.virtusa.gto.plugins.aitest.db.TPTCase;
import com.virtusa.gto.plugins.aitest.entity.TestPlanTestCases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Scanned
public class TestPlanTestCaseManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestStepManagement.class);
    private static final long serialVersionUID = -1235674836564419121L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    //Project Key
    BrowseContext context;

    @Inject
    public TestPlanTestCaseManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
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
                case "update":
                    resp.getWriter().write(update(data));
                    break;
                case "deleteFTP":
                    resp.getWriter().write(String.valueOf(removeTestCaseFromTestPlan(data)));
                    break;
                case "delete":
                    resp.getWriter().write(String.valueOf(delete(data)));
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


        TPTCase tptCase = new TPTCase(activeObjects);
        List<TestPlanTestCases> testCases = tptCase.get(jsonData.get("testPlanId").getAsString());
        for (TestPlanTestCases testCase : testCases) {
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("testCaseId", testCase.getTestCaseId());
                returnObject.put("testPlanId", testCase.getTestPlanId());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }

        return returnData.toString();
    }

    public List<TestPlanTestCases> getList(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();

        TPTCase tptCase = new TPTCase(activeObjects);
        return tptCase.get(jsonData.get("testPlanId").getAsString());

    }

    private String insert(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONArray resultArray = new JSONArray();

        TPTCase tptCase = new TPTCase(activeObjects);

        String testPlanId = jsonData.get("testPlanId").getAsString();
        JsonArray testCases = jsonData.get("testCases").getAsJsonArray();
        for (JsonElement testCase : testCases) {
            JsonObject testCaseObject = testCase.getAsJsonObject();
            String testCaseId = testCaseObject.get("id").getAsString();
            String agent = testCaseObject.get("agent").getAsString();
            String user = testCaseObject.get("user").getAsString();
            boolean isPoolAgent = false;
            if (testCaseObject.has("testAgentStatus")) {
                isPoolAgent = testCaseObject.get("testAgentStatus").getAsBoolean();
            }

            TestPlanTestCases inserted = tptCase.insert(testCaseId, testPlanId, agent, user, isPoolAgent);
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("testCaseId", inserted.getTestCaseId());
                returnObject.put("testPlanId", inserted.getTestPlanId());
                returnObject.put("agent", inserted.getAgent());
                returnObject.put("isPoolAgent", inserted.isPoolAgent());
                returnObject.put("user", inserted.getUser());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            resultArray.put(returnObject);
        }
        return resultArray.toString();
    }

    public boolean delete(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();

        TPTCase tptCase = new TPTCase(activeObjects);
        return tptCase.delete(jsonData.get("testPlanId").getAsString());
    }

    private String update(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();

        try {
            JSONObject deleteFilter = new JSONObject();
            deleteFilter.put("testPlanId", jsonData.get("testPlanId").getAsString());

            if (delete(deleteFilter.toString())) {
                return insert(data);
            }

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public boolean removeTestCaseFromTestPlan(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        TPTCase tptCase = new TPTCase(activeObjects);
        String testCaseID = jsonData.get("testPlanId").getAsString();
        return tptCase.deleteTestCase(testCaseID);
    }

}
