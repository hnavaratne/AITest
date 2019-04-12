package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.event.ComponentManagerShutdownEvent;
import com.atlassian.jira.event.ProjectCreatedEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.icon.IconType;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.option.OptionSet;
import com.atlassian.jira.issue.fields.option.OptionSetManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.virtusa.gto.plugins.aitest.db.ServerState;
import com.virtusa.gto.plugins.aitest.db.TConfig;
import com.virtusa.gto.plugins.aitest.entity.Configuration;
import org.apache.commons.text.StringEscapeUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.virtusa.gto.plugins.aitest.util.CustomFieldCreator.*;

@Component
@Scanned
@Named("AiTestOperationLauncher")
@ExportAsService(LifecycleAware.class)
public class EventManager implements LifecycleAware, InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    @JiraImport
    private final EventPublisher eventPublisher;
    @JiraImport
    private final CustomFieldManager customFieldManager;
    @JiraImport
    private final ActiveObjects activeObjects;

    private static boolean startupInitialized = false;

    enum LifecycleEvent {
        AFTER_PROPERTIES_SET, PLUGIN_ENABLED, LIFECYCLE_AWARE_ON_START
    }

    @GuardedBy("this")
    private final Set<LifecycleEvent> lifecycleEvents = EnumSet.noneOf(LifecycleEvent.class);

    @Inject
    public EventManager(EventPublisher eventPublisher, CustomFieldManager customFieldManager, ActiveObjects activeObjects) {
        this.eventPublisher = eventPublisher;
        this.customFieldManager = customFieldManager;
        this.activeObjects = activeObjects;
    }

    /**
     * Called when the plugin has been enabled.
     */
    @Override
    public void afterPropertiesSet() {
        log.info("Enabling plugin");
        eventPublisher.register(this);
        //   createCustomIssueTypes("Requirement", "This is Requirement Issue Type.");
        createCustomIssueTypes("This is TestCase Issue Type.");
        associateCustomFieldTypes();
        onLifecycleEvent(LifecycleEvent.AFTER_PROPERTIES_SET);
        eventPublisher.register(this);
    }

    @Override
    public void onStart() {
        log.info("plugin starting");
        onLifecycleEvent(LifecycleEvent.LIFECYCLE_AWARE_ON_START);
        //initMLServerStart();
    }

    private void initMLServerStart() {
        try {
            Scheduler schedulerInstance = SchedulerInstance.getInstance().getSchedulerInstance();
            schedulerInstance.start();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
        TConfig tConfig = new TConfig(activeObjects);
        Configuration configurationObj = tConfig.get();
        if (Objects.nonNull(configurationObj)) {
          //  if (Objects.nonNull(configurationObj.getPythonHome()) && (!configurationObj.getPythonHome().isEmpty()) && (!configurationObj.getPythonHome().equals(""))) {
                try {
                    ServerState serverState = new ServerState(activeObjects);
                    boolean serverStatus = serverState.getServerStatus();
                    Configuration configuration = tConfig.get();
                    final long startedTime = 0;
                    tConfig.setTime(startedTime);
                    if (!serverStatus) {
                        Configuration configObj = tConfig.getTime();
                        ScheduledFuture<String> schedule = Executor.executorService.schedule(() -> {
                            Executor.executorService.execute(() -> {
                                Worker worker = new Worker(activeObjects, configuration.getPythonHome());
                                worker.run();
                            });
                            serverState.updateServerStatus(true);
                            return null;
                        }, configObj.getStartedTime() + 15, TimeUnit.SECONDS);
                        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownAndAwaitTermination));
                        schedule.get();
                    } else {
                        MLServerManager mlServerManager = new MLServerManager();
                        mlServerManager.shutdownFlaskServer();
                        serverState.updateServerStatus(false);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage(), e);
                }

        }
    }

    @Override
    public void onStop() {
        log.info("plugin stopping");
        shutdownAndAwaitTermination();
    }

    /**
     * Called when the plugin is being disabled or removed.
     */
    @Override
    public void destroy() {
        log.info("Disabling plugin");
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();

        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
            if (Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ROOT).equals("story")) {
                addAIRCI(issue.getId(), issue.getCreator(), issue);
            }
            log.info("Issue {} has been created at {}.", issue.getKey(), issue.getCreated());
        } else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID)) {
            log.info("Issue {} has been resolved at {}.", issue.getKey(), issue.getResolutionDate());
        } else if (eventTypeId.equals(EventType.ISSUE_CLOSED_ID)) {
            log.info("Issue {} has been closed at {}.", issue.getKey(), issue.getUpdated());
        } else if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID) && Objects.requireNonNull(issue.getIssueType()).getName().toLowerCase(Locale.ROOT).equals("story")) {
            addAIRCI(issue.getId(), issue.getCreator(), issue);
        }
    }

    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        Plugin plugin = event.getPlugin();
        if ("com.virtusa.gto.plugins.aitest".equals(plugin.getKey())) {
            onLifecycleEvent(LifecycleEvent.PLUGIN_ENABLED);
            createCustomFieldTypes();
        }
    }

    @EventListener
    public void onProjectCreation(ProjectCreatedEvent projectCreatedEvent) {
        associateMissingCustomFieldsOnProjectCreation(projectCreatedEvent.getProject());
    }

    @EventListener
    public void onJiraShutdown(ComponentManagerShutdownEvent jiraStoppedEvent) {
        shutdownAndAwaitTermination();
    }

    @EventListener
    public void onPluginDisable(PluginDisabledEvent pluginDisabledEvent) {
        Plugin plugin = pluginDisabledEvent.getPlugin();
        if ("com.virtusa.gto.plugins.aitest".equals(plugin.getKey())) {
            shutdownAndAwaitTermination();
            // eventPublisher.unregister(this);
        }
    }

    private void shutdownAndAwaitTermination() {
        Scheduler schedulerInstance = SchedulerInstance.getInstance().getSchedulerInstance();
        try {
            schedulerInstance.shutdown();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
        MLServerManager mlServerManager = new MLServerManager();
        ServerState serverState = new ServerState(activeObjects);
        boolean shutdownFlaskServer = mlServerManager.shutdownFlaskServer();
        if (shutdownFlaskServer) {
            serverState.updateServerStatus(false);
        }
        Executor.executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!Executor.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                List<Runnable> runnables = Executor.executorService.shutdownNow();// Cancel currently executing tasks
                runnables.forEach(Runnable::run);
                // Wait a while for tasks to respond to being cancelled
                if (!Executor.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            Executor.executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void onLifecycleEvent(LifecycleEvent event) {
        log.info("onLifecycleEvent: " + event);
        if (isLifecycleReady(event) && !startupInitialized) {
            log.info("Got the last lifecycle event... Time to get started!");
            // unregisterListener();
            try {
                startupInitialized = true;
                initMLServerStart();
            } catch (Exception ex) {
                log.error("Unexpected error during launch", ex);
            }
        }
    }

    private void unregisterListener() {
        log.info("unregisterListeners");
        eventPublisher.unregister(this);
    }

    synchronized private boolean isLifecycleReady(LifecycleEvent event) {
        return lifecycleEvents.add(event) && lifecycleEvents.size() == LifecycleEvent.values().length;
    }

    /**
     * Creates Custom issue type and Associate to all available schemes.
     */
    private void createCustomIssueTypes(String description) {
        try {
            ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
            IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
            Collection<IssueType> allIssueTypeObjects = constantsManager.getAllIssueTypeObjects();
            boolean[] hasIssueType = {false};
            allIssueTypeObjects.forEach(issueTypeF -> {
                if (issueTypeF.getName().toLowerCase(Locale.ROOT).trim().replace("\\s", "").equals("TestCase".toLowerCase(Locale.ROOT).trim().replace("\\s", ""))) {
                    hasIssueType[0] = true;
                }
            });
            if (!Objects.equals(hasIssueType[0], true)) {
                Long defaultAvatarId = ComponentAccessor.getAvatarManager().getDefaultAvatarId(IconType.ISSUE_TYPE_ICON_TYPE);
                IssueType insertIssueType = constantsManager.insertIssueType("TestCase", Long.valueOf(allIssueTypeObjects.size() + 1), null, description, defaultAvatarId);
                final OptionSetManager optionSetManager = ComponentAccessor.getComponent(OptionSetManager.class);
                issueTypeSchemeManager.getAllSchemes().forEach(fieldConfigScheme -> {
                    FieldConfig oneAndOnlyConfig = fieldConfigScheme.getOneAndOnlyConfig();
                    OptionSet options = optionSetManager.getOptionsForConfig(Objects.requireNonNull(oneAndOnlyConfig));
                    options.addOption(IssueFieldConstants.ISSUE_TYPE, insertIssueType.getId());
                    issueTypeSchemeManager.update(fieldConfigScheme, options.getOptionIds());

                });
            }

        } catch (Exception e) {
            log.error("Unable to create custom issue type" + e.getMessage(), e);
        }
    }

    private void associateCustomFieldTypes() {
        try {
            ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
            //Requirement Issue type used when associating Custom fields with issue types.
            List<IssueType> testCaseTypeList = new ArrayList<>();
            List<IssueType> taskTypeList = new ArrayList<>();
            List<IssueType> bugTypeList = new ArrayList<>();

            constantsManager.getAllIssueTypeObjects().forEach(issueType -> {
                if (Objects.equals(issueType.getName(), "TestCase")) {
                    testCaseTypeList.add(issueType);
                } else if (Objects.equals(issueType.getName(), "Task")) {
                    taskTypeList.add(issueType);
                } else if (Objects.equals(issueType.getName(), "Bug")) {
                    bugTypeList.add(issueType);
                }

            });

            List<CustomFieldType<?, ?>> customFieldTypes = customFieldManager.getCustomFieldTypes();
            Map<String, CustomFieldType> customFields = createCustomFields(customFieldTypes);
            CustomFieldType radioButton = customFields.get("radioButton");
            CustomFieldType textField = customFields.get("textField");
            CustomFieldType readOnlyTextField = customFields.get("readOnlyTextField");
            CustomFieldType singleSelectDropDown = customFields.get("singleSelectDropDown");
            CustomFieldType singleUserSelect = customFields.get("singleUserSelect");
            CustomFieldType datePicker = customFields.get("datePicker");

            List<JiraContextNode> jiraContextNodes = new ArrayList<>();
            jiraContextNodes.add(GlobalIssueContext.getInstance());
            //CustomField createCustomField(String fieldName,String description,CustomFieldType fieldType,CustomFieldSearcher customFieldSearcher,List<JiraContextNode> contexts,List<IssueType> issueTypes)
            //create custom fields related to TestCase issue type.
            createTestCaseRelatedCustomFields1(radioButton, jiraContextNodes, textField, testCaseTypeList, customFieldManager);
            createTestCaseRelatedCustomFields2(readOnlyTextField, jiraContextNodes, textField, testCaseTypeList, customFieldManager);

            if (bugTypeList.size() > 0) {
                //create custom fields related to Bug issue type.
                createBugRelatedCustomFields1(singleSelectDropDown, jiraContextNodes, bugTypeList, customFieldManager);
                createBugRelatedCustomFields2(singleUserSelect, jiraContextNodes, bugTypeList, customFieldManager);
            }
            //create custom fields related to Task issue type.
            createTaskRelatedCustomFields(datePicker, jiraContextNodes, taskTypeList, singleSelectDropDown, customFieldManager);


            // FieldConfig oneAndOnlyConfig = qaRCICustomField.getConfigurationSchemes().iterator().next().getOneAndOnlyConfig();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createCustomFieldTypes() {

        try {
            ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
            //Requirement Issue type used when associating Custom fields with issue types.
            List<IssueType> allIssueTypeList = new ArrayList<>();
            List<IssueType> testCaseTypeList = new ArrayList<>();
            List<IssueType> taskBugTypeList = new ArrayList<>();

            constantsManager.getAllIssueTypeObjects().forEach(issueType -> {
                if (Objects.equals(issueType.getName(), "Story")) {
                    allIssueTypeList.add(issueType);
                } else if (Objects.equals(issueType.getName(), "TestCase")) {
                    testCaseTypeList.add(issueType);
                } else if (Objects.equals(issueType.getName(), "Task")) {
                    taskBugTypeList.add(issueType);
                } else if (Objects.equals(issueType.getName(), "Bug")) {
                    taskBugTypeList.add(issueType);
                }
            });
            if (testCaseTypeList.size() > 0) {
                List<CustomFieldType<?, ?>> customFieldTypes = customFieldManager.getCustomFieldTypes();
                Map<String, CustomFieldType> customFields = getCustomFields(customFieldTypes);
                CustomFieldType qaRCI = customFields.get("qaRCI");
                CustomFieldType teamAverage = customFields.get("teamAverage");
                CustomFieldType scripts = customFields.get("scripts");
                CustomFieldType rangeSlide = customFields.get("rangeSlide");
                CustomFieldType aiRCI = customFields.get("aiRCI");

                List<JiraContextNode> jiraContextNodes = new ArrayList<>();
                jiraContextNodes.add(GlobalIssueContext.getInstance());

                //CustomField createCustomField(String fieldName,String description,CustomFieldType fieldType,CustomFieldSearcher customFieldSearcher,List<JiraContextNode> contexts,List<IssueType> issueTypes)
                handleCustomFieldCreation(allIssueTypeList, testCaseTypeList, taskBugTypeList, jiraContextNodes, qaRCI, teamAverage, scripts, rangeSlide, aiRCI, customFieldManager);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    private void addAIRCI(Long id, ApplicationUser applicationUser, Issue issue) {
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        HttpURLConnection conn = null;
        try {
            log.info("content : ", StringEscapeUtils.escapeJava(issue.getDescription()));
            JSONObject data = new JSONObject();
            data.put("key", "requirement");
            data.put("content", StringEscapeUtils.escapeJava(issue.getDescription()));

            URL url = new URL("http://localhost:9154/api/v1/RQmetrics");
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(data.toString().getBytes(Charset.forName("UTF-8")));
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode() + "\\n" + conn.getResponseMessage());
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream()), StandardCharsets.UTF_8))) {

                log.info("Output from Server .... \n");
                StringBuilder stringBuilder = new StringBuilder();
                br.lines().forEach(stringBuilder::append);
                os.close();
                conn.disconnect();
                //JsonObject parsedData;
                if (stringBuilder.length() > 0) {
//            parsedData = new JsonParser().parse(stringBuilder.toString()).getAsJsonObject();
                    issueInputParameters
                            .addCustomFieldValue(customFieldManager.getCustomFieldObjectsByName("AI RCI").iterator().next().getId(), stringBuilder.toString());
                    IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(applicationUser, id, issueInputParameters);

                    if (updateValidationResult.isValid()) {
                        IssueService.IssueResult updateResult = issueService.update(applicationUser, updateValidationResult, EventDispatchOption.DO_NOT_DISPATCH, false);
                        if (!updateResult.isValid()) {
                            log.error(updateResult.getErrorCollection().getFlushedErrorMessages().toString());
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            log.error(e.getMessage(), e);
        } finally {
            closeConnection(conn);
        }
    }

    private void closeConnection(HttpURLConnection conn) {
        if (!(conn == null)) {
            conn.disconnect();
        }
    }
}
