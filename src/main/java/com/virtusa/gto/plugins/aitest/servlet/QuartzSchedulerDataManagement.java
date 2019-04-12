package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.EEntity;
import com.virtusa.gto.plugins.aitest.db.EEntityPlan;
import com.virtusa.gto.plugins.aitest.db.EEntityPlanTestCase;
import com.virtusa.gto.plugins.aitest.db.SchedulerEntityInstance;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTP;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTC;
import com.virtusa.gto.plugins.aitest.entity.SchedulerEntity;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.SchedulerInstance;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzSchedulerDataManagement {

    @JiraImport
    private final ActiveObjects activeObjects;

    BrowseContext context;

    private static final Logger log = LoggerFactory.getLogger(QuartzSchedulerDataManagement.class);

    public QuartzSchedulerDataManagement(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public String updateHierarchy(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject resultObject = new JSONObject();
        EEntity eEntity = new EEntity(activeObjects);
        EEntityPlan eEntityPlan = new EEntityPlan(activeObjects);
        boolean eEntityUpdated;
        boolean eEPlanUpdated = false;
        boolean testCaseUpdated = false;
        int eEntityInstanceId = jsonData.get("eEntityInstanceId").getAsInt();
        EEntityPlanTestCase eEntityPlanTestCase = new EEntityPlanTestCase(activeObjects);
        eEntityUpdated = eEntity.update(String.valueOf(eEntityInstanceId), jsonData.get("newReleaseVersion").getAsString(), jsonData.get("newReleaseDescription").getAsString());
        try {
            if (eEntityUpdated) {
                JsonArray plansToInsert;
                if (jsonData.get("isFromSchedule").getAsBoolean()) {
                    JsonElement jsonElement = new JsonParser().parse(jsonData.get("plans").getAsString());
                    plansToInsert = jsonElement.getAsJsonArray();
                } else {
                    plansToInsert = jsonData.get("plans").getAsJsonArray();
                }
                boolean deleteSuccess = eEntityPlan.delete(String.valueOf(eEntityInstanceId));

                for (JsonElement planToInsert : plansToInsert) {
                    JsonObject planToUpdateObject = planToInsert.getAsJsonObject();
                    if (deleteSuccess) {
                        ExecEntityTP insert = eEntityPlan.insert(eEntityInstanceId, planToUpdateObject.get("planId").getAsString(), planToUpdateObject.get("planName").getAsString(), new GetProjectKey(context).getSelectedProjectKey());
                        JsonArray testCases = planToUpdateObject.get("testCases").getAsJsonArray();
                        for (JsonElement testCase : testCases) {
                            JsonObject testCaseObject = testCase.getAsJsonObject();
                            boolean testCaseDeleted = eEntityPlanTestCase.delete(testCaseObject.get("id").getAsString());
                            String testCaseAgent = "";
                            boolean isPoolAgent = false;
                            if (testCaseObject.get("agent") != null && !testCaseObject.get("agent").getAsString().equals("Select")) {
                                testCaseAgent = testCaseObject.get("agent").getAsString();
                                isPoolAgent = testCaseObject.get("testAgentStatus").getAsBoolean();
                            }
                            String testCaseUser = "";
                            if (testCaseObject.get("user") != null && !testCaseObject.get("user").getAsString().equals("Select")) {
                                testCaseUser = testCaseObject.get("user").getAsString();
                            }
                            String testCaseOverallExpectedResult = testCaseObject.get("overallExpectedResult").getAsString();
                            if (testCaseDeleted) {
                                ExecEntityTPTC entityTPTC = eEntityPlanTestCase.insert(insert.getID(), testCaseObject.get("testCaseId").getAsString(), testCaseObject.get("testCaseName").getAsString(), testCaseObject.get("automated").getAsString(), testCaseObject.get("manual").getAsString(), isPoolAgent, testCaseAgent, testCaseUser, testCaseOverallExpectedResult, testCaseObject.get("testCaseBrowser").getAsString());
                                if (Objects.nonNull(entityTPTC.getExecutionEntityTestPlanId())) {
                                    testCaseUpdated = true;
                                }
                            }
                        }
                        if (Objects.nonNull(insert.getEntityId())) {
                            eEPlanUpdated = true;
                        }
                    }
                }
            }
            if (eEntityUpdated && eEPlanUpdated && testCaseUpdated) {
                resultObject.put("isUpdated", true);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return resultObject.toString();
    }

    public String startSchedule(AiTestSyncJob aiTestSyncJob, JsonObject jsonData, SchedulerInstance instance, boolean isScheduled) {

        String scheduleName = jsonData.get("scheduleName").getAsString();
        String cronValue = jsonData.get("cronSchedule").getAsString();

        if (Objects.nonNull(new GetProjectKey(context).getSelectedProjectKey())) {
            aiTestSyncJob.setProjectKey(new GetProjectKey(context).getSelectedProjectKey());
        } else {
            aiTestSyncJob.setProjectKey("");
        }
        jsonData.get("executionEntityInstance").getAsInt();
        aiTestSyncJob.setExecutionEntityInstance(jsonData.get("executionEntityInstance").getAsInt());

        if (!scheduleName.isEmpty() && !cronValue.isEmpty() && jsonData.get("executionEntityInstance").getAsInt() != 0) {
            JobDetail job = JobBuilder.newJob(AiTestSyncJob.class)
                    .withIdentity(scheduleName, String.valueOf(jsonData.get("executionEntityInstance").getAsInt()))
                    .build();

            CronTrigger trigger = newTrigger()
                    .withIdentity(scheduleName, String.valueOf(jsonData.get("executionEntityInstance").getAsInt()))
                    .withSchedule(cronSchedule(cronValue))
                    .build();
            try {
                SchedulerEntityInstance schedulerEntityInstance = new SchedulerEntityInstance(activeObjects);
                boolean scheduleUpdated = schedulerEntityInstance.insert(jsonData.get("executionEntityInstance").getAsInt(), true, String.valueOf(trigger.getKey().getName()), ((JobDetailImpl) job).getFullName(), String.valueOf(job.getKey().getName()), String.valueOf(trigger.getKey().getGroup()));
                instance.getSchedulerInstance().scheduleJob(job, trigger);

                if (scheduleUpdated) {
                    isScheduled = schedulerEntityInstance.updateStatus(jsonData.get("executionEntityInstance").getAsInt(), true);
                }
            } catch (SchedulerException e) {
                log.error(e.getMessage(), e);
            }
            return String.valueOf(isScheduled);
        }
        return String.valueOf(isScheduled);
    }

    public String unscheduleJob(boolean delete, SchedulerEntityInstance schedulerEntityInstance, JsonObject elementData) {
        List<SchedulerEntity> schedulerEntityList = schedulerEntityInstance.get(elementData.get("id").getAsInt());
        if (Objects.nonNull(schedulerEntityList.get(0).getTrigger())) {
            String triggerKeyName = schedulerEntityList.get(0).getTrigger();
            JobKey jobKey = JobKey.jobKey(schedulerEntityList.get(0).getJobKey(), schedulerEntityList.get(0).getGroup());
            SchedulerInstance instance = SchedulerInstance.getInstance();
            boolean statusUpdated = schedulerEntityInstance.updateStatus(elementData.get("id").getAsInt(), false);
            elementData.get("id").getAsInt();
            if (!triggerKeyName.isEmpty() && elementData.get("id").getAsInt() != 0 && statusUpdated) {
                try {
                    boolean isUnscheduled = instance.getSchedulerInstance().unscheduleJob(TriggerKey.triggerKey(triggerKeyName, schedulerEntityList.get(0).getGroup()));
                    if (isUnscheduled) {
                        instance.getSchedulerInstance().deleteJob(jobKey);
                        delete = true;
                    }

                } catch (SchedulerException e) {
                    log.error(e.getMessage(), e);
                }
                return String.valueOf(delete);
            } else {
                return String.valueOf(delete);
            }
        } else {
            return String.valueOf(delete);
        }
    }

}
