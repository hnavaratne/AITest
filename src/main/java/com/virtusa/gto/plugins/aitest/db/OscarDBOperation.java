package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.Oscar;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OscarDBOperation {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public OscarDBOperation(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public Oscar insert(String testCaseId, String mindMap) {
        return activeObjects.executeInTransaction(() -> {
            Oscar oscar = activeObjects.create(Oscar.class);
            oscar.setTestCaseId(testCaseId);
            oscar.setMindMap(mindMap);
            oscar.save();
            return oscar;
        });
    }

    public List<Oscar> get(String testCaseId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(Oscar.class, "TEST_CASE_ID = ?", testCaseId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean update(String testCaseId, String mindMap) {
        return activeObjects.executeInTransaction(() -> {
            try {
                Oscar[] oscars = activeObjects.find(Oscar.class, "TEST_CASE_ID = ?", testCaseId);
                for (Oscar oscar : oscars) {
                    oscar.setMindMap(mindMap);
                    oscar.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean delete(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(Oscar.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
