package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.OscarVE;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OscarVEDBOperation {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public OscarVEDBOperation(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public OscarVE insert(String testCaseId, String param, String paramV) {
        return activeObjects.executeInTransaction(() -> {
            OscarVE oscarVE = activeObjects.create(OscarVE.class);
            oscarVE.setTestCaseId(testCaseId);
            oscarVE.setParam(param);
            oscarVE.setParamV(paramV);
          //  oscarVE.setParamVE(paramVE);
            oscarVE.save();
            return oscarVE;
        });
    }

    public List<OscarVE> get(String testCaseId, String param, String paramV) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(OscarVE.class, "TEST_CASE_ID = ? AND PARAM = ? AND PARAM_V = ?", testCaseId, param, paramV)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public List<OscarVE> get(String testCaseId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(OscarVE.class, "TEST_CASE_ID = ?", testCaseId)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(OscarVE.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
