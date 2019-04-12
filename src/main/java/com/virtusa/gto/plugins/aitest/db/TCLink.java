package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.TestCaseLink;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;

/**
 * @author RPSPERERA on 7/2/2018
 */
@Scanned
public class TCLink {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TCLink(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public TestCaseLink insert(String issueID, String name, String type) {
        return activeObjects.executeInTransaction(() -> {
            TestCaseLink testCaseLink = activeObjects.create(TestCaseLink.class);
            testCaseLink.setIssueID(issueID);
            testCaseLink.setName(name);
            testCaseLink.setType(type);
            testCaseLink.save();
            return testCaseLink;
        });
    }

    public boolean update(String issueID, String name, String type) {
        return activeObjects.executeInTransaction(() -> {
            try {
                TestCaseLink[] testCaseLinks = activeObjects.find(TestCaseLink.class, "TESTCASE = ? and NAME = ? and TYPE = ? ", issueID, name, type);
                for (TestCaseLink testCaseLink : testCaseLinks) {
                    testCaseLink.setIssueID(issueID);
                    testCaseLink.setName(name);
                    testCaseLink.setType(type);
                    testCaseLink.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean delete(Issue testCase, String name, String type) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(TestCaseLink.class, "TESTCASE = ? and NAME = ? and TYPE = ? ", testCase, name, type);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
