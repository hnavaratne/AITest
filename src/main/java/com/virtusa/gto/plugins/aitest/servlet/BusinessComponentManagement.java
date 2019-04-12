package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.*;
import com.virtusa.gto.plugins.aitest.entity.BusComFolder;
import com.virtusa.gto.plugins.aitest.entity.BusComScript;
import com.virtusa.gto.plugins.aitest.entity.BusComSteps;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author RPSPERERA on 9/21/2018
 */
@Scanned
public class BusinessComponentManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BusinessComponentManagement.class);
    private static final long serialVersionUID = -6501974540420991674L;


    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    BrowseContext context;

    @Inject
    public BusinessComponentManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String payLoad = IOUtils.toString(req.getReader());
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        String entity = req.getParameter("entity");
        String action = req.getParameter("action");
        if (Objects.nonNull(payLoad) && payLoad.contains("user") && payLoad.contains("action") && payLoad.contains("data")) {
            JsonObject parse = new JsonParser().parse(payLoad).getAsJsonObject();
            user = parse.get("user").getAsString();
            data = parse.get("data").getAsString();
            action = parse.get("action").getAsString();
        }
        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            switch (action) {
                case "insert_business_folder":
                    resp.getWriter().write(createBusinessFolder(entity, data));
                    break;
                case "update_business_folder":
                    resp.getWriter().write(String.valueOf(updateBusinessFolder(entity, data)));
                    break;
                case "delete_business_folder":
                    resp.getWriter().write(String.valueOf(deleteBusinessFolder(data)));
                    break;
                case "get_business_folders":
                    resp.getWriter().write(getBusinessFolders(data));
                    break;
                default:
                    handleRemainingActions1(action, data, resp, entity);
                    break;
            }
        }
    }

    private void handleRemainingActions1(String action, String data, HttpServletResponse resp, String entity) {
        try {
            switch (action) {
                case "insert_business_script":
                    resp.getWriter().write(createBusinessFunction(entity, data));
                    break;
                case "update_business_script":
                    resp.getWriter().write(String.valueOf(updateBusinessFunction(data)));
                    break;
                case "get-project-bcscripts":
                    resp.getWriter().write(getProjectBusinessFunctions());
                    break;
                case "delete_business_script":
                    resp.getWriter().write(String.valueOf(deleteBusinessFunction(data)));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String createBusinessFolder(String entity, String data) {

        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (entity.toLowerCase(Locale.ENGLISH).equals(BusComFolder.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            BComponentFolder businessComponentFolder = new BComponentFolder(activeObjects);
            BusComFolder insertedFolder = businessComponentFolder.insert(selectedProjectKey, jsonData.get("parent").getAsString(), jsonData.get("name").getAsString(), jsonData.get("description").getAsString());
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("id", insertedFolder.getID());
                returnObject.put("name", insertedFolder.getName());
                returnObject.put("parent", insertedFolder.getParent());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            return returnObject.toString();
        }
        return "";
    }

    private boolean updateBusinessFolder(String entity, String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject dataF = element.getAsJsonObject();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        if (entity.toLowerCase(Locale.ENGLISH).equals(BusComFolder.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            BComponentFolder businessComponentFolder = new BComponentFolder(activeObjects);
            String parent = dataF.get("parent").getAsString();
            String oldName = dataF.get("oldname").getAsString();
            String newName = dataF.get("newname").getAsString();
            return businessComponentFolder.update(projectKey, parent, oldName, newName);
        }
        return false;
    }

    private boolean deleteBusinessFolder(String data) {
        List<BusComFolder> folderForRemove = new ArrayList<>();
        JsonElement element = new JsonParser().parse(data);
        JsonObject dataF = element.getAsJsonObject();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        String parentFor = dataF.get("parentfor").getAsString();
        BComponentFolder businessFolder = new BComponentFolder(activeObjects);
        BComponentScript businessScript = new BComponentScript(activeObjects);
        BComponentStep businessStep = new BComponentStep(activeObjects);
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        List<BusComScript> currentScripts = businessScript.getAll(projectKey, parentFor);
        List<BusComScript> scriptForRemove = new ArrayList<>(currentScripts);
        List<BusComFolder> folders = businessFolder.get(projectKey, parentFor);
        TSteps testSteps = new TSteps(activeObjects);

        for (BusComFolder busComFolder : folders) {
            folderForRemove.add(busComFolder);
            List<BusComScript> scripts = businessScript.getAll(projectKey, Long.toString(busComFolder.getID()));
            scriptForRemove.addAll(scripts);
            recursiveSearch(Long.toString(busComFolder.getID()), scriptForRemove, folderForRemove);
        }
        for (BusComScript aScriptForRemove : scriptForRemove) {
            removeParamsFromTC(selectedProjectKey, aScriptForRemove.getID(), businessStep, testSteps);
            testSteps.updateBFunctions(selectedProjectKey, Long.toString(aScriptForRemove.getID()));
            businessStep.deleteTestStepsById(Long.toString(aScriptForRemove.getID()));
        }
        for (BusComScript aScriptForRemove : scriptForRemove) {
            businessScript.delete(projectKey, Long.toString(aScriptForRemove.getID()));
        }
        for (BusComFolder aFolderForRemove : folderForRemove) {
            businessFolder.delete(projectKey, Long.toString(aFolderForRemove.getID()));
        }

        return businessFolder.delete(projectKey, parentFor);
    }

    private void recursiveSearch(String parent, List<BusComScript> scriptForRemove, List<BusComFolder> folderForRemove) {

        BComponentFolder businessComponentFolder = new BComponentFolder(activeObjects);
        BComponentScript businessScript = new BComponentScript(activeObjects);
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        List<BusComFolder> folders = businessComponentFolder.get(projectKey, parent);
        for (BusComFolder busComFolder : folders) {
            folderForRemove.add(busComFolder);
            List<BusComScript> scripts = businessScript.getAll(projectKey, Long.toString(busComFolder.getID()));
            scriptForRemove.addAll(scripts);
            recursiveSearch(Long.toString(busComFolder.getID()), scriptForRemove, folderForRemove);
        }
    }

    private String getBusinessFolders(String data) {

        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnData = new JSONObject();
        JSONArray folderArray = new JSONArray();
        // if (entity.toLowerCase(Locale.ENGLISH).equals(BusComFolder.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
        BComponentFolder bcomfolder = new BComponentFolder(activeObjects);
        List<BusComFolder> bcomfolders = bcomfolder.get(selectedProjectKey, jsonData.get("parent").getAsString());
        for (BusComFolder folder : bcomfolders) {
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
        try {
            returnData.put("folders", folderArray);
            returnData.put("businessScripts", getBusinessFunctions(data));
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        //  }
        return returnData.toString();
    }

    private String createBusinessFunction(String entity, String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnObject = new JSONObject();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        String businessScriptFolderId = jsonData.get("parent").getAsString();
        String name = jsonData.get("name").getAsString();
        if (entity.toLowerCase(Locale.ENGLISH).equals(BusComScript.class.getSimpleName().toLowerCase(Locale.ENGLISH))) {
            BComponentScript businessScript = new BComponentScript(activeObjects);
            BusComScript insertedScript = businessScript.insert(selectedProjectKey, businessScriptFolderId, name);
            try {
                returnObject.put("id", insertedScript.getID());
                returnObject.put("parent", insertedScript.getParent());
                returnObject.put("name", insertedScript.getName());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnObject.toString();
    }

    private boolean updateBusinessFunction(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        long businessScriptID = jsonData.get("id").getAsLong();
        String updatedName = jsonData.get("newname").getAsString();
        String oldName = jsonData.get("oldname").getAsString();
        BComponentScript businessScript = new BComponentScript(activeObjects);
        return businessScript.update(Long.toString(businessScriptID), oldName, updatedName);
    }

    private String getProjectBusinessFunctions() {
        JSONArray businessScriptArray = new JSONArray();

        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        BComponentScript businessScriptObject = new BComponentScript(activeObjects);
        List<BusComScript> businessScripts = businessScriptObject.get(selectedProjectKey);
        businessScripts.forEach(script -> {
            JSONObject scriptJsonObject = new JSONObject();
            try {
                scriptJsonObject.put("Id", script.getID());
                scriptJsonObject.put("Name", script.getName());
                businessScriptArray.put(scriptJsonObject);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        });
        return businessScriptArray.toString();
    }

    private boolean deleteBusinessFunction(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        long businessScriptID = jsonData.get("id").getAsLong();
        BComponentScript businessScript = new BComponentScript(activeObjects);
        BComponentStep businessStep = new BComponentStep(activeObjects);
        TSteps testSteps = new TSteps(activeObjects);
        removeParamsFromTC(selectedProjectKey, businessScriptID, businessStep, testSteps);
        testSteps.updateBFunctions(selectedProjectKey, Long.toString(businessScriptID));
        businessStep.deleteTestStepsById(Long.toString(businessScriptID));
        return businessScript.delete(selectedProjectKey, Long.toString(businessScriptID));
    }

    private void removeParamsFromTC(String selectedProjectKey, long businessScriptID, BComponentStep businessStep, TSteps testSteps) {
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        TestStepParamValue testStepParamValue = new TestStepParamValue(activeObjects);
        Set<String> testCases = testSteps.getTestSteps(selectedProjectKey, String.valueOf(businessScriptID));
        BusComSteps[] steps = businessStep.getSteps(String.valueOf(businessScriptID));
        for (int i = 0; i < steps.length; i++) {
            String dataParams = steps[i].getDataParams();
            List<String> params = getParams(dataParams);
            testCases.forEach(testcaseId -> {
                TestStepParams[] parameters = testStepParam.getParameters(testcaseId);
                params.forEach(bcParam -> {
                    for (int j = 0; j < parameters.length; j++) {
                        if (bcParam.equals(parameters[j].getParamName())) {
                            testStepParamValue.deleteParams(testcaseId, parameters[j].getParamName());
                            testStepParam.delete(testcaseId, parameters[j].getParamName());
                        }
                    }
                });
            });
        }
    }

    private String getBusinessFunctions(String data) {

        JSONArray businessScriptsArray = new JSONArray();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        BComponentScript businessScript = new BComponentScript(activeObjects);
        try {
            JsonElement element = new JsonParser().parse(data);
            JsonObject jsonData = element.getAsJsonObject();
            String parent = jsonData.get("parent").getAsString();
            String parentValue;
            if (parent.equals("null")) {
                parentValue = "0";
            } else {
                parentValue = parent;
            }
            List<BusComScript> scripts = businessScript.getAll(selectedProjectKey, parentValue);
            for (BusComScript bcs : scripts) {
                JSONObject returnObject = new JSONObject();
                returnObject.put("id", bcs.getID());
                returnObject.put("name", bcs.getName());
                returnObject.put("parent", bcs.getParent());
                businessScriptsArray.put(returnObject);
            }

        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return businessScriptsArray.toString();
    }

    public String getAll() {

        BComponentFolder businessFolder = new BComponentFolder(activeObjects);
        BComponentScript businessScript = new BComponentScript(activeObjects);
        JsonArray folderArray = new JsonArray();
        JsonArray scriptArray = new JsonArray();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        List<BusComFolder> folders = businessFolder.getByProject(selectedProjectKey);
        List<BusComScript> scripts = businessScript.get(selectedProjectKey);
        for (BusComFolder folder : folders) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", folder.getName());
            folderArray.add(obj);
        }
        for (BusComScript script : scripts) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", script.getName());
            scriptArray.add(obj);
        }
        JsonObject dataObj = new JsonObject();
        dataObj.add("bcfolders", folderArray);
        dataObj.add("bcscripts", scriptArray);
        return dataObj.toString();
    }

    private List<String> getParams(String dataString) {
        String regex = "\\{\\{[a-zA-Z0-9]*}}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dataString);
        List<String> paramList = new ArrayList<>();
        while (matcher.find()) {
            String paramName = matcher.group(0).replace("{{", "").replace("}}", "");
            paramList.add(paramName);
        }
        return paramList;
    }
}
