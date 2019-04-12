package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * @author RPSPERERA on 7/2/2018
 */
@Scanned
public class TCase {

    private static final Logger log = LoggerFactory.getLogger(TCase.class);

    public Issue insert(int testCaseFolderId, String project, String description, String expectedResult, String user) {
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(project);
        Collection<IssueType> issueTypesForProject = ComponentAccessor.getIssueTypeSchemeManager().getIssueTypesForProject(projectObjByKey);
        IssueType issueTypeF = null;
        for (IssueType issueType : issueTypesForProject) {
            if (issueType.getName().toLowerCase(Locale.ROOT).equals("testcase")) {
                issueTypeF = issueType;
                break;
            }
        }
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setDescription(description);
        issueInputParameters.setIssueTypeId(Objects.requireNonNull(issueTypeF).getName());
        issueInputParameters
                .addCustomFieldValue(Objects.requireNonNull(customFieldManager.getCustomFieldObject("overall-expected-result")).getId(), expectedResult)
                .addCustomFieldValue(Objects.requireNonNull(customFieldManager.getCustomFieldObject("parent")).getId(), String.valueOf(testCaseFolderId));


        IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(userByKey, issueInputParameters);

        if (createValidationResult.isValid()) {
            IssueService.IssueResult createResult = issueService.create(userByKey, createValidationResult);
            if (!createResult.isValid()) {
                log.error("Insert failed " + createResult.isValid());
            }
        }

        return null;
    }

    public boolean update(String testcaseKey, String user) {
        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueByCurrentKey(testcaseKey);
        IssueService issueService = ComponentAccessor.getIssueService();
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setSummary("I am a new summary");
        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(userByKey, issueByCurrentKey.getId(), issueInputParameters);

        if (updateValidationResult.isValid()) {
            IssueService.IssueResult updateResult = issueService.update(userByKey, updateValidationResult);
            if (!updateResult.isValid()) {
                log.error("Update failed " + updateResult.isValid());
            }
        }
        return true;
    }

    public boolean delete(String testcaseKey, String user) {
        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueByCurrentKey(testcaseKey);
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueService.DeleteValidationResult deleteValidationResult = issueService.validateDelete(userByKey, issueByCurrentKey.getId());

        if (deleteValidationResult.isValid()) {
            ErrorCollection deleteResult = issueService.delete(userByKey, deleteValidationResult);
            if (deleteResult.hasAnyErrors()) {
                log.error("Delete failed " + deleteResult.hasAnyErrors());
            }
        }
        return true;
    }

//    private List<Issue> getIssues(String project) {
//
//        try {
//            Long projectId = ComponentAccessor.getProjectManager().getProjectObjByName(project).getId();
//            IssueManager issueMgr = ComponentAccessor.getIssueManager();
//            List<Issue> searchResults = new ArrayList<>();
//            issueMgr.getIssueObjects(issueMgr.getIssueIdsForProject(projectId)).forEach(issue -> {
//                if (Objects.requireNonNull(issue.getIssueType()).getName().equals("testcase")) {
//                    searchResults.add(issue);
//                }
//            });
//            return searchResults;
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//        return null;
//    }
}
