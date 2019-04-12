package com.virtusa.gto.plugins.aitest.swagger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.*;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.gson.*;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.servlet.TestCase;
import com.virtusa.gto.plugins.aitest.servlet.TestCaseManagementViewProjectIssues;
import com.virtusa.gto.plugins.aitest.util.CommonsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Path("/testmanagementservices")
public class SwaggerServices {

    private static final Logger log = LoggerFactory.getLogger(SwaggerServices.class);

    @JiraImport
    private ActiveObjects activeObjects;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    @Inject
    public SwaggerServices(ActiveObjects activeObjects, JiraAuthenticationContext authenticationContext) {
        this.activeObjects = activeObjects;
        this.authenticationContext = authenticationContext;
    }


    //done
    @GET
    @Path("/getTestManagementProject")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getProjectList() {
        try {
            List<Project> projects = ComponentAccessor.getProjectManager().getProjects();
            List<JsonObject> projectList = new ArrayList<>();
            projects.forEach(project -> {
                        JsonObject projectObject = new JsonObject();
                        projectObject.addProperty("name", project.getName());
                        projectObject.addProperty("id", project.getId());
                        projectList.add(projectObject);
                    }
            );
            return Response.ok(projectList.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }

    }

    //done
    @POST
    @Path("/create-ai-test-TestCase")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createTestCase(String data) {
        String project_key = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("project_key").getAsString();
        String user = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("user").getAsString();
        int testCaseFolderId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsInt();
        String name = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("testcase_name").getAsString();
        String description = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("description").getAsString();
        String overallExpectedResult = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("overall_expected_result").getAsString();
        String automated = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("automated").getAsString();
        String manual = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("manual").getAsString();
        Project projectObjByKey;
        try {
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            projectObjByKey = projectManager.getProjectObjByKey(project_key);
            Collection<IssueType> issueTypesForProject = ComponentAccessor.getIssueTypeSchemeManager().getIssueTypesForProject(projectObjByKey);
            IssueType issueTypeF = null;
            for (IssueType issueType : issueTypesForProject) {
                if (issueType.getName().toLowerCase(Locale.ENGLISH).equals("testcase")) {
                    issueTypeF = issueType;
                    break;
                }
            }
            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            issueInputParameters.setSummary(name);
            issueInputParameters.setDescription(description);
            issueInputParameters.setReporterId(user);
            issueInputParameters.setIssueTypeId(Objects.requireNonNull(issueTypeF).getId());
            issueInputParameters.setProjectId(projectObjByKey.getId());
            CustomField automated1 = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Automated");
            Options options = ComponentAccessor.getOptionsManager().getOptions(automated1.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
            Option optionAutomated = null;
            Option optionManual = null;
            for (Option option1 : options) {
                if (option1.getValue().toLowerCase(Locale.ENGLISH).equals(automated.toLowerCase(Locale.ENGLISH))) {
                    optionAutomated = option1;
                    break;
                }
            }
            CustomField manual1 = customFieldManager.getCustomFieldObjectByName("Manual");
            Options optionsManual = ComponentAccessor.getOptionsManager().getOptions(manual1.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
            for (Option option : optionsManual) {
                if (option.getValue().toLowerCase(Locale.ENGLISH).equals(manual.toLowerCase(Locale.ENGLISH))) {
                    optionManual = option;
                    break;
                }
            }
            issueInputParameters
                    .addCustomFieldValue(automated1.getId(), Objects.requireNonNull(Objects.requireNonNull(optionAutomated).getOptionId()).toString())
                    .addCustomFieldValue(manual1.getId(), Objects.requireNonNull(Objects.requireNonNull(optionManual).getOptionId()).toString())
                    .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("OverallExpectedResult").getId(), overallExpectedResult)
                    .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("ParentFolder").getId(), String.valueOf(testCaseFolderId));

            IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(userByKey, issueInputParameters);
            JSONObject returnObject = new JSONObject();
            if (createValidationResult.isValid()) {
                IssueService.IssueResult createResult = issueService.create(userByKey, createValidationResult);
                if (!createResult.isValid()) {
                    log.error(createResult.getErrorCollection().getFlushedErrorMessages().toString());
                }
                try {
                    returnObject.put("id", createResult.getIssue().getId().toString());
                    returnObject.put("name", createResult.getIssue().getSummary());
                    returnObject.put("automated", createResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("Automated").iterator().next()));
                    returnObject.put("manual", createResult.getIssue().getCustomFieldValue(manual1));
                    returnObject.put("parent", createResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("ParentFolder").iterator().next()));
                    returnObject.put("overallExpectedResult", createResult.getIssue().getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("OverallExpectedResult").iterator().next()));
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            }
            return Response.ok(returnObject.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //done.
    @GET
    @Path("/get-ai-test-TestCase/{testCaseId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestCase(@PathParam("testCaseId") long testCaseId) {
        JSONObject testcaseObject = new JSONObject();
        try {
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            IssueManager issueMgr = ComponentAccessor.getIssueManager();
            Issue issue = issueMgr.getIssueObject(testCaseId);
            testcaseObject.put("id", issue.getId());
            testcaseObject.put("name", issue.getSummary());
            testcaseObject.put("description", issue.getDescription());
            testcaseObject.put("overall_expected_result", issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("OverallExpectedResult").iterator().next()));
            testcaseObject.put("automated", issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("Automated").iterator().next()));
            testcaseObject.put("manual", issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("Manual").iterator().next()));
            return Response.ok(testcaseObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //done
    @POST
    @Path("/update-ai-test-TestCase")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response editTestCase(String data) {
        JsonObject updatedSet = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject();
        long testCaseId = updatedSet.get("test_case_id").getAsLong();
        String user = updatedSet.get("user").getAsString();
        String tes_case_name = updatedSet.get("test_case_name").getAsString();
        String tes_case_description = updatedSet.get("test_case_description").getAsString();
        boolean isUpdated = false;
        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueObject(testCaseId);
        IssueService issueService = ComponentAccessor.getIssueService();
        UserStoryEntityInstance userStoryEntityInstance = new UserStoryEntityInstance(activeObjects);
        userStoryEntityInstance.updateTestCase(String.valueOf(testCaseId), tes_case_name);
        ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setSummary(tes_case_name);
        issueInputParameters.setDescription(tes_case_description);
        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(userByKey, issueByCurrentKey.getId(), issueInputParameters);
        if (updateValidationResult.isValid()) {
            IssueService.IssueResult updateResult = issueService.update(userByKey, updateValidationResult);
            if (!updateResult.isValid()) {
                log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
            }
            isUpdated = true;
        }
        if (isUpdated) {
            return Response.ok().build();
        } else {
            Response.ResponseBuilder status = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return status.build();
        }
    }

    //done
    @POST
    @Path("/delete-ai-test-TestCase")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteTestCase(String data) {
        long testCaseId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("test_case_id").getAsLong();
        String user = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("user").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueObject(testCaseId);
            ApplicationUser userByKey = ComponentAccessor.getUserManager().getUserByKey(user);
            IssueService issueService = ComponentAccessor.getIssueService();
            TestCaseManagementViewProjectIssues tcmvpi = new TestCaseManagementViewProjectIssues(authenticationContext);
            UserStoryEntityInstance usei = new UserStoryEntityInstance(activeObjects);
            DefectsEntity defectEntity = new DefectsEntity(activeObjects);
            defectEntity.deleteDefectsForTestCase(String.valueOf(testCaseId));
            tcmvpi.deleteIssueLinkForTestCase(String.valueOf(testCaseId), user);
            usei.deleteTestCase(String.valueOf(testCaseId));
            IssueService.DeleteValidationResult deleteValidationResult = issueService.validateDelete(userByKey, issueByCurrentKey.getId());
            if (deleteValidationResult.isValid()) {
                ErrorCollection deleteResult = issueService.delete(userByKey, deleteValidationResult);
                if (deleteResult.hasAnyErrors()) {
                    log.error(deleteResult.getFlushedErrorMessages().toString());
                    returnObject.put("isDeleted", false);
                }
                returnObject.put("isDeleted", true);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
        return Response.ok(returnObject.toString()).build();
    }

    @GET
    @Path("/getTestManagementTestCases/{project_key}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestCaseHierarchy(@PathParam("project_key") String project_key) {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(project_key);
        List<Issue> issueList = new SwaggerServicesHelper().getIssues(projectObjByKey.getId());
        TestCaseFolder[] testCaseFolders = activeObjects.find(TestCaseFolder.class);
        List<TestCaseFolder> testCaseFolders1 = Arrays.asList(testCaseFolders);
        Map<Integer, TestCaseFolder> parent = new HashMap<>();
        for (TestCaseFolder testCaseFolder : testCaseFolders1) {
            String testCaseFolderParent = testCaseFolder.getParent();
            if (testCaseFolderParent.equals("null")) {
                parent.put(testCaseFolder.getID(), testCaseFolder);
            }
        }
        List<Object> jsonElements = new ArrayList<>();
        //iterate parents where parentId id null
        parent.forEach((parentId, parentFolder) -> {
            Map<String, Object> parentFolders = new HashMap<>();
            parentFolders.put("name", parentFolder.getName());
            parentFolders.put("id", parentId);
            parentFolders.put("type", "TESTSUITE");
            List<Object> childFolder = new ArrayList<>();
            //get the children entities
            new SwaggerServicesHelper(activeObjects).getChildren(testCaseFolders1, String.valueOf(parentFolder.getID()), childFolder, issueList);
            if (childFolder.size() == 0) {
                new SwaggerServicesHelper(activeObjects).getLeafNodes(issueList, String.valueOf(parentFolder.getID()), childFolder);
            }
            parentFolders.put("entity", childFolder);
            jsonElements.add(parentFolders);
        });
        //add root testcases
        List<Issue> issueByParent = new SwaggerServicesHelper().getIssueByParent(Objects.requireNonNull(issueList), "0");
        issueByParent.forEach(issue -> {
            Map<String, Object> jsonObjectF = new HashMap<>();
            jsonObjectF.put("name", issue.getSummary());
            jsonObjectF.put("id", issue.getId());
            jsonObjectF.put("type", "TESTCASE");
            List<Map<String, Object>> attachedScriptObject = new SwaggerServicesHelper().getAttachedScriptObject(issue);
            jsonObjectF.put("scripts", attachedScriptObject);
            String testCaseId = String.valueOf(issue.getId());
            List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
            List<Map<String, Object>> testSteps = new SwaggerServicesHelper().getTestSteps(testCaseId);
            jsonObjectF.put("steps", testSteps);
            jsonObjectF.put("params", mappedVariables);
            jsonElements.add(jsonObjectF);
        });
        Response.status(Response.Status.OK.getStatusCode());
        Gson gson = new Gson();
        return Response.ok(gson.toJson(jsonElements)).build();

    }

    //Done
    @POST
    @Path("/create-ai-test-TestCase-TestSteps")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createTestStep(String data) {
        long testCaseId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("testCaseId").getAsLong();
        JsonArray steps = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("steps").getAsJsonArray();
        String selectedProjectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(selectedProjectKey);
        try {
            JSONArray resultArray = new JSONArray();
            TSteps tSteps = new TSteps(activeObjects);
            TestStepParam testStepParam = new TestStepParam(activeObjects);
            TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
            BComponentStep bComponentStep = new BComponentStep(activeObjects);
            tSteps.delete(String.valueOf(testCaseId));
            TestStepParams[] testStepParams = testStepParam.get(String.valueOf(testCaseId));
            List<String> oldParamList = new ArrayList<>();
            for (TestStepParams stepParam : testStepParams) {
                String paramName = stepParam.getParamName();
                oldParamList.add(paramName);
            }
            List<String> newParamList = new ArrayList<>();
            JSONArray stepParamArray = new JSONArray();
            for (JsonElement step : steps) {
                JsonObject stepObject = step.getAsJsonObject();
                String stepString = new SwaggerServicesHelper().getValue("step", stepObject);
                String expectedString = new SwaggerServicesHelper().getValue("expectedResult", stepObject);
                String dataString = new SwaggerServicesHelper().getValue("data", stepObject);
                String scriptIdString = new SwaggerServicesHelper().getValue("function", stepObject);
                TestSteps stepUpserted = tSteps.insert(String.valueOf(projectObjByKey.getId()), String.valueOf(testCaseId), stepString, expectedString, dataString, scriptIdString);
                if (Objects.nonNull(stepUpserted)) {
                    new SwaggerServicesHelper().getUpsertedSteps(resultArray, bComponentStep, newParamList, stepParamArray, dataString, scriptIdString, stepUpserted);
                }
            }
            new SwaggerServicesHelper().removeDuplicates(selectedProjectKey, testStepParam, testStepParamValue, String.valueOf(testCaseId), oldParamList, newParamList);
            return Response.ok(resultArray.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //done
    @GET
    @Path("/getTestManagementTestSteps/{testCaseId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestSteps(@PathParam("testCaseId") long testCaseId) {
        try {
            JSONObject returnData = new JSONObject();
            JSONArray stepsArray = new JSONArray();
            TSteps tSteps = new TSteps(activeObjects);
            List<TestSteps> steps = tSteps.get(String.valueOf(testCaseId));
            for (TestSteps step : steps) {
                JSONObject returnObject = new JSONObject();
                returnObject.put("step", step.getStep());
                returnObject.put("expectedResult", step.getExpectedResult());
                returnObject.put("data", step.getData());
                returnObject.put("function", step.getScriptId());
                stepsArray.put(returnObject);
            }
            returnData.put("testCaseId", String.valueOf(testCaseId));
            returnData.put("steps", stepsArray);
            return Response.ok(returnData.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/upsertTestManagementTestData")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response upsertTestData(String data) {
        try {
            long testCaseId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("testCaseId").getAsLong();
            String testData = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("testData").getAsString();
            TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
            String paramName;
            JSONObject dataObject;
            JSONArray valuesJArray;
            JSONArray testDataArray = new JSONArray(testData);
            if (testDataArray.length() > 0) {
                for (int i = 0; i < testDataArray.length(); i++) {
                    dataObject = (JSONObject) testDataArray.get(i);
                    paramName = dataObject.getString("name");
                    valuesJArray = dataObject.getJSONArray("values");
                    testStepParamValue.deleteParams(String.valueOf(testCaseId), paramName);
                    for (int j = 0; j < valuesJArray.length(); j++) {
                        testStepParamValue.upsert(String.valueOf(testCaseId), paramName, valuesJArray.getString(j));
                    }
                }
            } else {
                testStepParamValue.deleteAll(String.valueOf(testCaseId));
            }
            return Response.ok(testDataArray.toString()).build();
        } catch (JSONException | NullPointerException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/attachUserStory")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response attachUserStory(String data) {
        long testCaseId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("testCaseId").getAsLong();
        String user = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("user").getAsString();
        String number = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("number").getAsString();
        String issues = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("issues").getAsString();
        try {
            int count = Integer.parseInt(number);
            JsonElement element = new JsonParser().parse(issues);
            ArrayList<String> issueKeys = new ArrayList<>();
            JsonArray jsonData = element.getAsJsonArray();

            for (int i = 0; i < count; i++) {
                issueKeys.add(jsonData.get(i).getAsString());
            }
            JSONArray returnArray = new JSONArray();
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
                    int size = issueLinkManager.getIssueLinks(testCaseId).size();
                    Long sequence = (long) size + 1;

                    issueLinkManager.createIssueLink(testCaseId, issueId, linkTypes.get(0).getId(), sequence, applicationUser);
                    linkCollection = issueLinkManager.getLinkCollection(issueManager.getIssueByKeyIgnoreCase(issueKey), applicationUser);
                    Collection<Issue> allIssues = linkCollection.getAllIssues();
                    for (Issue issue : allIssues) {
                        JSONObject issueObj = new JSONObject();
                        issueObj.put("issue_id", issue.getId());
                        issueObj.put("issue_name", issue.getKey());
                        issueObj.put("summary", issue.getSummary());
                        returnArray.put(issueObj);
                    }
                }
            }
            return Response.ok(returnArray.toString()).build();
        } catch (JSONException | CreateException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/detachUserStory")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteUserStory(String data) {
        long testCaseId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("testCaseId").getAsLong();
        String user = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("user").getAsString();
        String issueKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("issueKey").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            UserManager userManager = ComponentAccessor.getUserManager();
            if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
                ApplicationUser applicationUser = userManager.getUserByName(user);
                Collection<Issue> issues;
                MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(testCaseId);
                if (!user.isEmpty()) {
                    issues = issueLinkManager.getLinkCollection(issueObject, applicationUser).getAllIssues();
                } else {
                    issues = issueLinkManager.getLinkCollection(issueObject, issueObject.getCreator()).getAllIssues();
                }
                IssueLinkTypeManager IssueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
                Collection<IssueLinkType> issueLinkTypes = IssueLinkTypeManager.getIssueLinkTypes();
                List<IssueLinkType> linkTypes = issueLinkTypes.stream().filter(issueLinkType -> Objects.equals(issueLinkType.getName(), "Relates")).collect(Collectors.toList());
                for (Issue issue : issues) {
                    if (!(issue.getIssueType() == null) && issue.getIssueType().getName().toLowerCase(Locale.ENGLISH).equals("story") && issue.getKey().equals(issueKey)) {
                        IssueLink issueLink = issueLinkManager.getIssueLink(testCaseId, issue.getId(), linkTypes.get(0).getId());
                        issueLinkManager.removeIssueLink(issueLink, applicationUser);
                        returnObject.put("isRemoved", true);
                    }
                }
            }
            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/create-ai-test-TestCase-BusinessFolder")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createBusinessFolder(String data) {
        String parent = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsString();
        String name = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("name").getAsString();
        String description = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("description").getAsString();
        String selectedProjectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            BComponentFolder businessComponentFolder = new BComponentFolder(activeObjects);
            BusComFolder insertedFolder = businessComponentFolder.insert(selectedProjectKey, parent, name, description);
            returnObject.put("id", insertedFolder.getID());
            returnObject.put("name", insertedFolder.getName());
            returnObject.put("parent", insertedFolder.getParent());
            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/updateBusinessFolder")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateBusinessFolder(String data) {
        String parent = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsString();
        String oldName = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("oldName").getAsString();
        String newName = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("newName").getAsString();
        String projectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            BComponentFolder businessComponentFolder = new BComponentFolder(activeObjects);
            boolean update = businessComponentFolder.update(projectKey, parent, oldName, newName);
            if (update) {
                returnObject.put("isUpdated", true);
            } else {
                returnObject.put("isUpdated", false);
            }

            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/deleteBusinessFolder")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteBusinessFolder(String data) {
        String parent = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsString();
        String projectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            List<BusComFolder> folderForRemove = new ArrayList<>();
            BComponentFolder businessFolder = new BComponentFolder(activeObjects);
            BComponentScript businessScript = new BComponentScript(activeObjects);
            BComponentStep businessStep = new BComponentStep(activeObjects);

            List<BusComScript> currentScripts = businessScript.getAll(projectKey, parent);
            List<BusComScript> scriptForRemove = new ArrayList<>(currentScripts);
            List<BusComFolder> folders = businessFolder.get(projectKey, parent);

            for (BusComFolder busComFolder : folders) {
                folderForRemove.add(busComFolder);
                List<BusComScript> scripts = businessScript.getAll(projectKey, Long.toString(busComFolder.getID()));
                scriptForRemove.addAll(scripts);
                new SwaggerServicesHelper().recursiveSearch(Long.toString(busComFolder.getID()), scriptForRemove, folderForRemove, projectKey);
            }
            for (BusComScript aScriptForRemove : scriptForRemove) {
                businessStep.deleteTestStepsById(Long.toString(aScriptForRemove.getID()));
            }
            for (BusComScript aScriptForRemove : scriptForRemove) {
                businessScript.delete(projectKey, Long.toString(aScriptForRemove.getID()));
            }
            for (BusComFolder aFolderForRemove : folderForRemove) {
                businessFolder.delete(projectKey, Long.toString(aFolderForRemove.getID()));
            }

            boolean delete = businessFolder.delete(projectKey, parent);
            if (delete) {
                returnObject.put("isDeleted", true);
            } else {
                returnObject.put("isDeleted", false);
            }
            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //
    @GET
    @Path("/getBusinessFolders/{parent}/{projectKey}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestSteps(@PathParam("projectKey") String projectKey, @PathParam("parent") String parent) {
        try {
            JSONObject returnData = new JSONObject();
            JSONArray folderArray = new JSONArray();
            BComponentFolder bComponentFolder = new BComponentFolder(activeObjects);
            List<BusComFolder> folders = bComponentFolder.get(projectKey, parent);
            for (BusComFolder folder : folders) {
                JSONObject returnObject = new JSONObject();
                returnObject.put("id", folder.getID());
                returnObject.put("name", folder.getName());
                returnObject.put("parent", folder.getParent());
                folderArray.put(returnObject);
            }
            returnData.put("folders", folderArray);
            returnData.put("businessScripts", new SwaggerServicesHelper().getBusinessFunctions(parent, projectKey));

            return Response.ok(returnData.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/createBusinessScript")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createBusinessScript(String data) {
        String businessScriptFolderId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsString();
        String selectedProjectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        String name = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("name").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            BComponentScript businessScript = new BComponentScript(activeObjects);
            BusComScript insertedScript = businessScript.insert(selectedProjectKey, businessScriptFolderId, name);
            returnObject.put("id", insertedScript.getID());
            returnObject.put("parent", insertedScript.getParent());
            returnObject.put("name", insertedScript.getName());
            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/updateBusinessScript")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateBusinessScript(String data) {
        long businessScriptID = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("id").getAsLong();
        String oldName = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("oldName").getAsString();
        String updatedName = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("newName").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            BComponentScript businessScript = new BComponentScript(activeObjects);
            boolean update = businessScript.update(Long.toString(businessScriptID), oldName, updatedName);
            if (update) {
                returnObject.put("isUpdated", true);
            } else {
                returnObject.put("isUpdated", false);
            }
            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/deleteBusinessScript")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteBusinessScript(String data) {
        long businessScriptID = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("id").getAsLong();
        String selectedProjectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            BComponentScript businessScript = new BComponentScript(activeObjects);
            BComponentStep businessStep = new BComponentStep(activeObjects);
            boolean stepsDeleted = businessStep.deleteTestStepsById(Long.toString(businessScriptID));
            if (stepsDeleted) {
                boolean delete = businessScript.delete(selectedProjectKey, Long.toString(businessScriptID));
                if (delete) {
                    returnObject.put("isDeleted", true);
                } else {
                    returnObject.put("isDeleted", false);
                }
            }
            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //done
    @GET
    @Path("/get-create-ai-test-TestCase-BusinessScripts/{projectKey}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getBusinessScripts(@PathParam("projectKey") String projectKey) {
        JSONArray businessScriptArray = new JSONArray();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
        BComponentScript businessScriptObject = new BComponentScript(activeObjects);
        List<BusComScript> businessScripts = businessScriptObject.get(String.valueOf(projectObjByKey.getId()));
        businessScripts.forEach(script -> {
            JSONObject scriptJsonObject = new JSONObject();
            try {
                scriptJsonObject.put("id", script.getID());
                scriptJsonObject.put("name", script.getName());
                scriptJsonObject.put("parent", script.getParent());
                businessScriptArray.put(scriptJsonObject);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        });
        return Response.ok(businessScriptArray.toString()).build();
    }

    //done
    @GET
    @Path("/getAllChildTestSuites/{projectKey}/{parent}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAllChildTestSuites(@PathParam("projectKey") String projectKey, @PathParam("parent") String parent) {
        try {
            JSONObject dataObject = new JSONObject();
            dataObject.put("parent", parent);
            JSONArray folderArray = new JSONArray();
            TCFolder tcFolder = new TCFolder(activeObjects);
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
            List<TestCaseFolder> folders = tcFolder.get(String.valueOf(projectObjByKey.getId()), parent);
            for (TestCaseFolder folder : folders) {
                JSONObject returnObject = new JSONObject();
                try {
                    returnObject.put("id", folder.getID());
                    returnObject.put("name", folder.getName());
                    returnObject.put("parent", folder.getParent());
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
                folderArray.put(returnObject);
            }
            return Response.ok(folderArray.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //done
    @GET
    @Path("/getAllTestSuites/{projectKey}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAllTestPlans(@PathParam("projectKey") String projectKey) {

        JSONArray folderArray = new JSONArray();
        TCFolder tcFolder = new TCFolder(activeObjects);
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
        List<TestCaseFolder> folders = tcFolder.get(String.valueOf(projectObjByKey.getId()));
        for (TestCaseFolder folder : folders) {
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("id", folder.getID());
                returnObject.put("name", folder.getName());
                returnObject.put("parent", folder.getParent());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return Response.serverError().build();
            }
            folderArray.put(returnObject);
        }
        return Response.ok(folderArray.toString()).build();
    }

    //done
    @GET
    @Path("/getTestSuiteTestCases/{projectKey}/{testSuiteId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestSuiteTestCases(@PathParam("projectKey") String projectKey, @PathParam("testSuiteId") String testSuiteId) {

        TestCase testCase = new TestCase(authenticationContext, activeObjects);
        JsonObject dataObject = new JsonObject();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
        dataObject.addProperty("projectKey", String.valueOf(projectObjByKey.getId()));
        dataObject.addProperty("parent", testSuiteId);
        String issues = testCase.getIssues(dataObject.toString());
        return Response.ok(issues).build();
    }

    //done
    @POST
    @Path("/createTestSuite")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createTestSuite(String data) {
        String parent = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsString();
        String selectedProjectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(selectedProjectKey);
        String name = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("name").getAsString();
        String description = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("description").getAsString();
        JSONObject returnObject = new JSONObject();
        try {
            TCFolder tcFolder = new TCFolder(activeObjects);
            TestCaseFolder inserted = tcFolder.insert(String.valueOf(projectObjByKey.getId()), parent, name, description);
            returnObject.put("id", inserted.getID());
            returnObject.put("name", inserted.getName());
            returnObject.put("parent", inserted.getParent());

            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/updateTestPlan")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateTestPlan(String data) {
        String parent = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("parent").getAsString();
        String projectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        String newName = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("newName").getAsString();
        String oldName = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("oldName").getAsString();
        JSONObject returnObject = new JSONObject();
        TCFolder tcFolder = new TCFolder(activeObjects);
        boolean update = tcFolder.update(projectKey, parent, oldName, newName);
        try {
            if (update) {
                returnObject.put("isUpdated", true);
            } else {
                returnObject.put("isUpdated", false);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
        return Response.ok(returnObject.toString()).build();
    }

    @POST
    @Path("/deleteTestPlan")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response deleteTestPlan(String data) {
        String selectedProjectKey = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("projectKey").getAsString();
        String folderId = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("folderId").getAsString();
        String user = new SwaggerServicesHelper().toJsonElement(data).getAsJsonObject().get("user").getAsString();
        ArrayList<Integer> foldersID = new ArrayList<>();
        JSONObject returnObject = new JSONObject();
        try {
            TCFolder tcFolder = new TCFolder(activeObjects);
            List<TestCaseFolder> folders = tcFolder.get(selectedProjectKey, folderId);
            foldersID.add(Integer.valueOf(folderId));
            for (TestCaseFolder folder : folders) {
                foldersID.add(folder.getID());
                new SwaggerServicesHelper().readFolderInfo(Integer.toString(folder.getID()), foldersID, selectedProjectKey);
            }
            TestCase testCase = new TestCase(authenticationContext, activeObjects);
            for (Integer parentFolderID : foldersID) {
                String id = Integer.toString(parentFolderID);
                testCase.removeIssue(new JSONObject().put("parent", id).toString(), user);
                JsonObject folderObj = new JsonObject();
                folderObj.addProperty("id", parentFolderID);
            }
            returnObject.put("isDeleted", true);

            return Response.ok(returnObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/getVersions/{projectKey}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getVersions(@PathParam("projectKey") String projectKey) {
        try {
            Collection<Version> versions;
            JSONArray returnArray = new JSONArray();
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
            if (Objects.nonNull(projectObjByKey)) {
                versions = Objects.requireNonNull(projectObjByKey).getVersions();
                for (Version version : versions) {
                    JSONObject returnObject = new JSONObject();
                    returnObject.put("id", version.getId());
                    returnObject.put("name", version.getName());
                    returnObject.put("description", version.getDescription());
                    returnObject.put("selected", false);
                    if (Objects.nonNull(version.getStartDate())) {
                        returnObject.put("startDate", version.getStartDate().toString());
                    } else {
                        returnObject.put("startDate", "");
                    }
                    if (Objects.nonNull(version.getReleaseDate())) {
                        returnObject.put("releaseDate", version.getReleaseDate().toString());
                    } else {
                        returnObject.put("releaseDate", "");
                    }
                    returnArray.put(returnObject);
                }
            }
            return Response.ok(returnArray.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    //done
    @GET
    @Path("/get-ai-test-testplans/{projectKey}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestPlans(@PathParam("projectKey") String projectKey) {

        JSONArray jsonArray = new JSONArray();
        TPlan tPlan = new TPlan(activeObjects);
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
        List<TestPlan> testPlans = tPlan.get(String.valueOf(projectObjByKey.getId()));
        for (TestPlan testPlan : testPlans) {
            JSONObject returnPlanTCObject = new JSONObject();
            try {
                returnPlanTCObject.put("id", testPlan.getID());
                returnPlanTCObject.put("name", testPlan.getName());
                jsonArray.put(returnPlanTCObject);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                Response.ResponseBuilder status = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return status.build();
            }
        }
        return Response.ok(jsonArray.toString()).build();
    }

    //done
    @GET
    @Path("/get-ai-test-testplan-testcases/{projectKey}/{testPlanId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestPlanTestCases(@PathParam("projectKey") String projectKey, @PathParam("testPlanId") String testPlanId) {
        JSONArray jsonArray = new JSONArray();

        TPTCase tptCase = new TPTCase(activeObjects);
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
        List<TestPlanTestCases> TestPlanTestCases = tptCase.get(testPlanId);
        TPlan tPlan = new TPlan(activeObjects);

        for (TestPlanTestCases testPlanTestCase : TestPlanTestCases) {
            try {
                JSONObject returnPlanTCObject = new JSONObject();
                returnPlanTCObject.put("test_plan_id", testPlanTestCase.getTestPlanId());
                List<TestPlan> testPlans = tPlan.getById(String.valueOf(projectObjByKey.getId()), testPlanTestCase.getTestPlanId());
                returnPlanTCObject.put("test_plan_name", testPlans.get(0).getName());
                returnPlanTCObject.put("test_case_id", testPlanTestCase.getTestCaseId());

                MutableIssue issueObject = issueManager.getIssueObject(new Long(testPlanTestCase.getTestCaseId()));
                returnPlanTCObject.put("test_case_name", issueObject.getSummary());
                returnPlanTCObject.put("test_case_description", issueObject.getDescription());
                returnPlanTCObject.put("overall_expected_result", issueObject.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("OverallExpectedResult").iterator().next()));
                returnPlanTCObject.put("automated", issueObject.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("Automated").iterator().next()));
                returnPlanTCObject.put("manual", issueObject.getCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("Manual").iterator().next()));
                jsonArray.put(returnPlanTCObject);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                Response.ResponseBuilder status = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return status.build();
            }
        }
        return Response.ok(jsonArray.toString()).build();
    }

    @GET
    @Path("/getExecutedTestCaseData/{projectKey}/{testCaseId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getExecutedTestCaseData(@PathParam("projectKey") String projectKey, @PathParam("testCaseId") String testCaseId) {
        try {
            JSONObject returnPlanTCObject = new JSONObject();
            EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
            EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
            EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
            List<ExecEntityI> eEntityInstanceAll = eEntityInstance.get(String.valueOf(projectObjByKey.getId()));
            for (ExecEntityI execEntityI : eEntityInstanceAll) {
                List<ExecEntityTPI> execEntityTPIS = eEntityPlanInstance.get(execEntityI.getID(), String.valueOf(projectObjByKey.getId()));
                for (ExecEntityTPI execEntityTPI : execEntityTPIS) {
                    List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanTestCaseInstance.get(execEntityTPI.getID());
                    for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                        if (execEntityTPTCI.getTestCaseId().equals(testCaseId)) {
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
                        }
                    }
                }
            }
            return Response.ok(returnPlanTCObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/getExecutedEntities/{projectKey}/{testCaseId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getExecutionEntities(@PathParam("projectKey") String projectKey, @PathParam("testCaseId") String testCaseId) {
        try {
            JSONArray returnArray = new JSONArray();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
            List<ExecEntityI> eEntityInstanceAll = eEntityInstance.get(projectKey);
            for (ExecEntityI execEntityI : eEntityInstanceAll) {
                JSONObject returnEntityObject = new JSONObject();
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
                returnArray.put(returnEntityObject);
            }
            return Response.ok(returnArray.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/getExecutedPlans/{projectKey}/{execEntityInstanceId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getExecutedPlans(@PathParam("projectKey") String projectKey, @PathParam("execEntityInstanceId") String execEntityInstanceId) {
        try {
            JSONObject returnEntityObject = new JSONObject();
            EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
            JSONArray returnEntityPlanArray = new JSONArray();
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
            return Response.ok(returnEntityObject.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/getExecutedTestCases/{execEntityTPInstanceId}/{hasUserStory}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getExecutedTestCases(@PathParam("execEntityTPInstanceId") String execEntityTPInstanceId, @PathParam("hasUserStory") boolean hasUserStory) {
        try {
            EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
            // JSONObject returnPlanObject = new JSONObject();
            JSONArray returnEntityPlanTCArray = new JSONArray();
            List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanTestCaseInstance.get(Integer.parseInt(execEntityTPInstanceId));
            for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                JSONObject returnPlanTCObject = new JSONObject();
                if (hasUserStory) {
                    new SwaggerServicesHelper().createUserStoryObj(returnEntityPlanTCArray, execEntityTPTCI, returnPlanTCObject);
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
            //    returnPlanObject.put("testCases", returnEntityPlanTCArray);
            return Response.ok(returnEntityPlanTCArray.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/getExecutedIterationData/{instance_id}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getExecutedIterationData(@PathParam("instance_id") String instanceId) {
        try {
            EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);
            TCaseDataIteration[] testCaseDataIterations = eEnityPlanTestCaseDataIteration.getTestCaseDataIterations(instanceId);
            JSONObject output = new JSONObject();
            if (Objects.nonNull(testCaseDataIterations)) {
                int inProgressCount = 0;
                int passCount = 0;
                int failCount = 0;
                JSONArray iterationsInfo = new JSONArray();
                for (TCaseDataIteration testCaseDataIteration : testCaseDataIterations) {
                    String iterationData = testCaseDataIteration.getIterationData();
                    String status = testCaseDataIteration.getStatus();
                    JSONObject dataIterations = new JSONObject();
                    dataIterations.put("data", iterationData);
                    dataIterations.put("status", status);
                    if (Objects.equals(status.toLowerCase(Locale.ENGLISH), EEntityTPTCDIStatus.IN_PROGRESS.value().toLowerCase(Locale.ENGLISH))) {
                        inProgressCount += 1;
                    } else if (Objects.equals(status.toLowerCase(Locale.ENGLISH), EEntityTPTCDIStatus.PASS.value().toLowerCase(Locale.ENGLISH))) {
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
            }
            return Response.ok(output.toString()).build();
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/getEntityOverallSummary/{projectKey}/{execEntityInstanceId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getEntityOverallSummary(@PathParam("projectKey") String projectKey, @PathParam("execEntityInstanceId") String execEntityInstanceId) {
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        List<Integer> entityOverallSummary = eEntityInstance.getEntityOverallSummary(projectKey, execEntityInstanceId);
        return Response.ok(entityOverallSummary.toString()).build();
    }


    @GET
    @Path("/getAllDefects/{projectKey}/{user}/{execEntityInstanceId}/{tCIMyTaskId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getAllDefects(@PathParam("projectKey") String project, @PathParam("user") String user, @PathParam("execEntityTPTCInstanceId") String execEntityTPTCInstanceId, @PathParam("tCIMyTaskId") String tCIMyTaskId) {
        JSONArray defectList = new JSONArray();
        ApplicationUser userName = ComponentAccessor.getUserManager().getUserByName(user);
        String savedDefectListStr = new SwaggerServicesHelper().getSavedDefects(tCIMyTaskId);
        JsonElement jsonElement = new JsonParser().parse(savedDefectListStr);
        JsonArray savedDefectList = jsonElement.getAsJsonArray();
        JqlQueryBuilder builderTCase = JqlQueryBuilder.newBuilder();
        SearchService searchProviderTestCases = ComponentAccessor.getComponentOfType(SearchService.class);
        builderTCase.where().project(Long.valueOf(project)).and().issueType("Bug");
        List<Issue> tempIssueArray = new ArrayList<>();
        try {
            com.atlassian.jira.issue.search.SearchResults results = searchProviderTestCases.searchOverrideSecurity(userName, builderTCase.buildQuery(), PagerFilter.getUnlimitedFilter());
            results.getIssues().forEach(issue -> {
                if (savedDefectList.size() > 0) {
                    for (int i = 0; savedDefectList.size() > i; i++) {
                        JsonObject savedDefectObj = savedDefectList.get(i).getAsJsonObject();
                        if (Objects.nonNull(savedDefectObj) && !issue.getId().equals(Long.valueOf(savedDefectObj.get("defectId").getAsString()))) {
                            tempIssueArray.add(issue);
                        }
                    }
                } else {
                    tempIssueArray.add(issue);
                }
            });
        } catch (SearchException e) {
            log.error(e.getMessage(), e);
        }
        Set<Issue> issueSet = new HashSet<>(tempIssueArray);
        tempIssueArray.clear();
        tempIssueArray.addAll(issueSet);
        tempIssueArray.forEach(issue -> {
            JSONObject defectObj = new JSONObject();
            try {
                defectObj.put(issue.getId().toString() + "-" + issue.getKey(), issue.getSummary());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            defectList.put(defectObj);
        });
        return Response.ok(defectList.toString()).build();
    }

    @GET
    @Path("/getDefectsForTestCase/{testCaseId}/{baseUrl}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getDefectsForTestCase(@PathParam("testCaseId") String testCaseId, @PathParam("baseUrl") String baseUrl) {
        DefectsEntity defectsEntity = new DefectsEntity(activeObjects);
        JsonArray defectJsonArray = new JsonArray();
        List<DefectsEntityI> defectList = defectsEntity.getTestCaseAllDefectList(testCaseId);
        CustomField severity = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Severity").iterator().next();
        defectList.forEach(defect -> {
            MutableIssue userStory = ComponentAccessor.getIssueManager().getIssueObject(Long.valueOf(defect.getDefectId()));
            JsonObject defectJsonObj = new JsonObject();
            defectJsonObj.addProperty("id", defect.getDefectId());
            defectJsonObj.addProperty("key", defect.getDefectKey());
            defectJsonObj.addProperty("name", defect.getDefectName());
            if (userStory.getPriority() != null) {
                defectJsonObj.addProperty("priority", userStory.getPriority().getName());
                defectJsonObj.addProperty("priorityurl", baseUrl + userStory.getPriority().getIconUrl());
            } else {
                defectJsonObj.addProperty("priority", "N/A");
                defectJsonObj.addProperty("priorityurl", "N/A");
            }
            if (userStory.getCustomFieldValue(severity) != null) {
                defectJsonObj.addProperty("siverity", userStory.getCustomFieldValue(severity).toString());
            } else {
                defectJsonObj.addProperty("siverity", "N/A");
            }

            defectJsonObj.addProperty("status", userStory.getStatus().getSimpleStatus().getName());
            defectJsonObj.addProperty("createddate", userStory.getCreated().toString());
            if (userStory.getAssignee() != null) {
                defectJsonObj.addProperty("assigned", userStory.getAssignee().getUsername());
            } else {
                defectJsonObj.addProperty("assigned", "N/A");
            }
            defectJsonArray.add(defectJsonObj);
        });
        return Response.ok(defectJsonArray.toString()).build();
    }

    @GET
    @Path("/getRCI/{userStoryId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getRCIRating(@PathParam("userStoryId") String userStoryId) {

        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueByCurrentKey(userStoryId);
        CustomField ai_rci = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("AI RCI").iterator().next();
        String customFieldValue = (String) issueByCurrentKey.getCustomFieldValue(ai_rci);
        JSONObject jsonObject = new JSONObject();
        if (!(customFieldValue == null) && !customFieldValue.isEmpty()) {
            try {
                JsonElement $finalValue = new JsonParser().parse(customFieldValue);
                int asInt = $finalValue.getAsJsonObject().get("RQmetrics").getAsJsonObject().get("overall_rating").getAsInt();

                JsonArray $continuances_rating = $finalValue.getAsJsonObject().get("continuances_rating").getAsJsonArray();
                String continuances = $continuances_rating.size() == 3 && !($continuances_rating.get(2).getAsString().isEmpty()) ? $continuances_rating.get(2).getAsString() : "";
                JsonArray $directives_rating = $finalValue.getAsJsonObject().get("directives_rating").getAsJsonArray();
                String directives_rating = $directives_rating.size() == 3 && !($directives_rating.get(2).getAsString().isEmpty()) ? $directives_rating.get(2).getAsString() : "";
                JsonArray $imperatives_rating = $finalValue.getAsJsonObject().get("imperatives_rating").getAsJsonArray();
                String imperatives_rating = $imperatives_rating.size() == 3 && !($imperatives_rating.get(2).getAsString().isEmpty()) ? $imperatives_rating.get(2).getAsString() : "";
                JsonArray $incomplete_rating = $finalValue.getAsJsonObject().get("incomplete_rating").getAsJsonArray();
                String incomplete_rating = $incomplete_rating.size() == 3 && !($incomplete_rating.get(2).getAsString().isEmpty()) ? $incomplete_rating.get(2).getAsString() : "";
                JsonArray $options_rating = $finalValue.getAsJsonObject().get("options_rating").getAsJsonArray();
                String options_rating = $options_rating.size() == 3 && !($options_rating.get(2).getAsString().isEmpty()) ? $options_rating.get(2).getAsString() : "";
                JsonArray $readability_rating = $finalValue.getAsJsonObject().get("readability_rating").getAsJsonArray();
                String readability_rating = $readability_rating.size() == 3 && !($readability_rating.get(2).getAsString().isEmpty()) ? $readability_rating.get(2).getAsString() : "";
                JsonArray $weak_phrases_rating = $finalValue.getAsJsonObject().get("weak_phrases_rating").getAsJsonArray();
                String weak_phrases_rating = $weak_phrases_rating.size() == 3 && !($weak_phrases_rating.get(2).getAsString().isEmpty()) ? $weak_phrases_rating.get(2).getAsString() : "";
                JsonArray $generalities_rating = $finalValue.getAsJsonObject().get("generalities_rating").getAsJsonArray();
                String generalities_rating = $generalities_rating.size() == 3 && !($generalities_rating.get(2).getAsString().isEmpty()) ? $generalities_rating.get(2).getAsString() : "";

                jsonObject.put("rci", asInt);
                JSONObject recommendations = new JSONObject();
                recommendations.put("continuances_rating", continuances);
                recommendations.put("directives_rating", directives_rating);
                recommendations.put("imperatives_rating", imperatives_rating);
                recommendations.put("incomplete_rating", incomplete_rating);
                recommendations.put("options_rating", options_rating);
                recommendations.put("readability_rating", readability_rating);
                recommendations.put("weak_phrases_rating", weak_phrases_rating);
                recommendations.put("generalities_rating", generalities_rating);
                jsonObject.put("recommendations", recommendations);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                Response.ResponseBuilder status = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return status.build();
            }
        }

        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path("/getQARating/{userStoryId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getQARating(@PathParam("userStoryId") String userStoryId) {

        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueByCurrentKey(userStoryId);
        CustomField ai_rci = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("QA RCI").iterator().next();
        Object customFieldValue = issueByCurrentKey.getCustomFieldValue(ai_rci);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("qa-rating", customFieldValue.toString());
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path("/getOverallRating/{userStoryId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getOverallRating(@PathParam("userStoryId") String userStoryId) {

        MutableIssue issueByCurrentKey = ComponentAccessor.getIssueManager().getIssueByCurrentKey(userStoryId);
        CustomField ai_rci = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Team Average").iterator().next();
        Object customFieldValue = issueByCurrentKey.getCustomFieldValue(ai_rci);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("overall-rating", customFieldValue.toString());
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path("/get-ai-test-TestCase-Data/{testCaseId}")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getIterationData(@PathParam("testCaseId") String testCaseId) {
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        TestStepParams[] testStepParams = testStepParam.get(testCaseId);
        JSONArray jsonArray = new JSONArray();
        try {
            TestParamV[] testStepParamValues = testStepParams[0].getTestStepParamValues();
            for (TestParamV testStepParamValue : testStepParamValues) {
                JSONObject jsonObject = new JSONObject();
                for (TestStepParams testStepParam1 : testStepParams) {
                    String value = testStepParamValue.getValue();
                    jsonObject.put("paramName", testStepParam1.getParamName());
                    jsonObject.put("paramValue", value);
                }
                jsonArray.put(jsonObject);
            }
        } catch (JSONException e) {
            log.error(e.getMessage());
            Response.ResponseBuilder status = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return status.build();
        }
        return Response.ok(jsonArray.toString()).build();
    }
}
