package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.DefectsEntityI;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefectsEntity {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public DefectsEntity(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public DefectsEntityI insert(int executionEntityId, int execEntityTPI, int execEntityTPTCI, String defectId, String defectKey, String defectName, String cycle, String testCaseName, String tPTCIMyTask, String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            DefectsEntityI defectsEntityI = activeObjects.create(DefectsEntityI.class);
            defectsEntityI.setExecutionEntityId(executionEntityId);
            defectsEntityI.setExecutionEntityTestPlanId(execEntityTPI);
            defectsEntityI.setExecEntityTPTC(execEntityTPTCI);
            defectsEntityI.setDefectId(defectId);
            defectsEntityI.setDefectKey(defectKey);
            defectsEntityI.setDefectName(defectName);
            defectsEntityI.setCycle(cycle);
            defectsEntityI.setTestCaseName(testCaseName);
            defectsEntityI.setTPTCIMyTaskId(tPTCIMyTask);
            defectsEntityI.setTestCaseId(testCaseId);
            defectsEntityI.save();
            return defectsEntityI;
        });
    }

    public DefectsEntityI insert(int executionEntityId, int execEntityTPI, int execEntityTPTCI, String defectId, String defectKey, String defectName, String cycle, String testCaseName, String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            DefectsEntityI defectsEntityI = activeObjects.create(DefectsEntityI.class);
            defectsEntityI.setExecutionEntityId(executionEntityId);
            defectsEntityI.setExecutionEntityTestPlanId(execEntityTPI);
            defectsEntityI.setExecEntityTPTC(execEntityTPTCI);
            defectsEntityI.setDefectId(defectId);
            defectsEntityI.setDefectKey(defectKey);
            defectsEntityI.setDefectName(defectName);
            defectsEntityI.setCycle(cycle);
            defectsEntityI.setTestCaseName(testCaseName);
            defectsEntityI.setTestCaseId(testCaseId);
            defectsEntityI.save();
            return defectsEntityI;
        });
    }

    public boolean update(String defectId, String defectKey, String defectName, String tPTCIMyTask) {
        return activeObjects.executeInTransaction(() -> {
            try {
                DefectsEntityI defectsEntityI = activeObjects.create(DefectsEntityI.class);
                defectsEntityI.setDefectId(defectId);
                defectsEntityI.setDefectKey(defectKey);
                defectsEntityI.setDefectName(defectName);
                defectsEntityI.setTPTCIMyTaskId(tPTCIMyTask);
                defectsEntityI.save();
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean delete(String tPTCIMyTask) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(DefectsEntityI.class, "TPTCIMY_TASK_ID = ?", tPTCIMyTask);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public Integer getPlanDefectCount(String execEntityPlanInstanceId) {
        try {
            Query defectCountQuery = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.EXECUTION_ENTITY_TEST_PLAN_ID = ? AND defectsEntityI.DEFECT_ID != 'NULL'", execEntityPlanInstanceId);
            return activeObjects.executeInTransaction(() -> activeObjects.count(DefectsEntityI.class, defectCountQuery));
        } catch (ActiveObjectsException e) {
            return 0;
        }
    }

    public Integer getTestCaseDefectCount(String execEntityTPTCInstanceId) {
        try {
            Query testCaseDefectCountQuery = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.EXEC_ENTITY_TPTC = ? AND defectsEntityI.DEFECT_ID != 'NULL'", execEntityTPTCInstanceId);

            return activeObjects.executeInTransaction(() -> activeObjects.count(DefectsEntityI.class, testCaseDefectCountQuery));
        } catch (ActiveObjectsException e) {
            return 0;
        }
    }

    public Integer getEntityDefectCount(String execEntityInstanceId) {
        try {
            Query defectHasIterationCountQuery = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.EXECUTION_ENTITY_ID = ? AND defectsEntityI.DEFECT_ID != 'NULL'", execEntityInstanceId);

            return activeObjects.executeInTransaction(() -> activeObjects.count(DefectsEntityI.class, defectHasIterationCountQuery));
        } catch (ActiveObjectsException e) {
            return 0;
        }
    }

    public List<DefectsEntityI> getPlanDefectList(String execEntityPlanInstanceId) {
        try {
            Query defectList = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.EXECUTION_ENTITY_TEST_PLAN_ID = ? AND defectsEntityI.DEFECT_ID != 'NULL'", execEntityPlanInstanceId);

            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(DefectsEntityI.class, defectList)));

        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<DefectsEntityI> getTestCaseDefectList(String execEntityTPTCInstanceId) {
        try {
            Query testCaseDefectList = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.EXEC_ENTITY_TPTC = ? AND defectsEntityI.DEFECT_ID != 'NULL'", execEntityTPTCInstanceId);

            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(DefectsEntityI.class, testCaseDefectList)));

        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<DefectsEntityI> getTestCaseAllDefectList(String testCaseId) {
        try {
            Query testCaseDefectList = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.TEST_CASE_ID = ? AND defectsEntityI.DEFECT_ID != 'NULL'", testCaseId);

            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(DefectsEntityI.class, testCaseDefectList)));

        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean deleteDefectsForTestCase(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(DefectsEntityI.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public Integer getTestCaseRelatedDefectCount(String testCaseId) {
        try {
            Query testCaseDefectCount = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.TEST_CASE_ID = ? AND defectsEntityI.DEFECT_ID != 'NULL'", testCaseId);
            return activeObjects.count(DefectsEntityI.class, testCaseDefectCount);
        } catch (ActiveObjectsException e) {
            return 0;
        }
    }

    public List<DefectsEntityI> getDefectsSaved(String tPTCIMyTask) {
        try {
            Query savedDefectList = Query.select()
                    .alias(DefectsEntityI.class, "defectsEntityI")
                    .where("defectsEntityI.TPTCIMY_TASK_ID = ? AND defectsEntityI.DEFECT_ID != 'NULL'", tPTCIMyTask);
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(DefectsEntityI.class, savedDefectList)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }
}
