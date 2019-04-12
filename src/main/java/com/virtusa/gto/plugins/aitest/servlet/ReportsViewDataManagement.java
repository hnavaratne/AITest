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
import com.virtusa.gto.plugins.aitest.db.EEnityPlanTestCaseDataIteration;
import com.virtusa.gto.plugins.aitest.db.EEntityInstance;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanInstance;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanTestCaseInstance;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReportsViewDataManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ReportsViewDataManagement.class);
    private static final long serialVersionUID = 8268312901789455541L;
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
    public ReportsViewDataManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
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
                if (redirectUrl.equals("/secure/TestReportsView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    String action = req.getParameter("action");
                    handleAction(action, data, resp);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
    }

    private void handleAction(String action, String data, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get-list":
                    resp.getWriter().write(getEEntityList());
                    break;
                case "get-plan-list":
                    resp.getWriter().write(getPlanList(data));
                    break;
                case "get-test-case-list":
                    resp.getWriter().write(getTestCaseList(data));
                    break;
                case "get-iteration-data":
                    resp.getWriter().write(getIterationData(data));
                    break;
                default:
                    handleRemainingActions1(action, data, resp);
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleRemainingActions1(String action, String data, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get-entity-test-case-count":
                    resp.getWriter().write(String.valueOf(getEntityTestCaseCount(data)));
                    break;
                case "get-plan-test-case-count":
                    resp.getWriter().write(String.valueOf(getPlanTestCaseCount(data)));
                    break;
                case "get-entity-overall-summary":
                    resp.getWriter().write(String.valueOf(getEntityOverallSummary(data)));
                    break;
                case "get-overall-summary":
                    resp.getWriter().write(String.valueOf(getOverallSummary()));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getEEntityList() {
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        JSONArray returnArray = new JSONArray();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);

        List<ExecEntityI> eEntityInstanceAll = eEntityInstance.get(projectKey);
        for (ExecEntityI execEntityI : eEntityInstanceAll) {
            JSONObject returnEntityObject = new JSONObject();
            try {
                returnEntityObject.put("id", execEntityI.getID());
                returnEntityObject.put("execEntity", execEntityI.getExecEntity());
                returnEntityObject.put("release", execEntityI.getRelease());
                returnEntityObject.put("description", execEntityI.getDescription());
                if (Objects.nonNull(execEntityI.getStatus())) {
                    returnEntityObject.put("status", execEntityI.getStatus());
                } else {
                    returnEntityObject.put("status", EEntityStatus.IN_PROGRESS.value());
                }
                if (Objects.nonNull(execEntityI.getExecutionDate())) {
                    returnEntityObject.put("executionDate", dateFormat.format(execEntityI.getExecutionDate()));
                } else {
                    returnEntityObject.put("executionDate", dateFormat.format(new Date(0)));
                }

            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            returnArray.put(returnEntityObject);
        }
        return returnArray.toString();
    }


    private String getPlanList(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String execEntityInstanceId = jsonData.get("execEntityInstanceId").getAsString();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        JSONObject returnEntityObject = new JSONObject();
        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);

        JSONArray returnEntityPlanArray = new JSONArray();
        try {
            List<ExecEntityTPI> execEntityTPIS = eEntityPlanInstance.get(Integer.parseInt(execEntityInstanceId), projectKey);
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                JSONObject returnPlanObject = new JSONObject();

                returnPlanObject.put("id", execEntityTPI.getID());
                returnPlanObject.put("execEntityTP", execEntityTPI.getExecEntityTP());
                returnPlanObject.put("entityId", execEntityTPI.getEntityId());
                returnPlanObject.put("testPlanId", execEntityTPI.getTestPlanId());
                returnPlanObject.put("testPlanName", execEntityTPI.getTestPlanName());
                if (Objects.nonNull(execEntityTPI.getStatus())) {
                    returnPlanObject.put("status", execEntityTPI.getStatus());
                } else {
                    returnPlanObject.put("status", EEntityTPStatus.IN_PROGRESS.value());
                }

                returnEntityPlanArray.put(returnPlanObject);
            }

            returnEntityObject.put("plans", returnEntityPlanArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnEntityObject.toString();
    }

    private String getTestCaseList(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String execEntityTPInstanceId = jsonData.get("execEntityTPInstanceId").getAsString();
        boolean hasUserStory = jsonData.get("hasUserStory").getAsBoolean();
        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
        JSONObject returnPlanObject = new JSONObject();
        try {
            JSONArray returnEntityPlanTCArray = new JSONArray();
            List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanTestCaseInstance.get(Integer.parseInt(execEntityTPInstanceId));
            for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                JSONObject returnPlanTCObject = new JSONObject();
                if (hasUserStory) {
                    createUserStoryObj(returnEntityPlanTCArray, execEntityTPTCI, returnPlanTCObject);
                } else {
                    if (Objects.nonNull(execEntityTPTCI.getUserStoryId()) && !(execEntityTPTCI.getUserStoryId().length() > 0)) {
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
                        String overallStatus = Objects.nonNull(execEntityTPTCI.getOverallStatus()) ? execEntityTPTCI.getOverallStatus() : EEntityTPTCStatus.IN_PROGRESS.value();
                        returnPlanTCObject.put("overallStatus", overallStatus);
                        returnEntityPlanTCArray.put(returnPlanTCObject);
                    }
                }
            }

            returnPlanObject.put("testCases", returnEntityPlanTCArray);

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return returnPlanObject.toString();
    }

    private void createUserStoryObj(JSONArray returnEntityPlanTCArray, ExecEntityTPTCI execEntityTPTCI, JSONObject returnPlanTCObject) throws JSONException {
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

    private List<Integer> getOverallSummary() {
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        return eEntityInstance.getOverallSummary(projectKey);
    }

    private List<Integer> getEntityOverallSummary(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String execEntityInstanceId = jsonData.get("execEntityInstanceId").getAsString();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        return eEntityInstance.getEntityOverallSummary(projectKey, execEntityInstanceId);
    }

    private List<Integer> getEntityTestCaseCount(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        String execEntityInstanceId = jsonData.get("execEntityInstanceId").getAsString();
        return eEntityInstance.getEntityTestCaseCount(execEntityInstanceId, projectKey);
    }

    private List<Integer> getPlanTestCaseCount(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        String execEntityPlanInstanceId = jsonData.get("execEntityPlanInstanceId").getAsString();
        return eEntityInstance.getPlanTestCaseCount(execEntityPlanInstanceId);
    }

    private String getIterationData(String data) {
        JsonElement jsonElement = new JsonParser().parse(data);
        String instanceId = jsonElement.getAsJsonObject().get("instance_id").getAsString();
        EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);

        TCaseDataIteration[] testCaseDataIterations = eEnityPlanTestCaseDataIteration.getTestCaseDataIterations(instanceId);
        JSONObject output = new JSONObject();
        if (Objects.nonNull(testCaseDataIterations)) {
            int inProgressCount = 0;
            int passCount = 0;
            int failCount = 0;

            try {
                JSONArray iterationsInfo = new JSONArray();
                for (TCaseDataIteration testCaseDataIteration : testCaseDataIterations) {
                    String iterationData = testCaseDataIteration.getIterationData();
                    String status = testCaseDataIteration.getStatus();
                    JSONObject dataIterations = new JSONObject();
                    dataIterations.put("data", iterationData);
                    dataIterations.put("status", status);

                    if (Objects.equals(status.toLowerCase(Locale.ENGLISH), EEntityTPTCDIStatus.IN_PROGRESS.value().toLowerCase(Locale.ENGLISH))) {
                        inProgressCount += 1;
                    } else if (Objects.equals(status.toLowerCase(Locale.ENGLISH), EEntityTPTCDIStatus.PASS.value().toLowerCase(Locale.ENGLISH)) || status.toLowerCase(Locale.ENGLISH).contains("pass")) {
                        passCount += 1;
                    } else if (Objects.equals(status.toLowerCase(Locale.ENGLISH), EEntityTPTCDIStatus.FAILED.value().toLowerCase(Locale.ENGLISH))) {
                        failCount += 1;
                    }
                    iterationsInfo.put(dataIterations);
                }
                JSONObject statuses = new JSONObject();
                statuses.put("pass", passCount);
                statuses.put("fail", failCount);
                statuses.put("inProgress", inProgressCount);

                output.put("iterations", iterationsInfo);
                output.put("status", statuses);

            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return output.toString();
    }
}
