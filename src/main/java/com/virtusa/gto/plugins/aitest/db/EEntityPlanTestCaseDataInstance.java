package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TCaseDataInstance;

import javax.inject.Inject;

/**
 * @author RPSPERERA on 7/31/2018
 */
public class EEntityPlanTestCaseDataInstance {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public EEntityPlanTestCaseDataInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TCaseDataInstance insert(int execEntityTPTCI) {
        return activeObjects.executeInTransaction(() -> {
            TCaseDataInstance tCaseDataInstance = activeObjects.create(TCaseDataInstance.class);
            tCaseDataInstance.setExecEntityTPTCI(execEntityTPTCI);
            tCaseDataInstance.save();
            return tCaseDataInstance;
        });
    }
}
