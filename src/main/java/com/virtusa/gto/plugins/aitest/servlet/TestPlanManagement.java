package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.TPlan;
import com.virtusa.gto.plugins.aitest.entity.TestPlan;
import com.virtusa.gto.plugins.aitest.entity.TestPlanTestCases;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Scanned
public class TestPlanManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(TestPlanManagement.class);
    private static final long serialVersionUID = -3076059977965457676L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    //Project Key
    BrowseContext context;

    @Inject
    public TestPlanManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager, LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("user");
        String entity = req.getParameter("entity");
        String data = req.getParameter("data");

        //User login authentication
        String redirectUrl = "";
        try {
            JSONObject jsonObject = new JSONObject(data);
            if (jsonObject.has("redirectView")) {
                redirectUrl = jsonObject.get("redirectView").toString();
            }
            UserProfile currentUser = userManager.getRemoteUser(req);
            if (currentUser == null) {
                resp.setStatus(HttpServletResponse.SC_FOUND);
                if (redirectUrl.equals("/secure/TestExecutionView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    String action = req.getParameter("action");
                    handleAction(action, entity, data, resp);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String entity, String data, HttpServletResponse resp) throws IOException {
        switch (action) {
            case "insert":
                resp.getWriter().write(insert(entity, data));
                break;
            case "update":
                resp.getWriter().write(update(entity, data));
                break;
            case "delete":
                resp.getWriter().write(String.valueOf(delete(entity, data)));
                break;
            case "get":
                resp.getWriter().write(get(entity));
                break;
            case "getPlan":
                resp.getWriter().write(getPlan(entity, data));
                break;
            default:
                break;
        }
    }

    private String update(String entity, String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject dataF = element.getAsJsonObject();

        JSONObject planObject = new JSONObject();

        if (entity.toLowerCase(Locale.ENGLISH).equals(TestPlan.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            String project = new GetProjectKey(context).getSelectedProjectKey();

            TPlan tPlan = new TPlan(activeObjects);
            if (tPlan.update(project, dataF.get("id").getAsString(), dataF.get("name").getAsString())) {
                List<TestPlan> planList = tPlan.getById(project, dataF.get("id").getAsString());
                if (planList.size() == 1) {
                    try {
                        planObject.put("id", planList.get(0).getID());
                        planObject.put("name", planList.get(0).getName());
                    } catch (JSONException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
        return planObject.toString();
    }

    private String insert(String entity, String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (entity.toLowerCase(Locale.ENGLISH).equals(TestPlan.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            String project = new GetProjectKey(context).getSelectedProjectKey();
            TPlan tPlan = new TPlan(activeObjects);
            TestPlan inserted = tPlan.insert(jsonData.get("name").getAsString(), project);
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("id", inserted.getID());
                returnObject.put("name", inserted.getName());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            return returnObject.toString();
        }
        return "";
    }

    private boolean delete(String entity, String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (entity.toLowerCase(Locale.ENGLISH).equals(TestPlan.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            String project = new GetProjectKey(context).getSelectedProjectKey();
            TPlan tPlan = new TPlan(activeObjects);
            if (tPlan.delete(project, jsonData.get("id").getAsString())) {
                try {
                    TestPlanTestCaseManagement tptcm = new TestPlanTestCaseManagement(authenticationContext, activeObjects);
                    JSONObject filterObject = new JSONObject();
                    filterObject.put("testPlanId", jsonData.get("id").getAsString());

                    return tptcm.delete(filterObject.toString());
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                    return false;
                }
            }
        }
        return false;
    }

    private String get(String entity) {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JSONObject returnData = new JSONObject();
        JSONArray testPlanArray = new JSONArray();

        if (entity.toLowerCase(Locale.ENGLISH).equals(TestPlan.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            TPlan tPlan = new TPlan(activeObjects);
            List<TestPlan> plans = tPlan.get(selectedProjectKey);
            for (TestPlan plan : plans) {
                JSONObject returnObject = new JSONObject();
                try {

                    TestPlanTestCaseManagement tptcm = new TestPlanTestCaseManagement(authenticationContext, activeObjects);
                    JSONObject filterObject = new JSONObject();
                    filterObject.put("testPlanId", plan.getID());


                    returnObject.put("id", plan.getID());
                    returnObject.put("name", plan.getName());
                    returnObject.put("count", tptcm.getList(filterObject.toString()).size());
                    returnObject.put("lastExecution", "N/A");

                } catch (JSONException e) {
                    log.error(e.getMessage());
                }
                testPlanArray.put(returnObject);
            }
            try {
                returnData.put("plans", testPlanArray);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }

        }
        return returnData.toString();
    }

    private String getPlan(String entity, String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();

        JSONObject returnData = new JSONObject();
        JSONArray testCasesArray = new JSONArray();
        if (entity.toLowerCase(Locale.ENGLISH).equals(TestPlan.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            String project = new GetProjectKey(context).getSelectedProjectKey();

            TPlan tPlan = new TPlan(activeObjects);
            List<TestPlan> plans = tPlan.getById(project, jsonData.get("id").getAsString());
            for (TestPlan plan : plans) {
                try {
                    returnData.put("id", plan.getID());
                    returnData.put("name", plan.getName());

                    TestPlanTestCaseManagement tptcm = new TestPlanTestCaseManagement(authenticationContext, activeObjects);
                    JSONObject filterObject = new JSONObject();
                    filterObject.put("testPlanId", plan.getID());

                    List<TestPlanTestCases> testCaseList = tptcm.getList(filterObject.toString());
                    for (TestPlanTestCases testCase : testCaseList) {
                        JSONObject testCaseObject = new JSONObject();

                        JSONObject tcFilterObject = new JSONObject();
                        tcFilterObject.put("id", testCase.getTestCaseId());

                        TestCase testCaseServlet = new TestCase(authenticationContext, activeObjects);
                        testCaseObject.put("testCase", testCaseServlet.getIssue(tcFilterObject.toString()));

                        testCaseObject.put("agent", testCase.getAgent());
                        testCaseObject.put("user", testCase.getUser());
                        testCasesArray.put(testCaseObject);
                    }

                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
        try {
            returnData.put("testCases", testCasesArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnData.toString();
    }
}
