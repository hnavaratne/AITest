package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.DefectsEntity;
import com.virtusa.gto.plugins.aitest.entity.DefectsEntityI;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class Components extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(Components.class);
    private static final long serialVersionUID = 5316386774926194575L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;
    BrowseContext context;

    @Inject
    public Components(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("user");
        String data = req.getParameter("data");

        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            String action = req.getParameter("action");
            switch (action) {
                case "get-components":
                    JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
                    String release = jsonObject.get("release").getAsString();
                    resp.getWriter().write(getComponents(release, user));
                    break;
                case "get-test-cases-for-component":
                    JsonObject componentObj = new JsonParser().parse(data).getAsJsonObject();
                    String releaseSelected = componentObj.get("release").getAsString();
                    String componentName = componentObj.get("componentName").getAsString();
                    resp.getWriter().write(getTestCasesForComponent(componentName, releaseSelected, user));
                    break;
                case "get-all-test-cases-for-component":
                    resp.getWriter().write(getAllTestCases(data, user));
                    break;
                case "get-all-defects-for-testcase":
                    resp.getWriter().write(getDefectsForTestCase(data));
                    break;
                default:
                    break;
            }
        }
    }

    private String getComponents(String release, String user) {
        Project project = new GetProjectKey(context).getSelectedProject();
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        Collection<ProjectComponent> components = project.getComponents();
        Map<ProjectComponent, Integer> componentTestCaseCount = new HashMap<>();
        Map<ProjectComponent, List<Issue>> componentRelatedTestCaseList = new HashMap<>();
        Map<ProjectComponent, List<Issue>> requirementRelatedTestCaseList = new HashMap<>();
        JqlQueryBuilder builderRequirement = JqlQueryBuilder.newBuilder();
        for (ProjectComponent projectComponent : components) {
            List<Issue> componentRelatedIssueList = new ArrayList<>();
            JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
            SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);
            builder.where().project(project.getId()).and().component(projectComponent.getId()).and().issueType("TestCase");
            try {
                SearchResults results = searchProvider.searchOverrideSecurity(userName, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
                results.getIssues().forEach(issue -> {
                    componentRelatedIssueList.add(issue);
                    componentTestCaseCount.put(projectComponent, 0);
                });
            } catch (SearchException e) {
                log.error(e.getMessage(), e);
            }
            componentRelatedTestCaseList.put(projectComponent, componentRelatedIssueList);
        }
        SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);
        builderRequirement.where().issueType("TestCase").and().project(project.getId());
        getComponentsByVersion(release, userName, componentTestCaseCount, requirementRelatedTestCaseList, builderRequirement, searchProvider);
        JSONArray componentListArray = createUniqueComponents(componentRelatedTestCaseList, requirementRelatedTestCaseList);
        return componentListArray.toString();
    }

    private void getComponentsByVersion(String release, ApplicationUser userName, Map<ProjectComponent, Integer> componentTestCaseCount, Map<ProjectComponent, List<Issue>> requirementRelatedTestCaseList, JqlQueryBuilder builderRequirement, SearchService searchProvider) {
        try {
            SearchResults results = searchProvider.searchOverrideSecurity(userName, builderRequirement.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                Collection<Issue> issueLinks = ComponentAccessor.getIssueLinkManager().getLinkCollection(issue, userName).getAllIssues();
                if (issueLinks.size() > 0) {
                    getComponentsByReleasedVersions(release, componentTestCaseCount, requirementRelatedTestCaseList, issue, issueLinks);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void getComponentsByReleasedVersions(String release, Map<ProjectComponent, Integer> componentTestCaseCount, Map<ProjectComponent, List<Issue>> requirementRelatedTestCaseList, Issue issue, Collection<Issue> issueLinks) {
        issueLinks.forEach(issueLink -> {
            boolean hasRelease = false;
            if (Objects.equals(Objects.requireNonNull(issueLink.getIssueType()).getName().toLowerCase(Locale.ENGLISH), "story")) {
                for (Version version : issueLink.getFixVersions()) {
                    if (version.getName().equals(release)) {
                        hasRelease = true;
                        break;
                    }
                }
            }
            if (hasRelease) {
                for (ProjectComponent component : issueLink.getComponents()) {
                    List<Issue> requirementRelatedIssueList = new ArrayList<>();
                    if (componentTestCaseCount.containsKey(component)) {
                        requirementRelatedIssueList.add(issue);
                        Integer val = componentTestCaseCount.get(component);
                        componentTestCaseCount.put(component, val + 1);
                    }
                    requirementRelatedTestCaseList.put(component, requirementRelatedIssueList);
                }
            }
        });
    }

    private JSONArray createUniqueComponents(Map<ProjectComponent, List<Issue>> componentRelatedTestCaseList, Map<ProjectComponent, List<Issue>> requirementRelatedTestCaseList) {
        Map<ProjectComponent, Set<Issue>> uniqueComponentTestCaseList = removeDuplicates(componentRelatedTestCaseList, requirementRelatedTestCaseList);
        JSONArray componentListArray = new JSONArray();
        if (uniqueComponentTestCaseList.values().stream().mapToInt(Set::size).count() > 0) {
            uniqueComponentTestCaseList.forEach((component, value) -> {
                if (value.size() > 0) {
                    int totalTestCasesCount = uniqueComponentTestCaseList.values().stream().mapToInt(Set::size).sum();
                    try {
                        JSONObject componentObj = new JSONObject();
                        int componentPercentageCounter = 0;
                        componentPercentageCounter += value.size();
                        componentObj.put("ComponentName", component.getName());
                        int componentPercentage = componentPercentageCounter * 100 / totalTestCasesCount;
                        componentObj.put("Percentage", componentPercentage);
                        componentListArray.put(componentObj);
                    } catch (JSONException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
        return componentListArray;
    }

    private Map<ProjectComponent, Set<Issue>> removeDuplicates(Map<ProjectComponent, List<Issue>> componentRelatedList, Map<ProjectComponent, List<Issue>> requirementRelatedList) {
        Map<ProjectComponent, Set<Issue>> uniqueComponentList = new HashMap<>();
        if (componentRelatedList.size() >= requirementRelatedList.size()) {
            removeDuplicatesInComponent(componentRelatedList, requirementRelatedList, uniqueComponentList);
        } else {
            removeDuplicatesInRequirement(componentRelatedList, requirementRelatedList, uniqueComponentList);
        }
        return uniqueComponentList;
    }

    private void removeDuplicatesInComponent(Map<ProjectComponent, List<Issue>> componentRelatedList, Map<ProjectComponent, List<Issue>> requirementRelatedList, Map<ProjectComponent, Set<Issue>> uniqueComponentList) {
        componentRelatedList.forEach((component, issues) -> {
            Iterator<Map.Entry<ProjectComponent, List<Issue>>> entries = requirementRelatedList.entrySet().iterator();
            if (issues.size() > 0 && entries.hasNext()) {
                while (entries.hasNext()) {
                    Map.Entry<ProjectComponent, List<Issue>> entry = entries.next();
                    ProjectComponent componentName = entry.getKey();
                    List<Issue> issuesList = entry.getValue();
                    if (component.getName().equals(componentName.getName())) {
                        Set<Issue> uniqueIssueList = new HashSet<>();
                        uniqueIssueList.addAll(issues);
                        uniqueIssueList.addAll(issuesList);
                        uniqueComponentList.put(component, uniqueIssueList);
                    } else {
                        Set<Issue> uniqueIssueList = new HashSet<>(issues);
                        uniqueComponentList.put(component, uniqueIssueList);
                    }
                }
            } else {
                Set<Issue> uniqueIssueList = new HashSet<>(issues);
                uniqueComponentList.put(component, uniqueIssueList);
            }
        });
    }

    private void removeDuplicatesInRequirement(Map<ProjectComponent, List<Issue>> componentRelatedList, Map<ProjectComponent, List<Issue>> requirementRelatedList, Map<ProjectComponent, Set<Issue>> uniqueComponentList) {
        requirementRelatedList.forEach((component, issues) -> {
            Iterator<Map.Entry<ProjectComponent, List<Issue>>> entries = componentRelatedList.entrySet().iterator();
            if (issues.size() > 0 && entries.hasNext()) {
                while (entries.hasNext()) {
                    Map.Entry<ProjectComponent, List<Issue>> entry = entries.next();
                    ProjectComponent componentName = entry.getKey();
                    List<Issue> issuesList = entry.getValue();
                    if (component.getName().equals(componentName.getName())) {
                        Set<Issue> uniqueIssueList = new HashSet<>();
                        uniqueIssueList.addAll(issues);
                        uniqueIssueList.addAll(issuesList);
                        uniqueComponentList.put(component, uniqueIssueList);
                    } else {
                        Set<Issue> uniqueIssueList = new HashSet<>(issues);
                        uniqueComponentList.put(component, uniqueIssueList);
                    }
                }
            } else {
                Set<Issue> uniqueIssueList = new HashSet<>(issues);
                uniqueComponentList.put(component, uniqueIssueList);
            }
        });
    }

    private String getTestCasesForComponent(String component, String release, String user) {
        JSONObject requirementTestCaseJsonMapObj = new JSONObject();
        SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        Project project = new GetProjectKey(context).getSelectedProject();

        //Component Related Test Cases
        JqlQueryBuilder builderTCase = JqlQueryBuilder.newBuilder();
        SearchService searchProviderTestCases = ComponentAccessor.getComponentOfType(SearchService.class);
        builderTCase.where().project(project.getId()).and().component(component).and().issueType("TestCase");
        try {
            SearchResults results = searchProviderTestCases.searchOverrideSecurity(userName, builderTCase.buildQuery(), PagerFilter.getUnlimitedFilter());
            JSONArray testCases = new JSONArray();
            results.getIssues().forEach(issue -> {
                try {
                    JSONObject testCaseObj = new JSONObject();
                    testCaseObj.put(issue.getId().toString(), issue.getSummary());
                    testCases.put(testCaseObj);
                    requirementTestCaseJsonMapObj.put(component, testCases);
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }

        // Requirement Related Test Cases
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        builder.where().issueType("TestCase").and().project(project.getId());
        getTestCasesForReleasedComponents(component, release, requirementTestCaseJsonMapObj, searchProvider, userName, builder);
        return requirementTestCaseJsonMapObj.toString();
    }

    private void getTestCasesForReleasedComponents(String component, String release, JSONObject requirementTestCaseJsonMapObj, SearchService searchProvider, ApplicationUser userName, JqlQueryBuilder builder) {
        try {
            SearchResults results = searchProvider.searchOverrideSecurity(userName, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                try {
                    Collection<Issue> issueLinks = ComponentAccessor.getIssueLinkManager().getLinkCollection(issue, userName).getAllIssues();
                    for (Issue issueF : issueLinks) {
                        if (Objects.equals(Objects.requireNonNull(issueF.getIssueType()).getName().toLowerCase(Locale.ENGLISH), "story")) {
                            boolean hasRelease = false;
                            for (Version version : issueF.getFixVersions()) {
                                if (version.getName().equals(release)) {
                                    hasRelease = true;
                                    break;
                                }
                            }
                            if (hasRelease) {
                                componentHasRelease(component, requirementTestCaseJsonMapObj, issue, issueF);
                            }
                        }
                    }
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void componentHasRelease(String component, JSONObject requirementTestCaseJsonMapObj, Issue issue, Issue issueF) throws JSONException {
        for (ProjectComponent projectComponent : issueF.getComponents()) {
            JSONObject testCaseObj = new JSONObject();
            if (projectComponent.getName().equals(component)) {
                JSONArray testCases = new JSONArray();
                if (!requirementTestCaseJsonMapObj.has(issueF.getSummary())) {
                    testCaseObj.put(issue.getId().toString(), issue.getSummary());
                    testCases.put(testCaseObj);
                    requirementTestCaseJsonMapObj.put(issueF.getSummary(), testCases);

                } else if (requirementTestCaseJsonMapObj.has(issueF.getSummary())) {
                    testCases = (JSONArray) requirementTestCaseJsonMapObj.get(issueF.getSummary());
                    testCaseObj.put(issue.getId().toString(), issue.getSummary());
                    testCases.put(testCaseObj);
                    requirementTestCaseJsonMapObj.put(issueF.getSummary(), testCases);
                }
                break;
            }
        }
    }

    private String getAllTestCases(String data, String user) {
        JsonObject componentObj = new JsonParser().parse(data).getAsJsonObject();
        String component = componentObj.get("componentName").getAsString();
        String release = componentObj.get("release").getAsString();
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);
        Project project = new GetProjectKey(context).getSelectedProject();

        // Component Related Test Cases
        JqlQueryBuilder builderTCase = JqlQueryBuilder.newBuilder();
        SearchService searchProviderTestCases = ComponentAccessor.getComponentOfType(SearchService.class);
        builderTCase.where().project(project.getId()).and().component(component).and().issueType("TestCase");
        HashSet<JSONObject> objectHashSet = new HashSet<>();
        try {
            SearchResults results = searchProviderTestCases.searchOverrideSecurity(userName, builderTCase.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                try {
                    JSONObject testCaseObj = new JSONObject();
                    testCaseObj.put(issue.getId().toString(), issue.getSummary());
                    objectHashSet.add(testCaseObj);
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }

        // Requirement Related Test Cases
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        builder.where().issueType("TestCase").and().project(project.getId());
        try {
            SearchResults results = searchProvider.searchOverrideSecurity(userName, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                try {
                    Collection<Issue> issueLinks = ComponentAccessor.getIssueLinkManager().getLinkCollection(issue, userName).getAllIssues();
                    for (Issue issueF : issueLinks) {
                        if (Objects.equals(Objects.requireNonNull(issueF.getIssueType()).getName().toLowerCase(Locale.ENGLISH), "story")) {
                            boolean hasRelease = false;
                            for (Version version : issueF.getFixVersions()) {
                                if (version.getName().equals(release)) {
                                    hasRelease = true;
                                    break;
                                }
                            }
                            if (hasRelease) {
                                JSONObject testCaseObj = new JSONObject();
                                testCaseObj.put(issue.getId().toString(), issue.getSummary());
                                objectHashSet.add(testCaseObj);
                            }
                        }
                    }
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
        return objectHashSet.toString();
    }

    private String getDefectsForTestCase(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JsonArray defectJsonArray = new JsonArray();
        String testCaseId = jsonData.get("testcaseId").getAsString();
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        List<DefectsEntityI> defectList = defectsEntity.getTestCaseAllDefectList(testCaseId);
        defectList.forEach(defect -> {
            JsonObject defectJsonObj = new JsonObject();
            defectJsonObj.addProperty("name", defect.getDefectName());
            defectJsonArray.add(defectJsonObj);
        });
        return defectJsonArray.toString();
    }
}
