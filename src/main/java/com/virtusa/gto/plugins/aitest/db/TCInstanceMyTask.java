package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.EEntityTPTCStatus;
import com.virtusa.gto.plugins.aitest.entity.TCIMyTask;
import com.virtusa.gto.plugins.aitest.entity.TCaseDataIteration;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TCInstanceMyTask {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TCInstanceMyTask(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }


    public TCIMyTask insert(int entityId, int executionEntityTestPlanId, int execEntityTPTCI, String projectKey, TCaseDataIteration tCaseDataIteration, String tcId, String tcName, String manual, String user, String expectedResult, int cycle, String testCaseBrowser) {
        return activeObjects.executeInTransaction(() -> {
            TCIMyTask tCaseInstanceMyTask = activeObjects.create(TCIMyTask.class);
            tCaseInstanceMyTask.setEntityId(entityId);
            tCaseInstanceMyTask.setExecutionEntityTestPlanId(executionEntityTestPlanId);
            tCaseInstanceMyTask.setExecEntityTPTCI(execEntityTPTCI);
            tCaseInstanceMyTask.setProjectKey(projectKey);
            tCaseInstanceMyTask.setTCaseDataIteration(tCaseDataIteration);
            tCaseInstanceMyTask.setTestCaseId(tcId);
            tCaseInstanceMyTask.setTestCaseName(tcName);
            tCaseInstanceMyTask.setTestCaseManual(manual);
            tCaseInstanceMyTask.setTestCaseUser(user);
            tCaseInstanceMyTask.setOverallExpectedResult(expectedResult);
            tCaseInstanceMyTask.setCycle(cycle);
            tCaseInstanceMyTask.setTestCaseBrowser(testCaseBrowser);
            tCaseInstanceMyTask.setOverallStatus(EEntityTPTCStatus.IN_PROGRESS.value());
            tCaseInstanceMyTask.setExecuted(false);
            tCaseInstanceMyTask.save();
            return tCaseInstanceMyTask;
        });
    }

    public boolean update(String id, String actualResult, String overallStatus, boolean isExecuted) {
        return activeObjects.executeInTransaction(() -> {
            try {
                TCIMyTask[] execEntityTPTCIS = activeObjects.find(TCIMyTask.class, "ID = ?", id);
                for (TCIMyTask execEntityTPTCI : execEntityTPTCIS) {
                    execEntityTPTCI.setActualResult(actualResult);
                    execEntityTPTCI.setOverallStatus(overallStatus);
                    execEntityTPTCI.setExecuted(isExecuted);
                    execEntityTPTCI.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<TCIMyTask> get(String projectKey, String user) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TCIMyTask.class, "PROJECT_KEY = ? AND TEST_CASE_USER = ? AND EXECUTED = ? ", projectKey, user, "false")));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<TCIMyTask> get(String execEntityTPTCI) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TCIMyTask.class, "EXEC_ENTITY_TPTCI = ? ", execEntityTPTCI)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<TCIMyTask> getMyTask(String tPTCIMyTask) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TCIMyTask.class, "ID = ? ", tPTCIMyTask)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

}
