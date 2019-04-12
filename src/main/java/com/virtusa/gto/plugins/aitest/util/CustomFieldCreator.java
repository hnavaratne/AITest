package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.option.OptionSet;
import com.atlassian.jira.issue.fields.option.OptionSetManager;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author RPSPERERA on 12/12/2018
 */
public class CustomFieldCreator {

    private static final Logger log = LoggerFactory.getLogger(CustomFieldCreator.class);

    public static Map<String, CustomFieldType> createCustomFields(List<CustomFieldType<?, ?>> customFieldTypes) {
        Map<String, CustomFieldType> fieldTypeMap = new HashMap<>();
        for (CustomFieldType<?, ?> customFieldType : customFieldTypes) {
            switch (customFieldType.getName()) {
                case "Text Field (single line)":
                    fieldTypeMap.put("textField", customFieldType);
                    break;
                case "Text Field (read only)":
                    fieldTypeMap.put("readOnlyTextField", customFieldType);
                    break;
                case "Radio Buttons":
                    fieldTypeMap.put("radioButton", customFieldType);
                    break;
                case "Select List (single choice)":
                    fieldTypeMap.put("singleSelectDropDown", customFieldType);
                    break;
                case "User Picker (single user)":
                    fieldTypeMap.put("singleUserSelect", customFieldType);
                    break;
                case "Date Picker":
                    fieldTypeMap.put("datePicker", customFieldType);
                    break;
                default:
                    break;
            }
            log.info(customFieldType.getName());
        }
        return fieldTypeMap;
    }

