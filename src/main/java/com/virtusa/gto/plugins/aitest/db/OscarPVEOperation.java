package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.OscarPVE;
import com.virtusa.gto.plugins.aitest.entity.OscarVE;
import net.java.ao.ActiveObjectsException;

public class OscarPVEOperation {
    @JiraImport
    private final ActiveObjects activeObjects;

    public OscarPVEOperation(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public OscarPVE insert(String testCaseId, String param, String paramV, String paramVE) {
        return activeObjects.executeInTransaction(() -> {
            OscarPVE oscarPVE = activeObjects.create(OscarPVE.class);
            OscarVEDBOperation oscarVEDBOperation = new OscarVEDBOperation(activeObjects);
            OscarVE oscarVE = oscarVEDBOperation.get(testCaseId, param, paramV).get(0);
            oscarPVE.setTestCaseId(testCaseId);
            oscarPVE.setValue(paramVE);
            oscarPVE.setOscarVE(oscarVE);
            oscarPVE.save();
            return oscarPVE;
        });
    }

    public boolean delete(String testCaseId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(OscarPVE.class, "TEST_CASE_ID = ?", testCaseId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }


}
