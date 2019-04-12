package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.link.*;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestCaseManagementViewProjectIssues extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestCaseManagementViewProjectIssues.class);
    private static final long serialVersionUID = 4784178821513123785L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    //Project Key
    BrowseContext context;

    @Inject
    public TestCaseManagementViewProjectIssues(JiraAuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
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
        switch (action) {
            case "add":
                resp.getWriter().write(viewIssues(user));
                break;
            case "create":
                resp.getWriter().write(createLink(data, user));
                break;
            case "delete-issue-link":
                resp.getWriter().write(deleteIssueLink(data, user));
                break;
            case "get":
                resp.getWriter().write(getLinks(data, user));
                break;
            default:
                break;
        }
    }

    /*get All Issue keys  project*/
    private String viewIssues(String user) {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JSONObject groupedIssueObjs = new JSONObject();
        JSONObject groupedIssues = new JSONObject();
        List<JSONObject> issuesObj = new ArrayList<>();
        if (authenticationContext.getLoggedInUser().getUsername().toLowerCase(Locale.ENGLISH).equals(user)) {
            try {
                IssueManager issueManager = ComponentAccessor.getIssueManager();
                issueManager.getIssueObjects(issueManager.getIssueIdsForProject(Long.valueOf(selectedProjectKey))).forEach(issue -> {
                    if (!(issue.getIssueType() == null) && issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story")) {
                        JSONObject tempIssueObj = new JSONObject();
                        String issueType = issue.getIssueType().getName();
                        try {
                            tempIssueObj.put("key", issue.getKey());
                            tempIssueObj.put("description", issue.getSummary());
                            issuesObj.add(tempIssueObj);
                            groupedIssues.put(issueType, issuesObj);
                        } catch (JSONException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                });

                for (int u = 0; u < groupedIssues.names().length(); u++) {
                    String key = groupedIssues.names().getString(u);
                    List<JSONObject> groupedIssueList = (List<JSONObject>) groupedIssues.get(key);
                    JSONArray groupedIssues1 = getGroupedIssues(groupedIssueList);
                    groupedIssueObjs.put(key, groupedIssues1);
                }
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return groupedIssueObjs.toString();
        }
        return groupedIssueObjs.toString();

    }

    private JSONArray getGroupedIssues(List<JSONObject> groupedIssueList) {
        JSONArray groupedIssueArray = new JSONArray();
        try {

            for (int i = 0; i < groupedIssueList.size(); i++) {
                JSONObject issueobj = new JSONObject().put("text", groupedIssueList.get(i).getString("description"));
                JSONArray issuekeyArray = new JSONArray();
                for (JSONObject aGroupedIssueList : groupedIssueList) {
                    if (groupedIssueList.get(i).getString("description").equals(aGroupedIssueList.getString("description"))) {
                        issuekeyArray.put(aGroupedIssueList.getString("key"));
                    }
                }
                issueobj.put("children", issuekeyArray);
                groupedIssueArray.put(issueobj);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return groupedIssueArray;
    }

    private String createLink(String data, String user) {
        JsonElement elementObj = new JsonParser().parse(data);
        JsonObject jsonDataObj = elementObj.getAsJsonObject();
        String number = jsonDataObj.get("number").getAsString();
        int count = Integer.parseInt(number);
        JsonElement element = new JsonParser().parse(jsonDataObj.get("issues").getAsString());
        ArrayList<String> issueKeys = new ArrayList<>();
        JsonArray jsonData = element.getAsJsonArray();

        for (int i = 0; i < count; i++) {
            issueKeys.add(jsonData.get(i).getAsString());
        }

        String testcaseid = jsonDataObj.get("testcaseid").getAsString();
        long testcaseId = Long.parseLong(testcaseid);
        JSONArray jsonArray = new JSONArray();
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        UserManager userManager = ComponentAccessor.getUserManager();

        if (authenticationContext.getLoggedInUser().getUsername().toLowerCase(Locale.ENGLISH).equals(user)) {
            IssueLinkManager issueLinkManager;
            ApplicationUser applicationUser;
            LinkCollection linkCollection;
            issueLinkManager = ComponentAccessor.getIssueLinkManager();

            for (String issueKey : issueKeys) {
                long issueId = issueManager.getIssueByKeyIgnoreCase(issueKey).getId();
                applicationUser = userManager.getUserByName(user);
                IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
                Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
                List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
                int size = issueLinkManager.getIssueLinks(testcaseId).size();
                Long sequence = (long) size + 1;
                try {
                    issueLinkManager.createIssueLink(testcaseId, issueId, linkTypes.get(0).getId(), sequence, applicationUser);
                    linkCollection = issueLinkManager.getLinkCollection(issueManager.getIssueByKeyIgnoreCase(issueKey), applicationUser);
                    Collection<Issue> allIssues = linkCollection.getAllIssues();
                    for (Issue issue : allIssues) {
                        JSONObject issueObj = new JSONObject();
                        issueObj.put("issue_id", issue.getId());
                        issueObj.put("issue_name", issue.getKey());
                        issueObj.put("summary", issue.getSummary());
                        jsonArray.put(issueObj);

                    }
                } catch (JSONException | CreateException e) {
                    log.error(e.getMessage(), e);
                }
            }
            return jsonArray.toString();
        }
        return jsonArray.toString();
    }

    private String deleteIssueLink(String data, String testCaseUser) {
        JsonElement elementObj = new JsonParser().parse(data);
        JsonObject jsonDataObj = elementObj.getAsJsonObject();
        String testCaseId = jsonDataObj.get("testcaseid").getAsString();
        String issueKey = jsonDataObj.get("issuekey").getAsString();
        JSONArray jsonArray = new JSONArray();
        UserManager userManager = ComponentAccessor.getUserManager();

        if (authenticationContext.getLoggedInUser().getUsername().equals(testCaseUser)) {
            IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
            ApplicationUser applicationUser = userManager.getUserByName(testCaseUser);
            Collection<Issue> issues;
            MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(testCaseId));
            if (!testCaseUser.isEmpty()) {
                issues = issueLinkManager.getLinkCollection(issueObject, applicationUser).getAllIssues();
            } else {
                issues = issueLinkManager.getLinkCollection(issueObject, issueObject.getCreator()).getAllIssues();
            }
            IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
            Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
            List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
            for (Issue issue : issues) {
                if (!(issue.getIssueType() == null) && issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story") && issue.getKey().equals(issueKey)) {
                    IssueLink issueLink = issueLinkManager.getIssueLink(Long.valueOf(testCaseId), issue.getId(), linkTypes.get(0).getId());
                    issueLinkManager.removeIssueLink(issueLink, applicationUser);
                }
            }
            return jsonArray.toString();
        }
        return jsonArray.toString();
    }

    public void deleteIssueLinkForTestCase(String testCaseId, String user) {
        UserManager userManager = ComponentAccessor.getUserManager();
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
        ApplicationUser applicationUser = userManager.getUserByName(user);
        Collection<Issue> issues;
        MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(testCaseId));
        if (!user.isEmpty()) {
            issues = issueLinkManager.getLinkCollection(issueObject, applicationUser).getAllIssues();
        } else {
            issues = issueLinkManager.getLinkCollection(issueObject, issueObject.getCreator()).getAllIssues();
        }
        IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
        Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
        List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
        for (Issue issue : issues) {
            if (!(issue.getIssueType() == null) && issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story")) {
                IssueLink issueLink = issueLinkManager.getIssueLink(Long.valueOf(testCaseId), issue.getId(), linkTypes.get(0).getId());
                issueLinkManager.removeIssueLink(issueLink, applicationUser);
            }
        }
    }


    private String getLinks(String data, String user) {
        JsonElement elementObj = new JsonParser().parse(data);
        JsonObject jsonDataObj = elementObj.getAsJsonObject();
        String testCaseId = jsonDataObj.get("testcaseid").getAsString();
        long testcaseId = Long.parseLong(testCaseId);
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
        MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(testcaseId);

        JSONArray jsonIssues = new JSONArray();
        JSONObject jsonIssue;
        UserManager userManager = ComponentAccessor.getUserManager();
        ApplicationUser applicationUser = userManager.getUserByName(user);
        if (authenticationContext.getLoggedInUser().getUsername().toLowerCase(Locale.ENGLISH).equals(user)) {
            Collection<Issue> issues = issueLinkManager.getLinkCollection(issueObject, applicationUser).getAllIssues();
            for (Issue issue : issues) {
                if (issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story")) {
                    jsonIssue = new JSONObject();
                    try {
                        jsonIssue.put("issue_id", issue.getId());
                        jsonIssue.put("issue_name", issue.getKey());
                        jsonIssue.put("summary", issue.getSummary());
                        jsonIssues.put(jsonIssue);
                    } catch (JSONException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
        return jsonIssues.toString();
    }

}