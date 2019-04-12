package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.BDTEntity;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;

public class BDTInstance {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public BDTInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public BDTEntity insert(String testCaseId, String description) {
        return activeObjects.executeInTransaction(() -> {
            BDTEntity bdtEntity = activeObjects.create(BDTEntity.class);
            bdtEntity.setTestCaseId(testCaseId);
            bdtEntity.setDescription(description);
            bdtEntity.save();
            return bdtEntity;
        });
    }

    public BDTEntity[] get(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.find(BDTEntity.class, "TEST_CASE_ID = ? ", testCaseId);
            } catch (ActiveObjectsException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public void delete(String testCaseId) {
        activeObjects.executeInTransaction(() -> {
            activeObjects.deleteWithSQL(BDTEntity.class, "TEST_CASE_ID = ?", testCaseId);
            return null;
        });
    }

}
