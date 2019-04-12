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
public class TestDesignEffort extends AbstractSingleFieldType<Integer> {

    private static final Logger log = LoggerFactory.getLogger(TestDesignEffort.class);

    public TestDesignEffort(@JiraImport CustomFieldValuePersister customFieldValuePersister, @JiraImport GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
    }

    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Override
    protected Object getDbValueFromObject(Integer tDesignEffortDatabaseValueCustomFieldObject) {
        return getStringFromSingularObject(tDesignEffortDatabaseValueCustomFieldObject);
    }

    @Override
    protected Integer getObjectFromDbValue(final Object tDesignEffortDatabaseValue) throws FieldValidationException {
        return getSingularObjectFromString((String) tDesignEffortDatabaseValue);
    }

    @Override
    public String getStringFromSingularObject(final Integer tDesignEffortObject) {
        if (!Objects.nonNull(tDesignEffortObject))
            return null;
        else
            return tDesignEffortObject.toString();
    }

    @Override
    public Integer getSingularObjectFromString(String tDesignEffortString) throws FieldValidationException {

        try {
            if (!Objects.nonNull(tDesignEffortString)) {
                return null;
            } else {
                return Integer.parseInt(tDesignEffortString);
            }
        } catch (NumberFormatException ex) {
            log.error(ex.getMessage(), ex);
            throw new FieldValidationException("Not a valid number.");
        }
    }

    @Override
    public void createValue(CustomField tDesignEffortField, Issue tDesignEffortIssue, Integer tDesignEffortValue) {
        super.createValue(tDesignEffortField, tDesignEffortIssue, tDesignEffortValue);
    }

    @Override
    public void updateValue(CustomField tDesignEffortField, Issue tDesignEffortIssue, Integer tDesignEffortValue) {
        super.updateValue(tDesignEffortField, tDesignEffortIssue, tDesignEffortValue);
    }

    @Override
    protected Integer getValueFromIssue(CustomField tDesignEffortField, Long tDesignEffortIssueId, String tDesignEffortIssueKey) {
        return super.getValueFromIssue(tDesignEffortField, tDesignEffortIssueId, tDesignEffortIssueKey);
    }

}