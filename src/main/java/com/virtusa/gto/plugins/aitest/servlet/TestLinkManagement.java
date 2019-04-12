package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.TLinkOperation;
import com.virtusa.gto.plugins.aitest.entity.TestLinkEntity;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TestLinkManagement extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TestLinkManagement.class);
    private static final long serialVersionUID = 4432235792473413225L;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ActiveObjects activeObjects;

    private static Collection<String> fieldCollection = new ArrayList<>();

    static {
        fieldCollection.add("TestCaseFolder");
        fieldCollection.add("TestCase");
        fieldCollection.add("TestSteps");
        fieldCollection.add("TestStep");
        fieldCollection.add("TestCaseFolder Description");
        fieldCollection.add("TestCase Description");
        fieldCollection.add("TestCase Execution Type");
        fieldCollection.add("TestSteps ExpectedResult");
        fieldCollection.add("TestSteps Step");
    }

    @Inject
    public TestLinkManagement(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects) {
        this.authenticationContext = authenticationContext;
        this.activeObjects = activeObjects;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("user");
        String data = req.getParameter("data");
        if (authenticationContext.getLoggedInUser().getUsername().equals(user)) {
            String action = req.getParameter("action");
            handleAction(action, user, data, resp);
        }
    }

    private void handleAction(String action, String user, String data, HttpServletResponse resp) throws IOException {
        switch (action) {
            case "import-xml-data":
                resp.getWriter().write(importFromXML(data, user));
                break;
            case "upload-xml-file":
                resp.getWriter().write(uploadXMLFile(data));
                break;
            case "get-all-projects":
                resp.getWriter().write(getAllProjects());
                break;
            case "delete-file":
                resp.getWriter().write(deleteFile(false, data));
                break;
            default:
                break;
        }
    }

    private String uploadXMLFile(String data) {

        JsonElement dataElement = new JsonParser().parse(data);
        String tempXmlNodes = "";
        JsonObject dataObj = dataElement.getAsJsonObject();
        String path = dataObj.get("path").getAsString();
        String baseFile = dataObj.get("file").getAsString();
        baseFile = baseFile.split("base64,")[1];
        byte[] decoded = Base64.getDecoder().decode(baseFile);
        String tempDirectoryPath = System.getProperty("java.io.tmpdir");
        String fileName = Paths.get(path).getFileName().toString();

        Path testLinkFilePath = Paths.get(tempDirectoryPath.replaceAll("Program Files", "Progra~1"), fileName);
        Path dest = Paths.get(testLinkFilePath.toFile().getParentFile().getParentFile().getPath(), "testlink", fileName);
        try {
            FileUtils.writeStringToFile(dest.toFile(), new String(decoded, "UTF-8"), "UTF-8", false);
            tempXmlNodes = sendKeysToView(dest.toFile().getAbsolutePath());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return tempXmlNodes;
    }

    private String importFromXML(String data, String user) {
        boolean isCompleted;
        boolean insertSuccess = false;
        XMLStreamReader xmlStreamReader = null;
        String path = null;
        String project;
        String automated;
        String manual;
        try {
            JsonElement element = new JsonParser().parse(data);
            JsonArray dataF = element.getAsJsonObject().get("map").getAsJsonArray();
            JsonObject dataObj = element.getAsJsonObject();
            path = dataObj.get("path").getAsString();
            project = dataObj.get("project").getAsString();
            automated = dataObj.get("automated").getAsString();
            manual = dataObj.get("manual").getAsString();
            if (Objects.nonNull(path) && Objects.nonNull(project)) {
                TestLinkEntity testLinkEntity = new TestLinkEntity(activeObjects);
                XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
                try (FileInputStream fileInputStream = new FileInputStream(new File(path))) {
                    xmlStreamReader = xmlInputFactory.createXMLStreamReader(fileInputStream);

                    List<TestLinkEntity.Folder> folderList = new ArrayList<>();
                    List<TestLinkEntity.Folder.TestCase> testCaseList = new ArrayList<>();
                    List<TestLinkEntity.Folder.TestCase.TestSteps.Step> testCaseStepList = new ArrayList<>();
                    TestLinkEntity.Folder folder = null;
                    TestLinkEntity.Folder.TestCase testCase = new TestLinkEntity.Folder.TestCase();
                    TestLinkEntity.Folder.TestCase.TestSteps testSteps = new TestLinkEntity.Folder.TestCase.TestSteps();
                    TestLinkEntity.Folder.TestCase.TestSteps.Step stepObj = null;
                    List<JsonObject> savedTCFolderList = new ArrayList<>();
                    JsonObject savedFolderObj = null;
                    String previousTag = null;
                    int prevFolderObjIndex = 0;
                    int prevSavedFolderObj = 0;
                    while (xmlStreamReader.hasNext()) {
                        int eventType = xmlStreamReader.next();
                        for (int i = 0; i < dataF.size(); i++) {
                            JsonObject xmlMapObj = dataF.get(i).getAsJsonObject();
                            String aiTestField = xmlMapObj.get("aiTestField").getAsString();
                            String xmlColumn = xmlMapObj.get("xmlColumn").getAsString();
                            String attribute = xmlMapObj.get("attributes").getAsString();
                            if (eventType == XMLStreamConstants.START_ELEMENT) {
                                previousTag = xmlStreamReader.getLocalName();
                                if (aiTestField.equals("TestCaseFolder") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
                                    folder = startElementTCFolderCreation(xmlStreamReader, folder, aiTestField, xmlColumn, attribute);
                                } else {
                                    stepObj = startElementCreation(xmlStreamReader, folder, testCase, aiTestField, xmlColumn, stepObj, attribute);
                                }
                            } else if (eventType == XMLStreamConstants.CDATA) {
                                savedFolderObj = cDataElementUpsert(xmlStreamReader, testLinkEntity, folderList, folder, testCase, savedFolderObj, previousTag, aiTestField, xmlColumn, stepObj, project, savedTCFolderList);
                            } else if (eventType == XMLStreamConstants.CHARACTERS) {
                                savedFolderObj = characterElementUpsert(xmlStreamReader, testLinkEntity, folderList, folder, testCase, savedFolderObj, previousTag, aiTestField, xmlColumn, stepObj, project);
                            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                                folder = endElementCreation(user, xmlStreamReader, testLinkEntity, testCaseList, folder, testCase, testSteps, savedFolderObj, aiTestField, xmlColumn, testCaseStepList, stepObj, project, prevFolderObjIndex, automated, manual);
                                savedFolderObj = removeSavedTCObj(xmlStreamReader, savedTCFolderList, savedFolderObj, aiTestField, xmlColumn);
                            }
                        }
                    }
                    insertSuccess = true;
                }
            }
        } catch (XMLStreamException | IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            closeStreams(xmlStreamReader);
            isCompleted = removeFile(insertSuccess, path);
        }
        return String.valueOf(isCompleted);
    }

    private JsonObject removeSavedTCObj(XMLStreamReader xmlStreamReader, List<JsonObject> savedTCFolderList, JsonObject savedFolderObj, String aiTestField, String xmlColumn) {
        if (aiTestField.equals("TestCaseFolder") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
            try {
                if (Objects.nonNull(savedFolderObj) && savedFolderObj.get("saveSuccess").getAsBoolean()) {
                    int prevSavedFolderObj = savedTCFolderList.indexOf(savedFolderObj);
                    savedTCFolderList.remove(savedFolderObj);
                    if (savedTCFolderList.size() > 0 && Objects.nonNull(savedFolderObj)) {
                        savedFolderObj = savedTCFolderList.get(prevSavedFolderObj - 1);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error(e.getMessage(), e);
            }
        }
        return savedFolderObj;
    }

    private boolean removeFile(boolean inserted, String filePath) {
        if (inserted) {
            try {
                FileUtils.touch(new File(filePath));
                return FileUtils.deleteQuietly(new File(filePath));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    private String deleteFile(boolean removeFile, String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject dataObj = element.getAsJsonObject();
        String filePath = dataObj.get("path").getAsString();
        if (!removeFile && filePath.length() > 0) {
            try {
                FileUtils.touch(new File(filePath));
                return String.valueOf(FileUtils.deleteQuietly(new File(filePath)));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return String.valueOf(false);
    }

    private TestLinkEntity.Folder startElementTCFolderCreation(XMLStreamReader xmlStreamReader, TestLinkEntity.Folder folder, String aiTestField, String xmlColumn, String attribute) {
        if (aiTestField.equals("TestCaseFolder") && xmlStreamReader.getLocalName().equals(xmlColumn) && xmlStreamReader.getAttributeCount() > 0) {
            folder = new TestLinkEntity.Folder();
            if (Objects.nonNull(xmlStreamReader.getAttributeValue(null, attribute)) && !(attribute.isEmpty())) {
                folder.setFolderName(xmlStreamReader.getAttributeValue(null, attribute));
            } else {
                folder.setFolderName("");
            }
        }
        return folder;
    }

    private TestLinkEntity.Folder.TestCase.TestSteps.Step startElementCreation(XMLStreamReader xmlStreamReader, TestLinkEntity.Folder folder, TestLinkEntity.Folder.TestCase testCase, String aiTestField, String xmlColumn, TestLinkEntity.Folder.TestCase.TestSteps.Step stepObj, String attribute) {
        if (aiTestField.equals("TestCase") && xmlStreamReader.getLocalName().equals(xmlColumn) && xmlStreamReader.getAttributeCount() > 0) {
            if (Objects.nonNull(xmlStreamReader.getAttributeValue(null, attribute)) && !(attribute.isEmpty())) {
                testCase.setName(xmlStreamReader.getAttributeValue(null, attribute));
            } else {
                testCase.setName("");
            }
        } else if (aiTestField.equals("TestStep") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
            stepObj = new TestLinkEntity.Folder.TestCase.TestSteps.Step();
        }
        return stepObj;
    }

    private JsonObject createTestCaseFolder(XMLStreamReader xmlStreamReader, TestLinkEntity testLinkEntity, List<TestLinkEntity.Folder> folderList, TestLinkEntity.Folder folder, String project, JsonObject savedFolderObj) {
        if (!Objects.nonNull(savedFolderObj)) {
            folder.setFolderId("null");
        } else {
            folder.setFolderId(savedFolderObj.get("parent").getAsString());
        }
        folder.setDescription(removeNewLines(xmlStreamReader.getText()));
        folder.setProjectKey(project);
        folderList.add(folder);
        testLinkEntity.setFolder(folderList);
        String testCaseFolderSaved = new TLinkOperation(activeObjects).saveTestCaseFolder(folder);
        JsonElement folderElement = new JsonParser().parse(testCaseFolderSaved);
        savedFolderObj = folderElement.getAsJsonObject();
        return savedFolderObj;
    }

    private JsonObject cDataElementUpsert(XMLStreamReader xmlStreamReader, TestLinkEntity testLinkEntity, List<TestLinkEntity.Folder> folderList, TestLinkEntity.Folder folder, TestLinkEntity.Folder.TestCase testCase, JsonObject savedFolderObj, String previousTag, String aiTestField, String xmlColumn, TestLinkEntity.Folder.TestCase.TestSteps.Step stepObj, String project, List<JsonObject> savedTCFolderList) {
        if (aiTestField.equals("TestCaseFolder Description") && previousTag.equals(xmlColumn) && (!folder.getFolderName().isEmpty())) {
            savedFolderObj = createTestCaseFolder(xmlStreamReader, testLinkEntity, folderList, folder, project, savedFolderObj);
            savedTCFolderList.add(savedFolderObj);
        } else if (aiTestField.equals("TestCase Description") && previousTag.equals(xmlColumn)) {
            testCase.setDescription(html2text(xmlStreamReader.getText()));

        } else if (aiTestField.equals("TestCase Execution Type") && previousTag.equals(xmlColumn)) {
            testCase.setExecution_type(removeNewLines(xmlStreamReader.getText()));

        } else if (aiTestField.equals("TestSteps ExpectedResult") && previousTag.equals(xmlColumn) && Objects.nonNull(stepObj)) {
            stepObj.setExpectedResult(beautifySteps(xmlStreamReader.getText()));

        } else if (aiTestField.equals("TestSteps Step") && previousTag.equals(xmlColumn) && Objects.nonNull(stepObj)) {
            stepObj.setStepDescription(beautifySteps(xmlStreamReader.getText()));

        }
        return savedFolderObj;
    }

    private TestLinkEntity.Folder endElementCreation(String user, XMLStreamReader xmlStreamReader, TestLinkEntity testLinkEntity, List<TestLinkEntity.Folder.TestCase> testCaseList, TestLinkEntity.Folder folder, TestLinkEntity.Folder.TestCase testCase, TestLinkEntity.Folder.TestCase.TestSteps testSteps, JsonObject savedFolderObj, String aiTestField, String xmlColumn, List<TestLinkEntity.Folder.TestCase.TestSteps.Step> testCaseStepList, TestLinkEntity.Folder.TestCase.TestSteps.Step stepObj, String project, int prevFolderObjIndex, String automated, String manual) {

        if (aiTestField.equals("TestCase") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
            insertTestCaseData(user, testCaseList, testCaseStepList, folder, testCase, savedFolderObj, project, automated, manual);
        } else if (aiTestField.equals("TestCaseFolder") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
            try {
                if (savedFolderObj.get("saveSuccess").getAsBoolean()) {
                    folder = updateFolderObj(testLinkEntity, folder);
                } else {
                    log.error("TestCaseFolder Saving Error");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error(e.getMessage(), e);
            }
        } else if (aiTestField.equals("TestStep") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
            testCaseStepList.add(stepObj);
        } else if (aiTestField.equals("TestSteps") && xmlStreamReader.getLocalName().equals(xmlColumn)) {
            testSteps.setStepList(testCaseStepList);
        }
        return folder;
    }

    private TestLinkEntity.Folder updateFolderObj(TestLinkEntity testLinkEntity, TestLinkEntity.Folder folder) {
        int prevFolderObjIndex;
        if (Objects.nonNull(folder)) {
            prevFolderObjIndex = testLinkEntity.getFolder().indexOf(folder);
            testLinkEntity.getFolder().remove(folder);
            if (prevFolderObjIndex >= 1 && testLinkEntity.getFolder().size() > 0 && Objects.nonNull(testLinkEntity.getFolder().get(prevFolderObjIndex - 1))) {
                folder = testLinkEntity.getFolder().get(prevFolderObjIndex - 1);
            }
            log.info("TestCaseFolder Saved");
        }
        return folder;
    }

    private String saveTestCase(JiraAuthenticationContext authenticationContext, ActiveObjects activeObjects, List<TestLinkEntity.Folder.TestCase> testCaseList, String user, String automated, String manual, String project) {
        TestCase tCaseObj = new TestCase(authenticationContext, activeObjects);
        JSONObject testCaseObj = new JSONObject();
        String executionTypeAutomated = "";
        String executionTypeManual = "";
        JSONObject returnObject = new JSONObject();
        if (Objects.nonNull(testCaseList.get(0))) {
            try {
                if (Objects.nonNull(manual) && testCaseList.get(0).getExecution_type().equals(manual)) {
                    executionTypeAutomated = "no";
                    executionTypeManual = "yes";
                } else if (Objects.nonNull(automated) && testCaseList.get(0).getExecution_type().equals(automated)) {
                    executionTypeManual = "no";
                    executionTypeAutomated = "yes";
                }
                testCaseObj.put("parent", Integer.parseInt(testCaseList.get(0).getParent()));
                testCaseObj.put("name", testCaseList.get(0).getName());
                testCaseObj.put("description", testCaseList.get(0).getDescription());
                testCaseObj.put("overall_expected_result", "");
                testCaseObj.put("automated", executionTypeAutomated);
                testCaseObj.put("manual", executionTypeManual);
                String issue = tCaseObj.createIssue(testCaseObj.toString(), user, project);
                JsonElement element = new JsonParser().parse(issue);
                JsonObject jsonData = element.getAsJsonObject();
                if (!jsonData.get("id").getAsString().isEmpty()) {
                    returnObject.put("saveSuccess", true);
                    returnObject.put("testCaseId", jsonData.get("id").getAsString());
                } else {
                    returnObject.put("saveSuccess", false);
                    returnObject.put("testCaseId", jsonData.get("id").getAsString());
                }
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return returnObject.toString();
    }

    private void insertTestCaseData(String user, List<TestLinkEntity.Folder.TestCase> testCaseList, List<TestLinkEntity.Folder.TestCase.TestSteps.Step> testCaseStepList, TestLinkEntity.Folder folder, TestLinkEntity.Folder.TestCase testCase, JsonObject savedFolderObj, String project, String automated, String manual) {
        testCase.setParent(savedFolderObj.get("parent").getAsString());
        testCaseList.add(testCase);
        folder.setTestCaseList(testCaseList);
        String testCaseSaved = saveTestCase(authenticationContext, activeObjects, folder.getTestCaseList(), user, automated, manual, project);
        JsonElement jsonElement = new JsonParser().parse(testCaseSaved);
        JsonObject testCaseJsonObj = jsonElement.getAsJsonObject();
        String testCaseId = testCaseJsonObj.get("testCaseId").getAsString();
        if (testCaseJsonObj.get("saveSuccess").getAsBoolean()) {
            folder.getTestCaseList().remove(testCase);
            boolean testStepsSaved = new TLinkOperation(activeObjects).saveTestSteps(project, testCaseId, testCaseStepList);
            if (testStepsSaved) {
                log.info("Test Steps Saved");
                testCaseStepList.clear();
            } else {
                log.info("No Test Steps To Save");
            }
            log.info("TestCase Saved");
        } else {
            log.error("TestCase Saving Error");
        }
    }

    private JsonObject characterElementUpsert(XMLStreamReader xmlStreamReader, TestLinkEntity testLinkEntity, List<TestLinkEntity.Folder> folderList, TestLinkEntity.Folder folder, TestLinkEntity.Folder.TestCase testCase, JsonObject savedFolderObj, String previousTag, String aiTestField, String xmlColumn, TestLinkEntity.Folder.TestCase.TestSteps.Step stepObj, String project) {
        if (aiTestField.equals("TestCaseFolder Description") && previousTag.equals(xmlColumn) && (!folder.getFolderName().isEmpty()) && !(removeNewLines(xmlStreamReader.getText()).equals("")) || !(removeNewLines(xmlStreamReader.getText()).isEmpty())) {
            savedFolderObj = createTestCaseFolder(xmlStreamReader, testLinkEntity, folderList, folder, project, savedFolderObj);

        } else if (aiTestField.equals("TestCase Description") && previousTag.equals(xmlColumn) && !(removeNewLines(xmlStreamReader.getText()).equals("")) || !(removeNewLines(xmlStreamReader.getText()).isEmpty())) {
            testCase.setDescription(html2text(xmlStreamReader.getText()));

        } else if (aiTestField.equals("TestCase Execution Type") && previousTag.equals(xmlColumn) && !(removeNewLines(xmlStreamReader.getText()).equals("")) || !(removeNewLines(xmlStreamReader.getText()).isEmpty())) {
            testCase.setExecution_type(removeNewLines(xmlStreamReader.getText()));

        } else if (aiTestField.equals("TestSteps ExpectedResult") && previousTag.equals(xmlColumn) && Objects.nonNull(stepObj) && !(removeNewLines(xmlStreamReader.getText()).equals("")) || !(removeNewLines(xmlStreamReader.getText()).isEmpty())) {
            stepObj.setExpectedResult(beautifySteps(xmlStreamReader.getText()));

        } else if (aiTestField.equals("TestSteps Step") && previousTag.equals(xmlColumn) && Objects.nonNull(stepObj) && !(removeNewLines(xmlStreamReader.getText()).equals("")) || !(removeNewLines(xmlStreamReader.getText()).isEmpty())) {
            stepObj.setStepDescription(beautifySteps(xmlStreamReader.getText()));

        }
        return savedFolderObj;
    }

    private String sendKeysToView(String path) {
        JSONObject returnObject = new JSONObject();
        Set<String> xmlColumn = new LinkedHashSet<>();
        Set<String> attributeList = new HashSet<>();
        List<String> aiTestFieldList = new ArrayList<>();
        XMLStreamReader xmlStreamReader = null;
        boolean fieldsAdded = aiTestFieldList.addAll(fieldCollection);
        try (FileInputStream fileInputStream = new FileInputStream(new File(path))) {
            if (fieldsAdded) {
                XMLInputFactory f = XMLInputFactory.newFactory();
                xmlStreamReader = f.createXMLStreamReader(fileInputStream);
                while (xmlStreamReader.hasNext()) {
                    int eventType = xmlStreamReader.next();
                    if (eventType == XMLStreamConstants.START_ELEMENT) {
                        xmlColumn.add(xmlStreamReader.getLocalName());
                        for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                            attributeList.add(xmlStreamReader.getAttributeLocalName(i));
                        }
                    }
                }
            }
            xmlColumn.add("none");
            returnObject.put("path", path);
            returnObject.put("xmlColumnList", xmlColumn);
            returnObject.put("attributeList", attributeList);
            returnObject.put("aiTestFieldList", aiTestFieldList);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (XMLStreamException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            closeStreams(xmlStreamReader);
        }
        return returnObject.toString();
    }

    private String getAllProjects() {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        List<Project> projects = projectManager.getProjects();
        JSONArray returnArray = new JSONArray();
        try {
            for (Project project : projects) {
                JSONObject projectObj = new JSONObject();
                projectObj.put(String.valueOf(project.getId()), project.getName());
                returnArray.put(projectObj);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnArray.toString();
    }

    private void closeStreams(XMLStreamReader xmlStreamReader) {
        if (!(xmlStreamReader == null)) {
            try {
                xmlStreamReader.close();
            } catch (XMLStreamException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private String html2text(String html) {
        if (!(html == null)) {
            Document document = Jsoup.parse(html);
            return Parser.unescapeEntities(Jsoup.clean(document.body().toString(), "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false)), false);
        }
        return null;
    }

    private String beautifySteps(String html) {
        if (!(html == null)) {
            Document document = Jsoup.parse(html);
            document.outputSettings(new Document.OutputSettings().prettyPrint(false));
            document.select("span").prepend("\n");
            String str = document.html().replaceAll("\n", "\n");
            return Parser.unescapeEntities(Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false)), false).trim();
        }
        return null;
    }

    private String removeNewLines(String str) {
        return Jsoup.parse(str).text();
    }
}