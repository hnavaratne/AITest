package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.TCInstanceMyTask;
import com.virtusa.gto.plugins.aitest.entity.TCIMyTask;
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
import java.util.Objects;

public class MyTaskEntityManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MyTaskEntityManagement.class);
    private static final long serialVersionUID = 4152074895570557646L;
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    //Project Key
    BrowseContext context;
    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    @Inject
    public MyTaskEntityManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
                                  LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String user = req.getParameter("user");
        String data = req.getParameter("data");

        //User login authentication
        String redirectUrl = "";
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(data);
            if (jsonObject.has("redirectView")) {
                redirectUrl = jsonObject.get("redirectView").toString();
            }

            UserProfile currentUser = userManager.getRemoteUser(req);
            if (!Objects.nonNull(currentUser)) {
                resp.setStatus(HttpServletResponse.SC_FOUND);
                if (redirectUrl.equals("/secure/MyTasksView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                } else if (redirectUrl.equals("/secure/TestReportsView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    String action = req.getParameter("action");
                    handleAction(action, data, user, resp);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String data, String user, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get-my-tasks":
                    resp.getWriter().write(getMyTasks(user, data));
                    break;
                case "get-test-iteration-data-list":
                    resp.getWriter().write(getIterationDataList(data));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getMyTasks(String user, String data) {

        JsonElement jsonElement = new JsonParser().parse(data);
        JsonObject parsedData = jsonElement.getAsJsonObject();
        int selectedPage = parsedData.get("selectedPage").getAsInt();

        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        TCInstanceMyTask tcInstanceMyTask = new TCInstanceMyTask(activeObjects);
        List<TCIMyTask> tciMyTasks = tcInstanceMyTask.get(projectKey, user);
        JSONArray returnArray = new JSONArray();
        int records_per_page = 7;
        double totalRecordCount = tciMyTasks.size();
        double tempV = totalRecordCount / (double) records_per_page;
        double pagesCount = Math.ceil(tempV);

        int startIndex = (selectedPage * records_per_page) - records_per_page;
        int endIndex = selectedPage * records_per_page;

        try {
            for (int i = startIndex; i < tciMyTasks.size(); i++) {
                if (i < endIndex) {
                    JSONObject returnObject = new JSONObject();
                    returnObject.put("page_count", pagesCount);
                    returnObject.put("tp_id", tciMyTasks.get(i).getExecutionEntityTestPlanId());
                    returnObject.put("tp_tc_id", tciMyTasks.get(i).getExecEntityTPTCI());
                    returnObject.put("tci_my_task", tciMyTasks.get(i).getID());
                    returnObject.put("tc_id", tciMyTasks.get(i).getTestCaseId());
                    returnObject.put("tc_name", tciMyTasks.get(i).getTestCaseName());
                    returnObject.put("tc_user", tciMyTasks.get(i).getTestCaseUser());
                    returnObject.put("instanceName", tciMyTasks.get(i).getCycle());//updated
                    if (!Objects.isNull(tciMyTasks.get(i).getTCaseDataIteration())) {
                        returnObject.put("overallStatus", tciMyTasks.get(i).getTCaseDataIteration().getStatus());
                        returnObject.put("iterationData", tciMyTasks.get(i).getTCaseDataIteration().getIterationData());
                        returnObject.put("iterationId", tciMyTasks.get(i).getTCaseDataIteration().getID());
                    } else {
                        returnObject.put("overallStatus", "");
                        returnObject.put("iterationData", "No Iteration Data");
                        returnObject.put("iterationId", "empty");
                    }
                    returnObject.put("expected_value", tciMyTasks.get(i).getOverallExpectedResult() == null ? "" : tciMyTasks.get(i).getOverallExpectedResult());
                    returnArray.put(returnObject);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnArray.toString();
    }

    private String getIterationDataList(String data) {
        TCInstanceMyTask tcInstanceMyTask = new TCInstanceMyTask(activeObjects);
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String execEntityTPTCI = jsonData.get("execEntityTPTCI").getAsString();
        JSONObject returnPlanListObj = new JSONObject();
        JSONArray returnPlanTCMYTTasksList = new JSONArray();
        try {
            List<TCIMyTask> tciMyTasks = tcInstanceMyTask.get(String.valueOf(execEntityTPTCI));
            if (tciMyTasks.size() > 0) {
                for (TCIMyTask tciMyTask : tciMyTasks) {
                    JSONObject returnPlanTCMYTTaskObject = new JSONObject();
                    returnPlanTCMYTTaskObject.put("tciMyTaskId", tciMyTask.getID());
                    if (!Objects.isNull(tciMyTask.getTCaseDataIteration())) {
                        returnPlanTCMYTTaskObject.put("iterationId", tciMyTask.getTCaseDataIteration().getID());
                        returnPlanTCMYTTaskObject.put("tciMyTaskIterationData", tciMyTask.getTCaseDataIteration().getIterationData());
                        returnPlanTCMYTTaskObject.put("tciMyTaskStatus", tciMyTask.getTCaseDataIteration().getStatus());
                    } else {
                        returnPlanTCMYTTaskObject.put("iterationId", "empty");
                        returnPlanTCMYTTaskObject.put("tciMyTaskIterationData", "");
                        returnPlanTCMYTTaskObject.put("tciMyTaskStatus", tciMyTask.getOverallStatus());
                    }
                    returnPlanTCMYTTaskObject.put("tciMyTaskActualResult", tciMyTask.getActualResult());
                    returnPlanTCMYTTasksList.put(returnPlanTCMYTTaskObject);
                }
                returnPlanListObj.put("tciMyTaskIterationDataObj", returnPlanTCMYTTasksList);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnPlanListObj.toString();
    }
}
