package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.EEntityTPTCStatus;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EEntityPlanTestCaseInstance {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntityPlanTestCaseInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public ExecEntityTPTCI insert(int eEntityId, int execEntityTPTC, int executionEntityTestPlanId, String tcId, String tcName, String automated, String manual, boolean isPoolAgent, String agent, String user, String expectedResult, int cycle, String testCaseBrowser, boolean hasIterationData, String userStoryId, String userStorySummary) {
        return activeObjects.executeInTransaction(() -> {
            ExecEntityTPTCI execEntityTPTCI = activeObjects.create(ExecEntityTPTCI.class);
            execEntityTPTCI.setEntityId(eEntityId);
            execEntityTPTCI.setExecEntityTPTC(execEntityTPTC);
            execEntityTPTCI.setExecutionEntityTestPlanId(executionEntityTestPlanId);
            execEntityTPTCI.setTestCaseId(tcId);
            execEntityTPTCI.setTestCaseName(tcName);
            execEntityTPTCI.setTestCaseAutomated(automated);
            execEntityTPTCI.setTestCaseManual(manual);
            execEntityTPTCI.setTestCaseAgent(agent);
            execEntityTPTCI.setPoolAgent(isPoolAgent);
            execEntityTPTCI.setTestCaseUser(user);
            execEntityTPTCI.setExecuted(false);
            execEntityTPTCI.setOverallExpectedResult(expectedResult);
            execEntityTPTCI.setOverallStatus(EEntityTPTCStatus.IN_PROGRESS.value());
            execEntityTPTCI.setCycle(cycle);
            execEntityTPTCI.setTestCaseBrowser(testCaseBrowser);
            execEntityTPTCI.setIterationData(hasIterationData);
            execEntityTPTCI.setUserStoryId(userStoryId);
            execEntityTPTCI.setUserStorySummary(userStorySummary);
            execEntityTPTCI.save();
            return execEntityTPTCI;
        });
    }

    public boolean update(String id, String actualResult, String overallStatus, boolean isExecuted) {
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntityTPTCI[] execEntityTPTCIS = activeObjects.find(ExecEntityTPTCI.class, "ID = ?", id);
                for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
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

    public boolean update(int id, String defectId, String defectName) {
        String execEntityTPTCId = String.valueOf(id);
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntityTPTCI[] execEntityTPTCIS = activeObjects.find(ExecEntityTPTCI.class, "ID = ?", execEntityTPTCId);
                for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                    execEntityTPTCI.setDefectId(defectId);
                    execEntityTPTCI.setDefectName(defectName);
                    execEntityTPTCI.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<ExecEntityTPTCI> get(int execEntityTPTC) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, "EXECUTION_ENTITY_TEST_PLAN_ID = ?", execEntityTPTC)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> get(int execEntityTPTC, String userStoryId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, "EXECUTION_ENTITY_TEST_PLAN_ID = ? AND USER_STORY_ID", execEntityTPTC, userStoryId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> getTCaseInstance(int execEntityTPTC) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, "ID = ?", execEntityTPTC)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> getTestCase(String execEntityTPTCI, String testcaseId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, "ID = ? AND TEST_CASE_ID", execEntityTPTCI, testcaseId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(int execEntityTPTC) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntityTPTCI.class, "EXECUTION_ENTITY_TEST_PLAN_ID = ?", execEntityTPTC);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }


    public ExecEntityTPTCI[] update(String curActVal, String curOveralStat, String curTcId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntityTPTCI[] execEntityTPTCIS = activeObjects.find(ExecEntityTPTCI.class, "ID = ?", Integer.valueOf(curTcId));
                for (ExecEntityTPTCI execEntityTPTCI : execEntityTPTCIS) {
                    execEntityTPTCI.setExecuted(true);
                    execEntityTPTCI.setActualResult(curActVal);
                    execEntityTPTCI.setOverallStatus(curOveralStat);
                    execEntityTPTCI.save();
                }
                return execEntityTPTCIS;
            } catch (ActiveObjectsException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public List<ExecEntityTPTCI> getEntityTpTc(int testPlanId, boolean isExecuted, String user) {
        try {
            return activeObjects.executeInTransaction(() -> activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, Query.select().where("EXECUTION_ENTITY_TEST_PLAN_ID = ? AND TEST_CASE_USER = ? AND EXECUTED = ? ", testPlanId, user, isExecuted)))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> getEntityTpTcList(int execEntityTPTCInstanceId, boolean isExecuted) {
        try {
            Query testCaseQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .where("eEPTCInstance.ID = ?  AND eEPTCInstance.EXECUTED = ?", execEntityTPTCInstanceId, isExecuted);

            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, testCaseQuery)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> getTestCasesForTPlan(String testPlanId) {
        try {
            return activeObjects.executeInTransaction(() -> activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, "EXECUTION_ENTITY_TEST_PLAN_ID = ? ", testPlanId))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

}
