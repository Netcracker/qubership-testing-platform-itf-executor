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

package org.qubership.automation.itf.ui.services.javers.history;

import static org.qubership.automation.itf.core.util.converter.IdConverter.toBigInt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.json.simple.JSONObject;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.EventTriggerBriefInfo;
import org.qubership.automation.itf.core.model.javers.history.HistoryAbstractEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryCallChain;
import org.qubership.automation.itf.core.model.javers.history.HistoryIdentified;
import org.qubership.automation.itf.core.model.javers.history.HistoryIntegrationConfig;
import org.qubership.automation.itf.core.model.javers.history.HistoryIntegrationStep;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperation;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationTemplate;
import org.qubership.automation.itf.core.model.javers.history.HistoryOutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.model.javers.history.HistorySituation;
import org.qubership.automation.itf.core.model.javers.history.HistorySituationEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryStep;
import org.qubership.automation.itf.core.model.javers.history.HistorySystemTemplate;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.executor.service.SecurityHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Throwables;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryRestoreService {

    private static final ModelMapper modelMapper = new ModelMapper();

    static {
        modelMapper.typeMap(HistoryIntegrationConfig.class, IntegrationConfig.class)
                .addMappings(mapper -> mapper.skip(Configuration::setTypeName))
                .addMapping(HistoryIntegrationConfig::getConfiguration, Configuration::fillConfiguration);
        modelMapper.typeMap(HistoryOperationTemplate.class, OperationTemplate.class)
                .addMappings(mapper -> mapper.skip(OperationTemplate::setLabels))
                .addMappings(mapper -> mapper.skip(OperationTemplate::setTransportProperties))
                .addMapping(HistoryOperationTemplate::getLabels, OperationTemplate::fillLabels)
                .addMapping(HistoryOperationTemplate::getTransportProperties,
                        OperationTemplate::fillTransportProperties);

        modelMapper.typeMap(HistorySystemTemplate.class, SystemTemplate.class)
                .addMappings(mapper -> mapper.skip(SystemTemplate::setID))
                .addMappings(mapper -> mapper.skip(SystemTemplate::setLabels))
                .addMappings(mapper -> mapper.skip(SystemTemplate::setTransportProperties))
                .addMapping(HistorySystemTemplate::getLabels, SystemTemplate::fillLabels)
                .addMapping(HistorySystemTemplate::getTransportProperties, SystemTemplate::fillTransportProperties);

        modelMapper.emptyTypeMap(HistoryOutboundTemplateTransportConfiguration.class,
                        OutboundTemplateTransportConfiguration.class)
                .addMapping(HistoryOutboundTemplateTransportConfiguration::getConfiguration,
                        OutboundTemplateTransportConfiguration::fillConfiguration)
                .addMapping(HistoryOutboundTemplateTransportConfiguration::getTypeName,
                        OutboundTemplateTransportConfiguration::setTypeName)
                .addMappings(
                        mapper -> mapper.using(getTemplateByIdConverter())
                                .map(HistoryOutboundTemplateTransportConfiguration::getParentId,
                                        OutboundTemplateTransportConfiguration::setParent));

        modelMapper.typeMap(HistoryCallChain.class, CallChain.class)
                .addMappings(mapper -> mapper.skip(CallChain::setID))
                .addMappings(mapper -> mapper.skip(CallChain::setProjectId))
                .addMappings(mapper -> mapper.skip(CallChain::setLabels))

                .addMapping(HistoryCallChain::getKeys, CallChain::fillKeys)
                .addMapping(HistoryCallChain::getLabels, CallChain::fillLabels)

                .addMappings(mapper -> mapper
                        .map(HistoryCallChain::getCompatibleDataSetLists, CallChain::setCompatibleDataSetListIds));

        modelMapper.emptyTypeMap(HistoryStep.class, SituationStep.class)
                .addMappings(mapper -> mapper.skip(SituationStep::setID))
                .addMappings(mapper -> mapper.skip(SituationStep::setProjectId))
                .addMappings(mapper -> mapper.skip(SituationStep::setOrder))

                .addMappings(mapper -> mapper
                        .using(referenceConverter(Situation.class))
                        .map(HistoryStep::getSituationId, SituationStep::setSituation))
                .addMappings(mapper -> mapper
                        .using(referenceCollectionConverter(Situation.class))
                        .map(HistoryStep::getEndSituationIds, SituationStep::setEndSituations))
                .addMappings(mapper -> mapper
                        .using(referenceCollectionConverter(Situation.class))
                        .map(HistoryStep::getExceptionalSituationIds, SituationStep::setExceptionalSituations))
                .implicitMappings();

        modelMapper.emptyTypeMap(HistoryStep.class, EmbeddedStep.class)
                .addMappings(mapper -> mapper.skip(EmbeddedStep::setID))
                .addMappings(mapper -> mapper.skip(EmbeddedStep::setProjectId))
                .addMappings(mapper -> mapper.skip(EmbeddedStep::setOrder))

                .addMappings(mapper -> mapper
                        .using(referenceConverter(CallChain.class))
                        .map(HistoryStep::getChainId, EmbeddedStep::setChain))
                .implicitMappings();

        modelMapper.emptyTypeMap(HistoryOperation.class, Operation.class)
                .addMappings(mapper -> mapper.skip(Operation::setID))
                .addMappings(mapper -> mapper.skip(Operation::setProjectId))
                .addMappings(mapper -> mapper.skip(Operation::setOperationParsingRules))
                .addMappings(mapper -> mapper.skip(Operation::setOperationTemplates))
                .addMappings(mapper -> mapper.skip(Operation::setSituations))
                .addMappings(mapper -> mapper.skip(Operation::setTransport))
                .addMappings(mapper -> mapper.using(referenceConverter(TransportConfiguration.class))
                        .map(HistoryOperation::getTransportConfiguration, Operation::setTransport))
                .implicitMappings();

        modelMapper.emptyTypeMap(HistorySituation.class, Situation.class)
                .addMappings(mapper -> mapper.skip(Situation::setLabels))
                .addMappings(mapper -> mapper.skip(Situation::setOperationEventTriggers))
                .addMappings(mapper -> mapper.skip(Situation::setSituationEventTriggers))
                .addMappings(mapper -> mapper.skip(Situation::setParsingRules))
                .addMappings(mapper -> mapper.skip((d, v) -> d.setParent((Operation) v)))
                .addMappings(mapper -> mapper.skip((d, v) -> d.setParent((Storable) v)))
                .addMapping(HistorySituation::getLabels, Situation::fillLabels)
                .addMappings(mapper -> mapper
                        .using(referenceListConverter(OperationParsingRule.class))
                        .map(HistorySituation::getParsingRulesIds, Situation::setParsingRules))
                .addMappings(mapper -> mapper
                        .using(integrationStepConverter())
                        .map(src -> src, Situation::fillSteps))
                .implicitMappings();

        modelMapper.emptyTypeMap(HistoryIntegrationStep.class, IntegrationStep.class)
                .addMappings(mapper -> mapper.skip(IntegrationStep::setID))
                .addMappings(mapper -> mapper.skip(IntegrationStep::setProjectId))
                .addMappings(mapper -> mapper.skip(IntegrationStep::setSystemTemplate))
                .addMappings(mapper -> mapper.skip(IntegrationStep::setOperationTemplate))
                .addMappings(mapper -> mapper.skip(IntegrationStep::setReceiver))
                .addMappings(mapper -> mapper.skip(IntegrationStep::setOperation))
                .addMappings(mapper -> mapper
                        .using(referenceToTemplateConverter())
                        .map(HistoryIntegrationStep::getOperationTemplateId, IntegrationStep::setOperationTemplate))
                .addMappings(mapper -> mapper
                        .using(referenceToTemplateConverter())
                        .map(HistoryIntegrationStep::getSystemTemplateId, IntegrationStep::setSystemTemplate))
                .addMappings(mapper -> mapper
                        .using(referenceConverter(System.class))
                        .map(HistoryIntegrationStep::getReceiverId, IntegrationStep::setReceiver))
                .implicitMappings();

        modelMapper.emptyTypeMap(HistoryOperationEventTrigger.class, OperationEventTrigger.class)
                .addMappings(mapper -> mapper.skip((d, v) -> d.setParent((Situation) v)))
                .addMappings(mapper -> mapper.skip(OperationEventTrigger::setConditionParameters))
                .addMappings(
                        mapper -> mapper.using(referenceConverter(Situation.class))
                                .map(HistoryOperationEventTrigger::getParentId,
                                        (d, v) -> d.setParent((Situation) v)))
                .addMapping(
                        HistoryOperationEventTrigger::getConditionParameters,
                        OperationEventTrigger::fillConditionParameters)
                .addMapping(HistoryOperationEventTrigger::getState, OperationEventTrigger::setState);

        modelMapper.emptyTypeMap(HistorySituationEventTrigger.class, SituationEventTrigger.class)
                .addMappings(mapper -> mapper.skip((d, v) -> d.setParent((Situation) v)))
                .addMappings(
                        mapper -> mapper.using(referenceConverter(Situation.class))
                                .map(HistorySituationEventTrigger::getParentId,
                                        (d, v) -> d.setParent((Situation) v)))
                .addMapping(
                        HistorySituationEventTrigger::getConditionParameters,
                        SituationEventTrigger::fillConditionParameters)
                .addMappings(
                        mapper -> mapper.using(referenceConverter(Situation.class))
                                .map(HistorySituationEventTrigger::getSituationId,
                                        SituationEventTrigger::setSituation))
                .addMapping(HistorySituationEventTrigger::getState, SituationEventTrigger::setState)
                .addMapping(HistorySituationEventTrigger::getOn, (d, v) -> d.setOn((SituationEventTrigger.On) v));
    }

    private final Javers javers;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final ProjectSettingsService projectSettingsService;

    private static Converter<Set<BigInteger>, Set<? extends Storable>> referenceCollectionConverter(Class<?
            extends Storable> clazz) {
        return mappingContext -> mappingContext.getSource().stream()
                .map(objectId -> {
                    Storable storable = CoreObjectManager.getInstance().getManager(clazz).getById(objectId);
                    if (Objects.isNull(storable)) {
                        throw new ObjectNotFoundException(objectId,
                                mappingContext.getDestinationType().getSimpleName());
                    }
                    return storable;
                })
                .collect(Collectors.toSet());
    }

    private static Converter<Set<Object>, Set<? extends Storable>> referenceListConverter(Class<? extends Storable> clazz) {
        return mappingContext -> mappingContext.getSource().stream().map(
                        id -> getReference(clazz, String.valueOf(id),
                                (storableId) -> CoreObjectManager.getInstance().getManager(clazz).getById(storableId)))
                .collect(Collectors.toSet());
    }

    private static Converter<HistorySituation, List<? extends Step>> integrationStepConverter() {
        return context -> {
            HistoryIntegrationStep historyIntegrationStep = context.getSource().getIntegrationStep();
            Situation situation = CoreObjectManager.getInstance().getManager(Situation.class)
                    .getById(context.getSource().getId());
            if (historyIntegrationStep != null) {
                IntegrationStep integrationStep = situation.getIntegrationStep();
                if (integrationStep == null) {
                    throw new RuntimeException(String.format(
                            "Situation %s [id=%s] has no integration step. Object cannot be restored.",
                            situation.getName(),
                            situation.getID()));
                }
                modelMapper.map(historyIntegrationStep, integrationStep);
            }
            return situation.getSteps();
        };
    }

    private static Converter<String, ? extends Storable> referenceConverter(Class<? extends Storable> clazz) {
        return mappingContext -> getReference(clazz, String.valueOf(mappingContext.getSource()),
                (storableId) -> CoreObjectManager.getInstance().getManager(clazz).getById(storableId));
    }

    private static Converter<String, ? extends Storable> referenceToTemplateConverter() {
        return mappingContext -> getReference(Template.class, String.valueOf(mappingContext.getSource()),
                TemplateHelper::getById);
    }

    private static Storable getReference(Class<? extends Storable> clazz, String id,
                                         Function<String, Storable> getStorable) {
        Storable reference = null;
        if (StringUtils.isNotBlank(id)) {
            reference = getStorable.apply(id);
            if (reference == null) {
                throw new ObjectNotFoundException(id, clazz.getSimpleName());
            }
        }
        return reference;
    }

    private static Converter<Set<? extends HistoryAbstractEventTrigger>, Set<? extends Storable>> eventTriggersConverter() {
        return mappingContext -> {
            if (!mappingContext.getSource().isEmpty() && !mappingContext.getDestination().isEmpty()) {
                if (mappingContext.getSource().size() != mappingContext.getDestination().size()
                        || !mappingContext.getSource()
                        .stream()
                        .map(s -> ((HistoryIdentified<?>) s).getId()).collect(Collectors.toList())
                        .containsAll(mappingContext.getDestination().stream()
                                .map(d -> d.getID()).collect(Collectors.toList()))) {
                    throw new RuntimeException("Cannot restore object because it has different set of triggers.");
                }
                modelMapper.map(
                        mappingContext.getSource().iterator().next(),
                        mappingContext.getDestination().iterator().next());
            }
            return mappingContext.getDestination();
        };
    }

    private static Converter<BigInteger, ? extends Storable> getTemplateByIdConverter() {
        return mappingContext -> Objects.nonNull(mappingContext.getSource())
                ? TemplateHelper.getById(mappingContext.getSource()) : null;
    }

    @Transactional
    public LinkedList<Runnable> restoreToRevision(BigInteger objectId,
                                                  Class<? extends Storable> itemType,
                                                  Long revisionId,
                                                  UUID projectUuid) {
        try {
            if (HistoryEntityHelper.isNotSupportEntity(itemType)) {
                String message = String.format("Entity with type %s is skip, because not supported in itf history.",
                        itemType.getName());
                log.warn(message);
                throw new HistoryRestoreException(message);
            }
            Class historyEntityClass = HistoryEntityHelper.getHistoryEntityClass(itemType);
            Optional<Shadow<Object>> optionalShadow = getShadow(objectId, historyEntityClass, revisionId);
            Object shadowObject;
            if (!optionalShadow.isPresent()) {
                String errorMessage = String.format("Failed to found shadow with id:%s, class:%s", objectId,
                        itemType.getName());
                log.error(errorMessage);
                throw new EntityNotFoundException(errorMessage);
            }
            shadowObject = optionalShadow.get().get();

            if (Objects.isNull(shadowObject)) {
                String errorMessage = String.format("Failed to found shadow with id:%s, class:%s", objectId,
                        itemType.getName());
                log.error(errorMessage);
                throw new EntityNotFoundException(errorMessage);
            }
            Object shadowObjectId = ((HistoryIdentified) shadowObject).getId();
            Storable storable = CoreObjectManager.getInstance().getManager(itemType).getById(shadowObjectId);
            copyValues(shadowObject, storable);
            postActionsToStorable(shadowObject, storable);
            return doAfter(storable, projectUuid);
        } catch (Exception e) {
            if (e instanceof HistoryRestoreException) {
                throw e;
            } else {
                throw new HistoryRestoreException(Throwables.getRootCause(e).getMessage(),
                        String.valueOf(objectId), itemType.getName());
            }
        }
    }

    private LinkedList<Runnable> doAfter(Storable storable, UUID projectUuid) {
        LinkedList<Runnable> runnableList = new LinkedList<>();
        runnableList.add(storable::store);
        if (storable instanceof SituationEventTrigger) {
            runnableList.add(() ->
                    getMessageToSynchronizeSituationEventTriggers((SituationEventTrigger) storable, projectUuid)
            );
        }
        if (storable instanceof Situation) {
            Optional<OperationEventTrigger> trigger =
                    ((Situation) storable).getOperationEventTriggers().stream().findFirst();
            if (trigger.isPresent() && trigger.get().getState().isOn()) {
                runnableList.add(() ->
                        getMessageToActivateOperationEventTrigger(trigger.get(), projectUuid)
                );
            }
        }
        return runnableList;
    }

    private void getMessageToSynchronizeSituationEventTriggers(SituationEventTrigger situationEventTrigger,
                                                               UUID projectUuid) {
        JSONObject triggerSyncRequest = new JSONObject();
        triggerSyncRequest.put("user", "");
        triggerSyncRequest.put("sessionId", "");
        triggerSyncRequest.put("type", "afterRestoreSituationEventTrigger");
        String action = situationEventTrigger.getState().isOn() ? "triggersToReactivate" : "triggersToDeactivate";
        ArrayList<EventTriggerBriefInfo> triggersList = new ArrayList<>();
        EventTriggerBriefInfo triggerBriefInfo = new EventTriggerBriefInfo(
                toBigInt(situationEventTrigger.getID()),
                situationEventTrigger.getType()
        );
        triggersList.add(triggerBriefInfo);
        triggerSyncRequest.put(action, triggersList);
        sendMessageForEventTriggersActivation(triggerSyncRequest, projectUuid, situationEventTrigger);
    }

    private void getMessageToActivateOperationEventTrigger(OperationEventTrigger operationEventTrigger,
                                                           UUID projectUuid) {
        JSONObject triggerActivationRequest = new JSONObject();
        triggerActivationRequest.put("user", "");
        triggerActivationRequest.put("sessionId", "");
        triggerActivationRequest.put("type", "afterRestoreOperationEventTrigger");
        EventTriggerBriefInfo triggerBriefInfo = new EventTriggerBriefInfo(
                toBigInt(operationEventTrigger.getID()),
                operationEventTrigger.getType()
        );
        triggerActivationRequest.put("trigger", triggerBriefInfo);
        sendMessageForEventTriggersActivation(triggerActivationRequest, projectUuid, operationEventTrigger);

    }

    private void sendMessageForEventTriggersActivation(JSONObject triggerActivationRequest,
                                                       UUID projectUuid,
                                                       EventTrigger trigger) {
        try {
            SecurityHelper.addAuthContextToMessage(triggerActivationRequest);
            executorToMessageBrokerSender.sendMessage(
                    triggerActivationRequest,
                    "message-broker.configurator-executor-event-triggers.topic",
                    "topic",
                    projectUuid.toString()
            );
        } catch (Exception e) {
            throw new HistoryRestoreException(
                    "Trigger was restored but reactivation failed with error: \n"
                            + Throwables.getRootCause(e).getMessage(), String.valueOf(trigger.getID()),
                    trigger.getClass().getName());
        }
    }

    private void postActionsToStorable(Object shadowObject, Storable storable) {
        if (storable instanceof CallChain) {
            HistoryCallChain historyCallChain = (HistoryCallChain) shadowObject;
            List<HistoryStep> historySteps = historyCallChain.getSteps();

            if (!historySteps.isEmpty()) {
                CallChain callChain = (CallChain) storable;
                List<Step> steps = callChain.getSteps();
                if (historySteps.size() == steps.size()) {

                    List<BigInteger> historyStepIds =
                            historySteps.stream().map(historyStep -> historyStep.getId()).collect(Collectors.toList());
                    List<BigInteger> stepIds =
                            steps.stream().map(step -> (BigInteger) step.getID()).collect(Collectors.toList());

                    List<Step> restoreStepsByOrder = new ArrayList<>();
                    if (historyStepIds.containsAll(stepIds)) {
                        for (HistoryStep historyStep : historySteps) {
                            for (Step step : steps) {
                                if (historyStep.getId().equals(step.getID())) {
                                    step.setOrder(historyStep.getOrder());
                                    restoreStepsByOrder.add(step);
                                }
                            }
                        }
                    }
                    callChain.fillSteps(restoreStepsByOrder);
                }
            }
        }
        if (storable instanceof Operation) {
            Operation operation = (Operation) storable;
            if (operation.getMep().isInboundRequest()) {
                HistoryOperation historyOperation = (HistoryOperation) shadowObject;
                Set<Situation> situations = operation.getSituations();
                List<HistorySituation> historySituations = historyOperation.getSituations()
                        .stream()
                        .filter(historySituation -> historySituation.getOperationEventTriggers()
                                .stream().findFirst().isPresent())
                        .collect(Collectors.toList());
                Map<String, Integer> situationIdAndPriority = historySituations
                        .stream()
                        .filter(historySituation -> historySituation.getOperationEventTriggers()
                                .stream().findFirst().isPresent())
                        .collect(Collectors.toMap(
                                historySituation -> String.valueOf(historySituation.getId()),
                                historySituation -> historySituation.getOperationEventTriggers().stream().findFirst()
                                        .map(HistoryOperationEventTrigger::getPriority).get()
                        ));
                if (situations.size() == historySituations.size() &&
                        historySituations.stream().allMatch(historySituation ->
                                situationIdAndPriority.containsKey(String.valueOf(historySituation.getId())))) {
                    situations.forEach(situation -> situation.getOperationEventTriggers()
                            .stream()
                            .findFirst()
                            .ifPresent(
                                    trigger -> trigger.setPriority(
                                            situationIdAndPriority.get(String.valueOf(situation.getID())))));
                }
            }
        }
        if (storable instanceof StubProject) {
            projectSettingsService.fillCache((StubProject) storable, storable.getStorableProp());
        }
    }

    private void copyValues(Object shadow, @NonNull Storable actualObject) {
        modelMapper.map(shadow, actualObject);
    }

    private Optional<Shadow<Object>> getShadow(BigInteger objectId, Class clazz, Long version) {
        JqlQuery query = QueryBuilder.byInstanceId(objectId, clazz)
                .withVersion(version)
                .withScopeDeepPlus()
                .build();
        List<CdoSnapshot> snapshots = javers.findSnapshots(query);
        QueryBuilder queryBuilder = QueryBuilder.byInstanceId(objectId, clazz).withVersion(version).withScopeDeepPlus();
        if (Objects.nonNull(snapshots) && !snapshots.isEmpty()) {
            queryBuilder.withCommitId(snapshots.get(0).getCommitId());
        }
        List<Shadow<Object>> shadows = javers.findShadows(queryBuilder.build());
        return shadows.stream().findFirst();
    }
}
