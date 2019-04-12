package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestCaseFolder;
import com.virtusa.gto.plugins.aitest.entity.TestLinkEntity;
import com.virtusa.gto.plugins.aitest.entity.TestSteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

public class TLinkOperation {

    @JiraImport
    private final ActiveObjects activeObjects;

    private static final Logger log = LoggerFactory.getLogger(TLinkOperation.class);

    @Inject
    public TLinkOperation(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public String saveTestCaseFolder(TestLinkEntity.Folder folder) {
        TCFolder tcFolder = new TCFolder(activeObjects);
        JSONObject returnObject = new JSONObject();
        try {
            if (Objects.nonNull(folder)) {
                TestCaseFolder insert = tcFolder.insert(folder.getProjectKey(), folder.getFolderId(), folder.getFolderName(), folder.getDescription());
                if (Objects.nonNull(insert)) {
                    returnObject.put("saveSuccess", true);
                    returnObject.put("parent", insert.getID());
                } else {
                    returnObject.put("saveSuccess", false);
                    returnObject.put("parent", insert.getID());
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return returnObject.toString();
    }


    public boolean saveTestSteps(String projectKey, String testCaseId, List<TestLinkEntity.Folder.TestCase.TestSteps.Step> stepList) {
        TSteps tSteps = new TSteps(activeObjects);
        boolean saveSucess = false;
        for (TestLinkEntity.Folder.TestCase.TestSteps.Step step : stepList) {
            TestSteps insert = tSteps.insert(projectKey, testCaseId, step.getStepDescription(), step.getExpectedResult(), null, null);
            if (Objects.nonNull(insert)) {
                saveSucess = true;
            }
        }
        return saveSucess;
    }
}
