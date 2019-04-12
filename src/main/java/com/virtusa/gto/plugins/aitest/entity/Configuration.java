package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface Configuration extends Entity {

    void setPort(String port);

    String getPort();

    void setHost(String host);

    String getHost();

    void setBaseUrl(String baseUrl);

    String getBaseUrl();

    String getPythonHome();

    void setPythonHome(String pythonHome);

    long getStartedTime();

    void setStartedTime(long startedTime);

}
