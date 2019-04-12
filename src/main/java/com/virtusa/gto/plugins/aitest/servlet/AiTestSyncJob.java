package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.component.SyncJobSupport;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class AiTestSyncJob implements Job {

    @JiraImport
    ActiveObjects activeObjects;

    @Inject
    public AiTestSyncJob(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public AiTestSyncJob() {
        super();
    }

    private static final Logger log = LoggerFactory.getLogger(AiTestSyncJob.class);

    private String releaseVersion;
    private String description;
    private int executionEntityInstance;
    private String projectKey;

    public ActiveObjects getActiveObjects() {
        return activeObjects;
    }

    public void setActiveObjects(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }


    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public int getExecutionEntityInstance() {
        return executionEntityInstance;
    }

    public void setExecutionEntityInstance(int executionEntityInstance) {
        this.executionEntityInstance = executionEntityInstance;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JSONObject cloneDataObj = new JSONObject();
        try {
            cloneDataObj.put("id", Integer.valueOf(jobExecutionContext.getJobDetail().getKey().getGroup()));
            ActiveObjects osGiComponentInstanceOfType = ComponentAccessor.getOSGiComponentInstanceOfType(SyncJobSupport.class).getActiveObject();
            JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getOSGiComponentInstanceOfType(SyncJobSupport.class).getJiraAuthenticationContext();
            ExecutionEntityManagement executionEntityManagement = new ExecutionEntityManagement(jiraAuthenticationContext, osGiComponentInstanceOfType);
            String cloneData = executionEntityManagement.clone(cloneDataObj.toString());
            JsonElement element = new JsonParser().parse(cloneData);
            JsonObject cloneObject = element.getAsJsonObject();
            int executionEntityInstance = cloneObject.get("id").getAsInt();
            setExecutionEntityInstance(executionEntityInstance);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }
}
