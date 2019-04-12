package com.virtusa.gto.plugins.aitest.entity;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class TestLinkEntity {

    @JiraImport
    private final ActiveObjects activeObjects;

    private static final Logger log = LoggerFactory.getLogger(TestLinkEntity.class);

    @Inject
    public TestLinkEntity(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    private List<Folder> folderList;

    public void setFolder(List<Folder> folderList) {
        this.folderList = folderList;
    }

    public List<Folder> getFolder() {
        return folderList;
    }

    public static class Folder {

        private String folderId;

        private String folderName;

        private String description;

        private List<TestCase> testCaseList;

        private String projectKey;

        public List<TestCase> getTestCaseList() {
            return testCaseList;
        }

        public void setTestCaseList(List<TestCase> testCaseList) {
            this.testCaseList = testCaseList;
        }

        public String getFolderId() {
            return folderId;
        }

        public void setFolderId(String folderId) {
            this.folderId = folderId;
        }

        public String getFolderName() {
            return folderName;
        }

        public void setFolderName(String folderName) {
            this.folderName = folderName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public void setProjectKey(String projectKey) {
            this.projectKey = projectKey;
        }

        @Override
        public String toString() {
            return "Folder{" +
                    "folderId='" + folderId + '\'' +
                    ", folderName='" + folderName + '\'' +
                    ", description='" + description + '\'' +
                    ", testCaseList=" + testCaseList +
                    '}';
        }

        public static class TestCase {

            private String parent;

            private String project;

            private String name;

            private String user;

            private String description;

            private String execution_type;


            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getExecution_type() {
                return execution_type;
            }

            public void setExecution_type(String execution_type) {
                this.execution_type = execution_type;
            }

            public String getProject() {
                return project;
            }

            public void setProject(String project) {
                this.project = project;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getParent() {
                return parent;
            }

            public void setParent(String parent) {
                this.parent = parent;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return "TestCase{" +
                        "description='" + description + '\'' +
                        ", execution_type='" + execution_type + '\'' +
                        '}';
            }

            public static class TestSteps {

                private List<Step> stepList;

                public List<Step> getStepList() {
                    return stepList;
                }

                public void setStepList(List<Step> stepList) {
                    this.stepList = stepList;
                }

                public static class Step {

                    private String stepDescription;

                    private String expectedResult;

                    public String getStepDescription() {
                        return stepDescription;
                    }

                    public void setStepDescription(String stepDescription) {
                        this.stepDescription = stepDescription;
                    }

                    public String getExpectedResult() {
                        return expectedResult;
                    }

                    public void setExpectedResult(String expectedResult) {
                        this.expectedResult = expectedResult;
                    }

                    @Override
                    public String toString() {
                        return "Step{" +
                                "stepDescription='" + stepDescription + '\'' +
                                ", expectedResult='" + expectedResult + '\'' +
                                '}';
                    }
                }
            }

        }

    }

}
