package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTP;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EEntityPlan {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntityPlan(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public ExecEntityTP insert(int entityId, String planId, String planName, String projectKey) {
        return activeObjects.executeInTransaction(() -> {
            ExecEntityTP execEntityTP = activeObjects.create(ExecEntityTP.class);
            execEntityTP.setEntityId(entityId);
            execEntityTP.setTestPlanId(planId);
            execEntityTP.setTestPlanName(planName);
            execEntityTP.setProjectKey(projectKey);
            execEntityTP.save();
            return execEntityTP;
        });
    }

    public boolean update(String id, int entityId, String planId, String planName) {
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntityTP[] execEntityTPS = activeObjects.find(ExecEntityTP.class, "ID = ?", id);
                for (ExecEntityTP execEntityTP : execEntityTPS) {
                    execEntityTP.setEntityId(entityId);
                    execEntityTP.setTestPlanId(planId);
                    execEntityTP.setTestPlanName(planName);
                    execEntityTP.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<ExecEntityTP> get(int entityId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTP.class, "ENTITY_ID = ?", entityId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String id) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntityTP.class, "ENTITY_ID = ?", id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
