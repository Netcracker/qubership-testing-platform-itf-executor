/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.core.util.copier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONObject;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.TriggerConfiguration;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;

public class SmartCopier {

    private static final String NEED_OTHER_SERVER = "needOtherServer";
    private static final String NEED_NEW_SERVER = "needNewServer";
    private static final String USE_STANDARD_TRIGGERS = "useStandardTriggers";
    private static final String SERVER = "server";
    private static final String SYSTEMS = "systems";
    private static final String STANDARD_TRIGGERS = "standardTriggers";
    private static final String TRIGGER_PROPERTIES = "triggerProperties";
    private static final String PROPERTY_VALUE = "propertyValue";
    private static final String PROPERTY = "property";
    private static final String PROPERTIES = "properties";
    private static final String DESTINATION_FOLDER = "destinationFolder";
    private static final String ID = "id";

    private static final String TYPE = "type";
    private static final String NAME = "name";

    /**
     * The method set param in storable.
     * Json has all param
     *
     * @param copiedSource - Copied object to set params
     * @param source       - Source object
     * @param jsonObject   - The json has all param for set
     * @return <code>copiedSource</code> It is storable
     */
    public static Storable setAllValuesOnCopyStorable(Storable copiedSource, Storable source, JSONObject jsonObject,
                                                      String projectId) {
        if (copiedSource instanceof Environment) {
            return setAllValuesOnCopyEnvironment((Environment) copiedSource, (Environment) source, jsonObject,
                    projectId);
        }
        return copiedSource;
    }

    /**
     * Method fills values on copied Environment.
     *
     * @param environment       - Environment to set param
     * @param sourceEnvironment - Source Environment
     * @param jsonObject        - The json has all param for copy
     * @return <code>environment</code> It is Environment
     */
    private static Storable setAllValuesOnCopyEnvironment(Environment environment, Environment sourceEnvironment,
                                                          JSONObject jsonObject, String projectId) {
        if (isJsonObjectValid(jsonObject)) {
            ArrayList<LinkedHashMap<String, String>> replacements
                    = (ArrayList<LinkedHashMap<String, String>>) jsonObject.get("replacements");
            environment.setName(jsonObject.get(NAME).toString());
            if (jsonObject.containsKey(DESTINATION_FOLDER)) {
                JSONObject folderProperties = new JSONObject((Map) jsonObject.get(DESTINATION_FOLDER));
                EnvFolder destinationFolder =
                        CoreObjectManager.getInstance().getManager(EnvFolder.class).getById(folderProperties.get(ID));
                environment.setParent(destinationFolder);
            }
            Map currentJsonMapForInbound = (Map) jsonObject.get(SERVER);
            if (!currentJsonMapForInbound.get("name").equals("")) {
                Server inboundServer = createNewServer(new JSONObject((Map) jsonObject.get(SERVER)), projectId);
                if (Objects.isNull(inboundServer)) {
                    return environment;
                }
                if (Boolean.valueOf(jsonObject.get(USE_STANDARD_TRIGGERS).toString())) {
                    prepareTemplateTriggers(environment.getInbound().entrySet(), inboundServer,
                            new JSONObject((Map) jsonObject.get(TRIGGER_PROPERTIES)));
                }
                Map<String, String> systemMap
                        = parseListJsonMapsToMap((List<Map<String, String>>) jsonObject.get(SYSTEMS),
                        "systemId", "type", "inbound", "status");
                for (Map.Entry<System, Server> entry : environment.getInbound().entrySet()) {
                    setServerIfNeed(inboundServer, entry, systemMap, false, replacements);
                }
            } else {
                if (Boolean.valueOf(jsonObject.get(USE_STANDARD_TRIGGERS).toString())) {
                    prepareTemplateTriggers(environment.getInbound().entrySet(), null,
                            new JSONObject((Map) jsonObject.get(TRIGGER_PROPERTIES)));
                }
                Map<String, String> systemMap
                        = parseListJsonMapsToMap((List<Map<String, String>>) jsonObject.get(SYSTEMS),
                        "systemId", "type", "inbound", "status");
                for (Map.Entry<System, Server> entry : environment.getInbound().entrySet()) {
                    setServerIfNeed(null, entry, systemMap, false, replacements);
                }
            }

            Map<String, String> systemMap = parseListJsonMapsToMap((List<Map<String, String>>) jsonObject.get(SYSTEMS),
                    "systemId", "type", "outbound", "status");
            ArrayList<LinkedHashMap> newServers = (ArrayList<LinkedHashMap>) jsonObject.get("uniqueOutbounds");
            ArrayList<Server> outboundServers = new ArrayList<>();
            for (int i = 0; i < newServers.size(); i++) {
                if (!newServers.get(i).get("newServerName").equals("")) {
                    Server outboundServer = createNewServer(new JSONObject((Map) jsonObject.get(SERVER)), projectId);
                    outboundServer.setName((String) newServers.get(i).get("newServerName"));
                    outboundServer.setUrl((String) newServers.get(i).get("newUrl"));
                    outboundServers.add(outboundServer);
                } else {
                    outboundServers.add(null);
                }
            }

            for (Map.Entry<System, Server> entry : environment.getOutbound().entrySet()) {
                for (int i = 0; i < newServers.size(); i++) {
                    LinkedHashMap currentObjectMap = (LinkedHashMap) newServers.get(i).get("currentObject");
                    LinkedHashMap currentServerMap = (LinkedHashMap) currentObjectMap.get("server");
                    String currentServerName = (String) currentServerMap.get("name");
                    if (currentServerName.equals(entry.getValue().getName())) {
                        setServerIfNeed(outboundServers.get(i), entry, systemMap, true, replacements);
                    }
                }
            }
        }
        environment.store();
        return environment;
    }

