package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.TCFolder;
import com.virtusa.gto.plugins.aitest.entity.TestCaseFolder;
import com.virtusa.gto.plugins.aitest.util.GetProjectKey;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Scanned
public class TestCaseManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestCaseManagement.class);
    private static final long serialVersionUID = -2292542604978717000L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    //Project Key
    BrowseContext context;

    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    @Inject
    public TestCaseManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, UserManager userManager,
                              LoginUriProvider loginUriProvider) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String payLoad = IOUtils.toString(req.getReader());
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        String entity = req.getParameter("entity");
        String action = req.getParameter("action");
        //User login authentication
        String redirectUrl = "";
        JSONObject jsonObject;
        if (Objects.nonNull(payLoad) && payLoad.contains("user") && payLoad.contains("action") && payLoad.contains("data")) {
            JsonObject parse = new JsonParser().parse(payLoad).getAsJsonObject();
            user = parse.get("user").getAsString();
            data = parse.get("data").getAsString();
            action = parse.get("action").getAsString();
        }
        try {
            jsonObject = new JSONObject(data);
            if (jsonObject.has("redirectView")) {
                redirectUrl = jsonObject.get("redirectView").toString();
            }

            UserProfile currentUser = userManager.getRemoteUser(req);
            if (!Objects.nonNull(currentUser)) {
                resp.setStatus(HttpServletResponse.SC_FOUND);
                if (redirectUrl.equals("/secure/TestCaseDesigningView.jspa") || redirectUrl.equals("/secure/TestPlanningView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
                    handleAction(action, data, user, resp, entity);
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String data, String user, HttpServletResponse resp, String entity) {
        try {
            switch (action) {
                case "insert":
                    resp.getWriter().write(insert(entity, data));
                    break;
                case "update":
                    resp.getWriter().write(String.valueOf(update(entity, data)));
                    break;
                case "delete":
                    resp.getWriter().write(String.valueOf(delete(entity, data)));
                    break;
                case "get-folders":
                    resp.getWriter().write(getFolders(entity, data));
                    break;
                default:
                    handleRemainingActions1(action, data, user, resp, entity);
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleRemainingActions1(String action, String data, String user, HttpServletResponse resp, String entity) {
        try {
            switch (action) {
                case "full-delete":
                    resp.getWriter().write(fullDelete(entity, data, user));
                    break;
                case "get-all":
                    resp.getWriter().write(String.valueOf(getAll()));
                    break;
                case "getTestCaseForStory":
                    resp.getWriter().write(getLinkedTestCases(data, user));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean update(String entity, String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject dataF = element.getAsJsonObject();
        String projectKey = new GetProjectKey(context).getSelectedProjectKey();
        if (entity.toLowerCase(Locale.ROOT).equals(TestCaseFolder.class.getSimpleName().toLowerCase(Locale.ROOT))) {
            TCFolder tcFolder = new TCFolder(activeObjects);
            String parent = dataF.get("parent").getAsString();
            String oldName = dataF.get("oldname").getAsString();
            String newName = dataF.get("newname").getAsString();
            return tcFolder.update(projectKey, parent, oldName, newName);
        }
        return false;
    }

    private String insert(String entity, String data) {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (entity.toLowerCase(Locale.ROOT).equals(TestCaseFolder.class.getSimpleName().toLowerCase(Locale.ROOT))) {
            TCFolder tcFolder = new TCFolder(activeObjects);
            TestCaseFolder inserted = tcFolder.insert(selectedProjectKey, jsonData.get("parent").getAsString(), jsonData.get("name").getAsString(), jsonData.get("description").getAsString());
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("id", inserted.getID());
                returnObject.put("name", inserted.getName());
                returnObject.put("parent", inserted.getParent());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
            return returnObject.toString();
        }
        return "";
    }


    private boolean delete(String entity, String data) {
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (entity.toLowerCase(Locale.ROOT).equals(TestCaseFolder.class.getSimpleName().toLowerCase(Locale.ROOT))) {
            TCFolder tcFolder = new TCFolder(activeObjects);
            return tcFolder.delete(selectedProjectKey, jsonData.get("id").getAsString());
        }
        return false;
    }

    private String getFolders(String entity, String data) {

        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject returnData = new JSONObject();
        JSONArray folderArray = new JSONArray();
        if (entity.toLowerCase(Locale.ROOT).equals(TestCaseFolder.class.getSimpleName().toLowerCase(Locale.ROOT))) {
            // Browse Folders
            TCFolder tcFolder = new TCFolder(activeObjects);
            List<TestCaseFolder> folders = tcFolder.get(selectedProjectKey, jsonData.get("parent").getAsString());
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
            try {
                returnData.put("folders", folderArray);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }

            // Browse Test Cases
            TestCase testCase = new TestCase(authenticationContext, activeObjects);
            try {
                returnData.put("testcases", testCase.getIssues(data));
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }

        }
        return returnData.toString();
    }


    private String fullDelete(String entity, String data, String user) {

        ArrayList<Integer> foldersID = new ArrayList<>();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        if (entity.toLowerCase(Locale.ROOT).equals(TestCaseFolder.class.getSimpleName().toLowerCase(Locale.ROOT))) {
            TCFolder tcFolder = new TCFolder(activeObjects);
            List<TestCaseFolder> folders = tcFolder.get(selectedProjectKey, jsonData.get("id").getAsString());
            foldersID.add(jsonData.get("id").getAsInt());
            for (TestCaseFolder folder : folders) {
                foldersID.add(folder.getID());
                readFolderInfo(Integer.toString(folder.getID()), foldersID);
            }
            TestCase testCase = new TestCase(authenticationContext, activeObjects);
            for (Integer parentFolderID : foldersID) {
                String id = Integer.toString(parentFolderID);
                try {
                    testCase.removeIssue(new JSONObject().put("parent", id).toString(), user);
                    JsonObject folderObj = new JsonObject();
                    folderObj.addProperty("id", parentFolderID);
                    delete(entity, folderObj.toString());

                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                    return "false";
                }
            }

        }
        return "true";
    }

    private void readFolderInfo(String id, ArrayList<Integer> foldersID) {

        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        TCFolder tcFolder = new TCFolder(activeObjects);
        List<TestCaseFolder> folders = tcFolder.get(selectedProjectKey, id);
        for (TestCaseFolder folder : folders) {
            foldersID.add(folder.getID());
            readFolderInfo(Integer.toString(folder.getID()), foldersID);
        }
    }

    public String getAll() {
        JSONObject returnObject = new JSONObject();
        JSONArray folderJsonArray = new JSONArray();
        JSONArray testCaseJsonArray = new JSONArray();
        JSONArray bcfolderJsonArray = new JSONArray();
        JSONArray bcscriptsJsonArray = new JSONArray();
        String selectedProjectKey = new GetProjectKey(context).getSelectedProjectKey();
        TCFolder tcFolder = new TCFolder(activeObjects);
        List<TestCaseFolder> folders = tcFolder.get(selectedProjectKey);
        for (TestCaseFolder folder : folders) {
            folderJsonArray.put(folder.getName());
        }

        TestCase testCases = new TestCase(authenticationContext, activeObjects);
        String allIssues = testCases.getAllIssues();
        JsonElement element = new JsonParser().parse(allIssues);
        JsonArray jsonData = element.getAsJsonArray();
        jsonData.forEach(item -> {
            JsonObject obj = (JsonObject) item;
            testCaseJsonArray.put(obj.get("name").getAsString());
        });

        BusinessComponentManagement businessComponentManagement = new BusinessComponentManagement(authenticationContext, activeObjects);
        JsonElement bcElement = new JsonParser().parse(businessComponentManagement.getAll());
        JsonArray bcfolderjsonData = bcElement.getAsJsonObject().get("bcfolders").getAsJsonArray();
        JsonArray bcscriptsjsonData = bcElement.getAsJsonObject().get("bcscripts").getAsJsonArray();
        bcfolderjsonData.forEach(folder -> {
            JsonObject obj = (JsonObject) folder;
            bcfolderJsonArray.put(obj.get("name").getAsString());
        });
        bcscriptsjsonData.forEach(script -> {
            JsonObject obj = (JsonObject) script;
            bcscriptsJsonArray.put(obj.get("name").getAsString());
        });
        try {
            returnObject.put("folders", folderJsonArray);
            returnObject.put("testcases", testCaseJsonArray);
            returnObject.put("bcfolders", bcfolderJsonArray);
            returnObject.put("bcscript", bcscriptsJsonArray);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }

    private String getLinkedTestCases(String data, String user) {
        JsonElement elementObj = new JsonParser().parse(data);
        JsonObject jsonDataObj = elementObj.getAsJsonObject();
        String storyKey = jsonDataObj.get("storykey").getAsString();
        JSONArray testCaseArray = new JSONArray();
        Long id = ComponentAccessor.getIssueManager().getIssueByCurrentKey(storyKey).getId();
        UserStoryEntityManagement userStoryEntityManagement = new UserStoryEntityManagement(authenticationContext, activeObjects, userManager, loginUriProvider);
        JSONObject storyObj = new JSONObject();
        try {
            storyObj.put("userStoryId", String.valueOf(id));
            storyObj.put("baseUrl", "");
            String testcaseString = userStoryEntityManagement.getAllTestCases(storyObj.toString(), user);
            JsonElement defectElement = new JsonParser().parse(testcaseString);
            JsonArray testcaseArray = defectElement.getAsJsonArray();
            for (int i = 0; i < testcaseArray.size(); i++) {
                JsonObject temp = testcaseArray.get(i).getAsJsonObject();
                JSONObject obj = new JSONObject();
                obj.put("key", temp.get("key").getAsString());
                obj.put("status", temp.get("status").getAsString());
                obj.put("id", temp.get("id").getAsString());
                obj.put("name", temp.get("Name").getAsString());
                obj.put("instanceid", temp.get("instanceid").getAsString());
                testCaseArray.put(obj);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

//        ComponentAccessor.getIssueManager().getIssueByCurrentKey()
        return testCaseArray.toString();
    }
}