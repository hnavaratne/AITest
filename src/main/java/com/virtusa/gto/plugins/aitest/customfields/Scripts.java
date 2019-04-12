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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.Objects;

@Scanned
public class Scripts extends AbstractSingleFieldType<Object> {

    public Scripts(
            @JiraImport CustomFieldValuePersister customFieldValuePersister,
            @JiraImport GenericConfigManager genericConfigManager, ActiveObjects activeObjects) {

        super(customFieldValuePersister, genericConfigManager);
    }

    @Override
    public String getStringFromSingularObject(final Object singularObject) {
        if (singularObject == null)
            return null;
        else if (singularObject instanceof JsonArray) {
            return singularObject.toString();
        }
        return (String) singularObject;
    }

    @Override
    public Object getSingularObjectFromString(final String string) throws FieldValidationException {
        if (string == null)
            return null;
        try {
            return string;
        } catch (NumberFormatException ex) {
            throw new FieldValidationException("Not a valid number.");
        }
    }

    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Override
    protected Object getObjectFromDbValue(final Object databaseValue)
            throws FieldValidationException {
        JsonElement jsonElement = new JsonParser().parse((String) databaseValue);
        return jsonElement.getAsJsonArray();
    }

    @Override
    protected Object getDbValueFromObject(final Object customFieldObject) {
        return getSingularObjectFromString((String) customFieldObject);
    }

    @Override
    public void updateValue(CustomField customField, Issue issue, Object value) {
        String stringFromSingularObject = getStringFromSingularObject(value);
        if(Objects.nonNull(value)) {
            super.updateValue(customField, issue, stringFromSingularObject);
        }

    }
}