    private static void copyInbounds(Map.Entry<System, Server> entry, Server server,
                                     Set<TriggerConfiguration> standardTriggers) {
        Collection<InboundTransportConfiguration> inbounds = entry.getValue().getInbounds(entry.getKey());
        for (InboundTransportConfiguration inbound : inbounds) {
            InboundTransportConfiguration newInboundTransportConfiguration
                    = copyInboundTransportConfiguration(inbound, server);
            fillTriggers(newInboundTransportConfiguration.getTriggerConfigurations(), standardTriggers);
        }
    }

    private static void setServerIfNeed(Server server, Map.Entry<System, Server> entry,
                                        Map<String, String> systemMapStatus, boolean isOutbound,
                                        ArrayList<LinkedHashMap<String, String>> replacements) {
        for (Map.Entry<String, String> systemStatus : systemMapStatus.entrySet()) {
            if (systemStatus.getKey().equals(entry.getKey().getID().toString())
                    && Boolean.valueOf(systemStatus.getValue())) {
                if (isOutbound) {
                    Collection<OutboundTransportConfiguration> confs = entry.getValue().getOutbounds(entry.getKey());
                    Collection<OutboundTransportConfiguration> newConfs = new ArrayList<>();
                    for (OutboundTransportConfiguration conf : confs) {
                        OutboundTransportConfiguration newConf
                                = new OutboundTransportConfiguration(conf.getTypeName(), server, conf.getSystem());
                        newConf.fillConfiguration(conf.getConfiguration());
                        for (LinkedHashMap<String, String> replaceMap : replacements) {
                            String oldValue = replaceMap.get("searchstring");
                            String newValue = replaceMap.get("replacement");
                            for (String checkingKey : newConf.getConfiguration().keySet()) {
                                String checkingValue = newConf.getConfiguration().get(checkingKey);
                                if (StringUtils.isNotEmpty(checkingValue)
                                        && checkingValue.trim().contains(oldValue.trim())) {
                                    int indexPosition = checkingValue.trim().indexOf(oldValue.trim());
                                    String firstPartSubString = checkingValue.trim().substring(0, indexPosition);
                                    String secondPartSubString = checkingValue.trim()
                                            .substring(indexPosition + oldValue.trim().length());
                                    newConf.getConfiguration()
                                            .replace(checkingKey, firstPartSubString + newValue + secondPartSubString);
                                }
                            }
                        }
                        newConfs.add(newConf);
                    }
                    server.getOutbounds().addAll(newConfs);
                }
                entry.setValue(server);
            }
        }
    }

    /**
     * The method populates the configuration parameters for the trigger.
     *
     * @param inConfigurations  - where it need to set all param
     * @param outConfigurations - where it need to get all param
     */
    private static void fillTriggers(Set<TriggerConfiguration> inConfigurations,
                                     Set<TriggerConfiguration> outConfigurations) {
        for (Configuration in : inConfigurations) {
            for (Configuration out : outConfigurations) {
                fillParamsConfigIfTypeIsSame(in, out);
            }
        }
    }

    /**
     * If you need, you can use this method for another copy.
     *
     * @param in  - where it need to set param
     * @param out - where it need to get param
     */
    private static void fillParamsConfigIfTypeIsSame(Configuration in, Configuration out) {
        if (out.getTypeName().equals(in.getTypeName())) {
            in.putAll(out.getConfiguration());
            in.store();
        }
    }

    /**
     * The method copy InboundTransportConfiguration get result.
     *
     * @param copiedInbound - copied object
     * @param parent        - The parent for new object
     * @return <code>InboundTransportConfiguration</code>
     */
    private static InboundTransportConfiguration copyInboundTransportConfiguration(
            InboundTransportConfiguration copiedInbound, Server parent) {
        InboundTransportConfiguration newInboundTransportConfiguration
                = new InboundTransportConfiguration(copiedInbound.getReferencedConfiguration(), parent);
        for (TriggerConfiguration trigger : copiedInbound.getTriggerConfigurations()) {
            TriggerConfiguration triggerConfiguration = new TriggerConfiguration(newInboundTransportConfiguration);
            triggerConfiguration.fillConfiguration(trigger.getConfiguration());
            triggerConfiguration.store();
            newInboundTransportConfiguration.getTriggerConfigurations().add(triggerConfiguration);
        }
        newInboundTransportConfiguration.store();
        return newInboundTransportConfiguration;
    }


