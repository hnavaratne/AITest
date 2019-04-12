package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.ExecEntityTPTCI;
import com.virtusa.gto.plugins.aitest.entity.UserStoryEntityI;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserStoryEntityInstance {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public UserStoryEntityInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public UserStoryEntityI insert(String userStoryId, String projectKey, String user, String userStorySummary) {
        return activeObjects.executeInTransaction(() -> {
            UserStoryEntityI userStoryEntityI = activeObjects.create(UserStoryEntityI.class);
            userStoryEntityI.setUserStoryId(userStoryId);
            userStoryEntityI.setProjectKey(projectKey);
            userStoryEntityI.setUser(user);
            userStoryEntityI.setUserStorySummary(userStorySummary);
            userStoryEntityI.save();
            return userStoryEntityI;
        });
    }

    public List<UserStoryEntityI> getUserStories(String projectKey, String user) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(UserStoryEntityI.class, "PROJECT_KEY = ? AND USER = ?", projectKey, user)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }


    public List<ExecEntityTPTCI> getTestCases(String userStoryId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, Query.select("TEST_CASE_ID").distinct().where("USER_STORY_ID = ?", userStoryId))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean deleteTestCase(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(ExecEntityTPTCI.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean updateTestCase(String testCaseId, String name) {
        return activeObjects.executeInTransaction(() -> {
            try {
                ExecEntityTPTCI[] find = activeObjects.find(ExecEntityTPTCI.class, "TEST_CASE_ID = ?", testCaseId);
                for (ExecEntityTPTCI testCase : find) {
                    testCase.setTestCaseName(name);
                    testCase.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<ExecEntityTPTCI> getTestCaseDefects(String testCaseId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, Query.select("DEFECT_ID").distinct().where("TEST_CASE_ID = ? AND DEFECT_ID !='null'", testCaseId))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<ExecEntityTPTCI> getTestCaseDefectList(String execEntityTPTCInstanceId) {
        try {
            Query defectTestCaseList = Query.select()
                    .alias(ExecEntityTPTCI.class, "eEPTCInstance")
                    .where("eEPTCInstance.ID = ?  AND eEPTCInstance.DEFECT_ID != 'NULL'", execEntityTPTCInstanceId);

            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(ExecEntityTPTCI.class, defectTestCaseList)));

        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }
}
