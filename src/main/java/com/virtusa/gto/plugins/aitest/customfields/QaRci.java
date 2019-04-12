package com.virtusa.gto.plugins.aitest.customfields;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.RCI;
import com.virtusa.gto.plugins.aitest.entity.RequirementClarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scanned
public class QaRci extends AbstractSingleFieldType<Integer> {
    private static final Logger log = LoggerFactory.getLogger(QaRci.class);

    @JiraImport
    private ActiveObjects activeObjects;
    private String user;

    public QaRci(
            @JiraImport CustomFieldValuePersister customFieldValuePersister,
            @JiraImport GenericConfigManager genericConfigManager, ActiveObjects activeObjects) {

        super(customFieldValuePersister, genericConfigManager);
        this.activeObjects = activeObjects;
    }

    @Override
    public String getStringFromSingularObject(final Integer singularObject) {
        if (singularObject == null)
            return "0";
        else
            return singularObject.toString();
    }

    @Override
    public Integer getSingularObjectFromString(final String string) throws FieldValidationException {
        if (string == null)
            return 0;
        try {
            if (string.contains("value")) {
                JsonElement element = new JsonParser().parse(string);
                JsonObject asJsonObject = element.getAsJsonObject();
                setUser(asJsonObject.get("user").getAsString());
                return Integer.parseInt(asJsonObject.get("value").getAsString());
            } else {
                return Integer.parseInt(string);
            }

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
        Integer valueFromIssue = super.getValueFromIssue(field, issue);
        return valueFromIssue == null ? Integer.valueOf(0) : valueFromIssue;
    }

    @Override
    public void updateValue(CustomField customField, Issue issue, Integer value) {
        //  JsonElement element = new JsonParser().parse(value);
        RCI rci = new RCI(activeObjects);
        RequirementClarity clarity = rci.upsert(String.valueOf(issue.getId()), String.valueOf(value));
        super.updateValue(customField, issue, value);
        update(issue, clarity.getRating());
    }

    @Override
    public void createValue(CustomField field, Issue issue, Integer value) {
        RCI rci = new RCI(activeObjects);
        RequirementClarity clarity = rci.upsert(String.valueOf(issue.getId()), String.valueOf(value));
        super.createValue(field, issue, value);
        update(issue, clarity.getRating());
    }

    private void update(Issue issue, String value) {
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        ApplicationUser userByName = ComponentAccessor.getUserManager().getUserByName(getUser());
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.addCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("Team Average").iterator().next().getId(), value);

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(userByName, issue.getId(), issueInputParameters);

        if (updateValidationResult.isValid()) {
            IssueService.IssueResult updateResult = issueService.update(userByName, updateValidationResult);
            if (!updateResult.isValid()) {
                log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
            }
        } else {
            log.error(updateValidationResult.getErrorCollection().getErrorMessages().toString());
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}