package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestParamV;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
import net.java.ao.ActiveObjectsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author RPSPERERA on 7/20/2018
 */
@Scanned
public class TestStepParamValue {
    private static final Logger log = LoggerFactory.getLogger(TestStepParamValue.class);

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TestStepParamValue(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }


    public void upsert(String testCaseId, String paramName, String paramValue) {

        activeObjects.executeInTransaction(() -> {
            TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? AND PARAM_NAME = ? ", testCaseId, paramName);
            TestStepParams testStepParam = testStepParams[0];
            TestParamV TestParamV = activeObjects.create(TestParamV.class);
            TestParamV.setTestParam(testStepParam);
            TestParamV.setValue(paramValue);
            TestParamV.save();
            return TestParamV;
        });
    }

    public void delete(String testCaseId) {
        activeObjects.executeInTransaction(() -> {
            try {
                TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? ", testCaseId);
                if (testStepParams.length > 0) {
                    TestStepParams testStepParam = testStepParams[0];
                    TestParamV[] testStepParamValues = testStepParam.getTestStepParamValues();
                    for (TestParamV testStepParamValue : testStepParamValues) {
                        activeObjects.delete(testStepParamValue);
                    }
                }
            } catch (ActiveObjectsException e) {
                log.error(e.getMessage());
            }
            return null;
        });
    }

    public void deleteAll(String testCaseId) {
        activeObjects.executeInTransaction(() -> {
            try {
                TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? ", testCaseId);
                if (testStepParams.length > 0) {
                    for (TestStepParams testStepParam : testStepParams) {
                        TestParamV[] testStepParamValues = testStepParam.getTestStepParamValues();
                        for (TestParamV testStepParamValue : testStepParamValues) {
                            activeObjects.delete(testStepParamValue);
                        }
                    }
                }
            } catch (ActiveObjectsException e) {
                log.error(e.getMessage());
            }
            return null;
        });
    }


    public void delete(String testCaseId, String paramName) {
        activeObjects.executeInTransaction(() -> {
            try {
                TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? AND PARAM_NAME = ? ", testCaseId, paramName);
                if (testStepParams.length > 0) {
                    TestStepParams testStepParam = testStepParams[0];
                    TestParamV[] testStepParamValues = testStepParam.getTestStepParamValues();
                    for (TestParamV testStepParamValue : testStepParamValues) {
                        activeObjects.delete(testStepParamValue);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return null;
        });
    }

    public void getOrdered(String testCaseId, String paramName) {
        activeObjects.executeInTransaction(() -> {
            try {
                TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ?", testCaseId);
                if (testStepParams.length > 0) {
                    TestStepParams testStepParam = testStepParams[0];
                    TestParamV[] testStepParamValues = testStepParam.getTestStepParamValues();
                    for (TestParamV testStepParamValue : testStepParamValues) {
                        activeObjects.delete(testStepParamValue);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return null;
        });
    }

    public void deleteParams(String testCaseId, String paramName) {

        activeObjects.executeInTransaction(() -> {
            try {
                TestStepParams[] testStepParams = activeObjects.find(TestStepParams.class, "TEST_CASE_ID = ? AND PARAM_NAME = ? ", testCaseId, paramName);
                if (testStepParams.length > 0) {
                    TestStepParams testStepParam = testStepParams[0];
                    TestParamV[] testStepParamValues = testStepParam.getTestStepParamValues();
                    for (TestParamV testStepParamValue : testStepParamValues) {
                        activeObjects.delete(testStepParamValue);
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return null;
        });

    }


}


