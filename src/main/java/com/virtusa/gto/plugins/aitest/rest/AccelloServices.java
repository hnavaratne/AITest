package com.virtusa.gto.plugins.aitest.rest;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.gson.*;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.*;
import com.virtusa.gto.plugins.aitest.util.CommonsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * A resource of services.
 */
@Path("/services")
public class AccelloServices {

    private static final Logger log = LoggerFactory.getLogger(AccelloServices.class);

    @JiraImport
    private ActiveObjects activeObjects;

    @Inject
    public AccelloServices(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    @GET
    @Path("/gettestmanagementproject")
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

    @POST
    @Path("/gettestmanagementtestcases")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTestCaseHierarchy(String data) {
        Long project_key = toJsonElement(data).getAsJsonObject().get("project_key").getAsLong();
        List<Issue> issueList = new AccelloServicesHelper().getIssues(project_key);
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
            new AccelloServicesHelper(activeObjects).getChildren(testCaseFolders1, String.valueOf(parentFolder.getID()), childFolder, issueList);
            if (childFolder.size() == 0) {
                new AccelloServicesHelper(activeObjects).getLeafNodes(issueList, String.valueOf(parentFolder.getID()), childFolder);
            }
            parentFolders.put("entity", childFolder);
            //  parentF.add();
            jsonElements.add(parentFolders);
        });
        //add root testcases
        List<Issue> issueByParent = new AccelloServicesHelper().getIssueByParent(Objects.requireNonNull(issueList), "0");
        issueByParent.forEach(issue -> {
            Map<String, Object> jsonObjectF = new HashMap<>();
            jsonObjectF.put("name", issue.getSummary());
            jsonObjectF.put("id", issue.getId());
            jsonObjectF.put("type", "TESTCASE");
            List<Map<String, Object>> attachedScriptObject = new AccelloServicesHelper().getAttachedScriptObject(issue);
            jsonObjectF.put("scripts", attachedScriptObject);
            String testCaseId = String.valueOf(issue.getId());
            List<Map<String, Object>> mappedVariables = CommonsUtil.getMappedVariables(testCaseId, activeObjects);
            List<Map<String, Object>> testSteps = new AccelloServicesHelper().getTestSteps(testCaseId);
            jsonObjectF.put("steps", testSteps);
            jsonObjectF.put("params", mappedVariables);
            jsonElements.add(jsonObjectF);
        });
        Response.status(Response.Status.OK.getStatusCode());
        Gson gson = new Gson();
        return Response.ok(gson.toJson(jsonElements)).build();

    }

    @POST
    @Path("/savescript")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response update(String data) {
        JsonArray jsonScriptElement = toJsonElement(data).getAsJsonObject().get("scripts").getAsJsonArray();
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        IssueService issueService = ComponentAccessor.getIssueService();
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        CustomField scripts = customFieldManager.getCustomFieldObjectsByName("Scripts").iterator().next();
        CScripts cScripts = new CScripts(activeObjects);
        //get the testcase objects from the object
        for (JsonElement testCase : jsonScriptElement) {
            JsonArray diffArray = new JsonArray();
            //each object
            JsonObject tCase = testCase.getAsJsonObject();
            //get the testcase id from from the testcase object
            MutableIssue issueObject = issueManager.getIssueObject(tCase.get("test-case-id").getAsLong());
            if (Objects.nonNull(issueObject) && !(issueObject.getProjectObject() == null)) {
                String name = issueObject.getProjectObject().getName();
                String testCaseId = tCase.get("test-case-id").toString();
                String scriptId = tCase.get("scripts").getAsJsonArray().get(0).getAsJsonObject().get("script-id").getAsString();
                cScripts.insert(name, testCaseId, scriptId);
            }
            JsonArray customFieldValue = (JsonArray) issueObject.getCustomFieldValue(scripts);
            JsonArray jsonElementF = new JsonArray();
            if (!(customFieldValue == null)) {
                //parse the contents of the custom field.
                jsonElementF = customFieldValue;
                JsonArray customFieldValues = jsonElementF.getAsJsonArray();
                JsonArray scriptsArray = tCase.getAsJsonArray("scripts");
                for (JsonElement jsonElement1 : scriptsArray) {
                    boolean[] containsScript = {false};
                    customFieldValues.forEach(jsonElement2 -> {
                        if (jsonElement2.equals(jsonElement1)) {
                            containsScript[0] = true;
                        }
                    });
                    if (!containsScript[0]) {
                        jsonElementF.add(jsonElement1);
                    }
                }
                diffArray.forEach(jsonElementF::add);
            } else {
                tCase.get("scripts").getAsJsonArray().forEach(jsonElementF::add);
            }
            List<JsonElement> jsonElements = new ArrayList<>();
            jsonElementF.forEach(jsonElements::add);
            ApplicationUser creator = issueObject.getCreator();
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            issueInputParameters.addCustomFieldValue(scripts.getId(), jsonElements.toString());
            IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(creator, issueObject.getId(), issueInputParameters);
            Response response = new AccelloServicesHelper().handleUpdateValidation(updateValidationResult, issueService, creator);
            if (response != null) {
                return response;
            }
        }
        return Response.status(Response.Status.OK.getStatusCode()).build();
    }

    @POST
    @Path("/searchscript")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response searchscript(String data) {
        String jsonScriptElement = toJsonElement(data).getAsJsonObject().get("script_id").getAsString();
        CScripts cScripts = new CScripts(activeObjects);
        Scripts scripts = cScripts.get(jsonScriptElement);
        JSONArray finalDataArray = new JSONArray();
        try {
            if (Objects.nonNull(scripts) && scripts.isAttached()) {
                JSONObject response = new JSONObject();
                response.put("project_name", scripts.getProject());
                JSONArray jsonElements = new JSONArray();
                jsonElements.put(new JSONObject().put("test_case_id", scripts.getTestCaseId()));
                response.put("scripts", jsonElements);
                finalDataArray.put(response);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
        }
        return Response.ok(finalDataArray.toString()).build();
    }

    @POST
    @Path("/removetestmanagementmapping")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response remove(String data) {
        JsonObject dataObject = toJsonElement(data).getAsJsonObject();
        String scriptId = dataObject.get("script-id").getAsString();
        long testCaseId = dataObject.get("test-case-id").getAsLong();
        CScripts cScripts = new CScripts(activeObjects);
        cScripts.remove(String.valueOf(testCaseId), scriptId);
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        IssueService issueService = ComponentAccessor.getIssueService();
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        CustomField scripts = customFieldManager.getCustomFieldObjectsByName("Scripts").iterator().next();
        //get the testcase objects from the object
        JsonArray diffArray = new JsonArray();
        //each object get the testcase id from from the testcase object
        MutableIssue issueObject = issueManager.getIssueObject(testCaseId);
        JsonArray customFieldValue = (JsonArray) issueObject.getCustomFieldValue(scripts);
        JsonArray jsonElementF;
        if (customFieldValue.size() > 0) {
            //parse the contents of the custom field.
            jsonElementF = customFieldValue;
            JsonArray customFieldValues = jsonElementF.getAsJsonArray();
            customFieldValues.forEach(jsonElement2 -> {

                if (jsonElement2.getAsJsonObject().get("script-id").getAsString().equals(scriptId)) {
                    diffArray.add(jsonElement2);
                }
            });
            List<JsonElement> jsonElements = new ArrayList<>();
            jsonElementF.forEach(jsonElements::add);
            diffArray.forEach(jsonElements::remove);
            ApplicationUser creator = issueObject.getCreator();
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            issueInputParameters.addCustomFieldValue(scripts.getId(), jsonElements.toString());
            IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(creator, issueObject.getId(), issueInputParameters);
            return new AccelloServicesHelper().handleUpdateValidation(updateValidationResult, issueService, creator);
        }
        return Response.status(Response.Status.OK.getStatusCode()).build();
    }

    @POST
    @Path("/updateresults")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateClonedInstances(String data) {
        JsonObject convertedData = toJsonElement(data).getAsJsonObject();
        String entityId = convertedData.get("entityid").getAsString();
        String planInstanceId = convertedData.get("planinstanceid").getAsString();
        Integer tcInstanceId = convertedData.get("tcinstanceid").getAsInt();
        String state = convertedData.get("state").getAsString();
        String iterationId = null;
        if (!convertedData.get("IterationId").isJsonNull()) {
            iterationId = convertedData.get("IterationId").getAsString();
        }
        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstance = new EEntityPlanTestCaseInstance(activeObjects);
        List<ExecEntityTPTCI> execEntityTPTCIS = eEntityPlanTestCaseInstance.getTCaseInstance(tcInstanceId);
        EEnityPlanTestCaseDataIteration eEnityPlanTestCaseDataIteration = new EEnityPlanTestCaseDataIteration(activeObjects);
        TCaseDataIteration[] testCaseDataIterations = eEnityPlanTestCaseDataIteration.getTestCaseDataIterations(String.valueOf(tcInstanceId));
        TStepsInstance tStepsInstance = new TStepsInstance(activeObjects);
        if (iterationId != null) {
            new AccelloServicesHelper().handleIterationNonNull(testCaseDataIterations, iterationId, state, execEntityTPTCIS, tStepsInstance);
        } else {
            new AccelloServicesHelper().handleIterationNull(execEntityTPTCIS, state, tStepsInstance);
        }
        EEntityPlanTestCaseInstance eEntityPlanTestCaseInstanceNew = new EEntityPlanTestCaseInstance(activeObjects);
        List<ExecEntityTPTCI> testCasesForTPlan = eEntityPlanTestCaseInstanceNew.getTestCasesForTPlan(planInstanceId);
        boolean hasPending = false;
        boolean hasFailed = false;
        for (ExecEntityTPTCI execEntityTPTCI : testCasesForTPlan) {
            boolean executed = execEntityTPTCI.isExecuted();
            String overallStatus = execEntityTPTCI.getOverallStatus();
            if (executed) {
                if (overallStatus.toLowerCase(Locale.ROOT).equals("in progress") || overallStatus.toLowerCase(Locale.ROOT).contains("in")) {
                    hasPending = true;
                } else if (overallStatus.toLowerCase(Locale.ROOT).equals("failed")) {
                    hasFailed = true;
                }
            } else {
                if (overallStatus.toLowerCase(Locale.ROOT).equals("in progress") || overallStatus.toLowerCase(Locale.ROOT).contains("in")) {
                    hasPending = true;
                } else if (overallStatus.toLowerCase(Locale.ROOT).equals("failed")) {
                    hasFailed = true;
                }
            }
        }
        EEntityPlanInstance eEntityPlanInstance = new EEntityPlanInstance(activeObjects);
        new AccelloServicesHelper().handleTestPlanInstance(eEntityPlanInstance, planInstanceId, hasFailed, hasPending);
        boolean entityInstanceHasPending = false;
        List<ExecEntityTPI> eEntityTPIByEntityInstance = eEntityPlanInstance.getEEntityTPIByEntityInstance(entityId);
        for (ExecEntityTPI execEntityI : eEntityTPIByEntityInstance) {
            String status = execEntityI.getStatus();
            if (status.toLowerCase(Locale.ROOT).equals("in progress")) {
                entityInstanceHasPending = true;
            }
        }
        EEntityInstance eEntityInstance = new EEntityInstance(activeObjects);
        new AccelloServicesHelper().updateInstance(eEntityInstance, entityId, entityInstanceHasPending);
        return Response.ok(Response.Status.ACCEPTED.getStatusCode()).build();
    }

    private JsonElement toJsonElement(String data) {
        return new JsonParser().parse(data);
    }

    @GET
    @Path("/test")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getTest() {
        return Response.ok().status(Response.Status.OK).build();
    }
}