    public static void createTestCaseRelatedCustomFields1(CustomFieldType radioButton, List<JiraContextNode> jiraContextNodes, CustomFieldType textField, List<IssueType> testCaseTypeList, CustomFieldManager customFieldManager) {
        try {
            int automatedSize = customFieldManager.getCustomFieldObjectsByName("Automated").size();
            if (!(automatedSize > 0)) {
                CustomField automated = customFieldManager.createCustomField("Automated", "The Automated Custom Field", radioButton, null, jiraContextNodes, testCaseTypeList);
                addOptions(automated);
                associateCustomFieldsWithScreens(automated);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Automated").iterator().next());
            }
            int manualSize = customFieldManager.getCustomFieldObjectsByName("Manual").size();
            if (!(manualSize > 0)) {
                CustomField manual = customFieldManager.createCustomField("Manual", "The Manual Custom Field", radioButton, null, jiraContextNodes, testCaseTypeList);
                addOptions(manual);
                associateCustomFieldsWithScreens(manual);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Manual").iterator().next());
            }
            int OverallExpectedResultSize = customFieldManager.getCustomFieldObjectsByName("OverallExpectedResult").size();
            if (!(OverallExpectedResultSize > 0)) {
                CustomField customFieldOverallExpectedResult = customFieldManager.createCustomField("OverallExpectedResult", "The Overall Result Custom Field", textField, null, jiraContextNodes, testCaseTypeList);
                associateCustomFieldsWithScreens(customFieldOverallExpectedResult);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("OverallExpectedResult").iterator().next());
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    public static void createTestCaseRelatedCustomFields2(CustomFieldType readOnlyTextField, List<JiraContextNode> jiraContextNodes, CustomFieldType textField, List<IssueType> testCaseTypeList, CustomFieldManager customFieldManager) {
        try {
            int overallStatusSize = customFieldManager.getCustomFieldObjectsByName("OverallStatus").size();
            if (!(overallStatusSize > 0)) {
                CustomField customFieldOverallStatus = customFieldManager.createCustomField("OverallStatus", "The Overall Result  Custom Field", textField, null, jiraContextNodes, testCaseTypeList);
                associateCustomFieldsWithScreens(customFieldOverallStatus);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("OverallStatus").iterator().next());
            }
            int parentFolderSize = customFieldManager.getCustomFieldObjectsByName("ParentFolder").size();
            if (!(parentFolderSize > 0)) {
                CustomField customFieldParentFolder = customFieldManager.createCustomField("ParentFolder", "The Parent Folder Custom Field", readOnlyTextField, null, jiraContextNodes, testCaseTypeList);
                associateCustomFieldsWithScreens(customFieldParentFolder);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("ParentFolder").iterator().next());
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    public static void createTaskRelatedCustomFields(CustomFieldType datePicker, List<JiraContextNode> jiraContextNodes, List<IssueType> taskTypeList, CustomFieldType singleSelectDropDown, CustomFieldManager customFieldManager) {
        try {
            int taskCategorySize = customFieldManager.getCustomFieldObjectsByName("Category").size();
            if (!(taskCategorySize > 0)) {
                CustomField customFieldTaskCategory = customFieldManager.createCustomField("Category", "The Category Custom Field", singleSelectDropDown, null, jiraContextNodes, taskTypeList);
                addOptionForCategory(customFieldTaskCategory);
                associateCustomFieldsWithScreens(customFieldTaskCategory);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Category").iterator().next());
            }

            int startDateSize = customFieldManager.getCustomFieldObjectsByName("Start Date").size();
            if (!(startDateSize > 0)) {
                CustomField customFieldStartDate = customFieldManager.createCustomField("Start Date", "The Start Date", datePicker, null, jiraContextNodes, taskTypeList);
                associateCustomFieldsWithScreens(customFieldStartDate);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Start Date").iterator().next());
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public static void createBugRelatedCustomFields1(CustomFieldType singleSelectDropDown, List<JiraContextNode> jiraContextNodes, List<IssueType> bugTypeList, CustomFieldManager customFieldManager) {
        try {
            int severitySize = customFieldManager.getCustomFieldObjectsByName("Severity").size();
            if (!(severitySize > 0)) {
                CustomField customFieldBugSeverity = customFieldManager.createCustomField("Severity", "The Severity Custom Field", singleSelectDropDown, null, jiraContextNodes, bugTypeList);
                addOptionsForSeverity(customFieldBugSeverity);
                associateCustomFieldsWithScreens(customFieldBugSeverity);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Severity").iterator().next());
            }
            int issueTypeSize = customFieldManager.getCustomFieldObjectsByName("Issue Type").size();
            if (!(issueTypeSize > 0)) {
                CustomField customFieldBugIssueType = customFieldManager.createCustomField("Issue Type", "The Issue Type Custom Field", singleSelectDropDown, null, jiraContextNodes, bugTypeList);
                addOptionForIssueType(customFieldBugIssueType);
                associateCustomFieldsWithScreens(customFieldBugIssueType);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Issue Type").iterator().next());
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void createBugRelatedCustomFields2(CustomFieldType singleUserSelect, List<JiraContextNode> jiraContextNodes, List<IssueType> bugTypeList, CustomFieldManager customFieldManager) {
        try {
            int injectedBySize = customFieldManager.getCustomFieldObjectsByName("Injected By").size();
            if (!(injectedBySize > 0)) {
                CustomField customFieldInjectedBy = customFieldManager.createCustomField("Injected By", "The Injected User Custom Field", singleUserSelect, null, jiraContextNodes, bugTypeList);
                associateCustomFieldsWithScreens(customFieldInjectedBy);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Injected By").iterator().next());
            }
            int fixedBySize = customFieldManager.getCustomFieldObjectsByName("Fixed By").size();
            if (!(fixedBySize > 0)) {
                CustomField customFieldFixedBy = customFieldManager.createCustomField("Fixed By", "The Fixed User Custom Field", singleUserSelect, null, jiraContextNodes, bugTypeList);
                associateCustomFieldsWithScreens(customFieldFixedBy);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Fixed By").iterator().next());
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void addOptions(CustomField customField) {
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        if (customField != null) {

            FieldConfig oneAndOnlyConfig = customField.getConfigurationSchemes().iterator().next().getOneAndOnlyConfig();

            //Option createOption(FieldConfig fieldConfig,Long parentOptionId,Long sequence,String value)
            int size = optionsManager.getOptions(oneAndOnlyConfig).size();
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 1), "Yes");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 2), "No");
        }
    }

    private static void addOptionForCategory(CustomField customField) {
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        if (customField != null) {

            FieldConfig oneAndOnlyConfig = customField.getConfigurationSchemes().iterator().next().getOneAndOnlyConfig();

            //Option createOption(FieldConfig fieldConfig,Long parentOptionId,Long sequence,String value)
            int size = optionsManager.getOptions(oneAndOnlyConfig).size();
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 1), "Project Management");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 2), "Requirements");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 3), "Analysis and Design");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 4), "Implementation");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 5), "Testing");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 6), "Deployment");
        }

    }

    private static void addOptionsForSeverity(CustomField customField) {
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        if (customField != null) {

            FieldConfig oneAndOnlyConfig = customField.getConfigurationSchemes().iterator().next().getOneAndOnlyConfig();

            //Option createOption(FieldConfig fieldConfig,Long parentOptionId,Long sequence,String value)
            int size = optionsManager.getOptions(oneAndOnlyConfig).size();
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 1), "Show Stopper");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 2), "High");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 3), "Medium");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 4), "Low");
        }
    }

    private static void addOptionForIssueType(CustomField customField) {
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        if (customField != null) {
            FieldConfig oneAndOnlyConfig = customField.getConfigurationSchemes().iterator().next().getOneAndOnlyConfig();
            int size = optionsManager.getOptions(oneAndOnlyConfig).size();
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 1), "Bug");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 2), "Enhancement");
            optionsManager.createOption(oneAndOnlyConfig, null, (long) (size + 3), "Clarification");
        }
    }

    private static void associateCustomFieldsWithScreens(CustomField customField) {
        Collection<FieldScreen> fieldScreens = ComponentAccessor.getFieldScreenManager().getFieldScreens();

        fieldScreens.forEach(fieldScreen -> {
            FieldScreenTab firstTab = fieldScreen.getTab(0);
            if (!fieldScreen.containsField(customField.getId())) {
                int currentPosition = getCurrentPosition(firstTab, customField.getName());
                if (currentPosition != 0) {
                    firstTab.addFieldScreenLayoutItem(customField.getId(), currentPosition);
                } else {
                    firstTab.addFieldScreenLayoutItem(customField.getId());
                }
            }
            if (!fieldScreen.containsField("fixVersions")) {
                firstTab.addFieldScreenLayoutItem("fixVersions");
            }
            if (!fieldScreen.containsField("components")) {
                firstTab.addFieldScreenLayoutItem("components");
            }
        });
    }

    private static int getCurrentPosition(FieldScreenTab fieldScreenTab, String name) {

        if (Objects.equals(name, "Start Date") && fieldScreenTab.isContainsField("Due Date")) {
            FieldScreenLayoutItem due_date = fieldScreenTab.getFieldScreenLayoutItem("Due Date");
            return due_date.getPosition() - 1;
        } else if (Objects.equals(name, "Severity") && fieldScreenTab.isContainsField("Priority")) {
            FieldScreenLayoutItem due_date = fieldScreenTab.getFieldScreenLayoutItem("Priority");
            return due_date.getPosition() + 1;
        } else if (Objects.equals(name, "Issue Type") && fieldScreenTab.isContainsField("Priority")) {
            FieldScreenLayoutItem due_date = fieldScreenTab.getFieldScreenLayoutItem("Priority");
            return due_date.getPosition() + 2;
        } else if (Objects.equals(name, "Injected By") && fieldScreenTab.isContainsField("Assignee")) {
            FieldScreenLayoutItem due_date = fieldScreenTab.getFieldScreenLayoutItem("Assignee");
            return due_date.getPosition() + 1;
        } else if (Objects.equals(name, "Fixed By") && fieldScreenTab.isContainsField("Assignee")) {
            FieldScreenLayoutItem due_date = fieldScreenTab.getFieldScreenLayoutItem("Assignee");
            return due_date.getPosition() + 2;
        }

        return 0;
    }

    public static void associateMissingCustomFieldsOnProjectCreation(Project project) {
        IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
        FieldConfigScheme configScheme = issueTypeSchemeManager.getConfigScheme(project);
        final OptionSetManager optionSetManager = ComponentAccessor.getComponent(OptionSetManager.class);
        Collection<IssueType> allIssueTypeObjects = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
        List<IssueType> cloneIssueType = new ArrayList<>(allIssueTypeObjects);
        Collection<IssueType> associatedIssueTypes = issueTypeSchemeManager.getIssueTypesForProject(project);
        for (IssueType associatedIssueType : associatedIssueTypes) {
            cloneIssueType.remove(associatedIssueType);
        }
        cloneIssueType.forEach(issueType -> {
            FieldConfig oneAndOnlyConfig = configScheme.getOneAndOnlyConfig();
            OptionSet options = optionSetManager.getOptionsForConfig(oneAndOnlyConfig);
            options.addOption(IssueFieldConstants.ISSUE_TYPE, issueType.getId());
            issueTypeSchemeManager.update(configScheme, options.getOptionIds());
        });
        ComponentAccessor.getCustomFieldManager().getCustomFieldObjects().forEach(CustomFieldCreator::associateCustomFieldsWithScreens);
    }


    public static Map<String, CustomFieldType> getCustomFields(List<CustomFieldType<?, ?>> customFieldTypes) {
        Map<String, CustomFieldType> fieldTypeMap = new HashMap<>();
        for (CustomFieldType<?, ?> customFieldType : customFieldTypes) {
            if (Objects.equals(customFieldType.getName(), "Qa Rci")) {
                fieldTypeMap.put("qaRCI", customFieldType);
            } else if (Objects.equals(customFieldType.getName(), "Team Average")) {
                fieldTypeMap.put("teamAverage", customFieldType);
            } else if (Objects.equals(customFieldType.getName(), "Scripts")) {
                fieldTypeMap.put("scripts", customFieldType);
            } else if (Objects.equals(customFieldType.getName(), "Rangeslide")) {
                fieldTypeMap.put("rangeSlide", customFieldType);
            } else if (Objects.equals(customFieldType.getName(), "AIRCI")) {
                fieldTypeMap.put("aiRCI", customFieldType);
            }
            log.info(customFieldType.getName());
        }
        return fieldTypeMap;
    }

    public static void handleCustomFieldCreation(List<IssueType> allIssueTypeList, List<IssueType> testCaseTypeList, List<IssueType> taskBugTypeList, List<JiraContextNode> jiraContextNodes, CustomFieldType qaRCI, CustomFieldType teamAverage, CustomFieldType scripts, CustomFieldType rangeSlide, CustomFieldType aiRCI, CustomFieldManager customFieldManager) {
        try {
            int qARCISize = customFieldManager.getCustomFieldObjectsByName("QA RCI").size();
            if (!(qARCISize > 0)) {
                CustomField customFieldQARCI = customFieldManager.createCustomField("QA RCI", "This QA RCI Custom Field", qaRCI, null, jiraContextNodes, allIssueTypeList);
                associateCustomFieldsWithScreens(customFieldQARCI);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("QA RCI").iterator().next());
            }

            int teamAverageSize = customFieldManager.getCustomFieldObjectsByName("Team Average").size();
            if (!(teamAverageSize > 0)) {
                CustomField customFieldTeamAverage = customFieldManager.createCustomField("Team Average", "This Team Average Custom Field", teamAverage, null, jiraContextNodes, allIssueTypeList);
                associateCustomFieldsWithScreens(customFieldTeamAverage);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Team Average").iterator().next());
            }

            int scriptsSize = customFieldManager.getCustomFieldObjectsByName("Scripts").size();
            if (!(scriptsSize > 0)) {
                CustomField customFieldScripts = customFieldManager.createCustomField("Scripts", "The Attached Script CustomField", scripts, null, jiraContextNodes, testCaseTypeList);
                associateCustomFieldsWithScreens(customFieldScripts);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Scripts").iterator().next());
            }
            handleCustomRemainingFields(allIssueTypeList, jiraContextNodes, taskBugTypeList, rangeSlide, aiRCI, customFieldManager);
        } catch (RuntimeException e1) {
            log.error(e1.getMessage(), e1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void handleCustomRemainingFields(List<IssueType> allIssueTypeList, List<JiraContextNode> jiraContextNodes, List<IssueType> taskBugTypeList, CustomFieldType rangeSlide, CustomFieldType aiRCI, CustomFieldManager customFieldManager) {
        try {
            int progressSize = customFieldManager.getCustomFieldObjectsByName("Progress").size();
            if (!(progressSize > 0)) {
                CustomField customFieldScripts = customFieldManager.createCustomField("Progress", "The Progress CustomField", rangeSlide, null, jiraContextNodes, taskBugTypeList);
                associateCustomFieldsWithScreens(customFieldScripts);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("Progress").iterator().next());
            }
            int aiRCISize = customFieldManager.getCustomFieldObjectsByName("AI RCI").size();
            if (!(aiRCISize > 0)) {
                CustomField customFieldAiRCI = customFieldManager.createCustomField("AI RCI", "The AI RCI CustomField", aiRCI, null, jiraContextNodes, allIssueTypeList);
                associateCustomFieldsWithScreens(customFieldAiRCI);
            } else {
                associateCustomFieldsWithScreens(customFieldManager.getCustomFieldObjectsByName("AI RCI").iterator().next());
            }
        } catch (RuntimeException e1) {
            log.error(e1.getMessage(), e1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
