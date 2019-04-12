package com.virtusa.gto.plugins.aitest.util;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author rpsperera on 8/31/2018
 */
public class MLServerManager {

    private static final Logger log = LoggerFactory.getLogger(MLServerManager.class);

    public void startMLServer(String pythonHome) {
        String tempDirectoryPath = System.getProperty("java.io.tmpdir");
        Path rqMeasurementFilePath = Paths.get(tempDirectoryPath.replaceAll("Program Files", "Progra~1"), "RQMeasurement.py");
        // Path yamlFilePath = Paths.get(tempDirectoryPath.replaceAll("Program Files", "Progra~1"), "threshold.yaml");
        CommonsUtil.copyExecutables("ml/ml.zip");
        log.warn("Starting Thread pool temp Path" + tempDirectoryPath + "updated Path " + rqMeasurementFilePath.toFile().getPath());
        String pytHome = Objects.nonNull(pythonHome) && pythonHome.isEmpty() ? "python.exe " : pythonHome;
        executeCommand(rqMeasurementFilePath.toFile().getPath(), "RQMeasurement.py", true, pytHome);
        log.warn("pool started successfully");
    }

    public void executeCommand(String filePath, String command, boolean isPython, String pythonHome) {
        try {
            File file = isPython ? new File(filePath).getAbsoluteFile().getParentFile() : new File(filePath).getParentFile();
            String commands = isPython ? pythonHome + " " + command : command;
            CommandLine cmdLine = CommandLine.parse(commands);
            DefaultExecutor executor = new DefaultExecutor();
            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            executor.setWorkingDirectory(file);
            executor.execute(cmdLine, resultHandler);
            executor.setStreamHandler(new PumpStreamHandler());
            resultHandler.waitFor();
            int exitValue = resultHandler.getExitValue();
            if (exitValue != 0) {
                log.error(resultHandler.getException().getMessage(), resultHandler.getException());
            }
        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean shutdownFlaskServer() {
        int retryCount = 0;
        boolean connectionSuccess = false;
        HttpURLConnection conn = null;
        while (retryCount < 3 && !connectionSuccess) {
            try {
                URL url = new URL("http://localhost:9154/api/v1/shutdown");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    log.error("Unable to connect to server , retry count : " + retryCount);
                    log.error("Response code : " + conn.getResponseCode());
                } else {
                    connectionSuccess = true;
                    log.info(conn.getContent().toString());
                }
                conn.disconnect();
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                log.error(e.getMessage());
            } finally {
                closeConnection(conn);
            }
            retryCount++;
        }

        return connectionSuccess;
    }

    private void closeConnection(HttpURLConnection conn) {
        if (!(conn == null)) {
            conn.disconnect();
        }
    }

}