package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.EEntityTPTCDIStatus;
import com.virtusa.gto.plugins.aitest.entity.TCaseDataInstance;
import com.virtusa.gto.plugins.aitest.entity.TCaseDataIteration;
import net.java.ao.Query;

import javax.inject.Inject;

/**
 * @author RPSPERERA on 7/31/2018
 */
public class EEnityPlanTestCaseDataIteration {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEnityPlanTestCaseDataIteration(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TCaseDataIteration insertTestCaseDataIteration(TCaseDataInstance tCaseDataInstance, String data) {
        return activeObjects.executeInTransaction(() -> {
            TCaseDataIteration tCaseDataIteration = activeObjects.create(TCaseDataIteration.class);
            tCaseDataIteration.setTestCaseDataInstance(tCaseDataInstance);
            tCaseDataIteration.setIterationData(data);
            tCaseDataIteration.setStatus(EEntityTPTCDIStatus.IN_PROGRESS.value());
            tCaseDataIteration.save();
            return tCaseDataIteration;
        });
    }

    public TCaseDataIteration[] getTestCaseDataIterations(String ExecEntityTPTCI) {
        return activeObjects.executeInTransaction(() -> {
            TCaseDataInstance[] tCaseDataInstances = activeObjects.find(TCaseDataInstance.class, "EXEC_ENTITY_TPTCI = ? ", ExecEntityTPTCI);
            if (tCaseDataInstances.length > 0) {
                return tCaseDataInstances[0].getTestCaseDataIterations();
            } else {
                return null;
            }

        });
    }

    public int getInprogressIterationsCount(String tCaseDataInstance) {
        return activeObjects.executeInTransaction(() -> {
            Query query = Query.select().alias(TCaseDataInstance.class, "tCInstance").
                    alias(TCaseDataIteration.class, "tCIteration")
                    .join(TCaseDataInstance.class, "tCInstance.ID = tCIteration.TEST_CASE_DATA_INSTANCE_ID")
                    .where("tCIteration.STATUS = ? AND tCInstance.ID = ?", "In Progress", tCaseDataInstance);
            TCaseDataIteration[] tCaseDataInstances = activeObjects.find(TCaseDataIteration.class, query);
            if (tCaseDataInstances.length > 0) {
                return tCaseDataInstances.length;
            } else {
                return 0;
            }
        });
    }

    public int getFailedCount(String tCaseDataInstance) {
        return activeObjects.executeInTransaction(() -> {
            Query query = Query.select().alias(TCaseDataInstance.class, "tCInstance").
                    alias(TCaseDataIteration.class, "tCIteration")
                    .join(TCaseDataInstance.class, "tCInstance.ID = tCIteration.TEST_CASE_DATA_INSTANCE_ID")
                    .where("tCIteration.STATUS = ? AND tCInstance.ID = ?", "Failed", tCaseDataInstance);
            TCaseDataIteration[] tCaseDataInstances = activeObjects.find(TCaseDataIteration.class, query);
            if (tCaseDataInstances.length > 0) {
                return tCaseDataInstances.length;
            } else {
                return 0;
            }
        });
    }

    public TCaseDataIteration getTestCaseDataIteration(String iterationId) {
        return activeObjects.executeInTransaction(() -> {
            TCaseDataIteration[] tCaseDataInstances = activeObjects.find(TCaseDataIteration.class, "ID = ? ", iterationId);
            if (tCaseDataInstances.length > 0) {
                return tCaseDataInstances[0];
            } else {
                return null;
            }

        });
    }

    public TCaseDataIteration updateTestCaseIteration(String tptcDataIteationId, String actualResult, String actualStatus) {
        return activeObjects.executeInTransaction(() -> {
            TCaseDataIteration[] tCaseDataInstances = activeObjects.find(TCaseDataIteration.class, "ID = ? ", tptcDataIteationId);
            if (tCaseDataInstances.length > 0) {
                tCaseDataInstances[0].setActualResult(actualResult);
                tCaseDataInstances[0].setStatus(actualStatus);
                tCaseDataInstances[0].save();
                return tCaseDataInstances[0];
            } else {
                return null;
            }

        });
    }

}
