package com.virtusa.gto.plugins.aitest.customfields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Scanned
public class TestExecutionEffort extends AbstractSingleFieldType<Integer> {
    private static final Logger log = LoggerFactory.getLogger(TestExecutionEffort.class);

    public TestExecutionEffort(@JiraImport CustomFieldValuePersister customFieldValuePersister,@JiraImport GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
    }

    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Override
    protected Object getDbValueFromObject(Integer tExecutionEffortCustomFieldObject) {
        return getStringFromSingularObject(tExecutionEffortCustomFieldObject);
    }

    @Override
    protected Integer getObjectFromDbValue(final Object tExecutionEffortDatabaseValue) throws FieldValidationException {
        return getSingularObjectFromString((String) tExecutionEffortDatabaseValue);
    }

    @Override
    public String getStringFromSingularObject(final Integer tExecutionEffortSingularObject) {
        if (!Objects.nonNull(tExecutionEffortSingularObject))
            return null;
        else
            return tExecutionEffortSingularObject.toString();
    }

    @Override
    public Integer getSingularObjectFromString(String tExecutionEffortString) throws FieldValidationException {

        try {
            if (!Objects.nonNull(tExecutionEffortString)) {
                return null;
            } else {
                return Integer.parseInt(tExecutionEffortString);
            }
        } catch (NumberFormatException ex) {
            log.error(ex.getMessage(), ex);
            throw new FieldValidationException("Not a valid number.");
        }
    }

    @Override
    public void createValue(CustomField tExecutionEffortField, Issue tExecutionEffortIssue, Integer tExecutionEffortValue) {
        super.createValue(tExecutionEffortField, tExecutionEffortIssue, tExecutionEffortValue);
    }

    @Override
    public void updateValue(CustomField tExecutionEffortCustomField, Issue tExecutionEffortIssue, Integer tExecutionEffortValue) {
        super.updateValue(tExecutionEffortCustomField, tExecutionEffortIssue, tExecutionEffortValue);
    }

    @Override
    protected Integer getValueFromIssue(CustomField tExecutionEffortField, Long tExecutionEffortIssueId, String tExecutionEffortIssueKey) {
        return super.getValueFromIssue(tExecutionEffortField, tExecutionEffortIssueId, tExecutionEffortIssueKey);
    }
}