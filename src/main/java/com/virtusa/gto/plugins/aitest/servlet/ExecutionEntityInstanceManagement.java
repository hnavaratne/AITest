package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExecutionEntityInstanceManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEntityManagement.class);
    private static final long serialVersionUID = -2679308244285442225L;

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
    public ExecutionEntityInstanceManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
                                             LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
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
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    handleAction(action, data, user, resp);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String data, String user, HttpServletResponse resp) {
        try {
            if ("update-exec".equals(action)) {
                resp.getWriter().write(update(data, user));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String update(String data, String user) {
        JsonElement jsonElement = new JsonParser().parse(data);
        JsonObject parsedData = jsonElement.getAsJsonObject();
        String curActVal = parsedData.get("curActVal").getAsString();
        String curOverallStat = parsedData.get("curOveralStat").getAsString();
        String curTcId = parsedData.get("curTcId").getAsString();
        String execEntityTPTCInstanceId = parsedData.get("execEntityTPTCI").getAsString();
        String iterationId = parsedData.get("iterationId").getAsString();
        String tPTCIMyTask = parsedData.get("tPTCI_MyTask").getAsString();

        JSONObject returnPlanTCObject = new JSONObject();
        try {
            EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);
            TCInstanceMyTask tcInstanceMyTask = new TCInstanceMyTask(activeObjects);
            EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
            DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
            if (Objects.nonNull(iterationId) && !iterationId.equals("empty")) {
                iterationExecEntityTPTCIUpdate(parsedData, curActVal, curOverallStat, curTcId, execEntityTPTCInstanceId, iterationId, tPTCIMyTask, eEnityPlanTestCaseDataIteration, tcInstanceMyTask, eEntityPlanTestCaseInstance, defectsEntity, user);
            } else {
                execEntityTPTCIUpdate(parsedData, curActVal, curOverallStat, curTcId, execEntityTPTCInstanceId, iterationId, tPTCIMyTask, tcInstanceMyTask, eEntityPlanTestCaseInstance, defectsEntity, user);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return returnPlanTCObject.toString();
    }

    private void execEntityTPTCIUpdate(JsonObject parsedData, String curActVal, String curOverallStat, String curTcId, String execEntityTPTCInstanceId, String iterationId, String tPTCIMyTask, TCInstanceMyTask tcInstanceMyTask, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, DefectsEntity defectsEntity, String user) {
        ExecEntityTPTCI[] execEntityTPTCIS;
        boolean update = tcInstanceMyTask.update(tPTCIMyTask, curActVal, curOverallStat, true);
        if (update) {
            if (curOverallStat.equals("Pass")) {
                execEntityTPTCIS = eEntityPlanTestCaseInstance.update(EEntityTPTCDIStatus.PASS.value(), EEntityTPTCDIStatus.PASS.value(), curTcId);
            } else {
                execEntityTPTCIS = eEntityPlanTestCaseInstance.update(EEntityTPTCDIStatus.FAILED.value(), EEntityTPTCDIStatus.FAILED.value(), curTcId);
            }
            for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                int executionEntityTestPlanId = execEntityTPTCI.getExecutionEntityTestPlanId();
                updateHierarchy(String.valueOf(executionEntityTestPlanId), parsedData.get("testCaseUser").getAsString(), parsedData.get("testCaseId").getAsString());
            }
        }
        if (curOverallStat.toLowerCase(Locale.ENGLISH).equals("failed")) {
            handleOverallStatFailed(parsedData, eEntityPlanTestCaseInstance, defectsEntity, tcInstanceMyTask, tPTCIMyTask, execEntityTPTCInstanceId, iterationId, parsedData.get("testCaseId").getAsString(), user);
        }
    }

    private void handleOverallStatFailed(JsonObject parsedData, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, DefectsEntity defectsEntity, TCInstanceMyTask tcInstanceMyTask, String tPTCIMyTask, String execEntityTPTCInstanceId, String iterationId, String testCaseId, String testCaseUser) {
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByName(testCaseUser);
        if (parsedData.get("attachDefectStatus").getAsString().equals("2")) {
            List<ExecEntityTPTCI> entityTpTcList = eEntityPlanTestCaseInstance.getEntityTpTcList(Integer.parseInt(execEntityTPTCInstanceId), true);
            for (ExecEntityTPTCI execEntityTPTCI : entityTpTcList) {
                String jsonObj = new DefectManagement().createBug(parsedData.get("testCaseUser").getAsString(), parsedData.get("testCaseId").getAsString(), execEntityTPTCI, iterationId, null, activeObjects, context);
                JsonElement element = new JsonParser().parse(jsonObj);
                JsonObject bugObj = element.getAsJsonObject();
                defectsEntity.insert(execEntityTPTCI.getEntityId(), execEntityTPTCI.getExecutionEntityTestPlanId(), execEntityTPTCI.getID(), bugObj.get("id").getAsString(), bugObj.get("key").getAsString(), bugObj.get("name").getAsString(), String.valueOf(execEntityTPTCI.getCycle()), execEntityTPTCI.getTestCaseName(), parsedData.get("testCaseId").getAsString());
                eEntityPlanTestCaseInstance.update(Integer.parseInt(execEntityTPTCInstanceId), bugObj.get("id").getAsString(), bugObj.get("name").getAsString());
            }
        } else {
            JsonArray attachedDefectList = parsedData.get("attachedDefectList").getAsJsonArray();
            List<TCIMyTask> tciMyTasksList = tcInstanceMyTask.getMyTask(tPTCIMyTask);
            for (TCIMyTask tciMyTask : tciMyTasksList) {
                for (int i = 0; i < attachedDefectList.size(); i++) {
                    JsonObject attachedDefectObj = attachedDefectList.get(i).getAsJsonObject();
                    IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
                    IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
                    Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
                    List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
                    int size = issueLinkManager.getIssueLinks(Long.valueOf(testCaseId)).size();
                    Long sequence = (long) size + 1;
                    try {
                        issueLinkManager.createIssueLink(Long.valueOf(testCaseId), Long.valueOf(attachedDefectObj.get("defectId").getAsString()), linkTypes.get(0).getId(), sequence, userByKey);
                    } catch (CreateException e) {
                        log.error(e.getMessage(), e);
                    }
                    defectsEntity.insert(tciMyTask.getEntityId(), tciMyTask.getExecutionEntityTestPlanId(), tciMyTask.getExecEntityTPTCI(), attachedDefectObj.get("defectId").getAsString(), attachedDefectObj.get("defectKey").getAsString(), attachedDefectObj.get("defectName").getAsString(), String.valueOf(tciMyTask.getCycle()), tciMyTask.getTestCaseName(), parsedData.get("testCaseId").getAsString());
                }
            }
        }
    }

    private void iterationExecEntityTPTCIUpdate(JsonObject parsedData, String curActVal, String curOverallStat, String curTcId, String execEntityTPTCInstanceId, String iterationId, String tPTCIMyTask, EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration, TCInstanceMyTask tcInstanceMyTask, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, DefectsEntity defectsEntity, String user) {
        ExecEntityTPTCI[] execEntityTPTCIS;
        TCaseDataIteration tCaseDataIteration = eEnityPlanTestCaseDataIteration.updateTestCaseIteration(iterationId, curActVal, curOverallStat);
        boolean update = tcInstanceMyTask.update(tPTCIMyTask, curActVal, curOverallStat, true);
        if (update) {
            int inProgressIterationsCount = eEnityPlanTestCaseDataIteration.getInprogressIterationsCount(String.valueOf(tCaseDataIteration.getTestCaseDataInstance().getID()));
            if (inProgressIterationsCount <= 0) {
                int failedCount = eEnityPlanTestCaseDataIteration.getFailedCount(String.valueOf(tCaseDataIteration.getTestCaseDataInstance().getID()));
                if (failedCount <= 0) {
                    execEntityTPTCIS = eEntityPlanTestCaseInstance.update(EEntityTPTCDIStatus.PASS.value(), EEntityTPTCDIStatus.PASS.value(), curTcId);
                } else {
                    execEntityTPTCIS = eEntityPlanTestCaseInstance.update(EEntityTPTCDIStatus.FAILED.value(), EEntityTPTCDIStatus.FAILED.value(), curTcId);
                }
                for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                    int executionEntityTestPlanId = execEntityTPTCI.getExecutionEntityTestPlanId();
                    updateHierarchy(String.valueOf(executionEntityTestPlanId), parsedData.get("testCaseUser").getAsString(), parsedData.get("testCaseId").getAsString());
                }
            }
        }
        if (curOverallStat.toLowerCase(Locale.ENGLISH).equals("failed") && Objects.nonNull(parsedData.get("attachDefectStatus").getAsString())) {
            handleIterationOverallStatFailed(parsedData, eEntityPlanTestCaseInstance, defectsEntity, tcInstanceMyTask, tPTCIMyTask, execEntityTPTCInstanceId, iterationId);
        }
    }

    private void handleIterationOverallStatFailed(JsonObject parsedData, EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance, DefectsEntity defectsEntity, TCInstanceMyTask tcInstanceMyTask, String tPTCIMyTask, String execEntityTPTCInstanceId, String iterationId) {
        if (parsedData.get("attachDefectStatus").getAsString().equals("2")) {
            List<TCIMyTask> tciMyTasksList = tcInstanceMyTask.getMyTask(tPTCIMyTask);
            for (TCIMyTask tciMyTask : tciMyTasksList) {
                String jsonObj = new DefectManagement().createBug(parsedData.get("testCaseUser").getAsString(), parsedData.get("testCaseId").getAsString(), null, iterationId, tciMyTask, activeObjects, context);
                JsonElement element = new JsonParser().parse(jsonObj);
                JsonObject bugObj = element.getAsJsonObject();
                defectsEntity.insert(tciMyTask.getEntityId(), tciMyTask.getExecutionEntityTestPlanId(), tciMyTask.getExecEntityTPTCI(), bugObj.get("id").getAsString(), bugObj.get("key").getAsString(), bugObj.get("name").getAsString(), String.valueOf(tciMyTask.getCycle()), tciMyTask.getTestCaseName(), tPTCIMyTask, parsedData.get("testCaseId").getAsString());
                eEntityPlanTestCaseInstance.update(Integer.parseInt(execEntityTPTCInstanceId), bugObj.get("id").getAsString(), bugObj.get("name").getAsString());
            }
        } else {
            JsonArray attachedDefectList = parsedData.get("attachedDefectList").getAsJsonArray();
            List<TCIMyTask> tciMyTasksList = tcInstanceMyTask.getMyTask(tPTCIMyTask);
            for (TCIMyTask tciMyTask : tciMyTasksList) {
                for (int i = 0; i < attachedDefectList.size(); i++) {
                    JsonObject attachedDefectObj = attachedDefectList.get(i).getAsJsonObject();
                    defectsEntity.insert(tciMyTask.getEntityId(), tciMyTask.getExecutionEntityTestPlanId(), tciMyTask.getExecEntityTPTCI(), attachedDefectObj.get("defectId").getAsString(), attachedDefectObj.get("defectKey").getAsString(), attachedDefectObj.get("defectName").getAsString(), String.valueOf(tciMyTask.getCycle()), tciMyTask.getTestCaseName(), tPTCIMyTask, parsedData.get("testCaseId").getAsString());
                }
            }
        }
    }

    private void updateHierarchy(String planInstanceId, String userName, String tcId) {
        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstanceNew = new EEntityPlanTestCaseInstance(activeObjects);
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        List<ExecEntityTPTCI> testCasesForTPlan = eEntityPlanTestCaseInstanceNew.getTestCasesForTPlan(planInstanceId);
        boolean hasPending = false;
        boolean hasFailed = false;
        for (ExecEntityTPTCI execEntityTPTCI : testCasesForTPlan) {
            boolean executed = execEntityTPTCI.isExecuted();
            if (executed) {
                String overallStatus = execEntityTPTCI.getOverallStatus();
                if (overallStatus.toLowerCase(Locale.ENGLISH).equals("in progress")) {
                    hasPending = true;
                } else if (overallStatus.toLowerCase(Locale.ENGLISH).equals("failed")) {
                   // handleFailedAutomatedTestCases(execEntityTPTCI, userName, tcId, defectsEntity);
                    hasFailed = true;
                }
            } else {
                String overallStatus = execEntityTPTCI.getOverallStatus();
                if (overallStatus.toLowerCase(Locale.ENGLISH).equals("in progress")) {
                    hasPending = true;
                } else if (overallStatus.toLowerCase(Locale.ENGLISH).equals("failed")) {
                    //handleFailedAutomatedTestCases(execEntityTPTCI, userName, tcId, defectsEntity);
                    hasFailed = true;
                }
            }
        }

        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
        List<ExecEntityTPI> execEntityTPIS = eEntityPlanInstance.getEEntityTPI(planInstanceId);
        int entityId = execEntityTPIS.get(0).getEntityId();
        execEntityTPIUpdate(hasPending, hasFailed, execEntityTPIS);

        List<ExecEntityTPI> eEntityTPIByEntityInstance = eEntityPlanInstance.getEEntityTPIByEntityInstance(String.valueOf(entityId));
        boolean entityInstanceHasPending = execEntityInstanceStatus(eEntityTPIByEntityInstance);

        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        List<ExecEntityI> byInstanceId = eEntityInstance.getByInsatanceId(String.valueOf(entityId));

        execEntityInstanceUpdate(entityInstanceHasPending, byInstanceId);
    }

    private void handleFailedAutomatedTestCases(ExecEntityTPTCI execEntityTPTCI, String userName, String tcId, DefectsEntity defectsEntity) {
        if (execEntityTPTCI.getTestCaseAutomated().toLowerCase(Locale.ENGLISH).equals("yes")) {
            String jsonObj = new DefectManagement().createBug(userName, tcId, execEntityTPTCI, "", null, activeObjects, context);
            JsonElement element = new JsonParser().parse(jsonObj);
            JsonObject bugObj = element.getAsJsonObject();
            defectsEntity.insert(execEntityTPTCI.getEntityId(), execEntityTPTCI.getExecutionEntityTestPlanId(), execEntityTPTCI.getID(), bugObj.get("id").getAsString(), bugObj.get("key").getAsString(), bugObj.get("name").getAsString(), String.valueOf(execEntityTPTCI.getCycle()), execEntityTPTCI.getTestCaseName(), tcId);
        }
    }

    private void execEntityTPIUpdate(boolean hasPending, boolean hasFailed, List<ExecEntityTPI> execEntityTPIS) {
        if (!hasPending && !hasFailed) {
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                execEntityTPI.setStatus(EEntityTPStatus.COMPLETED.value());
                execEntityTPI.save();
            }
        } else if (hasFailed && !hasPending) {
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                execEntityTPI.setStatus(EEntityTPStatus.COMPLETED.value());
                execEntityTPI.save();
            }
        } else if (!hasFailed) {
            for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                execEntityTPI.setStatus(EEntityTPStatus.IN_PROGRESS.value());
                execEntityTPI.save();
            }
        }
    }

    private void execEntityInstanceUpdate(boolean entityInstanceHasPending, List<ExecEntityI> byInstanceId) {
        if (!entityInstanceHasPending) {
            for (ExecEntityI execEntityI : byInstanceId) {
                execEntityI.setStatus(EEntityStatus.COMPLETED.value());
                execEntityI.save();
            }
        } else {
            for (ExecEntityI execEntityI : byInstanceId) {
                execEntityI.setStatus(EEntityStatus.IN_PROGRESS.value());
                execEntityI.save();
            }
        }
    }

    private boolean execEntityInstanceStatus(List<ExecEntityTPI> eEntityTPIByEntityInstance) {
        boolean entityInstanceHasPending = false;
        for (ExecEntityTPI execEntityI : eEntityTPIByEntityInstance) {
            String status = execEntityI.getStatus();
            switch (status.toLowerCase(Locale.ENGLISH)) {
                case "in progress":
                    entityInstanceHasPending = true;
                    break;
                case "failed":
                    entityInstanceHasPending = false;
                    break;
                default:
                    break;
            }
        }
        return entityInstanceHasPending;
    }
}
