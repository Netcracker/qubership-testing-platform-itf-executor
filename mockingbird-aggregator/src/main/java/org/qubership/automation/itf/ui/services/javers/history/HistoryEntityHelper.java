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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.javers.history.HistoryCallChain;
import org.qubership.automation.itf.core.model.javers.history.HistoryIdentified;
import org.qubership.automation.itf.core.model.javers.history.HistoryIntegrationConfig;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperation;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationParsingRule;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationTemplate;
import org.qubership.automation.itf.core.model.javers.history.HistoryOutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.model.javers.history.HistorySituation;
import org.qubership.automation.itf.core.model.javers.history.HistorySituationEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryStep;
import org.qubership.automation.itf.core.model.javers.history.HistoryStubProject;
import org.qubership.automation.itf.core.model.javers.history.HistorySystemParsingRule;
import org.qubership.automation.itf.core.model.javers.history.HistorySystemTemplate;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HistoryEntityHelper {

    public static final Map<String, Class<? extends HistoryIdentified<?>>> ENTITIES_MODEL_MAPPER = new HashMap<>();
    private static final ModelMapper modelMapper = new ModelMapper();

    private static Converter<Storable, BigInteger> isIdConverter =
            (src) -> Objects.nonNull(src.getSource()) ? (BigInteger) src.getSource().getID() : null;

    private static Converter<Storable, String> storableIdConverterToString =
            (src) -> Objects.nonNull(src.getSource()) ? src.getSource().getID().toString() : StringUtils.EMPTY;

    private static Converter<Set<? extends Storable>, Set<BigInteger>> listStorableToListIdConverter =
            (src) -> src.getSource().stream()
                    .map((storable) -> (BigInteger) storable.getID()).collect(Collectors.toSet());

    private static Converter<List<Step>, List<HistoryStep>> isListStepConverter =
            (src) -> src.getSource().stream()
                    .map(step -> modelMapper.map(step, HistoryStep.class))
                    .collect(Collectors.toList());

    static {
        ENTITIES_MODEL_MAPPER.put(
                SystemParsingRule.class.getName(),
                HistorySystemParsingRule.class);
        ENTITIES_MODEL_MAPPER.put(
                OperationParsingRule.class.getName(),
                HistoryOperationParsingRule.class);
        ENTITIES_MODEL_MAPPER.put(
                OperationTemplate.class.getName(),
                HistoryOperationTemplate.class);
        ENTITIES_MODEL_MAPPER.put(
                SystemTemplate.class.getName(),
                HistorySystemTemplate.class);
        ENTITIES_MODEL_MAPPER.put(
                CallChain.class.getName(),
                HistoryCallChain.class);
        ENTITIES_MODEL_MAPPER.put(
                SituationStep.class.getName(),
                HistoryStep.class);
        ENTITIES_MODEL_MAPPER.put(
                EmbeddedStep.class.getName(),
                HistoryStep.class);
        ENTITIES_MODEL_MAPPER.put(
                StubProject.class.getName(),
                HistoryStubProject.class);
        ENTITIES_MODEL_MAPPER.put(
                IntegrationConfig.class.getName(),
                HistoryIntegrationConfig.class);
        ENTITIES_MODEL_MAPPER.put(
                Operation.class.getName(),
                HistoryOperation.class);
        ENTITIES_MODEL_MAPPER.put(
                Situation.class.getName(),
                HistorySituation.class);
        ENTITIES_MODEL_MAPPER.put(
                OperationEventTrigger.class.getName(),
                HistoryOperationEventTrigger.class);
        ENTITIES_MODEL_MAPPER.put(
                SituationEventTrigger.class.getName(),
                HistorySituationEventTrigger.class);
    }

    static {
        Configuration configuration = modelMapper.getConfiguration();
        configuration.setAmbiguityIgnored(true);
        configuration.setMatchingStrategy(MatchingStrategies.STRICT);

        modelMapper.createTypeMap(IntegrationConfig.class, HistoryIntegrationConfig.class);

        modelMapper.createTypeMap(OutboundTemplateTransportConfiguration.class,
                        HistoryOutboundTemplateTransportConfiguration.class)
                .addMappings(mapper -> mapper
                        .using(isIdConverter)
                        .map(OutboundTemplateTransportConfiguration::getParent,
                                (dist, v) -> dist.setParentId((BigInteger) (v))));

        modelMapper.createTypeMap(CallChain.class, HistoryCallChain.class)
                .addMappings(mapper -> mapper
                        .using(isListStepConverter).map(CallChain::getSteps, HistoryCallChain::setSteps))
                .addMappings(mapper -> mapper
                        .map(CallChain::getCompatibleDataSetListIds, HistoryCallChain::setCompatibleDataSetLists));

        modelMapper.createTypeMap(SituationStep.class, HistoryStep.class)
                .addMappings(mapper -> mapper.skip(HistoryStep::setChainId))

                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(SituationStep::getSituation, HistoryStep::setSituationId))
                .addMappings(mapper -> mapper
                        .using(listStorableToListIdConverter)
                        .map(SituationStep::getEndSituations, HistoryStep::setEndSituationIds))
                .addMappings(mapper -> mapper
                        .using(listStorableToListIdConverter)
                        .map(SituationStep::getExceptionalSituations, HistoryStep::setExceptionalSituationIds));

        modelMapper.createTypeMap(EmbeddedStep.class, HistoryStep.class)
                .addMappings(mapper -> mapper.skip(HistoryStep::setSituationId))
                .addMappings(mapper -> mapper.skip(HistoryStep::setEndSituationIds))
                .addMappings(mapper -> mapper.skip(HistoryStep::setExceptionalSituationIds))
                .addMappings(mapper -> mapper.skip(HistoryStep::setWaitAllEndSituations))
                .addMappings(mapper -> mapper.skip(HistoryStep::setRetryOnFail))
                .addMappings(mapper -> mapper.skip(HistoryStep::setRetryTimeout))
                .addMappings(mapper -> mapper.skip(HistoryStep::setRetryTimeoutUnit))
                .addMappings(mapper -> mapper.skip(HistoryStep::setValidationMaxAttempts))
                .addMappings(mapper -> mapper.skip(HistoryStep::setValidationMaxTime))
                .addMappings(mapper -> mapper.skip(HistoryStep::setValidationUnitMaxTime))

                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(EmbeddedStep::getChain, HistoryStep::setChainId));

        modelMapper.createTypeMap(Operation.class, HistoryOperation.class)
                .addMappings(mapper -> mapper.skip(HistoryOperation::setOperationParsingRules))
                .addMappings(mapper -> mapper.skip(HistoryOperation::setOperationTemplates))
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(Operation::getTransport, HistoryOperation::setTransportConfiguration));

        modelMapper.createTypeMap(Situation.class, HistorySituation.class)
                .addMappings(mapper -> mapper
                        .using(listStorableToListIdConverter)
                        .map(Situation::getParsingRules, HistorySituation::setParsingRulesIds))
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(situation -> situation.getIntegrationStep().getSystemTemplate(),
                                (d, v) -> d.getIntegrationStep().setSystemTemplateId((String) v)))
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(situation -> situation.getIntegrationStep().getOperationTemplate(),
                                (d, v) -> d.getIntegrationStep().setOperationTemplateId((String) v)))
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(situation -> situation.getIntegrationStep().getReceiver(),
                                (d, v) -> d.getIntegrationStep().setReceiverId((String) v)));

        modelMapper.createTypeMap(OperationEventTrigger.class, HistoryOperationEventTrigger.class)
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(OperationEventTrigger::getParent, HistoryOperationEventTrigger::setParentId));

        modelMapper.createTypeMap(SituationEventTrigger.class, HistorySituationEventTrigger.class)
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(SituationEventTrigger::getParent, HistorySituationEventTrigger::setParentId))
                .addMappings(mapper -> mapper
                        .using(storableIdConverterToString)
                        .map(SituationEventTrigger::getSituation, HistorySituationEventTrigger::setSituationId));
    }

    public static Optional<Object> fromStorable(Storable storable) {
        Class entityClass = ENTITIES_MODEL_MAPPER.get(storable.getClass().getName());
        return Optional.of(modelMapper.map(storable, entityClass));
    }

    public static Class getHistoryEntityClass(Class<? extends Storable> itemType) {
        return ENTITIES_MODEL_MAPPER.get(itemType.getName());
    }

    public static boolean isNotSupportEntity(Class itemType) {
        return Objects.isNull(ENTITIES_MODEL_MAPPER.get(itemType.getName()));
    }

    public static void isSupportEntityByType(Class itemType) {
        if (isNotSupportEntity(itemType)) {
            String message = String.format("Entity with type %s is skip, because not supported in itf history.",
                    itemType.getName());
            log.warn(message);
            throw new HistoryRetrieveException(message);
        }
    }
}
