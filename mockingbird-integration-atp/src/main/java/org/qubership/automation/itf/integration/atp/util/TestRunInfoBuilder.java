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

package org.qubership.automation.itf.integration.atp.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.IDataSetListManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.LabeledObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByParameterAndProjectIdManager;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.pcap.PcapHelper;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.model.ContextEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestRunInfoBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void setCallchain(TestRunInfo testRunInfo, Matcher matcher) {
        String callchainIdOrName = matcher.group(ATPActionConstants.CALLCHAIN_INDEX.intValue());
        if (StringUtils.isEmpty(callchainIdOrName)) {
            throw testRunInfo.reportException("Call chain ID/NAME is empty in the Execute Step Request from ATP!\n");
        }
        Collection<? extends CallChain> callchains = getCallchainsByIdOrName(callchainIdOrName,
                testRunInfo.getProjectId());
        CallChain firstCallchain = Iterables.getFirst(callchains, null);
        if (Objects.isNull(firstCallchain)) {
            throw testRunInfo.reportException(getCallchainNotFoundErrorMessage(callchainIdOrName));
        }
        testRunInfo.setCallchainsToExecute(Lists.newArrayList(new CallchainRunInfo(firstCallchain, null)));
    }

    public static void setDataset(TestRunInfo testRunInfo, Matcher matcher) {
        String datasetFullPath = matcher.group(ATPActionConstants.DATASET_INDEX.intValue());
        if (StringUtils.isNotEmpty(datasetFullPath)) {
            if (!ATPActionConstants.DEFAULT.stringValue().equals(datasetFullPath)) {
                setSelectedDataset(testRunInfo, matcher, datasetFullPath);
            } else {
                setDefaultDataset(testRunInfo, matcher);
            }
        }
    }

    public static void setDatasetsWithLabel(TestRunInfo testRunInfo, Matcher matcher) {
        String datasetFullPath = matcher.group(ATPActionConstants.DATASETLIST_PATH_INDEX.intValue());
        if (StringUtils.isNotEmpty(datasetFullPath)) {
            if (!ATPActionConstants.DEFAULT.stringValue().equals(datasetFullPath)) {
                String label = matcher.group(ATPActionConstants.DATASET_LABEL_INDEX.intValue());
                setSelectedDatasetWithLabel(testRunInfo, matcher, datasetFullPath, label);
            } else {
                throw testRunInfo.reportException("Datasetlist Path ('" + datasetFullPath + "') can't be empty or "
                        + "'DEFAULT'!");
            }
        }
    }

    private static Collection<CallChain> getCallchainsByLabel(TestRunInfo testRunInfo, Matcher matcher) {
        String label = matcher.group(ATPActionConstants.LABEL_INDEX.intValue());
        if (StringUtils.isEmpty(label)) {
            throw testRunInfo.reportException("Label name is empty in ATP action!\n");
        }
        Collection<CallChain> callChains = CoreObjectManager.getInstance().getSpecialManager(CallChain.class,
                LabeledObjectManager.class).getByLabel(label, testRunInfo.getProjectId());
        if (callChains.isEmpty()) {
            throw testRunInfo.reportException("Call chains with label \"" + label + "\" aren't found! \n");
        }
        return callChains;
    }

    @SuppressWarnings("unchecked")
    public static void setCallchainsWithDefaultDatasetsByLabel(TestRunInfo testRunInfo, Matcher matcher) {
        Collection<CallChain> callChains = getCallchainsByLabel(testRunInfo, matcher);
        List<CallchainRunInfo> callchainRunInfos = new ArrayList<>();
        try {
            for (CallChain callChain : callChains) {
                IDataSet datasetForRun = getDefaultDataset(callChain);
                if (Objects.isNull(datasetForRun)) {
                    datasetForRun = getFirstDataset(callChain, testRunInfo.getProjectUuid());
                }
                CallchainRunInfo callchainRunInfo = new CallchainRunInfo(callChain, datasetForRun);
                callchainRunInfos.add(callchainRunInfo);
            }
            testRunInfo.setCallchainsToExecute(callchainRunInfos);
        } catch (Exception ex) {
            throw testRunInfo.reportException(String.format("Exception getting default dataset(s): %s %s",
                    ex.getMessage(), (ex.getCause() == null) ? "" : "\nCaused by: " + ex.getCause().getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    public static void setCallchainsWithAllDatasetsByLabel(TestRunInfo testRunInfo, Matcher matcher) {
        Collection<CallChain> callChains = getCallchainsByLabel(testRunInfo, matcher);
        List<CallchainRunInfo> callchainRunInfos = new ArrayList<>();
        try {
            for (CallChain callChain : callChains) {
                for (DataSetList dataSetList : callChain.getCompatibleDataSetLists(testRunInfo.getProjectUuid())) {
                    for (IDataSet dataSet : dataSetList.getDataSets(testRunInfo.getProjectUuid())) {
                        callchainRunInfos.add(new CallchainRunInfo(callChain, dataSet));
                    }
                }
            }
            testRunInfo.setCallchainsToExecute(callchainRunInfos);
        } catch (Exception ex) {
            throw testRunInfo.reportException(String.format("Exception getting datasets(s): %s %s", ex.getMessage(),
                    (ex.getCause() == null) ? "" : "\nCaused by: " + ex.getCause().getMessage()));
        }
    }

    public static void setTcpDumpOption(TestRunInfo testRunInfo, Matcher matcher) {
        String tcpDumpEnabled = matcher.group(ATPActionConstants.TCPDUMP_ENABLED_INDEX.intValue());
        if (ATPActionConstants.TRUE.stringValue().equals(tcpDumpEnabled)) {
            BigInteger projectId = testRunInfo.getProjectId();
            Map<String, String> tcpDumpOptions = Maps.newHashMap();
            tcpDumpOptions.put(PcapHelper.TCPDUMP_NETWORK_INTERFACE_NAME_KEY,
                    CoreServices.getProjectSettingsService().get(testRunInfo.getProjectId(),
                            ProjectSettingsConstants.TCP_DUMP_NI_DEFAULT,
                            ProjectSettingsConstants.TCP_DUMP_NI_DEFAULT_VALUE));
            String filterValue = matcher.group(ATPActionConstants.TCPDUMP_FILTER_INDEX.intValue());
            tcpDumpOptions.put(PcapHelper.TCPDUMP_FILTER_KEY, StringUtils.isNotEmpty(filterValue) ? filterValue :
                    CoreServices.getProjectSettingsService().get(projectId,
                            ProjectSettingsConstants.TCPDUMP_CAPTURING_FILTER_DEFAULT,
                            ProjectSettingsConstants.TCPDUMP_CAPTURING_FILTER_DEFAULT_DEFAULT_VALUE));
            String packetCountValue = matcher.group(ATPActionConstants.TCPDUMP_PACKET_COUNT_INDEX.intValue());
            tcpDumpOptions.put(PcapHelper.TCPDUMP_PACKET_COUNT_KEY, StringUtils.isNotEmpty(packetCountValue)
                    ? packetCountValue : CoreServices.getProjectSettingsService().get(projectId,
                    ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT,
                    ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT_DEFAULT_VALUE));
            testRunInfo.setTcpDumpOptions(tcpDumpOptions);
        }
    }

    public static void setBVOption(TestRunInfo testRunInfo, Matcher matcher) {
        String bvValidationEnabled = matcher.group(ATPActionConstants.BV_ENABLED_INDEX.intValue());
        if (ATPActionConstants.TRUE.stringValue().equals(bvValidationEnabled)) {
            String bvActionValue = matcher.group(ATPActionConstants.BV_ACTION_INDEX.intValue());
            testRunInfo.setBvAction(StringUtils.isNotEmpty(bvActionValue) ? bvActionValue :
                    CoreServices.getProjectSettingsService().get(testRunInfo.getProjectId(),
                            ProjectSettingsConstants.BV_DEFAULT_ACTION,
                            ProjectSettingsConstants.BV_DEFAULT_ACTION_DEFAULT_VALUE));
        }
    }

    public static boolean getOptionDefaultValue(BigInteger projectId, String integrationName, String propName) {
        Set<IntegrationConfig> integrationConfs =
                CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId).getIntegrationConfs();
        for (IntegrationConfig config : integrationConfs) {
            if (config.getTypeName().equals(integrationName)) {
                return Boolean.parseBoolean(config.get(propName));
            }
        }
        return false;
    }

    public static Collection<? extends CallChain> getCallchainsByIdOrName(String parameter, BigInteger projectId) {
        SearchByParameterAndProjectIdManager<CallChain> specialManager =
                CoreObjectManager.getInstance().getSpecialManager(CallChain.class,
                        SearchByParameterAndProjectIdManager.class);
        return parameter.startsWith("~") ? Lists.newArrayList(specialManager.getById(parameter.substring(1))) :
                specialManager.getByNameAndProjectId(parameter, projectId);
    }

    public static String getCallchainNotFoundErrorMessage(String idOrName) {
        return String.format("Can't find a callchain with %s: %s", idOrName.startsWith("~") ? "id" : "name", idOrName);
    }

    private static void setSelectedDataset(TestRunInfo testRunInfo, Matcher matcher, String datasetFullPath) {
        String[] splittedFullPath = datasetFullPath.split("\\|");
        if (splittedFullPath.length != 3) {
            throw testRunInfo.reportException("Dataset full path is incorrect: '" + datasetFullPath + "'. Correct "
                    + "format: DatasetListSource|DatasetList|Dataset.");
        }
        try {
            DataSetListsSource foundSource = findDatasetListSourceByNameAndProjectUuid(splittedFullPath[0],
                    testRunInfo.getProjectUuid());
            if (foundSource != null) {
                DataSetList dataSetList = foundSource.getDataSetList(splittedFullPath[1]);
                if (dataSetList != null) {
                    IDataSet foundDataSet = dataSetList.getDataSet(splittedFullPath[2], testRunInfo.getProjectId());
                    if (foundDataSet != null) {
                        CallchainRunInfo callchainRunInfo = testRunInfo.getCallchainsToExecute().get(0);
                        for (CallChain callChain :
                                getCallchainsByIdOrName(matcher.group(ATPActionConstants.CALLCHAIN_INDEX.intValue()),
                                        testRunInfo.getProjectId())) {
                            if (Objects.equals(callchainRunInfo.getCallChain().getID(), callChain.getID())) {
                                callchainRunInfo.setDataset(foundDataSet);
                                break;
                            }
                        }
                    } else {
                        throw testRunInfo.reportException(String.format("Dataset with name '%s' isn't found in '%s' "
                                        + "dataset list of '%s' %s.", splittedFullPath[2], splittedFullPath[1],
                                splittedFullPath[0], foundSource.getSourceType()));
                    }
                } else {
                    throw testRunInfo.reportException(String.format("DatasetList with name '%s' isn't found in the "
                            + "'%s' %s.", splittedFullPath[1], splittedFullPath[0], foundSource.getSourceType()));
                }
            } else {
                throw testRunInfo.reportException(String.format("Source (Visibility area or Excel file) with name "
                        + "'%s' isn't found.", splittedFullPath[0]));
            }
        } catch (IllegalArgumentException ex) {
            throw testRunInfo.reportException(String.format("Exception getting dataset by path '%s': %s %s",
                    datasetFullPath,
                    ex.getMessage(),
                    (ex.getCause() == null) ? "" : "\nCaused by: " + ex.getCause().getMessage()));
        }
    }

    private static void setSelectedDatasetWithLabel(TestRunInfo testRunInfo, Matcher matcher, String datasetFullPath,
                                                    String datasetLabel) {
        String[] splittedFullPath = datasetFullPath.split("\\|");
        if (splittedFullPath.length != 2) {
            throw testRunInfo.reportException("Datasetlist full path is incorrect: '" + datasetFullPath + "'. Correct"
                    + " format: DatasetListSource|DatasetList.");
        }
        try {
            DataSetListsSource foundSource = findDatasetListSourceByNameAndProjectUuid(splittedFullPath[0],
                    testRunInfo.getProjectUuid());
            if (foundSource != null) {
                DataSetList dataSetList = foundSource.getDataSetList(splittedFullPath[1]);
                if (dataSetList != null) {
                    List<? extends IDataSet> dataSets =
                            CoreObjectManager.getInstance().getSpecialManager(DataSetList.class,
                                    IDataSetListManager.class).getDataSetsWithLabel(dataSetList, datasetLabel, null);
                    if (dataSets.size() > 0) {
                        CallchainRunInfo callchainRunInfo = testRunInfo.getCallchainsToExecute().get(0);
                        testRunInfo.getCallchainsToExecute().clear();
                        for (IDataSet dataSet : dataSets) {
                            CallchainRunInfo newRunInfo = new CallchainRunInfo(callchainRunInfo.getCallChain(),
                                    dataSet);
                            testRunInfo.getCallchainsToExecute().add(newRunInfo);
                        }
                    } else {
                        throw testRunInfo.reportException(String.format("Datasets with label '%s' aren't found in "
                                        + "'%s' dataset list of '%s' %s.", datasetLabel, splittedFullPath[1],
                                splittedFullPath[0], foundSource.getSourceType()));
                    }
                } else {
                    throw testRunInfo.reportException(String.format("DatasetList with name '%s' isn't found in '%s' "
                            + "%s.", splittedFullPath[1], splittedFullPath[0], foundSource.getSourceType()));
                }
            } else {
                throw testRunInfo.reportException(String.format("Source (Visibility area or Excel file) with name "
                        + "'%s' isn't found.", splittedFullPath[0]));
            }
        } catch (IllegalArgumentException ex) {
            throw testRunInfo.reportException(String.format("Exception getting dataset by path '%s': %s %s",
                    datasetFullPath,
                    ex.getMessage(),
                    (ex.getCause() == null) ? "" : "\nCaused by: " + ex.getCause().getMessage()));
        }
    }

    public static void setDefaultDataset(TestRunInfo testRunInfo, Matcher matcher) {
        try {
            CallchainRunInfo callchainRunInfo = testRunInfo.getCallchainsToExecute().get(0);
            for (CallChain callChain :
                    getCallchainsByIdOrName(matcher.group(ATPActionConstants.CALLCHAIN_INDEX.intValue()),
                            testRunInfo.getProjectId())) {
                if (Objects.equals(callchainRunInfo.getCallChain().getID(), callChain.getID())) {
                    callchainRunInfo.setDataset(getDefaultDataset(callChain));
                    break;
                }
            }
        } catch (Exception ex) {
            throw testRunInfo.reportException(String.format("Exception getting default dataset: %s %s",
                    ex.getMessage(), (ex.getCause() == null) ? "" : "\nCaused by: " + ex.getCause().getMessage()));
        }
    }

    public static String getParameterValueFromContextEntity(ContextEntity contextEntity, String paramName)
            throws JsonProcessingException {
        return objectMapper.readTree(contextEntity.getJsonString()).get(paramName).asText();
    }

    private static IDataSet getFirstDataset(CallChain callChain, UUID projectUuid) {
        Iterator<DataSetList> dslIterator = callChain.getCompatibleDataSetLists(projectUuid).iterator();
        if (dslIterator.hasNext()) {
            Iterator<IDataSet> dsIterator = dslIterator.next().getDataSets(projectUuid).iterator();
            if (dsIterator.hasNext()) {
                return dsIterator.next();
            }
        }
        return null;
    }

    private static IDataSet getDefaultDataset(CallChain callChain) {
        IDataSet defaultDataSet = callChain.findDataSetByName(callChain.getDatasetId(), callChain.getProjectId());
        if (defaultDataSet == null) {
            defaultDataSet = callChain.findDataSetById(callChain.getDatasetId(), callChain.getProjectId());
        }
        return defaultDataSet;
    }

    private static DataSetListsSource findDatasetListSourceByNameAndProjectUuid(String name, UUID projectUuid) {
        DataSetListsSource foundSource = null;
        Collection<? extends DataSetListsSource> allSources = CoreObjectManager.getInstance()
                        .getSpecialManager(DataSetList.class, IDataSetListManager.class).getAllSources(projectUuid);
        for (DataSetListsSource source : allSources) {
            if (source.getName().equals(name)) {
                foundSource = source;
                break;
            }
        }
        return foundSource;
    }
}
