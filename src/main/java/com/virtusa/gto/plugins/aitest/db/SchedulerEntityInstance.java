package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.SchedulerEntity;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulerEntityInstance {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public SchedulerEntityInstance(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public SchedulerEntity insert(int eEntityInstanceId, String schedulerName, String cronValue) {
        return activeObjects.executeInTransaction(() -> {
            SchedulerEntity schedulerEntity = activeObjects.create(SchedulerEntity.class);
            schedulerEntity.setExecEntityInstanceId(eEntityInstanceId);
            schedulerEntity.setSchedulerName(schedulerName);
            schedulerEntity.setCronValue(cronValue);
            schedulerEntity.save();
            return schedulerEntity;
        });
    }

    public boolean insert(int eEntityInstanceId, boolean status, String triggerName, String jobDetail, String jobKey, String group) {
        try {
            SchedulerEntity[] schedulerEntities = activeObjects.find(SchedulerEntity.class, "EXEC_ENTITY_INSTANCE_ID = ?", eEntityInstanceId);
            for (SchedulerEntity schedulerEntity : schedulerEntities) {
                schedulerEntity.setStatus(status);
                schedulerEntity.setTrigger(triggerName);
                schedulerEntity.setJobKey(jobKey);
                schedulerEntity.setJobDetail(jobDetail);
                schedulerEntity.setGroup(group);
                schedulerEntity.save();
            }
        } catch (ActiveObjectsException e) {
            return false;
        }
        return true;
    }

    public List<SchedulerEntity> get(int eEntityInstanceId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(SchedulerEntity.class, Query.select().where("EXEC_ENTITY_INSTANCE_ID = ?", eEntityInstanceId))));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean delete(int eEntityInstanceId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(SchedulerEntity.class, "EXEC_ENTITY_INSTANCE_ID = ?", eEntityInstanceId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }


    public boolean deleteSchedule(String execEntityInstanceId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(SchedulerEntity.class, "EXEC_ENTITY_INSTANCE_ID = ?", execEntityInstanceId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public List<SchedulerEntity> getAll() {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(SchedulerEntity.class)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public boolean update(int eEntityInstanceId, String schedulerName, String cronValue, String release, String description) {
        return activeObjects.executeInTransaction(() -> {
            try {
                SchedulerEntity[] schedulerEntities = activeObjects.find(SchedulerEntity.class, "EXEC_ENTITY_INSTANCE_ID = ?", eEntityInstanceId);
                for (SchedulerEntity schedulerEntity : schedulerEntities) {
                    schedulerEntity.setSchedulerName(schedulerName);
                    schedulerEntity.setCronValue(cronValue);
                    schedulerEntity.setReleaseDescription(description);
                    schedulerEntity.setReleaseVersion(release);
                    schedulerEntity.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean updateStatus(int eEntityInstanceId, boolean status) {
        return activeObjects.executeInTransaction(() -> {
            try {
                SchedulerEntity[] schedulerEntities = activeObjects.find(SchedulerEntity.class, "EXEC_ENTITY_INSTANCE_ID = ?", eEntityInstanceId);
                for (SchedulerEntity schedulerEntity : schedulerEntities) {
                    schedulerEntity.setStatus(status);
                    schedulerEntity.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
