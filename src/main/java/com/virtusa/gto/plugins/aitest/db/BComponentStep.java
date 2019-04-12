package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.virtusa.gto.plugins.aitest.entity.BusComScript;
import com.virtusa.gto.plugins.aitest.entity.BusComSteps;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author RPSPERERA on 9/21/2018
 */

public class BComponentStep {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public BComponentStep(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public BusComSteps insert(BusComScript busComScript, String stepDescription, String expectedValue, String dataParams) {
        return activeObjects.executeInTransaction(() -> {
            BusComSteps componentSteps = activeObjects.create(BusComSteps.class);
            componentSteps.setBusComScript(busComScript);
            componentSteps.setStepDescription(stepDescription);
            componentSteps.setExpectedValue(expectedValue);
            componentSteps.setDataParams(dataParams);
            componentSteps.save();
            return componentSteps;
        });
    }

    public BusComSteps update(BusComScript busComScript, String stepDescription, String expectedValue, String dataParams) {
        return activeObjects.executeInTransaction(() -> {
            BusComSteps componentSteps = activeObjects.create(BusComSteps.class);
            componentSteps.setBusComScript(busComScript);
            componentSteps.setStepDescription(stepDescription);
            componentSteps.setExpectedValue(expectedValue);
            componentSteps.setDataParams(dataParams);
            componentSteps.save();
            return componentSteps;
        });
    }

    public boolean deleteTestStepsById(String id) {

        return activeObjects.executeInTransaction(() -> {
            try {
                BusComSteps[] steps = activeObjects.find(BusComScript.class, "ID = ? ", id)[0].getBusComSteps();
                for (BusComSteps step : steps) {
                    activeObjects.deleteWithSQL(BusComSteps.class, "ID = ?", step.getID());
                }
            } catch (ActiveObjectsException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        });
    }

    public Map<String, HashMap<String, String>> deleteTestStepsById(String id, JsonArray changeSetJsonElements) {
        Map<String, HashMap<String, String>> hashMap = new HashMap<>();

        return activeObjects.executeInTransaction(() -> {
            try {
                BusComSteps[] steps = activeObjects.find(BusComScript.class, "ID = ? ", id)[0].getBusComSteps();
                for (BusComSteps step : steps) {
                    for (int i = 0; i < changeSetJsonElements.size(); i++) {
                        JsonObject jsonObject = changeSetJsonElements.get(i).getAsJsonObject();
                        JsonElement id1 = jsonObject.get("id");
                        if (jsonObject.has("id") && !id1.getAsString().isEmpty() && Integer.valueOf(id1.getAsString()) == step.getID()) {
                            String param_name = jsonObject.get("param_Name").getAsString();
                            List<String> params1 = getParams(step.getDataParams());
                            List<String> params = getParams(param_name);
                            params.forEach(para -> {
                                if (!params1.contains(para)) {
                                    HashMap<String, String> paramStateHashMap1 = new HashMap<>();
                                    paramStateHashMap1.put(para, "new");
                                    hashMap.put(para, paramStateHashMap1);
                                } else {
                                    params1.remove(para);
                                }
                            });
                            params1.forEach(param_nameF -> {
                                HashMap<String, String> paramStateHashMap = new HashMap<>();
                                paramStateHashMap.put(param_nameF, "deleted");
                                hashMap.put(param_nameF, paramStateHashMap);
                            });
                        } else {
                            String param_name = jsonObject.get("param_Name").getAsString();
                            List<String> params = getParams(param_name);
                            params.forEach(param_nameF -> {
                                HashMap<String, String> paramStateHashMap = new HashMap<>();
                                if  (jsonObject.has("state")){
                                    paramStateHashMap.put(param_nameF, jsonObject.get("state").getAsString());
                                    hashMap.put(param_nameF, paramStateHashMap);
                                }
                            });
                        }
                    }
                }
            } catch (ActiveObjectsException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return hashMap;
        });
    }

    public BusComSteps[] getSteps(String scriptId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                return activeObjects.find(BusComScript.class, "ID = ? ", scriptId)[0].getBusComSteps();
            } catch (ActiveObjectsException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return null;
        });


    }

    private List<String> getParams(String dataString) {
        String regex = "\\{\\{[a-zA-Z0-9]*}}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dataString);
        List<String> paramList = new ArrayList<>();
        while (matcher.find()) {
            String paramName = matcher.group(0).replace("{{", "").replace("}}", "");
            paramList.add(paramName);
        }
        return paramList;
    }
}
