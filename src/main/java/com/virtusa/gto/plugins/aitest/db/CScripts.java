package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.Scripts;
import net.java.ao.ActiveObjectsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author RPSPERERA on 10/5/2018
 */
public class CScripts {

    @JiraImport
    private final ActiveObjects activeObjects;

    private static final Logger logger = LoggerFactory.getLogger(CScripts.class);

    public CScripts(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public Scripts get(String script_id) {
        return activeObjects.executeInTransaction(() -> {
            Scripts[] serverStatuses = activeObjects.find(Scripts.class, "SCRIPT = ? ", script_id);
            if (serverStatuses.length > 0) {
                return serverStatuses[0];
            }
            return null;
        });
    }

    public void remove(String testCaseId, String scriptId) {
        activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(Scripts.class, "SCRIPT = ? AND TEST_CASE_ID = ? ", scriptId, testCaseId);
            } catch (ActiveObjectsException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        });
    }

    public Scripts insert(String project, String testCaseId, String script_id) {
        return activeObjects.executeInTransaction(() -> {
            Scripts execEntity = activeObjects.create(Scripts.class);
            execEntity.setAttached(true);
            execEntity.setProject(project);
            execEntity.setScript(script_id);
            execEntity.setTestCaseId(testCaseId);
            execEntity.save();
            return execEntity;
        });
    }
}