    /**
     * Method for creating a server object.
     *
     * @param json - The json has all for to need to create new server (Name, Url)
     * @return <code>Server</code>
     */
    private static Server createNewServer(JSONObject json, String projectId) {
        Server server = CoreObjectManager.getInstance().getManager(Server.class)
                .create(CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId));
        server.setName(json.get("name").toString());
        server.setUrl(json.get("url").toString());
        return server;
    }

    /**
     * The method prepare standard triggers.
     *
     * @param json - The json has properties for triggers
     */
    private static void prepareTemplateTriggers(Set<Map.Entry<System, Server>> entries, Server server,
                                                JSONObject json) {
        for (Map.Entry<System, Server> entry : entries) {
            Collection<InboundTransportConfiguration> inbounds = entry.getValue().getInbounds(entry.getKey());
            for (InboundTransportConfiguration inbound : inbounds) {
                InboundTransportConfiguration newInboundTransportConfiguration
                        = copyInboundTransportConfiguration(inbound, server);
                for (Object triggerProperty : json.entrySet()) {
                    JSONObject jsonObject = new JSONObject((Map) ((Map.Entry) triggerProperty).getValue());

                    // get all parameters from json object
                    TriggerConfiguration triggerConfiguration = new TriggerConfiguration();
                    triggerConfiguration.setTypeName(jsonObject.get(TYPE).toString());
                    String property = jsonObject.get(PROPERTY).toString();
                    String propertyValue = jsonObject.get(PROPERTY_VALUE).toString();
                    Map<String, String> configuration = parseListJsonMapsToMap(
                            (LinkedHashMap<String, Map<String, String>>) jsonObject.get(PROPERTIES), "name", "value");
                    triggerConfiguration.fillConfiguration(configuration);

                    // set parameters only for filtered triggers
                    for (Configuration in : newInboundTransportConfiguration.getTriggerConfigurations()) {
                        if ((in.get(property) != null) && (in.get(property).toString().equals(propertyValue))) {
                            fillParamsConfigIfTypeIsSame(in, triggerConfiguration);
                        }
                    }
                }
            }
        }
    }

    /**
     * Method parse Json object to Map.
     *
     * @param mapParams - {@link List} of JSONObject
     * @param nameKey   - The field will be found and used as a key
     * @param nameValue - The field will be found and used as a value
     * @return {@link Map} It in the map key is key field and value is value field
     */
    private static Map<String, String> parseListJsonMapsToMap(List<Map<String, String>> mapParams,
                                                              String nameKey,
                                                              String nameFilter,
                                                              String filter,
                                                              String nameValue) {
        Map<String, String> configuration = new HashMap<>();
        for (Map<String, String> param : mapParams) {
            if (param.get(nameFilter).equals(filter)) {
                Pair<String, String> pair = parseJsonMapToPair(param, nameKey, nameValue);
                if (StringUtils.isNoneBlank(pair.getKey(), pair.getValue())) {
                    configuration.put(pair.getKey(), pair.getValue());
                }
            }
        }
        return configuration;
    }

    private static Map<String, String> parseListJsonMapsToMap(LinkedHashMap<String, Map<String, String>> mapParams,
                                                              String nameKey, String nameValue) {
        Map<String, String> configuration = new HashMap<>();
        for (Map<String, String> param : mapParams.values()) {
            Pair<String, String> pair = parseJsonMapToPair(param, nameKey, nameValue);
            if (StringUtils.isNoneBlank(pair.getKey(), pair.getValue())) {
                configuration.put(pair.getKey(), pair.getValue());
            }
        }
        return configuration;
    }

    /**
     * Method parse Json object to Pair.
     *
     * @param map       - Map of JSONObject
     * @param nameKey   - The field will be found and used as a key
     * @param nameValue - The field will be found and used as a value
     * @return {@link Pair} It in the pair key is key field and value is value field
     */
    private static Pair<String, String> parseJsonMapToPair(Map<String, String> map, String nameKey, String nameValue) {
        String key = "";
        String value = "";
        for (Map.Entry<String, String> option : map.entrySet()) {
            if (Objects.nonNull(option.getKey()) && Objects.nonNull(option.getValue())) {
                if (option.getKey().equals(nameKey)) {
                    key = option.getValue();
                }
                if (option.getKey().equals(nameValue)) {
                    value = String.valueOf(option.getValue());
                }
            }
        }
        return Pair.of(key, value);
    }

    /**
     * The method validate what all field exist in the json.
     * The method suitable only for environment now
     * If you need you can expand it for other types
     *
     * @param jsonObject The json has all param for copy
     * @return <tt>true</tt> if all fields exist for "smart copy" of Environment
     */
    private static boolean isJsonObjectValid(JSONObject jsonObject) {
        if (jsonObject.containsKey(SYSTEMS)
                && jsonObject.containsKey(NEED_OTHER_SERVER)
                && jsonObject.containsKey(USE_STANDARD_TRIGGERS)
                && jsonObject.containsKey(STANDARD_TRIGGERS)
                && jsonObject.containsKey(NEED_NEW_SERVER)
                && jsonObject.containsKey(NAME)
                && jsonObject.containsKey(SERVER)) {
            return true;
        }
        return false;
    }
}
