package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.ExecEntity;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RPSPERERA on 7/6/2018
 */
public class EEntity {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntity(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public ExecEntity insert(String projectKey, String release, String description) {
        return activeObjects.executeInTransaction(() -> {
            ExecEntity execEntity = activeObjects.create(ExecEntity.class);
            execEntity.setProjectKey(projectKey);
            execEntity.setRelease(release);
            execEntity.setDescription(description);
            execEntity.save();
            return execEntity;
        });
    }

    public List<ExecEntity> get(String id) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntity.class, "ID = ?", id)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean update(String id, String release, String description) {
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntity[] executionEntities = activeObjects.find(ExecEntity.class, "ID = ?", id);
                for (ExecEntity execEntity : executionEntities) {
                    execEntity.setRelease(release);
                    execEntity.setDescription(description);
                    execEntity.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean delete(String id) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntity.class, "ID = ?", id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

}
