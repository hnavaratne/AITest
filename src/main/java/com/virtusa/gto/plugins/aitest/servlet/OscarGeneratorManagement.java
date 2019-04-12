package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.OscarDBOperation;
import com.virtusa.gto.plugins.aitest.db.OscarPVEOperation;
import com.virtusa.gto.plugins.aitest.db.OscarVEDBOperation;
import com.virtusa.gto.plugins.aitest.db.TestStepParam;
import com.virtusa.gto.plugins.aitest.entity.Oscar;
import com.virtusa.gto.plugins.aitest.entity.OscarPVE;
import com.virtusa.gto.plugins.aitest.entity.OscarVE;
import com.virtusa.gto.plugins.aitest.entity.TestStepParams;
import com.virtusa.gto.plugins.aitest.util.RoundRobin;
import com.virtusa.gto.plugins.aitest.util.UserLoggedInCondition;
import org.apache.commons.codec.digest.DigestUtils;
import org.cornutum.tcases.TestCase;
import org.cornutum.tcases.*;
import org.cornutum.tcases.generator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;


public class OscarGeneratorManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OscarGeneratorManagement.class);
    private static final long serialVersionUID = 8291610472984725279L;

    @JiraImport
    private ActiveObjects activeObjects;
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    public OscarGeneratorManagement(ActiveObjects activeObjects, UserManager userManager,
                                    LoginUriProvider loginUriProvider) {
        this.activeObjects = activeObjects;
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("application/json");
        String data = req.getParameter("data");
        String redirectUrl = "";
        try {
            JSONObject jsonObject = new JSONObject(data);
            if (jsonObject.has("redirectView")) {
                redirectUrl = jsonObject.get("redirectView").toString();
            }
            UserProfile currentUser = userManager.getRemoteUser(req);
            if (Objects.isNull(currentUser)) {
                resp.setStatus(HttpServletResponse.SC_FOUND);
                if (redirectUrl.equals("/secure/TestCaseDesigningView.jspa")) {
                    resp.getWriter().write(new UserLoggedInCondition(loginUriProvider).redirectToLogin(redirectUrl));
                }
            } else {
                String action = req.getParameter("action");
                handleAction(action, data, resp);
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction(String action, String data, HttpServletResponse resp) {
        try {
            switch (action) {
                case "get":
                    resp.getWriter().write(getOscar(data));
                    break;
                case "insert":
                    resp.getWriter().write(insertOscar(data));
                    break;
                case "update":
                    resp.getWriter().write(updateOscar(data));
                    break;
                case "save":
                    resp.getWriter().write(saveTCasesInput(data));
                    break;
                default:
                    handleAction1(action, data, resp);
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleAction1(String action, String data, HttpServletResponse resp) {
        try {
            switch (action) {
                case "generate":
                    resp.getWriter().write(generatePermutation(data));
                    break;
                case "insert-param-ve":
                    resp.getWriter().write(insertOscarParamVE(data));
                    break;
                case "get-param-ves":
                    resp.getWriter().write(getOscarParamVEs(data));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private JsonObject generateNewMindMapFields(String paramId, String rootId, String paramName, int ParamCount, int multiplierOne, int multiplierTwo, int multiplierThree, int multiplierFour) {
        int xValue = 200;
        int yValue = 130;
        int minusXValue = -200;
        int minusYValue = -130;
        JsonObject paramObject = new JsonObject();
        try {
            JsonObject offsetObject = new JsonObject();
            if (ParamCount % 4 == 0) {
                offsetObject.addProperty("y", (minusYValue * (multiplierOne)));
                offsetObject.addProperty("x", xValue);
            } else if (ParamCount % 4 == 1) {
                offsetObject.addProperty("y", (yValue * (multiplierTwo)));
                offsetObject.addProperty("x", xValue);
            } else if (ParamCount % 4 == 2) {
                offsetObject.addProperty("y", (yValue * (multiplierThree)));
                offsetObject.addProperty("x", minusXValue);
            } else if (ParamCount % 4 == 3) {
                offsetObject.addProperty("y", (minusYValue * (multiplierFour)));
                offsetObject.addProperty("x", minusXValue);
            }
            JsonObject fontObject = new JsonObject();
            fontObject.addProperty("style", "normal");
            fontObject.addProperty("weight", "bold");
            fontObject.addProperty("decoration", "none");
            fontObject.addProperty("size", "17");
            fontObject.addProperty("color", "#000000");
            JsonObject textObject = new JsonObject();
            textObject.addProperty("caption", paramName);
            textObject.add("font", fontObject);
            paramObject.addProperty("id", paramId);
            paramObject.addProperty("parentId", rootId);
            paramObject.add("text", textObject);
            paramObject.add("offset", offsetObject);
            paramObject.addProperty("foldChildren", false);
            paramObject.addProperty("branchColor", getRandomColor());
            paramObject.add("children", new JsonArray());

        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return paramObject;
    }

    private String getRandomColor() {
        String letters = "0123456789ABCDEF";
        StringBuilder color = new StringBuilder("#");
        for (int i = 0; i < 6; i++) {
            Double floor = Math.floor(Math.random() * 16);
            color.append(letters.charAt(floor.intValue()));
        }
        return color.toString();
    }

    private String getGuid() {
        return genGuidSec() + genGuidSec() + '-' + genGuidSec() + '-' + genGuidSec() + '-' + genGuidSec() + '-' + genGuidSec() + genGuidSec() + genGuidSec();
    }

    private String genGuidSec() {
        String letters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder uId = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            Double aDouble = Math.floor(Math.random() * 35);
            uId.append(letters.charAt(aDouble.intValue()));
        }
        return uId.toString();
    }


    private String getOscar(String data) {
        JSONArray responseArray = new JSONArray();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String testCaseId = jsonData.get("testCaseId").getAsString();
        OscarDBOperation oscarDBOperation = new OscarDBOperation(activeObjects);
        List<Oscar> oscars = oscarDBOperation.get(testCaseId);
        TestStepParam testStepParam = new TestStepParam(activeObjects);
        TestStepParams[] testStepParams = testStepParam.get(testCaseId);
        List<String> paramList = new ArrayList<>();
        List<JsonObject> newParamList = new ArrayList<>();
        final int[] multiplierOne = {1};
        final int[] multiplierTwo = {1};
        final int[] multiplierThree = {1};
        final int[] multiplierFour = {1};
        for (TestStepParams stepParam : testStepParams) {
            paramList.add(stepParam.getParamName());
        }
        for (Oscar oscar : oscars) {
            JSONObject responseObect = new JSONObject();
            String mindMap = oscar.getMindMap();
            JsonObject mindMapObject = new JsonParser().parse(mindMap).getAsJsonObject();
            JsonObject root = mindMapObject.get("mindmap").getAsJsonObject().get("root").getAsJsonObject();
            String rootId = root.get("id").getAsString();
            JsonArray rootNodes = root.get("children").getAsJsonArray();
            int[] size = {rootNodes.size()};
            JsonArray cloneRootNodes = new JsonArray();
            rootNodes.forEach(node -> {
                String paramNodeName = node.getAsJsonObject().get("text").getAsJsonObject().get("caption").getAsString();
                if (paramList.contains(paramNodeName)) {
                    cloneRootNodes.add(node);
                }
            });
            setValuesToMultipliers(multiplierOne, multiplierTwo, multiplierThree, multiplierFour, rootNodes);
            mergeParamNodes(paramList, newParamList, multiplierOne, multiplierTwo, multiplierThree, multiplierFour, rootId, size, cloneRootNodes);
            newParamList.forEach(cloneRootNodes::add);
            mindMapObject.get("mindmap").getAsJsonObject().get("root").getAsJsonObject().remove("children");
            mindMapObject.get("mindmap").getAsJsonObject().get("root").getAsJsonObject().getAsJsonObject().add("children", cloneRootNodes);
            try {
                responseObect.put("id", oscar.getID());
                responseObect.put("testCaseId", oscar.getTestCaseId());
                responseObect.put("mindMap", mindMapObject.toString());

                responseArray.put(responseObect);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return responseArray.toString();
    }

    private void mergeParamNodes(List<String> paramList, List<JsonObject> newParamList, int[] multiplierOne, int[] multiplierTwo, int[] multiplierThree, int[] multiplierFour, String rootId, int[] size, JsonArray cloneRootNodes) {
        paramList.forEach(paramName -> {
            final boolean[] paramFound = {false};
            for (JsonElement rootNode : cloneRootNodes) {
                String paramNodeName = rootNode.getAsJsonObject().get("text").getAsJsonObject().get("caption").getAsString();
                if (Objects.equals(paramName, paramNodeName)) {
                    paramFound[0] = true;
                    break;
                }
            }
            if (!paramFound[0]) {
                newParamList.add(generateNewMindMapFields(getGuid(), rootId, paramName, size[0], multiplierOne[0], multiplierTwo[0], multiplierThree[0], multiplierFour[0]));
                size[0] += 1;
            }
        });
    }

    private void setValuesToMultipliers(int[] multiplierOne, int[] multiplierTwo, int[] multiplierThree, int[] multiplierFour, JsonArray rootNodes) {
        IntStream.of(0, rootNodes.size()).forEach(value -> {
            if (value % 4 == 0) {
                multiplierOne[0] += 1;
            } else if (value % 4 == 1) {
                multiplierTwo[0] += 1;
            } else if (value % 4 == 2) {
                multiplierThree[0] += 1;
            } else if (value % 4 == 3) {
                multiplierFour[0] += 1;
            }
        });
    }

    private String insertOscar(String data) {
        JSONObject response = new JSONObject();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String testCaseId = jsonData.get("testCaseId").getAsString();
        String mindMap = jsonData.get("mindMap").getAsString();
        OscarDBOperation oscarDBOperation = new OscarDBOperation(activeObjects);
        Oscar oscar = oscarDBOperation.insert(testCaseId, mindMap);
        try {
            response.put("id", oscar.getID());
            response.put("testCaseId", oscar.getTestCaseId());
            response.put("mindMap", oscar.getMindMap());
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return response.toString();
    }

    private String updateOscar(String data) {
        JSONObject response = new JSONObject();
        try {
            JsonElement element = new JsonParser().parse(data);
            JsonObject jsonData = element.getAsJsonObject();
            String testCaseId = jsonData.get("testCaseId").getAsString();
            String mindMap = jsonData.get("mindMap").getAsString();
            OscarDBOperation oscarDBOperation = new OscarDBOperation(activeObjects);
            boolean result = oscarDBOperation.update(testCaseId, mindMap);
            response.put("result", result);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return response.toString();
    }

    private String generatePermutation(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String testCaseId = jsonData.get("test_case_id").getAsString();
        JSONObject responseObject = new JSONObject();
        try {
            responseObject.put("data", generateTestPermutationsUsingTestAutomationTool(testCaseId));
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return responseObject.toString();
    }

    private Map<String, LinkedList<String>> generateTestPermutationsUsingTestAutomationTool(String testCaseId) {

        SystemInputDef inputDef = new SystemInputDef("Oscar");
        FunctionInputDef functionInputDef = new FunctionInputDef("Oscar");
        inputDef.addFunctionInputDef(functionInputDef);

        OscarVEDBOperation oscarVEDBOperation = new OscarVEDBOperation(activeObjects);
        List<OscarVE> oscarVES = oscarVEDBOperation.get(testCaseId);
        Set<String> paramValues = new HashSet<>();
        Set<Integer> paramValuesV = new HashSet<>();
        Map<Integer, RoundRobin> valueExpansions = new HashMap<>();
        Map<String, String> paramNameMap = new HashMap<>();
        Map<String, String> hashParamNameMap = new HashMap<>();
        final String[] sha256hex = {""};
        final String[] currentParam = {""};
        oscarVES
                .parallelStream()
                .parallel()
                .forEach(oscarVE -> {
                    String param = oscarVE.getParam();
                    int paramV = oscarVE.getID();
                    paramValues.add(param);
                    paramValuesV.add(paramV);
                    if (sha256hex[0].isEmpty() || !currentParam[0].equals(param)) {
                        if (!paramNameMap.containsKey(param)) {
                            sha256hex[0] = DigestUtils.sha256Hex(param);
                            currentParam[0] = param;
                            paramNameMap.put(param, sha256hex[0]);
                            hashParamNameMap.put(sha256hex[0], param);
                        }
                    }
                });

        paramValues
                .parallelStream()
                .parallel()
                .forEach(paramValue -> {
                    VarDef varDef = new VarDef(paramNameMap.get(paramValue));
                    oscarVES.parallelStream()
                            .parallel()
                            .filter(oscarVE -> oscarVE.getParam().equals(paramValue))
                            .forEach(oscarVE -> {
                                VarValueDef varValueDef = new VarValueDef(oscarVE.getID());
                                varDef.addValue(varValueDef);
                            });
                    functionInputDef.addVarDef(varDef);
                });

        paramValuesV
                .parallelStream()
                .parallel()
                .forEach($paramValues -> {
                    List<String> valueList = new ArrayList<>();
                    oscarVES.parallelStream()
                            .parallel()
                            .filter(oscarVE -> oscarVE.getID() == $paramValues)
                            .forEach(oscarVE -> Arrays.asList(oscarVE.getOscarValueExpansion()).forEach(OscarPVE -> valueList.add(OscarPVE.getValue())));
                    valueExpansions.put($paramValues, new RoundRobin(valueList));
                });


        return getTests(inputDef, null, null, null, valueExpansions, hashParamNameMap);
    }


    private Map<String, LinkedList<String>> getTests(SystemInputDef inputDef, IGeneratorSet genDef, SystemTestDef baseDef, GeneratorOptions options, Map<Integer, RoundRobin> valueExpansions, Map<String, String> hashParamNameMap) {
        if (genDef == null) {
            genDef = GeneratorSet.basicGenerator();
        }

        Long seed = options == null ? null : options.getRandomSeed();
        int defaultTupleSize = 2;

        SystemTestDef testDef = new SystemTestDef(inputDef.getName());
        for (Iterator<FunctionInputDef> functionDefs = inputDef.getFunctionInputDefs(); functionDefs.hasNext(); ) {
            FunctionInputDef functionDef = functionDefs.next();
            FunctionTestDef functionBase = baseDef == null ? null : baseDef.getFunctionTestDef(functionDef.getName());
            ITestCaseGenerator functionGen = genDef.getGenerator(functionDef.getName());

            if (functionGen == null) {
                throw new RuntimeException("No generator for function=" + functionDef.getName());
            }

            if (seed != null) {
                functionGen.setRandomSeed(seed);
            }
            if (functionGen instanceof TupleGenerator) {
                ((TupleGenerator) functionGen).setDefaultTupleSize(defaultTupleSize);
            }

            testDef.addFunctionTestDef(functionGen.getTests(functionDef, functionBase));
        }

        testDef.addAnnotations(inputDef);
        Iterator<TestCase> testCases = testDef.getFunctionTestDef("Oscar").getTestCases();

        Map<String, LinkedList<String>> result = new ConcurrentHashMap<>();

        while (testCases.hasNext()) {
            org.cornutum.tcases.TestCase testCase = testCases.next();

            Iterator<VarBinding> varBindings = testCase.getVarBindings();
            while (varBindings.hasNext()) {
                VarBinding varBinding = varBindings.next();
                String paramName = hashParamNameMap.get(varBinding.getVar());
                String paramValue = valueExpansions.get(varBinding.getValue()).next();
                boolean containsKey = result.containsKey(paramName);
                if (containsKey) {
                    result.get(paramName).push(paramValue);
                } else {
                    LinkedList<String> linkedList = new LinkedList<>();
                    linkedList.push(paramValue);
                    result.put(paramName, linkedList);
                }
            }
        }

        return result;
    }

    private String saveTCasesInput(String data) {
        JSONObject response = new JSONObject();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        try {
            response.put("result", false);
           // OscarOperation.saveTCasesInput(jsonData.get("xml").getAsString());
            response.accumulate("result", true);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return response.toString();
    }

    private String insertOscarParamVE(String data) {
        JSONArray response = new JSONArray();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String testCaseId = jsonData.get("testCaseId").getAsString();
        JsonArray parameters = jsonData.get("parameters").getAsJsonArray();
        OscarVEDBOperation oscarVEDBOperation = new OscarVEDBOperation(activeObjects);
        OscarPVEOperation oscarPVEOperation = new OscarPVEOperation(activeObjects);
        boolean deletePVE = oscarPVEOperation.delete(testCaseId);
        boolean deleteVE = oscarVEDBOperation.delete(testCaseId);
        if (deleteVE && deletePVE) {
            for (JsonElement parameter : parameters) {
                JSONObject responseObject = new JSONObject();
                JsonObject parameterObject = parameter.getAsJsonObject();
                String param = parameterObject.get("param").getAsString();
                String paramV = parameterObject.get("paramV").getAsString();
                JsonArray paramVE = parameterObject.get("paramVE").getAsJsonArray();
                OscarVE oscarVE = oscarVEDBOperation.insert(testCaseId, param, paramV);
                paramVE.forEach(jsonElement -> {
                    String valueExpansion = jsonElement.getAsJsonObject().get("value").getAsString();
                    oscarPVEOperation.insert(testCaseId, param, paramV, valueExpansion);
                });
                try {
                    responseObject.put("id", oscarVE.getID());
                    responseObject.put("testCaseId", oscarVE.getTestCaseId());
                    responseObject.put("param", oscarVE.getParam());
                    responseObject.put("paramV", oscarVE.getParamV());
                    responseObject.put("paramVE", Arrays.toString(oscarVE.getOscarValueExpansion()));

                    response.put(responseObject);
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return response.toString();
    }

    private String getOscarParamVEs(String data) {
        JSONObject responseObject = new JSONObject();
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String testCaseId = jsonData.get("testCaseId").getAsString();
        OscarVEDBOperation oscarVEDBOperation = new OscarVEDBOperation(activeObjects);
        List<OscarVE> parameters = oscarVEDBOperation.get(testCaseId);
        for (OscarVE parameter : parameters) {
            try {
                OscarPVE[] oscarValueExpansion = parameter.getOscarValueExpansion();
                List<String> ve = new ArrayList<>();
                for (int i = 0; i < oscarValueExpansion.length; i++) {
                    ve.add(oscarValueExpansion[i].getValue());
                }
                responseObject.put(parameter.getParam() + "-" + parameter.getParamV(), ve);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
        return responseObject.toString();
    }

}
