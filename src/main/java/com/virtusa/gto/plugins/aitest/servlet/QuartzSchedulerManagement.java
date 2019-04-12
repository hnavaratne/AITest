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
import com.virtusa.gto.plugins.aitest.db.EEntity;
import com.virtusa.gto.plugins.aitest.db.EEntityPlan;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanTestCase;
import com.virtusa.gto.plugins.aitest.db.SchedulerEntityInstance;
import com.virtusa.gto.plugins.aitest.entity.ExecEntity;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTP;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTC;
import com.virtusa.gto.plugins.aitest.entity.SchedulerEntity;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.SchedulerInstance;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Scanned
public class QuartzSchedulerManagement extends HttpServlet {

    private static final long serialVersionUID = 6493225718109542841L;
    private static final Logger log = LoggerFactory.getLogger(QuartzSchedulerManagement.class);

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private final ActiveObjects activeObjects;
    BrowseContext context;
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    @Inject
    public QuartzSchedulerManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
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
                if (redirectUrl.equals("/secure/ScheduleView.jspa") || redirectUrl.equals("/secure/ScheduleView.jspa")) {
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
            switch (action) {
                case "save-schedule":
                    resp.getWriter().write(saveSchedule(data));
                    break;
                case "start-schedule":
                    resp.getWriter().write(startSchedule(data));
                    break;
                case "remove-schedule":
                    resp.getWriter().write(removeSchedule(data));
                    break;
                case "update-schedule":
                    resp.getWriter().write(updateSchedule(data));
                    break;
                case "get-all-schedules":
                    resp.getWriter().write(getAllSchedules());
                    break;
                case "get-schedule-data":
                    resp.getWriter().write(getScheduleData(data));
                    break;
                case "edit-schedule-data":
                    resp.getWriter().write(getEditScheduleData(data));
                    break;
                case "delete-schedule":
                    resp.getWriter().write(deleteSchedule(data));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String saveSchedule(String data) {
        JSONObject savedScheduleObj = new JSONObject();
        boolean isInserted = false;
        JsonElement element = new JsonParser().parse(data);
        JsonObject elementData = element.getAsJsonObject();
        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("description", elementData.get("description").getAsString());
            returnObject.put("release", elementData.get("release").getAsString());
            returnObject.put("plans", elementData.get("plans").getAsJsonArray());
            returnObject.put("projectKey", new GetProjectKey(context).getSelectedProjectKey());
            returnObject.put("isFromSchedule", true);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        ExecutionEntityManagement executionEntityManagement = new ExecutionEntityManagement(authenticationContext, activeObjects);
        String insert = executionEntityManagement.insert(returnObject.toString());
        JsonElement insertedElement = new JsonParser().parse(insert);
        JsonObject insertedObj = insertedElement.getAsJsonObject();
        int execEntityId = insertedObj.get("id").getAsInt();
        if (Objects.nonNull(execEntityId)) {
            SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
            SchedulerEntity insertedScheduledObj = schedulerEntityInstance.insert(execEntityId, elementData.get("schedulerName").getAsString(), elementData.get("cronValue").getAsString());
            if (Objects.nonNull(insertedScheduledObj.getSchedulerName())) {
                isInserted = true;
            }
        }
        try {
            savedScheduleObj.put("isInserted", isInserted);
            savedScheduleObj.put("executionEntityInstance", execEntityId);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return savedScheduleObj.toString();
    }

    private synchronized String startSchedule(String data) {
        boolean isScheduled = false;
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        SchedulerInstance instance = SchedulerInstance.getInstance();
        AiTestSyncJob aiTestSyncJob = new AiTestSyncJob(activeObjects);
        aiTestSyncJob.setActiveObjects(activeObjects);
        if (jsonData.has("status") && jsonData.get("status").getAsBoolean()) {
            String eEntityId = jsonData.get("id").getAsString();
            SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
            List<SchedulerEntity> schedulerEntityList = schedulerEntityInstance.get(Integer.valueOf(eEntityId));
            if (Objects.nonNull(schedulerEntityList.get(0))) {
                JsonObject jsonObject = new JsonObject();
                SchedulerEntity schedulerEntity = schedulerEntityList.get(0);
                if (Objects.nonNull(schedulerEntity.getReleaseDescription())) {
                    jsonObject.addProperty("description", schedulerEntity.getReleaseDescription());
                }
                if (Objects.nonNull(schedulerEntity.getExecEntityInstanceId())) {
                    jsonObject.addProperty("executionEntityInstance", schedulerEntity.getExecEntityInstanceId());
                }
                if (Objects.nonNull(schedulerEntity.getReleaseVersion())) {
                    jsonObject.addProperty("releaseVersion", schedulerEntity.getReleaseVersion());
                }
                if (Objects.nonNull(schedulerEntity.getSchedulerName())) {
                    jsonObject.addProperty("scheduleName", schedulerEntity.getSchedulerName());
                }
                if (Objects.nonNull(schedulerEntity.getCronValue())) {
                    jsonObject.addProperty("cronSchedule", schedulerEntity.getCronValue());
                }
                return new QuartzSchedulerDataManagement(activeObjects).startSchedule(aiTestSyncJob, jsonObject, instance, isScheduled);
            } else {
                JsonObject jsonObject = new JsonObject();
                return new QuartzSchedulerDataManagement(activeObjects).startSchedule(aiTestSyncJob, jsonObject, instance, isScheduled);
            }
        } else {
            return (new QuartzSchedulerDataManagement(activeObjects).startSchedule(aiTestSyncJob, jsonData, instance, isScheduled));
        }
    }

    private synchronized String removeSchedule(String data) {
        boolean delete = false;
        JsonElement element = new JsonParser().parse(data);
        SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
        JsonObject elementData = element.getAsJsonObject();
        if (elementData.has("status") && (!elementData.get("status").getAsBoolean())) {
            schedulerEntityInstance.updateStatus(Integer.valueOf(elementData.get("id").getAsString()), false);
            return new QuartzSchedulerDataManagement(activeObjects).unscheduleJob(delete, schedulerEntityInstance, elementData);
        } else {
            if (elementData.get("isFromDelete").getAsBoolean()) {
                schedulerEntityInstance.updateStatus(Integer.valueOf(elementData.get("id").getAsString()), false);
                return new QuartzSchedulerDataManagement(activeObjects).unscheduleJob(delete, schedulerEntityInstance, elementData);
            } else {
                return String.valueOf(delete);
            }
        }
    }

    private String updateSchedule(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject elementData = element.getAsJsonObject();
        JSONObject returnObj = new JSONObject();
        boolean isUpdated;
        boolean updateSuccess = false;
        AiTestSyncJob aiTestSyncJob = new AiTestSyncJob(activeObjects);
        aiTestSyncJob.setActiveObjects(activeObjects);
        int eEntityInstanceId = elementData.get("eEntityInstanceId").getAsInt();
        String newScheduleName = elementData.get("newScheduleName").getAsString();
        String newCronValue = elementData.get("newCronValue").getAsString();
        String newReleaseVersion = elementData.get("newReleaseVersion").getAsString();
        String newReleaseDescription = elementData.get("newReleaseDescription").getAsString();
        SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
        String updatedHierarchyStr = new QuartzSchedulerDataManagement(activeObjects).updateHierarchy(data);
        JsonElement updatedElement = new JsonParser().parse(updatedHierarchyStr);
        JsonObject updatedData = updatedElement.getAsJsonObject();
        try {
            if (updatedData.get("isUpdated").getAsBoolean()) {
                isUpdated = schedulerEntityInstance.update(eEntityInstanceId, newScheduleName, newCronValue, newReleaseVersion, newReleaseDescription);
                if (isUpdated) {
                    if (elementData.has("status") && (!elementData.get("status").getAsBoolean())) {
                        List<SchedulerEntity> schedulerEntityList = schedulerEntityInstance.get(elementData.get("eEntityInstanceId").getAsInt());
                        if (Objects.nonNull(schedulerEntityList.get(0).getTrigger())) {
                            String triggerKey = schedulerEntityList.get(0).getTrigger();
                            SchedulerInstance instance = SchedulerInstance.getInstance();
                            if (!triggerKey.isEmpty() && elementData.get("eEntityInstanceId").getAsInt() != 0) {
                                try {
                                    instance.getSchedulerInstance().unscheduleJob(TriggerKey.triggerKey(triggerKey));
                                    JsonObject jsonObject = new JsonObject();
                                    SchedulerEntity schedulerEntity = schedulerEntityList.get(0);
                                    if (Objects.nonNull(eEntityInstanceId)) {
                                        jsonObject.addProperty("executionEntityInstance", schedulerEntity.getExecEntityInstanceId());
                                    } else {
                                        jsonObject.addProperty("executionEntityInstance", 0);
                                    }
                                    if (Objects.nonNull(newReleaseVersion)) {
                                        jsonObject.addProperty("releaseVersion", schedulerEntity.getReleaseVersion());
                                    } else {
                                        jsonObject.addProperty("releaseVersion", "");
                                    }
                                    if (Objects.nonNull(newScheduleName)) {
                                        jsonObject.addProperty("scheduleName", schedulerEntity.getSchedulerName());
                                    } else {
                                        jsonObject.addProperty("scheduleName", "");
                                    }
                                    if (Objects.nonNull(newCronValue)) {
                                        jsonObject.addProperty("cronSchedule", schedulerEntity.getCronValue());
                                    } else {
                                        jsonObject.addProperty("cronSchedule", "");
                                    }
                                    String startedObj = new QuartzSchedulerDataManagement(activeObjects).startSchedule(aiTestSyncJob, jsonObject, instance, false);
                                    JsonElement element1 = new JsonParser().parse(startedObj);
                                    updateSuccess = element1.getAsBoolean();
                                } catch (SchedulerException e) {
                                    log.error(e.getMessage(), e);
                                }
                                returnObj.put("isUpdated", String.valueOf(updateSuccess));
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObj.toString();
    }

    private String getAllSchedules() {
        SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
        List<SchedulerEntity> schedulerList = schedulerEntityInstance.getAll();
        JSONArray returnArray = new JSONArray();
        for (SchedulerEntity schedulerEntity : schedulerList) {
            JSONObject schedulerObj = new JSONObject();
            try {
                schedulerObj.put("ExecEntityInstanceId", schedulerEntity.getExecEntityInstanceId());
                schedulerObj.put("ScheduleName", schedulerEntity.getSchedulerName());
                schedulerObj.put("CronValue", schedulerEntity.getCronValue());
                schedulerObj.put("Status", schedulerEntity.getStatus());
                returnArray.put(schedulerObj);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnArray.toString();
    }

    private String getScheduleData(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnData = new JSONObject();
        JSONArray planList = new JSONArray();
        EEntity eEntity = new EEntity(activeObjects);
        EEntityPlan eEntityPlan = new EEntityPlan(activeObjects);
        EEntityPlanTestCase eEntityPlanTestCase = new EEntityPlanTestCase(activeObjects);
        List<ExecEntity> execEntities = eEntity.get(jsonData.get("id").getAsString());
        ExecEntity execEntity = execEntities.get(0);
        List<ExecEntityTP> execEntityTPList = eEntityPlan.get(execEntity.getID());
        try {
            for (ExecEntityTP execEntityTP : execEntityTPList) {
                JSONObject execEntityTPObject = new JSONObject();
                execEntityTPObject.put("id", execEntityTP.getID());
                execEntityTPObject.put("entityId", execEntityTP.getEntityId());
                execEntityTPObject.put("planId", execEntityTP.getTestPlanId());
                execEntityTPObject.put("planName", execEntityTP.getTestPlanName());

                JSONArray returnDataPlanTCArray = new JSONArray();
                List<ExecEntityTPTC> execEntityTPTCList = eEntityPlanTestCase.get(execEntityTP.getID());
                for (ExecEntityTPTC execEntityTPTC : execEntityTPTCList) {
                    JSONObject execEntityTPTCObject = new JSONObject();
                    execEntityTPTCObject.put("id", execEntityTPTC.getID());
                    execEntityTPTCObject.put("planId", execEntityTPTC.getExecutionEntityTestPlanId());
                    execEntityTPTCObject.put("testCaseId", execEntityTPTC.getTestCaseId());
                    execEntityTPTCObject.put("testCaseName", execEntityTPTC.getTestCaseName());
                    execEntityTPTCObject.put("automated", execEntityTPTC.getTestCaseAutomated());
                    execEntityTPTCObject.put("manual", execEntityTPTC.getTestCaseManual());
                    execEntityTPTCObject.put("agent", execEntityTPTC.getTestCaseAgent());
                    execEntityTPTCObject.put("isPoolAgent", execEntityTPTC.isPoolAgent());
                    execEntityTPTCObject.put("user", execEntityTPTC.getTestCaseUser());
                    execEntityTPTCObject.put("overallExpectedResult", execEntityTPTC.getOverallExpectedResult());
                    execEntityTPTCObject.put("testCaseBrowser", execEntityTPTC.getTestCaseBrowser());
                    returnDataPlanTCArray.put(execEntityTPTCObject);
                }
                execEntityTPObject.put("testCases", returnDataPlanTCArray);
                planList.put(execEntityTPObject);
            }
            returnData.put("plans", planList);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnData.toString();
    }

    private String getEditScheduleData(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnData = new JSONObject();
        EEntity eEntity = new EEntity(activeObjects);
        SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
        List<ExecEntity> execEntities = eEntity.get(jsonData.get("id").getAsString());
        List<SchedulerEntity> schedulerEntityList = schedulerEntityInstance.get(Integer.valueOf(jsonData.get("id").getAsString()));
        ExecEntity execEntity = execEntities.get(0);
        SchedulerEntity schedulerEntity = schedulerEntityList.get(0);
        try {
            returnData.put("release", execEntity.getRelease());
            returnData.put("description", execEntity.getDescription());
            returnData.put("schedulerName", schedulerEntity.getSchedulerName());
            returnData.put("cronValue", schedulerEntity.getCronValue());
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnData.toString();
    }

    private String deleteSchedule(String data) {
        boolean isDeleted = false;
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String scheduleId = jsonData.get("id").getAsString();
        SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
        String removeScheduleStr = removeSchedule(data);
        JsonElement jsonElement = new JsonParser().parse(removeScheduleStr);
        boolean scheduleRemoved = jsonElement.getAsBoolean();
        boolean scheduleDeleted = schedulerEntityInstance.deleteSchedule(scheduleId);
        if (scheduleRemoved && scheduleDeleted) {
            isDeleted = true;
        }
        return String.valueOf(isDeleted);
    }

}
