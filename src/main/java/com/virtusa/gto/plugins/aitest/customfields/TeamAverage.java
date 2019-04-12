package com.virtusa.gto.plugins.aitest.customfields;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.db.RCI;

/**
 * @author RPSPERERA on 6/19/2018
 */
@Scanned
public class TeamAverage extends AbstractSingleFieldType<Integer> {

    @JiraImport
    private ActiveObjects activeObjects;

    private int value;

    protected TeamAverage(@JiraImport CustomFieldValuePersister customFieldValuePersister, @JiraImport GenericConfigManager genericConfigManager, ActiveObjects activeObjects) {
        super(customFieldValuePersister, genericConfigManager);
        this.activeObjects = activeObjects;
    }

    @Override
    public String getStringFromSingularObject(final Integer singularObject) {
        return String.valueOf(getValue());
    }

    @Override
    public Integer getSingularObjectFromString(final String string) throws FieldValidationException {
        if (string == null) {

            return getValue();
        }
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ex) {
            throw new FieldValidationException("Not a valid number.");
        }
    }

    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Override
    protected Integer getObjectFromDbValue(final Object databaseValue)
            throws FieldValidationException {
        return getSingularObjectFromString((String) databaseValue);
    }

    @Override
    protected Object getDbValueFromObject(final Integer customFieldObject) {
        return getStringFromSingularObject(customFieldObject);
    }

    @Override
    public Integer getValueFromIssue(CustomField field, Issue issue) {
        RCI rci = new RCI(activeObjects);
        String average = rci.getAverage(String.valueOf(issue.getId()));
        setValue(Integer.parseInt(average));
        return Integer.parseInt(average);
    }

    @Override
    public void updateValue(CustomField customField, Issue issue, Integer value) {
        super.updateValue(customField, issue, getValue());
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
