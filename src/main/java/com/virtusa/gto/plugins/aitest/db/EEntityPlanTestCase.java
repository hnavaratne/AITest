package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.ExecEntity;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTC;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EEntityPlanTestCase {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntityPlanTestCase(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public ExecEntityTPTC insert(int executionEntityTestPlanId, String tcId, String tcName, String automated, String manual, boolean isPoolAgent, String agent, String user, String expectedResult, String testCaseBrowser) {
        return activeObjects.executeInTransaction(() -> {
            ExecEntityTPTC execEntityTPTC = activeObjects.create(ExecEntityTPTC.class);
            execEntityTPTC.setExecutionEntityTestPlanId(executionEntityTestPlanId);
            execEntityTPTC.setTestCaseId(tcId);
            execEntityTPTC.setTestCaseName(tcName);
            execEntityTPTC.setTestCaseAutomated(automated);
            execEntityTPTC.setTestCaseManual(manual);
            execEntityTPTC.setTestCaseAgent(agent);
            execEntityTPTC.setPoolAgent(isPoolAgent);
            execEntityTPTC.setTestCaseUser(user);
            execEntityTPTC.setOverallExpectedResult(expectedResult);
            execEntityTPTC.setTestCaseBrowser(testCaseBrowser);
            execEntityTPTC.save();
            return execEntityTPTC;
        });
    }

    public boolean update(int execEntityTPId, String tcId, String tcName, String automated, String manual, boolean isPoolAgent, String agent, String user, String expectedResult, String testCaseBrowser) {
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntityTPTC[] execEntityTPTCS = activeObjects.find(ExecEntityTPTC.class, "EXECUTION_ENTITY_TEST_PLAN_ID = ?", execEntityTPId);
                for (ExecEntityTPTC execEntityTPTC : execEntityTPTCS) {
                    execEntityTPTC.setTestCaseId(tcId);
                    execEntityTPTC.setTestCaseName(tcName);
                    execEntityTPTC.setTestCaseAutomated(automated);
                    execEntityTPTC.setTestCaseManual(manual);
                    execEntityTPTC.setTestCaseAgent(agent);
                    execEntityTPTC.setPoolAgent(isPoolAgent);
                    execEntityTPTC.setTestCaseUser(user);
                    execEntityTPTC.setOverallExpectedResult(expectedResult);
                    execEntityTPTC.setTestCaseBrowser(testCaseBrowser);
                    execEntityTPTC.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<ExecEntityTPTC> get(int execEntityTPId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTC.class, "EXECUTION_ENTITY_TEST_PLAN_ID = ?", execEntityTPId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String id) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntityTPTC.class, "ID = ?", id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
