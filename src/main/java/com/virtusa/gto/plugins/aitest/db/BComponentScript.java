package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.BusComScript;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RPSPERERA on 9/21/2018
 */

public class BComponentScript {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public BComponentScript(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public BusComScript insert(String projectKey, String parent, String functionName) {
        return activeObjects.executeInTransaction(() -> {
            BusComScript componentFolder = activeObjects.create(BusComScript.class);
            componentFolder.setName(functionName);
            componentFolder.setParent(parent);
            componentFolder.setProjectKey(projectKey);
            componentFolder.save();
            return componentFolder;
        });
    }

    public boolean update(String scriptId, String oldScriptName, String newScriptName) {
        return activeObjects.executeInTransaction(() -> {
            try {
                BusComScript[] scripts = activeObjects.find(BusComScript.class, "ID = ? and NAME = ? ", scriptId, oldScriptName);
                for (BusComScript script : scripts) {
                    script.setName(newScriptName);
                    script.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public BusComScript get(String projectKey, String id) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(BusComScript.class, "PROJECT_KEY = ? and ID = ?", projectKey, id))).get(0);
        } catch (ActiveObjectsException e) {
            return null;
        }
    }

    public List<BusComScript> getAll(String projectKey, String parent) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(BusComScript.class, "PROJECT_KEY = ? and PARENT = ?", projectKey, parent)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String projectKey, String id) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(BusComScript.class, "PROJECT_KEY = ? and ID = ?", projectKey, id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<BusComScript> get(String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(BusComScript.class, "PROJECT_KEY = ?", projectKey)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

}
