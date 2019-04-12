package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestCaseFolder;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author RPSPERERA on 7/2/2018
 */
@Scanned
public class TCFolder {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TCFolder(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestCaseFolder insert(String projectKey, String parent, String folderName, String description) {
        return activeObjects.executeInTransaction(() -> {
            TestCaseFolder testCaseFolder = activeObjects.create(TestCaseFolder.class);
            testCaseFolder.setName(folderName);
            testCaseFolder.setDescription(description);
            testCaseFolder.setParent(parent);
            testCaseFolder.setProjectKey(projectKey);
            testCaseFolder.save();
            return testCaseFolder;
        });
    }

    public boolean update(String projectKey, String parent, String folderName, String newFolderName) {
        return activeObjects.executeInTransaction(() -> {
            try {
                TestCaseFolder[] find = activeObjects.find(TestCaseFolder.class, "PROJECT_KEY = ? and PARENT = ? and NAME = ? ", projectKey, parent, folderName);
                for (TestCaseFolder testCaseFolder : find) {
                    testCaseFolder.setName(newFolderName);
                    testCaseFolder.save();
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
                activeObjects.deleteWithSQL(TestCaseFolder.class, "PROJECT_KEY = ? and ID = ?", projectKey, id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<TestCaseFolder> get(String projectKey, String parent) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestCaseFolder.class, "PROJECT_KEY = ? and PARENT = ?", projectKey, parent)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<TestCaseFolder> get(String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestCaseFolder.class, "PROJECT_KEY = ?", projectKey)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }
}
