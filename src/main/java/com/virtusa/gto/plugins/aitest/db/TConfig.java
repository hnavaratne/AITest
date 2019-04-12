package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.Configuration;

import javax.inject.Inject;
import java.util.Objects;

@Scanned
public class TConfig {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public TConfig(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public Configuration insert(String host, String port, String baseUrl, String pythonHome) {
        return activeObjects.executeInTransaction(() -> {
            Configuration configurations = activeObjects.create(Configuration.class);
            configurations.setPort(port);
            configurations.setHost(host);
            configurations.setBaseUrl(baseUrl);
            configurations.setPythonHome(pythonHome);
            configurations.save();
            return configurations;
        });

    }

    public Configuration upsert(String host, String port, String baseUrl, String pythonHome) {
        return activeObjects.executeInTransaction(() -> {
            Configuration[] configurations = activeObjects.find(Configuration.class);
            if (configurations.length > 0) {
                configurations[0].setPort(port);
                configurations[0].setHost(host);
                configurations[0].setBaseUrl(baseUrl);
                configurations[0].setPythonHome(pythonHome);
                configurations[0].save();
            } else {
                return insert(host, port, baseUrl, pythonHome);
            }

            return configurations[0];
        });
    }

    public Configuration get() {
        return activeObjects.executeInTransaction(() -> {
            Configuration[] configurations = activeObjects.find(Configuration.class);
            if (Objects.nonNull(configurations) && configurations.length > 0) {
                return configurations[0];
            }
            return null;
        });
    }

    public Configuration setTime(long startedTime) {
        return activeObjects.executeInTransaction(() -> {
            Configuration configurations = activeObjects.create(Configuration.class);
            configurations.setStartedTime(startedTime);
            configurations.save();
            return configurations;
        });
    }

    public Configuration getTime() {
        return activeObjects.executeInTransaction(() -> {
            Configuration[] configurations = activeObjects.find(Configuration.class);
            return configurations[0];
        });
    }
}
