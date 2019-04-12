package com.virtusa.gto.plugins.aitest.customfields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AIRCI extends AbstractSingleFieldType<Object> {
    private static final Logger log = LoggerFactory.getLogger(AIRCI.class);

    public AIRCI(
            @JiraImport CustomFieldValuePersister customFieldValuePersister,
            @JiraImport GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
    }

    @Override
    public String getStringFromSingularObject(final Object singularObject) {
        if (singularObject == null)
            return null;
        else if (singularObject instanceof JsonObject) {
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
        return jsonElement.getAsJsonObject();
    }

    @Override
    protected Object getDbValueFromObject(final Object customFieldObject) {
        return getSingularObjectFromString((String) customFieldObject);
    }

    @Override
    public void updateValue(CustomField customField, Issue issue, Object value) {
        String stringFromSingularObject = getStringFromSingularObject(value);
        if (Objects.nonNull(value)) {
            super.updateValue(customField, issue, stringFromSingularObject);
        }

    }
}