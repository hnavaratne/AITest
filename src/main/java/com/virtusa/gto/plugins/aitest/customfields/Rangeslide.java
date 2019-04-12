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
public class Rangeslide extends AbstractSingleFieldType<Integer> {
    private static final Logger log = LoggerFactory.getLogger(Rangeslide.class);

    public Rangeslide(
            @JiraImport CustomFieldValuePersister customFieldValuePersister,
            @JiraImport GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
    }

    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Override
    protected Object getDbValueFromObject(Integer customFieldObject) {
        return getStringFromSingularObject(customFieldObject);
    }

    @Override
    protected Integer getObjectFromDbValue(final Object databaseValue) throws FieldValidationException {
        return getSingularObjectFromString((String) databaseValue);
    }

    @Override
    public String getStringFromSingularObject(final Integer singularObject) {
        if (!Objects.nonNull(singularObject))
            return null;
        else
            return singularObject.toString();
    }

    @Override
    public Integer getSingularObjectFromString(String string) throws FieldValidationException {

        try {
            if (!Objects.nonNull(string)) {
                return null;
            } else {
                return Integer.parseInt(string);
            }
        } catch (NumberFormatException ex) {
            log.error(ex.getMessage());
            throw new FieldValidationException("Not a valid number.");
        }
    }


    @Override
    public void createValue(CustomField field, Issue issue, Integer value) {
        super.createValue(field, issue, value);
    }

    @Override
    public void updateValue(CustomField customField, Issue issue, Integer value) {
        super.updateValue(customField, issue, value);
    }

    @Override
    protected Integer getValueFromIssue(CustomField field, Long issueId, String issueKey) {
        return super.getValueFromIssue(field, issueId, issueKey);
    }

}