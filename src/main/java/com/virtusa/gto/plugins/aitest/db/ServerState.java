package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.ServerStatus;

/**
 * @author RPSPERERA on 9/5/2018
 */
@Scanned
public class ServerState {

    @JiraImport
    private final ActiveObjects activeObjects;

    public ServerState(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public boolean getServerStatus() {
        return activeObjects.executeInTransaction(() -> {
            ServerStatus[] serverStatuses = activeObjects.find(ServerStatus.class);
            if (serverStatuses.length > 0) {
                return serverStatuses[0].getStatus();
            }
            return false;
        });
    }

    public void updateServerStatus(boolean state) {
        activeObjects.executeInTransaction(() -> {
            ServerStatus[] serverStatuses = activeObjects.find(ServerStatus.class);
            ServerStatus serverStatus;
            serverStatus = serverStatuses.length == 0 ? activeObjects.create(ServerStatus.class) : serverStatuses[0];
            serverStatus.setStatus(state);
            serverStatus.save();
            return null;
        });

    }

    public void updateProcessList(String processList) {
        activeObjects.executeInTransaction(() -> {
            ServerStatus[] serverStatuses = activeObjects.find(ServerStatus.class);
            if (serverStatuses.length > 0) {
                ServerStatus serverStatus = serverStatuses[0];
                serverStatus.setProcessList(processList);
                serverStatus.save();
            }
            return null;
        });
    }

    public String getProcessList() {
      return activeObjects.executeInTransaction(() -> {
            ServerStatus[] serverStatuses = activeObjects.find(ServerStatus.class);
            if (serverStatuses.length > 0) {
                return serverStatuses[0].getProcessList();

            }
            return null;
        });
    }
}
