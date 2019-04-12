package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.EEntityInstance;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanInstance;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanTestCaseInstance;
import com.virtusa.gto.plugins.aitest.entity.EEntityTPTCStatus;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityI;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPI;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
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

public class ExecutionEntityInstanceDataMangement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEntityInstanceDataMangement.class);
    private static final long serialVersionUID = -2261833964217250169L;
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
    public ExecutionEntityInstanceDataMangement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
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
                if (redirectUrl.equals("/secure/TestExecutionView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    String action = req.getParameter("action");
                    handleAction(action, data, resp);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String data, HttpServletResponse resp) {
        try {
            if ("get-executed-test-case-data".equals(action)) {
                JsonObject obj = new JsonParser().parse(data).getAsJsonObject();
                String test_case_id = obj.get("test_case_id").getAsString();
                resp.getWriter().write(getExecutedTestCaseData(test_case_id));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getExecutedTestCaseData(String data) {
        JSONObject returnPlanTCObject = new JSONObject();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);

        List<ExecEntityI> eEntityInstanceAll = eEntityInstance.get(projectKey);
        for (ExecEntityI execEntityI : eEntityInstanceAll) {
            try {
                List<ExecEntityTPI> execEntityTPIS = eEntityPlanInstance.get(execEntityI.getID(), projectKey);
                for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                    List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanTestCaseInstance.get(execEntityTPI.getID());
                    for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                        if (execEntityTPTCI.getTestCaseId().equals(data)) {
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
                            if (Objects.nonNull(execEntityTPTCI.getOverallStatus())) {
                                returnPlanTCObject.put("overallStatus", execEntityTPTCI.getOverallStatus());
                            } else {
                                returnPlanTCObject.put("overallStatus", EEntityTPTCStatus.IN_PROGRESS.value());
                            }
                            return returnPlanTCObject.toString();
                        }
                    }
                }
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnPlanTCObject.toString();
    }
}
