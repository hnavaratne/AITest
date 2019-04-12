package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.BusComFolder;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RPSPERERA on 9/21/2018
 */

public class BComponentFolder {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public BComponentFolder(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public BusComFolder insert(String projectKey, String parent, String folderName, String description) {
        return activeObjects.executeInTransaction(() -> {
            BusComFolder componentFolder = activeObjects.create(BusComFolder.class);
            componentFolder.setName(folderName);
            componentFolder.setDescription(description);
            componentFolder.setParent(parent);
            componentFolder.setProjectKey(projectKey);
            componentFolder.save();
            return componentFolder;
        });
    }

    public boolean update(String projectKey, String parent, String folderName, String newFolderName) {
        return activeObjects.executeInTransaction(() -> {
            try {
                BusComFolder[] find = activeObjects.find(BusComFolder.class, "PROJECT_KEY = ? and PARENT = ? and NAME = ? ", projectKey, parent, folderName);
                for (BusComFolder businessComponentFolder : find) {
                    businessComponentFolder.setName(newFolderName);
                    businessComponentFolder.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean delete(String projectKey, String id) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(BusComFolder.class, "PROJECT_KEY = ? and ID = ?", projectKey, id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<BusComFolder> get(String projectKey, String parent) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(BusComFolder.class, "PROJECT_KEY = ? and PARENT = ?", projectKey, parent)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<BusComFolder> getByProject(String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(BusComFolder.class, "PROJECT_KEY = ?", projectKey)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

}
