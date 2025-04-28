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

package org.qubership.automation.itf.ui.services.faststubs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SituationObjectManager;
import org.qubership.automation.itf.core.model.FastStubsCandidate;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.stub.fast.FastConfigurationRequest;
import org.qubership.automation.itf.core.stub.fast.FastConfigurationResponse;
import org.qubership.automation.itf.core.stub.fast.FastResponseConfig;
import org.qubership.automation.itf.core.stub.fast.FastStubConfigurationAction;
import org.qubership.automation.itf.core.stub.fast.ResponseDescription;
import org.qubership.automation.itf.core.stub.fast.StubEndpointConfig;
import org.qubership.automation.itf.core.stub.fast.TransportConfig;
import org.qubership.automation.itf.core.util.eds.ExternalDataManagementService;
import org.qubership.automation.itf.core.util.eds.model.FileEventType;
import org.qubership.automation.itf.core.util.eds.service.EdsContentType;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.integration.users.UserService;
import org.qubership.automation.itf.ui.controls.FastStubsProcessingException;
import org.qubership.automation.itf.ui.model.User;
import org.qubership.automation.itf.ui.util.FileUploadHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FastStubsService {

    private ObjectMapper objectMapper;
    private UserService userService;
    private ExternalDataManagementService externalDataManagementService;
    private ExecutorToMessageBrokerSender executorToMessageBrokerSender;
//    private EventTriggerActivationService eventTriggerActivationService;

    @Autowired
    public FastStubsService(ExternalDataManagementService externalDataManagementService,
                            ExecutorToMessageBrokerSender executorToMessageBrokerSender,
//                            EventTriggerActivationService eventTriggerActivationService,
                            @Qualifier("executorIntegrationObjectMapper") ObjectMapper objectMapper,
                            UserService userService) {
        this.externalDataManagementService = externalDataManagementService;
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
//        this.eventTriggerActivationService = eventTriggerActivationService;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    public List<FastStubsCandidate> getFastStubsCandidates(List<BigInteger> operationIds, UUID projectUuid) {
        try {
            Optional<List<FastStubsCandidate>> fastStubsCandidates = CoreObjectManager.getInstance()
                    .getSpecialManager(Situation.class, SituationObjectManager.class)
                    .getFastStubsCandidates(projectUuid, operationIds);
            return fastStubsCandidates.orElse(new ArrayList<>());
        } catch (Exception e) {
            String error = String.format("An error occurred while retrieving the list of candidates. %s",
                    (Objects.nonNull(e.getCause()) ? e.getCause().getMessage() : e.getMessage()));
            log.error(error, e);
            throw new FastStubsProcessingException(error);
        }
    }

    public FastConfigurationResponse generateFastStubsConfigs(FastConfigurationRequest fastConfigurationRequest,
                                                              FastStubConfigurationAction action, UUID projectUuid) {
        FastConfigurationResponse fastConfigurationResponse = new FastConfigurationResponse();
        List<FastConfigurationResponse.FastInfoConfig> fastInfoConfigs = new ArrayList<>();
        try {
            collectRequestFastConfigs(fastConfigurationRequest, fastInfoConfigs);
            FastResponseConfig fastResponseConfig = new FastResponseConfig(fastConfigurationRequest);
            for (TransportConfig transportConfig : fastResponseConfig.getTransportConfigs()) {
                for (StubEndpointConfig stubEndpointConfig : transportConfig.getEndpoints()) {

                    FastResponseConfig fastConfig = new FastResponseConfig();
                    fastConfig.setProjectUuid(projectUuid.toString());

                    TransportConfig trConfig = new TransportConfig();
                    trConfig.setTransportType(transportConfig.getTransportType());
                    trConfig.setEndpoints(new ArrayList<StubEndpointConfig>() {
                        {
                            add(stubEndpointConfig);
                        }
                    });
                    fastConfig.setTransportConfigs(new ArrayList<TransportConfig>() {
                        {
                            add(trConfig);
                        }
                    });

                    String configAsString = objectMapper.writeValueAsString(fastConfig);
                    String fileName = String.format("%s__%s__%s.json",
                            projectUuid, transportConfig.getTransportType().name(),
                            URLEncoder.encode(stubEndpointConfig.getConfiguredEndpoint(), "UTF-8"));
                    storeFileAndNotifyInstances(fileName, configAsString, userService.getCurrentUserInfo(),
                            projectUuid);
                    collectAppliedFastConfigs(stubEndpointConfig, fastInfoConfigs);

                    /*
                     * In the initial implementation this was disabled

                    if (FastStubConfigurationAction.APPLY_AND_DEACTIVATE.equals(action)) {
                        List<BigInteger> situationsIds = stubEndpointConfig.getConditionalResponses()
                                .stream()
                                .map(responseDescription -> new BigInteger(responseDescription.getId()))
                                .collect(Collectors.toList());
                        ResponseDescription defaultResponse = stubEndpointConfig.getDefaultResponse();
                        if (Objects.nonNull(defaultResponse)) {
                            situationsIds.add(new BigInteger(defaultResponse.getId()));
                        }
                        if (!situationsIds.isEmpty()) {
                            User user = userService.getCurrentUserInfo();
                            StubUser stubUser = new StubUser();
                            stubUser.setId(user.getId());
                            stubUser.setName(user.getName());
                            EventTriggerStateResponse eventTriggerStateResponse = performEventTriggers(situationsIds,
                            *  stubUser);
                            collectDeactivatedFastConfigs(fastInfoConfigs);
                        }
                    }
                    */
                }
            }
            fastConfigurationResponse.setFastInfoConfigs(fastInfoConfigs);
        } catch (Exception e) {
            String error = String.format("Error while generate fast stubs configuration. %s",
                    (Objects.nonNull(e.getCause()) ? e.getCause().getMessage() : e.getMessage()));
            log.error(error, e);
            throw new FastStubsProcessingException(error);
        }
        return fastConfigurationResponse;
    }

    private void collectRequestFastConfigs(FastConfigurationRequest fastConfigurationRequest,
                                           List<FastConfigurationResponse.FastInfoConfig> fastInfoConfigs) {
        fastConfigurationRequest.getTransportConfigs().forEach(fastTransportConfig -> {
            StubEndpointConfig.TransportTypes transportType = fastTransportConfig.getTransportType();
            fastTransportConfig.getSystems().forEach(fastSystem -> {
                fastSystem.getOperations().forEach(fastOperation -> {
                    fastOperation.getSituations().forEach(fastSituation -> {
                        FastConfigurationResponse.FastInfoConfig fastInfoConfig =
                                new FastConfigurationResponse.FastInfoConfig();
                        fastInfoConfig.setTransportType(transportType);
                        fastInfoConfig.setSituationId(fastSituation.getId());
                        fastInfoConfig.setIsApplied(Boolean.FALSE);
                        fastInfoConfigs.add(fastInfoConfig);
                    });
                });
            });
        });
    }

    private void collectAppliedFastConfigs(StubEndpointConfig stubEndpointConfig,
                                           List<FastConfigurationResponse.FastInfoConfig> fastInfoConfigs) {
        stubEndpointConfig.getConditionalResponses()
                .forEach(responseDescription -> {
                    Optional<FastConfigurationResponse.FastInfoConfig> optionalFastInfoConfig =
                            fastInfoConfigs
                                    .stream()
                                    .filter(fastInfoConfig -> fastInfoConfig.getSituationId().equals(responseDescription.getId()))
                                    .findFirst();
                    if (optionalFastInfoConfig.isPresent()) {
                        FastConfigurationResponse.FastInfoConfig fastInfoConfig = optionalFastInfoConfig.get();
                        fastInfoConfig.setIsApplied(Boolean.TRUE);
                    }
                });

        ResponseDescription defaultResponse = stubEndpointConfig.getDefaultResponse();
        if (Objects.nonNull(defaultResponse)) {
            Optional<FastConfigurationResponse.FastInfoConfig> optionalFastInfoConfig =
                    fastInfoConfigs
                            .stream()
                            .filter(fastInfoConfig -> fastInfoConfig.getSituationId().equals(defaultResponse.getId()))
                            .findFirst();
            if (optionalFastInfoConfig.isPresent()) {
                FastConfigurationResponse.FastInfoConfig fastInfoConfig = optionalFastInfoConfig.get();
                fastInfoConfig.setIsApplied(Boolean.TRUE);
            }
        }
    }

    /*
     * In the initial implementation this was disabled

    private void collectDeactivatedFastConfigs(List<FastConfigurationResponse.FastInfoConfig> fastInfoConfigs) {

    }

    private EventTriggerStateResponse performEventTriggers(List<BigInteger> situationsIds, StubUser user) {
        List<BigInteger> triggersToDeactivate =
                CoreObjectManager.getInstance()
                        .getSpecialManager(OperationEventTrigger.class, OperationEventTriggerObjectManager.class)
                        .getActiveTriggersBySituationIdsNative(situationsIds);
        UUID sessionId = UUID.randomUUID();
        EventTriggerStateResponse eventTriggerStateResponse =
                eventTriggerActivationService.switchTriggerState(triggersToDeactivate,
                ControllerConstants.OPERATION_EVENT_TRIGGER_TYPE.getStringValue(), sessionId.toString(), user);
        return eventTriggerStateResponse;
    }
    */

    private void storeFileAndNotifyInstances(String fileName, String content, User user, UUID projectUuid)
            throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());

        ObjectId storedObjectId = externalDataManagementService.getExternalStorageService()
                .store(EdsContentType.FAST_STUB.getStringValue(), projectUuid, user.getName(),
                        Objects.isNull(user.getId()) ? null : UUID.fromString(user.getId()),
                        "", fileName, byteArrayInputStream);

        FileUploadHelper.checkStoredObjectIdAndSendMessageToExternalDataStorageUpdateTopic(storedObjectId,
                fileName, "", EdsContentType.FAST_STUB.getStringValue(), projectUuid,
                byteArrayInputStream, FileEventType.UPLOAD, executorToMessageBrokerSender, projectUuid.toString());

        byteArrayInputStream.close();
    }
}
