package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.virtusa.gto.plugins.aitest.db.TestStepParam;
import com.virtusa.gto.plugins.aitest.entity.TestParamV;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author RPSPERERA on 12/6/2018
 */
@Scanned
public class CommonsUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonsUtil.class);

    public static List<Map<String, Object>> getMappedVariables(String testCaseId, ActiveObjects activeObjects) {
        List<Map<String, Object>> jsonElements = new ArrayList<>();
        try {
            TestStepParam testStepParam = new TestStepParam(activeObjects);
            TestStepParams[] testStepParams = testStepParam.get(testCaseId);
            if (Objects.nonNull(testStepParams)) {
                for (TestStepParams stepParam : testStepParams) {
                    TestParamV[] testStepParamValues = testStepParams[0].getTestStepParamValues();
                    Map<String, Object> jsonObject = new HashMap<>();
                    jsonObject.put("name", stepParam.getParamName());
                    List<String> jArray = new ArrayList<>();
                    for (TestParamV testStepParamValue : testStepParamValues) {
                        jArray.add(testStepParamValue.getValue());
                    }
                    jsonObject.put("values", jArray);
                    jsonElements.add(jsonObject);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return jsonElements;
    }

    public static void copyExecutables(String resourcePath) {
        try {
            String tempDirectoryPath = System.getProperty("java.io.tmpdir");
            try (InputStream is = CommonsUtil.class.getClassLoader().getResourceAsStream(resourcePath);
                 ZipInputStream zis = new ZipInputStream(Objects.requireNonNull(is))) {
                ZipEntry zipEntry = zis.getNextEntry();
                byte[] buffer = new byte[1024];
                while (zipEntry != null) {
                    String fileName = zipEntry.getName();
                    if (zipEntry.isDirectory()) {
                        File folder = Paths.get(tempDirectoryPath, fileName).toFile();
                        boolean mkdirs = folder.mkdir();
                        logger.info("successfully created folder " + mkdirs);

                    } else {
                        File newFile = Paths.get(tempDirectoryPath, fileName).toFile();
                        boolean mkdirs = newFile.getParentFile().mkdirs();
                        logger.info("successfully created folders " + mkdirs);
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }

                        }
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static String getAllProjects() {
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
            logger.error(e.getMessage(), e);
        }
        return returnArray.toString();
    }

    public static void closeStreams(XMLStreamReader xmlStreamReader) {
        if (!(xmlStreamReader == null)) {
            try {
                xmlStreamReader.close();
            } catch (XMLStreamException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static String html2text(String html) {
        if (!(html == null)) {
            return Parser.unescapeEntities(Jsoup.clean(Jsoup.parse(html).body().toString(), "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false)), false);
        }
        return null;
    }

    public static String beautifySteps(String html) {
        if (!(html == null)) {
            Document document = Jsoup.parse(html);
            String str = document.outputSettings(new Document.OutputSettings().prettyPrint(false)).select("span").prepend("\n").html().replaceAll("\n", "\n");
            return Parser.unescapeEntities(Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false)), false);
        }
        return null;
    }

    public static String removeNewLines(String str) {
        return Jsoup.parse(str).text();
    }
}
