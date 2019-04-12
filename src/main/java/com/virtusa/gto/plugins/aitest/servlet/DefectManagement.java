package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.virtusa.gto.plugins.aitest.db.TStepsInstance;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
import com.virtusa.gto.plugins.aitest.entity.TCIMyTask;
import com.virtusa.gto.plugins.aitest.entity.TestStepsI;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefectManagement {

    private static final Logger log = LoggerFactory.getLogger(DefectManagement.class);

    public String createBug(String testCaseUser, String tcId, ExecEntityTPTCI execEntityTPTCIS, String iterationId, TCIMyTask tciMyTask, ActiveObjects activeObjects, BrowseContext context) {
        JSONObject returnObject = new JSONObject();
        IssueManager issueMgr = ComponentAccessor.getIssueManager();
        Issue issue = issueMgr.getIssueObject(Long.valueOf(tcId));
        TStepsInstance tSteps = new TStepsInstance(activeObjects);

        if (Objects.nonNull(issue)) {
            try {
                String projectID = new GetProjectKey(context).getSelectedProjectKey();
                ProjectManager projectManager = ComponentAccessor.getProjectManager();
                Project projectObjByKey = projectManager.getProjectObj(Long.valueOf(projectID));
                ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(testCaseUser);

                Collection<IssueType> issueTypesForProject = ComponentAccessor.getIssueTypeSchemeManager().getIssueTypesForProject(projectObjByKey);
                IssueType issueTypeF = getIssueType(issueTypesForProject);
                CustomField severity = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Severity").iterator().next();
                Options options = ComponentAccessor.getOptionsManager().getOptions(severity.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
                final Priority[] priorityF = {null};
                Option severityF = getSeverity(options);
                ComponentAccessor.getConstantsManager().getPriorities().forEach(priority -> {
                    if (Objects.equals(priority.getName().toLowerCase(Locale.ENGLISH), "medium") || Objects.equals(priority.getName().toLowerCase(Locale.ENGLISH), "major")) {
                        priorityF[0] = priority;
                    }
                });
                IssueService issueService = ComponentAccessor.getIssueService();
                IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
                StringBuilder bugDescription = new StringBuilder();
                StringBuilder bugSummary = new StringBuilder();
                StringBuilder iterationData = new StringBuilder();
                if (Objects.nonNull(execEntityTPTCIS)) {
                    createBugWithoutIteration(testCaseUser, execEntityTPTCIS, tSteps.get(String.valueOf(execEntityTPTCIS.getID()), iterationId), returnObject, projectID, userByKey, issueTypeF, severity, severityF, priorityF[0], issueService, issueInputParameters, bugDescription, bugSummary, tcId);
                } else {
                    createBugWithIteration(testCaseUser, iterationId, tciMyTask, returnObject, tSteps, projectID, userByKey, issueTypeF, issueService, issueInputParameters, bugDescription, bugSummary, iterationData, tcId);
                }
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnObject.toString();
    }

    private void createBugWithIteration(String testCaseUser, String iterationId, TCIMyTask tciMyTask, JSONObject returnObject, TStepsInstance tSteps, String projectID, ApplicationUser userByKey, IssueType issueTypeF, IssueService issueService, IssueInputParameters issueInputParameters, StringBuilder bugDescription, StringBuilder bugSummary, StringBuilder iterationData, String testCaseId) throws JSONException {
        List<TestStepsI> steps = tSteps.get(String.valueOf(tciMyTask.getExecEntityTPTCI()), iterationId);

        for (TestStepsI step : steps) {
            if (step.getActualStatus().toLowerCase(Locale.ENGLISH).equals("failed")) {
                iterationData.append(step.getData()).append(" ");
                bugDescription.append(step.getData());
                bugDescription.append("/n");
                bugDescription.append(step.getActualResult());
            }
        }
        bugSummary.append(" ,Data - ").append(iterationData);

        issueInputParameters.setSummary("Failed Scenario for execution cycle - " + tciMyTask.getCycle() + " test case - " + tciMyTask.getTestCaseName() + bugSummary);
        issueInputParameters.setDescription(bugDescription.toString());
        issueInputParameters.setReporterId(testCaseUser);
        issueInputParameters.setIssueTypeId(Objects.requireNonNull(issueTypeF).getId());
        issueInputParameters.setProjectId(Long.valueOf(projectID));

        returnObject.put("testCaseUser", testCaseUser);
        returnObject.put("projectId", projectID);

        IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(userByKey, issueInputParameters);

        if (createValidationResult.isValid()) {
            IssueService.IssueResult createResult = issueService.create(userByKey, createValidationResult);
            IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
            IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
            Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
            List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
            int size = issueLinkManager.getIssueLinks(Long.valueOf(testCaseId)).size();
            Long sequence = (long) size + 1;
            try {
                issueLinkManager.createIssueLink(Long.valueOf(testCaseId), createResult.getIssue().getId(), linkTypes.get(0).getId(), sequence, userByKey);
                if (!createResult.isValid()) {
                    log.error(createResult.getErrorCollection().getFlushedErrorMessages().toString());
                }
            } catch (CreateException e) {
                log.error(e.getMessage(), e);
            }
            returnObject.put("id", createResult.getIssue().getId().toString());
            returnObject.put("description", createResult.getIssue().getDescription());
            returnObject.put("name", createResult.getIssue().getSummary());
            returnObject.put("key", createResult.getIssue().getKey());
        }
    }

    private void createBugWithoutIteration(String testCaseUser, ExecEntityTPTCI execEntityTPTCIS, List<TestStepsI> steps1, JSONObject returnObject, String projectID, ApplicationUser userByKey, IssueType issueTypeF, CustomField severity, Option severityF, Priority priority, IssueService issueService, IssueInputParameters issueInputParameters, StringBuilder bugDescription, StringBuilder bugSummary, String testCaseId) throws JSONException {
        for (TestStepsI step : steps1) {
            if (step.getActualStatus().toLowerCase(Locale.ENGLISH).equals("failed")) {
                bugDescription.append(step.getData());
                bugSummary.append(" ,Data - ");
                bugSummary.append(step.getData());
                bugDescription.append("/n");
                bugDescription.append(step.getActualResult());
            }
        }

        issueInputParameters.setSummary("Failed Scenario for execution cycle - " + execEntityTPTCIS.getCycle() + " test case - " + execEntityTPTCIS.getTestCaseName() + bugSummary);
        issueInputParameters.setDescription(bugDescription.toString());
        issueInputParameters.setReporterId(testCaseUser);
        if (Objects.nonNull(issueTypeF)) {
            issueInputParameters.setIssueTypeId(Objects.requireNonNull(issueTypeF).getId());
        } else {
            throw new NullPointerException();
        }
        issueInputParameters.setProjectId(Long.valueOf(projectID));
        issueInputParameters.setPriorityId(priority.getId());
        if (!(severityF.getOptionId() == null)) {
            issueInputParameters.addCustomFieldValue(severity.getId(), severityF.getOptionId().toString());
        }

        returnObject.put("testCaseUser", testCaseUser);
        returnObject.put("projectId", projectID);

        IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(userByKey, issueInputParameters);

        if (createValidationResult.isValid()) {
            IssueService.IssueResult createResult = issueService.create(userByKey, createValidationResult);
            IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
            IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
            Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
            List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
            int size = issueLinkManager.getIssueLinks(Long.valueOf(testCaseId)).size();
            Long sequence = (long) size + 1;
            try {
                issueLinkManager.createIssueLink(Long.valueOf(testCaseId), createResult.getIssue().getId(), linkTypes.get(0).getId(), sequence, userByKey);
                if (!createResult.isValid()) {
                    log.error(createResult.getErrorCollection().getFlushedErrorMessages().toString());
                }
            } catch (CreateException e) {
                log.error(e.getMessage(), e);
            }
            returnObject.put("id", createResult.getIssue().getId().toString());
            returnObject.put("description", createResult.getIssue().getDescription());
            returnObject.put("name", createResult.getIssue().getSummary());
            returnObject.put("key", createResult.getIssue().getKey());
        }
    }

    private Option getSeverity(Options options) {
        Option severityF = null;
        for (Option option1 : options) {
            if (option1.getValue().toLowerCase(Locale.ENGLISH).equals("medium")) {
                severityF = option1;
                break;
            }
        }
        return severityF;
    }

    private IssueType getIssueType(Collection<IssueType> issueTypesForProject) {
        IssueType issueTypeF = null;
        for (IssueType issueType : issueTypesForProject) {
            if (issueType.getName().toLowerCase(Locale.ENGLISH).equals("bug")) {
                issueTypeF = issueType;
                break;
            }
        }
        return issueTypeF;
    }
}
