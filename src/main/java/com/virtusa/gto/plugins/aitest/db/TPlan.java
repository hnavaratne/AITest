package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestPlan;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Scanned
public class TPlan {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TPlan(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestPlan insert(String name, String project) {
        return activeObjects.executeInTransaction(() -> {
            TestPlan testPlan = activeObjects.create(TestPlan.class);
            testPlan.setName(name);
            testPlan.setProjectKey(project);
            testPlan.save();
            return testPlan;
        });
    }

    public boolean update(String project, String id, String name) {
        return activeObjects.executeInTransaction(() -> {
            try {
                List<TestPlan> find = Arrays.asList(activeObjects.find(TestPlan.class, "PROJECT_KEY = ? and ID = ?", project, id));
                for (TestPlan testPlan : find) {
                    testPlan.setName(name);
                    testPlan.save();
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
                activeObjects.deleteWithSQL(TestPlan.class, "PROJECT_KEY = ? and ID = ?", projectKey, id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<TestPlan> get(String projectKey) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestPlan.class, "PROJECT_KEY = ?", projectKey)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<TestPlan> getById(String projectKey, String id) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(TestPlan.class, "PROJECT_KEY = ? and ID = ?", projectKey, id)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }
}
