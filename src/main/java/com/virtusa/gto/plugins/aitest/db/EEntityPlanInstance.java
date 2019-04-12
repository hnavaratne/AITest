package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.EEntityTPStatus;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPI;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EEntityPlanInstance {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntityPlanInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public ExecEntityTPI insert(int execEntityTP, int entityId, String planId, String planName, String projectKey) {
        return activeObjects.executeInTransaction(() -> {
            ExecEntityTPI execEntityTPI = activeObjects.create(ExecEntityTPI.class);
            execEntityTPI.setExecEntityTP(execEntityTP);
            execEntityTPI.setEntityId(entityId);
            execEntityTPI.setTestPlanId(planId);
            execEntityTPI.setTestPlanName(planName);
            execEntityTPI.setStatus(EEntityTPStatus.IN_PROGRESS.value());
            execEntityTPI.setProjectKey(projectKey);
            execEntityTPI.save();
            return execEntityTPI;
        });
    }

    public List<ExecEntityTPI> get(int execEntityTP, String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPI.class, "ENTITY_ID = ? and PROJECT_KEY = ?", execEntityTP, projectKey)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> get(int execEntityPlanInstanceId) {
        try {
            Query userStoryQuery = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .alias(ExecEntityTPI.class, "eEPlanInstance")
                    .join(ExecEntityTPI.class, "eEPlanInstance.ID = eEPTCInstance.EXECUTION_ENTITY_TEST_PLAN_ID")
                    .where("eEPlanInstance.ID = ? ", execEntityPlanInstanceId);

            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, userStoryQuery)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String execEntityTP) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntityTPI.class, "ENTITY_ID = ?", execEntityTP);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }


    public List<ExecEntityTPI> getEntityTp(int entityId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPI.class, "ENTITY_ID = ?", entityId)));
            } catch (ActiveObjectsException e) {
                return new ArrayList<>();
            }
        });
    }

    public List<ExecEntityTPI> getEEntityTPI(String testPlanInstance) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPI.class, "ID = ?", testPlanInstance)));
            } catch (ActiveObjectsException e) {
                return new ArrayList<>();
            }
        });
    }

    public List<ExecEntityTPI> getEEntityTPIByEntityInstance(String entityTnstance) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPI.class, "ENTITY_ID = ?", entityTnstance)));
            } catch (ActiveObjectsException e) {
                return new ArrayList<>();
            }
        });
    }
}
