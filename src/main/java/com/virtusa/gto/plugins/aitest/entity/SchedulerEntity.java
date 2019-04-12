package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface SchedulerEntity extends Entity {

    int getExecEntityInstanceId();

    void setExecEntityInstanceId(int eEntityInstanceId);

    String getSchedulerName();

    void setSchedulerName(String schedulerName);

    String getCronValue();

    void setCronValue(String cronValue);

    boolean getStatus();

    void setStatus(boolean status);

    String getTrigger();

    void setTrigger(String trigger);

    String getGroup();

    void setGroup(String group);

    String getJobKey();

    void setJobKey(String jobKey);

    String getJobDetail();

    void setJobDetail(String jobDetail);

    String getReleaseVersion();

    void setReleaseVersion(String releaseVersion);

    String getReleaseDescription();

    void setReleaseDescription(String releaseDescription);

}
