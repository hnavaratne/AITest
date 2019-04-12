package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.db.TConfig;
import com.virtusa.gto.plugins.aitest.entity.Configuration;

import javax.sql.rowset.serial.SerialStruct;

/**
 * @author rpsperera on 9/3/2018
 */
public class Worker implements Runnable {

    @JiraImport
    private final ActiveObjects activeObjects;

    private String pythonHome;

    public Worker(ActiveObjects activeObjects, String pythonHome) {
        this.activeObjects = activeObjects;
        this.pythonHome = pythonHome;
    }

    @Override
    public void run() {
        MLServerManager mlServerManager = new MLServerManager();
        mlServerManager.startMLServer(pythonHome);
    }
}