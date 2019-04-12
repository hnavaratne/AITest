package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestSteps;
import net.java.ao.ActiveObjectsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * @author RPSPERERA on 7/2/2018
 */
@Scanned
public class TSteps {
    private static final Logger log = LoggerFactory.getLogger(TSteps.class);

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TSteps(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestSteps insert(String project, String testCaseId, String step, String expectedResult, String data, String scriptId) {
        return activeObjects.executeInTransaction(() -> {
            TestSteps testStep = activeObjects.create(TestSteps.class);
            testStep.setProjectKey(project);
            testStep.setTestCaseId(testCaseId);
            testStep.setStep(step);
            testStep.setExpectedResult(expectedResult);
            testStep.setData(data);
            testStep.setScriptId(scriptId);
            testStep.save();
            return testStep;
        });
    }

    public TestSteps upsert(String project, String testCaseId, String step, String expectedResult, String data, String scriptid) {
        return activeObjects.executeInTransaction(() -> {
            TestSteps[] testSteps = activeObjects.find(TestSteps.class, "TEST_CASE_ID = ? AND PROJECT_KEY = ? AND STEP = ? AND EXPECTED_RESULT = ? AND DATA = ?", testCaseId, project, step, expectedResult, data);
            try {
                if (!(testSteps.length > 0)) {
                    return insert(project, testCaseId, step, expectedResult, data, scriptid);
                } else {
                    TestSteps testStep = testSteps[0];
                    testStep.setProjectKey(project);
                    testStep.setTestCaseId(testCaseId);
                    testStep.setStep(step);
                    testStep.setExpectedResult(expectedResult);
                    testStep.setData(data);
                    testStep.setScriptId(scriptid);
                    testStep.save();
                    return testStep;
                }
            } catch (NullPointerException e) {
                log.error(e.getMessage());
            }
            return testSteps[0];
        });
    }

    public void updateBFunctions(String project, String scriptid) {
        activeObjects.executeInTransaction(() -> {
            TestSteps[] testSteps = activeObjects.find(TestSteps.class, "PROJECT_KEY = ? AND SCRIPT_ID = ?", project, scriptid);
            TestStepParam testStepParam = new TestStepParam(activeObjects);
            try {
                if (testSteps.length > 0) {
                    for (TestSteps testStep : testSteps) {
                        testStepParam.delete(testStep.getTestCaseId(), testStep.getData());
                        testStep.setStep("This business function was deleted.");
                        testStep.setScriptId("");
                        testStep.save();
                    }
                }
            } catch (NullPointerException e) {
                log.error(e.getMessage());
            }
            return "";
        });
    }

    public TestSteps get(String project, String testCaseId, String step, String expectedResult, String data) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestSteps.class, "TEST_CASE_ID = ? AND PROJECT_KEY = ? AND STEP = ? AND EXPECTED_RESULT = ? AND DATA = ?", testCaseId, project, step, expectedResult, data))).get(0);
        } catch (ActiveObjectsException e) {
            return null;
        }
    }

    public Set<String> getTestSteps(String project, String functionId) {
        try {
            Set<String> set = new HashSet<>();
           return activeObjects.executeInTransaction(() -> {
                List<TestSteps> testSteps = Arrays.asList(activeObjects.find(TestSteps.class, "PROJECT_KEY = ? AND SCRIPT_ID = ?", project, functionId));
                testSteps.forEach(testSteps1 -> {
                    String testCaseId = testSteps1.getTestCaseId();
                    set.add(testCaseId);
                });
                return set;
            });
        } catch (ActiveObjectsException e) {
            return null;
        }
    }

    public List<TestSteps> get(String testCaseId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestSteps.class, "TEST_CASE_ID = ? ", testCaseId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<TestSteps> get(String testCaseId, String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestSteps.class, "TEST_CASE_ID = ?  AND PROJECT_KEY = ?", testCaseId, projectKey)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(TestSteps.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
