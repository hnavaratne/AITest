package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestPlanTestCases;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Scanned
public class TPTCase {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TPTCase(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestPlanTestCases insert(String testCaseId, String testPlanId, String agent, String user, boolean isPoolAgent) {
        return activeObjects.executeInTransaction(() -> {
            TestPlanTestCases testPlanTestCases = activeObjects.create(TestPlanTestCases.class);
            testPlanTestCases.setTestCaseId(testCaseId);
            testPlanTestCases.setTestPlanId(testPlanId);
            testPlanTestCases.setPoolAgent(isPoolAgent);
            testPlanTestCases.setAgent(agent);
            testPlanTestCases.setUser(user);
            testPlanTestCases.save();
            return testPlanTestCases;
        });
    }

    public List<TestPlanTestCases> get(String testPlanId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestPlanTestCases.class, "TEST_PLAN_ID = ?", testPlanId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String testPlanId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(TestPlanTestCases.class, "TEST_PLAN_ID = ?", testPlanId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean deleteTestCase(String testCaseId) {

        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(TestPlanTestCases.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
