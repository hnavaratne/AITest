package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.EEntityStatus;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityI;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPI;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class EEntityInstance {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntityInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public ExecEntityI insert(int eEntity, String release, String description, String projectKey) {
        return activeObjects.executeInTransaction(() -> {
            ExecEntityI execEntityI = activeObjects.create(ExecEntityI.class);
            execEntityI.setExecEntity(eEntity);
            execEntityI.setRelease(release);
            execEntityI.setDescription(description);
            execEntityI.setExecutionDate(new Date());
            execEntityI.setStatus(EEntityStatus.IN_PROGRESS.value());
            execEntityI.setProjectKey(projectKey);
            execEntityI.save();
            return execEntityI;
        });
    }

    public List<ExecEntityI> get(String data, String filter) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityI.class, Query.select().where("RELEASE = ?", filter).order("EXECUTION_DATE DESC"))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityI> getAll() {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityI.class, Query.select().order("EXECUTION_DATE DESC"))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public int getCycle(String release) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityI.class, Query.select().where("RELEASE = ?", release).order("EXECUTION_DATE DESC"))).size());
        } catch (ActiveObjectsException e) {
            return 0;
        }
    }

    public boolean delete(String execEntity) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntityI.class, "EXEC_ENTITY = ?", execEntity);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<ExecEntityI> getByInsatanceId(String instanceId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityI.class, "ID = ?", instanceId)));
            } catch (ActiveObjectsException e) {
                return new ArrayList<>();
            }
        });
    }

    public List<ExecEntityI> get(String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityI.class, Query.select().where("PROJECT_KEY = ?", projectKey).order("EXECUTION_DATE DESC"))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<Integer> getEntityOverallSummary(String projectKey, String execEntityInstanceId) {
        List<Integer> entityOverallSummaryList = new ArrayList<>();
        activeObjects.executeInTransaction(() -> {
            Query passCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.OVERALL_STATUS = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ?", "Pass", execEntityInstanceId, projectKey);

            int passCount = activeObjects.count(ExecEntityTPTCI.class, passCountQuery);
            if (passCount > 0) {
                entityOverallSummaryList.add(passCount);
                return passCount;
            } else {
                passCount = 0;
                entityOverallSummaryList.add(passCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query failedCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.OVERALL_STATUS = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ?", "Failed", execEntityInstanceId, projectKey);

            int failedCount = activeObjects.count(ExecEntityTPTCI.class, failedCountQuery);
            if (failedCount > 0) {
                entityOverallSummaryList.add(failedCount);
                return failedCount;
            } else {
                failedCount = 0;
                entityOverallSummaryList.add(failedCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query inProgressCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.OVERALL_STATUS = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ?", "In Progress", execEntityInstanceId, projectKey);

            int inProgressCount = activeObjects.count(ExecEntityTPTCI.class, inProgressCountQuery);
            if (inProgressCount > 0) {
                entityOverallSummaryList.add(inProgressCount);
                return inProgressCount;
            } else {
                inProgressCount = 0;
                entityOverallSummaryList.add(inProgressCount);
                return 0;
            }
        });
        return entityOverallSummaryList;
    }

    public List<Integer> getOverallSummary(String projectKey) {
        List<Integer> overallSummary = new ArrayList<>();

        activeObjects.executeInTransaction(() -> {
            Query passCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.OVERALL_STATUS = ? AND eEInstance.PROJECT_KEY = ?", "Pass", projectKey);

            int passCount = activeObjects.count(ExecEntityTPTCI.class, passCountQuery);
            if (passCount > 0) {
                overallSummary.add(passCount);
                return passCount;
            } else {
                passCount = 0;
                overallSummary.add(passCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query failCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.OVERALL_STATUS = ? AND eEInstance.PROJECT_KEY = ?", "Failed", projectKey);

            int failedCount = activeObjects.count(ExecEntityTPTCI.class, failCountQuery);
            if (failedCount > 0) {
                overallSummary.add(failedCount);
                return failedCount;
            } else {
                failedCount = 0;
                overallSummary.add(failedCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query inProgressCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.OVERALL_STATUS = ? AND eEInstance.PROJECT_KEY = ?", "In Progress", projectKey);

            int inProgressCount = activeObjects.count(ExecEntityTPTCI.class, inProgressCountQuery);
            if (inProgressCount > 0) {
                overallSummary.add(inProgressCount);
                return inProgressCount;
            } else {
                inProgressCount = 0;
                overallSummary.add(inProgressCount);
                return 0;
            }
        });
        return overallSummary;
    }

    public List<Integer> getEntityTestCaseCount(String execEntityInstanceId, String projectKey) {
        List<Integer> entityTestCaseCount = new ArrayList<>();

        activeObjects.executeInTransaction(() -> {
            Query passCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.TEST_CASE_AUTOMATED = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ?", "Yes", execEntityInstanceId, projectKey);

            int automatedCount = activeObjects.count(ExecEntityTPTCI.class, passCountQuery);
            if (automatedCount > 0) {
                entityTestCaseCount.add(automatedCount);
                return automatedCount;
            } else {
                automatedCount = 0;
                entityTestCaseCount.add(automatedCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query passCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.TEST_CASE_MANUAL = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ?", "Yes", execEntityInstanceId, projectKey);

            int manualCount = activeObjects.count(ExecEntityTPTCI.class, passCountQuery);
            if (manualCount > 0) {
                entityTestCaseCount.add(manualCount);
                return manualCount;
            } else {
                manualCount = 0;
                entityTestCaseCount.add(manualCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query entityCompletedManualPassCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.TEST_CASE_MANUAL = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ? AND eEInstance.STATUS = ?", "Yes", execEntityInstanceId, projectKey, "Completed");

            int entityCompletedManualPassCountCount = activeObjects.count(ExecEntityTPTCI.class, entityCompletedManualPassCountQuery);
            if (entityCompletedManualPassCountCount > 0) {
                entityTestCaseCount.add(entityCompletedManualPassCountCount);
                return entityCompletedManualPassCountCount;
            } else {
                entityCompletedManualPassCountCount = 0;
                entityTestCaseCount.add(entityCompletedManualPassCountCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query entityCompletedAutomatedPassCountCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.TEST_CASE_AUTOMATED = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ? AND eEInstance.STATUS = ?", "Yes", execEntityInstanceId, projectKey, "Completed");

            int entityCompletedAutomatedPassCount = activeObjects.count(ExecEntityTPTCI.class, entityCompletedAutomatedPassCountCountQuery);
            if (entityCompletedAutomatedPassCount > 0) {
                entityTestCaseCount.add(entityCompletedAutomatedPassCount);
                return entityCompletedAutomatedPassCount;
            } else {
                entityCompletedAutomatedPassCount = 0;
                entityTestCaseCount.add(entityCompletedAutomatedPassCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query entityInProgressManualPassCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.TEST_CASE_MANUAL = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ? AND eEInstance.STATUS = ?", "Yes", execEntityInstanceId, projectKey, "In Progress");

            int entityInProgressManualPassCount = activeObjects.count(ExecEntityTPTCI.class, entityInProgressManualPassCountQuery);
            if (entityInProgressManualPassCount > 0) {
                entityTestCaseCount.add(entityInProgressManualPassCount);
                return entityInProgressManualPassCount;
            } else {
                entityInProgressManualPassCount = 0;
                entityTestCaseCount.add(entityInProgressManualPassCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query entityInProgressAutomatedPassCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .alias(ExecEntityI.class, "eEInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .join(ExecEntityI.class, "eEInstance.ID = eEPlanInstance.ENTITY_ID")
                    .where("eEPTCInstance.TEST_CASE_AUTOMATED = ? AND eEInstance.ID = ? AND eEInstance.PROJECT_KEY = ? AND eEInstance.STATUS = ?", "Yes", execEntityInstanceId, projectKey, "In Progress");

            int entityInProgressAutomatedPassCount = activeObjects.count(ExecEntityTPTCI.class, entityInProgressAutomatedPassCountQuery);
            if (entityInProgressAutomatedPassCount > 0) {
                entityTestCaseCount.add(entityInProgressAutomatedPassCount);
                return entityInProgressAutomatedPassCount;
            } else {
                entityInProgressAutomatedPassCount = 0;
                entityTestCaseCount.add(entityInProgressAutomatedPassCount);
                return 0;
            }
        });

        return entityTestCaseCount;
    }

    public List<Integer> getPlanTestCaseCount(String execEntityPlanInstanceId) {
        List<Integer> planTestCaseCount = new ArrayList<>();

        activeObjects.executeInTransaction(() -> {
            Query passCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPTCInstance.TEST_CASE_AUTOMATED = ?  AND eEPlanInstance.ID = ? ", "Yes", execEntityPlanInstanceId);

            int automatedCount = activeObjects.count(ExecEntityTPTCI.class, passCountQuery);
            if (automatedCount > 0) {
                planTestCaseCount.add(automatedCount);
                return automatedCount;
            } else {
                automatedCount = 0;
                planTestCaseCount.add(automatedCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query passCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPTCInstance.TEST_CASE_MANUAL = ? AND eEPlanInstance.ID = ? ", "Yes", execEntityPlanInstanceId);

            int manualCount = activeObjects.count(ExecEntityTPTCI.class, passCountQuery);
            if (manualCount > 0) {
                planTestCaseCount.add(manualCount);
                return manualCount;
            } else {
                manualCount = 0;
                planTestCaseCount.add(manualCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query planCompletedTCAutomatedCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPTCInstance.TEST_CASE_AUTOMATED = ? AND eEPlanInstance.ID = ? AND eEPlanInstance.STATUS = ? ", "Yes", execEntityPlanInstanceId, "Completed");

            int planCompletedTCAutomatedCount = activeObjects.count(ExecEntityTPTCI.class, planCompletedTCAutomatedCountQuery);
            if (planCompletedTCAutomatedCount > 0) {
                planTestCaseCount.add(planCompletedTCAutomatedCount);
                return planCompletedTCAutomatedCount;
            } else {
                planCompletedTCAutomatedCount = 0;
                planTestCaseCount.add(planCompletedTCAutomatedCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query planCompletedTCManualCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPTCInstance.TEST_CASE_MANUAL = ? AND eEPlanInstance.ID = ? AND eEPlanInstance.STATUS = ? ", "Yes", execEntityPlanInstanceId, "Completed");

            int planCompletedTCManualCount = activeObjects.count(ExecEntityTPTCI.class, planCompletedTCManualCountQuery);
            if (planCompletedTCManualCount > 0) {
                planTestCaseCount.add(planCompletedTCManualCount);
                return planCompletedTCManualCount;
            } else {
                planCompletedTCManualCount = 0;
                planTestCaseCount.add(planCompletedTCManualCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query planInProgressTCAutomatedCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPTCInstance.TEST_CASE_AUTOMATED = ? AND eEPlanInstance.ID = ? AND eEPlanInstance.STATUS = ? ", "Yes", execEntityPlanInstanceId, "In Progress");

            int planInProgressTCAutomatedCount = activeObjects.count(ExecEntityTPTCI.class, planInProgressTCAutomatedCountQuery);
            if (planInProgressTCAutomatedCount > 0) {
                planTestCaseCount.add(planInProgressTCAutomatedCount);
                return planInProgressTCAutomatedCount;
            } else {
                planInProgressTCAutomatedCount = 0;
                planTestCaseCount.add(planInProgressTCAutomatedCount);
                return 0;
            }
        });

        activeObjects.executeInTransaction(() -> {
            Query planInProgressTCManualCountQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPTCInstance.TEST_CASE_MANUAL = ? AND eEPlanInstance.ID = ? AND eEPlanInstance.STATUS = ? ", "Yes", execEntityPlanInstanceId, "In Progress");

            int planInProgressTCManualCount = activeObjects.count(ExecEntityTPTCI.class, planInProgressTCManualCountQuery);
            if (planInProgressTCManualCount > 0) {
                planTestCaseCount.add(planInProgressTCManualCount);
                return planInProgressTCManualCount;
            } else {
                planInProgressTCManualCount = 0;
                planTestCaseCount.add(planInProgressTCManualCount);
                return 0;
            }
        });

        return planTestCaseCount;
    }
}
