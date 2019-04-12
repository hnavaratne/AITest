package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.BusComSteps;
import com.virtusa.gto.plugins.aitest.entity.TestStepStatus;
import com.virtusa.gto.plugins.aitest.entity.TestSteps;
import com.virtusa.gto.plugins.aitest.entity.TestStepsI;
import net.java.ao.ActiveObjectsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TStepsInstance {

    private static final Logger log = LoggerFactory.getLogger(TStepsInstance.class);

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TStepsInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestStepsI insert(TestSteps step, String execEntityTPTC) {
        return activeObjects.executeInTransaction(() -> {
            TestStepsI testStepI = activeObjects.create(TestStepsI.class);
            testStepI.setProjectKey(step.getProjectKey());
            testStepI.setTestCaseId(step.getTestCaseId());
            testStepI.setExecEntityTPTCI(execEntityTPTC);
            testStepI.setStep(step.getStep());
            testStepI.setExpectedResult(step.getExpectedResult());
            testStepI.setData(step.getData());
            testStepI.setActualStatus(TestStepStatus.IN_PROGRESS.value());
            testStepI.save();
            return testStepI;
        });
    }

    public TestStepsI insertBCSteps(BusComSteps step, String projectKey, String testCaseId, String execEntityTPTC) {
        return activeObjects.executeInTransaction(() -> {
            TestStepsI testStepI = activeObjects.create(TestStepsI.class);
            testStepI.setProjectKey(projectKey);
            testStepI.setTestCaseId(testCaseId);
            testStepI.setExecEntityTPTCI(execEntityTPTC);
            testStepI.setStep(step.getStepDescription());
            testStepI.setExpectedResult(step.getExpectedValue());
            testStepI.setData(step.getDataParams());
            testStepI.setActualStatus(TestStepStatus.IN_PROGRESS.value());
            testStepI.save();
            return testStepI;
        });
    }

    public TestStepsI insert(TestSteps step, String execEntityTPTC, String dataIteration) {
        return activeObjects.executeInTransaction(() -> {
            TestStepsI testStepI = activeObjects.create(TestStepsI.class);
            testStepI.setProjectKey(step.getProjectKey());
            testStepI.setTestCaseId(step.getTestCaseId());
            testStepI.setExecEntityTPTCI(execEntityTPTC);
            testStepI.setStep(step.getStep());
            testStepI.setExpectedResult(step.getExpectedResult());
            testStepI.setData(step.getData());
            testStepI.setDataIterationId(dataIteration);
            testStepI.setActualStatus(TestStepStatus.IN_PROGRESS.value());
            testStepI.save();
            return testStepI;
        });
    }

    public TestStepsI insertBCSteps(BusComSteps step, String projectKey, String testCaseId, String execEntityTPTC, String dataIteration) {
        return activeObjects.executeInTransaction(() -> {
            TestStepsI testStepI = activeObjects.create(TestStepsI.class);
            testStepI.setProjectKey(projectKey);
            testStepI.setTestCaseId(testCaseId);
            testStepI.setExecEntityTPTCI(execEntityTPTC);
            testStepI.setStep(step.getStepDescription());
            testStepI.setExpectedResult(step.getExpectedValue());
            testStepI.setData(step.getDataParams());
            testStepI.setDataIterationId(dataIteration);
            testStepI.setActualStatus(TestStepStatus.IN_PROGRESS.value());
            testStepI.save();
            return testStepI;
        });
    }

    public boolean upsert(String stepId, String actualResult, String actualStatus) {
        return activeObjects.executeInTransaction(() -> {
            TestStepsI[] testStepsIS = activeObjects.find(TestStepsI.class, "ID = ?", stepId);
            try {
                if (Objects.nonNull(testStepsIS[0])) {
                    TestStepsI tSI = testStepsIS[0];
                    tSI.setActualResult(actualResult);
                    tSI.setActualStatus(actualStatus);
                    tSI.save();
                    return true;
                }
            } catch (NullPointerException e) {
                log.error(e.getMessage());
            }
            return false;
        });
    }

    public List<TestStepsI> get(String execEntityTPTC) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestStepsI.class, "EXEC_ENTITY_TPTCI = ? ", execEntityTPTC)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<TestStepsI> get(String execEntityTPTC, String dataIterationId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestStepsI.class, "EXEC_ENTITY_TPTCI = ? AND DATA_ITERATION_ID = ?", execEntityTPTC, dataIterationId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String execEntityTPTC) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(TestSteps.class, "EXEC_ENTITY_TPTCI = ?", execEntityTPTC);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
