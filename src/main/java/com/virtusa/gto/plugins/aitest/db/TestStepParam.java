package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
import net.java.ao.ActiveObjectsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author RPSPERERA on 7/20/2018
 */
@Scanned
public class TestStepParam {
    private static final Logger log = LoggerFactory.getLogger(TestStepParam.class);
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TestStepParam(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestStepParams[] get(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                log.error(e.getMessage());
            }
            return null;
        });

    }

    public void upsertParams(String projectKey, String testCaseId, Map<String, HashMap<String, String>> mappedChangeSet, TestStepParamValue testStepParamValue) {
        mappedChangeSet.forEach((paramName, stringStringHashMap) -> {
            switch (stringStringHashMap.values().iterator().next()) {
                case "deleted":
                    activeObjects.executeInTransaction(() -> {
                        testStepParamValue.deleteParams(testCaseId, paramName);
                        activeObjects.deleteWithSQL(TestStepParams.class, "PROJECT_KEY = ? AND TEST_CASE_ID = ? AND PARAM_NAME = ?", projectKey, testCaseId, paramName);
                        return null;
                    });
                    break;
                case "change":
                    activeObjects.executeInTransaction(() -> {
                        TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "PROJECT_KEY = ? AND TEST_CASE_ID = ? AND PARAM_NAME = ?", projectKey, testCaseId, paramName);
                        Set<String> keySet = stringStringHashMap.keySet();
                        if (keySet.contains(paramName)) {
                            keySet.remove(paramName);
                        } else {
                            for (TestStepParams testStepParam : testStepParams) {
                                testStepParam.setParamName(keySet.iterator().next());
                                testStepParam.save();
                            }
                        }
                        keySet.forEach(key -> insertSingleParam(testCaseId, key, projectKey));
                        return null;
                    });
                    break;
                case "new":
                    insertSingleParam(testCaseId, paramName, projectKey);
                    break;
            }
        });
    }

    public TestStepParams[] getParameters(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? ", testCaseId);
            } catch (ActiveObjectsException e) {
                e.printStackTrace();
            }
            return null;
        });

    }

    public void delete(String testCaseId, String paramName) {
        activeObjects.executeInTransaction(() -> {
            activeObjects.deleteWithSQL(TestStepParams.class, "TEST_CASE_ID = ? AND PARAM_NAME = ?", testCaseId, paramName);
            return null;
        });
    }


    public boolean insert(String testCaseId, String params, String projectKey) {

        JsonElement jsonElement = new JsonParser().parse(params);
        JsonArray existingParams = jsonElement.getAsJsonArray();


        //iterate and get the new params and add to db
        //update params
        for (JsonElement json : existingParams) {
            JsonObject param = json.getAsJsonObject();
            activeObjects.executeInTransaction(() -> {
                TestStepParams testStepParams1 = activeObjects.create(TestStepParams.class);
                testStepParams1.setTestCaseId(testCaseId);
                testStepParams1.setProjectKey(projectKey);
                testStepParams1.setParamName(param.get("name").getAsString());
                testStepParams1.setData(param.get("data").getAsString());
                testStepParams1.save();
                return null;
            });

        }

        return true;
    }

    public void insertSingleParam(String testCaseId, String paramName, String projectKey) {
        activeObjects.executeInTransaction(() -> {
            TestStepParams testStepParams1 = activeObjects.create(TestStepParams.class);
            testStepParams1.setTestCaseId(testCaseId);
            testStepParams1.setProjectKey(projectKey);
            testStepParams1.setParamName(paramName);
            testStepParams1.save();
            return null;
        });
    }


    public int getCount(String testCaseId, String projectKey, String paramName) {
        return activeObjects.executeInTransaction(() -> activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? and PROJECT_KEY = ? AND PARAM_NAME = ? ", testCaseId, projectKey, paramName).length);
    }

}
