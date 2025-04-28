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

package org.qubership.automation.itf.ui.controls.eci;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvConfigurationManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.convert.ConverterFactory;
import org.qubership.automation.itf.environments.object.ECEntity;
import org.qubership.automation.itf.environments.object.impl.ECConnection;
import org.qubership.automation.itf.environments.object.impl.ECEnvironment;
import org.qubership.automation.itf.environments.object.impl.ECSystem;
import org.qubership.automation.itf.environments.parse.ParserFactory;
import org.qubership.automation.itf.environments.util.validation.ECIErrorsCache;
import org.qubership.automation.itf.environments.util.validation.ECIValidationError;
import org.qubership.automation.itf.environments.util.validation.ValidationLevel;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/eci")
public class EciController {

    private final Map<UUID, List<ECIValidationError>> errorsCache =
            ECIErrorsCache.getInstance().getErrorsCache().asMap();

    /**
     * Get project ids from atp-environment.
     *
     * @param projectUuid project UUID
     * @return ids collection
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get ATP-Environments Project Id bound")
    public Set<String> getEcProjectId(@RequestParam("projectId") BigInteger projectId,
                                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        Set<String> projects = new HashSet<>();
        projects.addAll(((EnvConfigurationManager<Environment>) CoreObjectManager.getInstance()
                .getManager(Environment.class)).getEcProjectIds(projectId));
        projects.addAll(((EnvConfigurationManager<System>) CoreObjectManager.getInstance()
                .getManager(System.class)).getEcProjectIds(projectId));
        projects.addAll(((EnvConfigurationManager<TransportConfiguration>) CoreObjectManager.getInstance()
                .getManager(TransportConfiguration.class)).getEcProjectIds(projectId));
        return projects;
    }

    /**
     * Binding entity from service atp-environments.
     *
     * @param entityId    object id
     * @param entityClass class for an entity
     * @param ecId        string format of id an entity from service atp-environments
     * @param ecName      string format of name an entity from service atp-environments
     * @param ecProjectId string format of project id from service atp-environments
     * @param projectUuid project UUID
     * @return new object in UIResult style
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/bind", method = RequestMethod.PUT)
    @AuditAction(auditAction =
            "Bind Entity with id {{#entityId}} to ATP-Environments Object id {{#ecId}} from project {{#ecProjectId}}")
    public UIResult bindEntityToEC(@RequestParam("id") BigInteger entityId,
                                   @RequestParam("class") EciEntityConstant entityClass,
                                   @RequestParam("ecId") String ecId,
                                   @RequestParam("ecName") String ecName,
                                   @RequestParam("ecProjectId") String ecProjectId,
                                   @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            Class<? extends EciConfigurable> asClass = entityClass.getEntityClass();
            EciConfigurable storable = CoreObjectManager.getInstance().getManager(asClass).getById(entityId);
            storable.setEciParameters(ecId, ecProjectId);
            if (storable instanceof System) {
                ((System) storable).setEcLabel(ecName);
            }
            storable.store();
            log.info("Storable [id={}, name={}, type={}] was bound to EC[id={}, project={}]", storable.getID(),
                    storable.getName(), entityClass, ecId, ecProjectId);
            return new UIResult(true, String.format("%s was bound to EC", asClass.getSimpleName()));
        } catch (Exception e) {
            log.error("Storable [id={}, type={}] was not bound to EC[id={}, project={}]. Exception: {}", entityId,
                    entityClass, ecId, ecProjectId, e);
            return new UIResult(false,
                    String.format("%s wasn't bound to EC", entityClass.getEntityClass().getSimpleName()));
        }
    }

    /**
     * Update entity from service atp-environments.
     *
     * @param entityId        object id
     * @param entityParentId  parent object unique identifier
     * @param entityClassName class for an entity
     * @param entity          string format of an entity
     * @param projectUuid     project UUID
     * @return new object in UIResult style
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    @AuditAction(auditAction
            = "Update Entity with id {{#entityId}} from EC (type {{#entityClassName}}, parent id {{#entityParentId}})")
    public UIResult updateEntityFromEc(@RequestParam("entityId") BigInteger entityId,
                                       @RequestParam("parentId") BigInteger entityParentId,
                                       @RequestParam("className") EciEntityConstant entityClassName,
                                       @RequestBody(required = false) String entity,
                                       @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            Class<? extends EciConfigurable> entityClass = entityClassName.getEntityClass();
            ECEntity<? extends EciConfigurable> eciConfigurable = ParserFactory.getParser(entityClass).parse(entity);
            if (!StringUtils.isEmpty(eciConfigurable.getEcId())) {
                Storable parent = entityClass.isAssignableFrom(TransportConfiguration.class)
                        ? CoreObjectManager.managerFor(System.class).getById(entityParentId) :
                        CoreObjectManager.managerFor(Folder.class).getById(entityParentId);
                convertAndStoreEntity(eciConfigurable, entityClass, parent);
                return new UIResult(true, String.format("%s was updated from EC", entityClass.getSimpleName()));
            } else {
                EciConfigurable storable = CoreObjectManager.managerFor(entityClass).getById(entityId);
                storable.unbindEntityWithHierarchy();
                storable.store();
                log.warn("Storable [id={}, name={}, type={}] was unbound from EC during update", storable.getID(),
                        storable.getName(), entityClassName);
                UIResult warn = new UIResult();
                warn.setSuccess(false);
                warn.setMessage(String.format("%s was unbound from EC during update", entityClass.getSimpleName()));
                return warn;
            }
        } catch (Exception e) {
            log.error("Storable [id={}, type={}] wasn't updated from EC. Exception: {}", entityId, entityClassName, e);
            return new UIResult(false, String.format("%s wasn't updated from EC",
                    entityClassName.getEntityClass().getSimpleName()));
        }
    }

    /**
     * Unbinding entity from service atp-environments.
     *
     * @param objects         array of objects to bind
     * @param entityClassName class for an entity
     * @param projectUuid     project UUID
     * @return new object in UIResult style
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/unbind", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Unbind Entities with type {{#entityClass}} from EC")
    public UIResult unbindEntityFromEC(@RequestBody UIIdentifiedObject[] objects,
                                       @RequestParam("entityClass") EciEntityConstant entityClassName,
                                       @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            Class<? extends EciConfigurable> entityClass = entityClassName.getEntityClass();
            for (UIIdentifiedObject object : objects) {
                EciConfigurable storable = CoreObjectManager.getInstance()
                        .getManager(entityClass).getById(object.getId());
                log.info("Storable [id={}, name={}, type={}] was unbound from EC[id={}, project={}]",
                        storable.getID(), storable.getName(), entityClass, storable.getEcId(),
                        storable.getEcProjectId());
                storable.unbindEntityWithHierarchy();
                storable.store();
            }
            return new UIResult(true, String.format("%s was unbound from EC", entityClass.getSimpleName()));
        } catch (Exception e) {
            log.error("Unbinding was failed. Exception: {}", e.getMessage());
            return new UIResult(false,
                    String.format("%s wasn't unbound from EC", entityClassName.getEntityClass().getSimpleName()));
        }
    }

    /**
     * Upload environments from service atp-environments.
     *
     * @param projectId      ITF project id
     * @param projectUuid    project UUID
     * @param ecEnvironments array environment models
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/upload/environments", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Upload Environments from EC from project {{#projectId}}/{{#projectUuid}}")
    public void uploadEnvironmentsFromEc(@RequestParam BigInteger projectId,
                                         @RequestParam(value = "projectUuid") UUID projectUuid,
                                         @RequestBody ECEnvironment[] ecEnvironments) {
        Folder<Environment> environmentFolder = CoreObjectManager.getInstance().getManager(StubProject.class)
                .getById(projectId).getEnvironments();
        for (ECEnvironment ecEnvironment : ecEnvironments) {
            removeSystemWithoutItfParameters(ecEnvironment);
            fillEcProjectIdHierarchically(ecEnvironment);
            convertAndStoreEntity(ecEnvironment, ecEnvironment.getGenericType(), environmentFolder);
        }
    }

    private void removeSystemWithoutItfParameters(ECEnvironment ecEnvironment) {
        List<ECSystem> systems = ecEnvironment.getSystems();
        if (systems != null) {
            String systemNames = StringUtils.EMPTY;
            systems.removeIf(ecSystem -> ecSystem.getServer() == null
                    || StringUtils.EMPTY.equals(ecSystem.getServer().getName())
                    || StringUtils.EMPTY.equals(ecSystem.getServer().getUrl()));
            for (ECSystem ecSystem : systems) {
                systemNames += ecSystem.getName() + ",";
            }
            if (!StringUtils.EMPTY.equals(systemNames)) {
                log.info("System(s) [{}] to be uploaded for the environment [name={}] from EC", systemNames,
                        ecEnvironment.getName());
            } else {
                log.info("There is no systems to upload for the environment [name={}] from EC",
                        ecEnvironment.getName());
            }
        } else {
            log.info("There is no systems under environment [name={}] from EC", ecEnvironment.getName());
        }
    }

    /**
     * Update environments from environment configurator.
     *
     * @param ecEnvironments environment from environment configurator
     * @param projectUuid    project UUID
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/update/environments", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Environments from EC")
    public void updateEnvironmentsFromEc(@RequestBody ECEnvironment[] ecEnvironments,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        updateEntitiesFromEc(ecEnvironments);
    }

    /**
     * Update systems from environment configurator.
     *
     * @param ecSystems   systems from environment configurator
     * @param projectUuid project UUID
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/update/systems", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Systems from EC")
    public void updateSystemsFromEc(@RequestBody ECSystem[] ecSystems,
                                    @RequestParam(value = "projectUuid") UUID projectUuid) {
        updateEntitiesFromEc(ecSystems);
    }

    /**
     * Upload transports from environment configurator.
     *
     * @param ecConnections connections from environment configurator
     * @param projectUuid   project UUID
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/update/transports", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Transports from EC")
    public void uploadTransportsFromEc(@RequestBody ECConnection[] ecConnections,
                                       @RequestParam(value = "projectUuid") UUID projectUuid) {
        updateEntitiesFromEc(ecConnections);
    }

    private void updateEntitiesFromEc(ECEntity<? extends EciConfigurable>[] ecEntities) {
        for (ECEntity<? extends EciConfigurable> ecEntity : ecEntities) {
            if (StringUtils.isEmpty(ecEntity.getEcId())) {
                CoreObjectManager.getInstance().getManager(ecEntity.getGenericType())
                        .getById(ecEntity.getSourceEntityId()).unbindEntityWithHierarchy();
                log.info("Storable [id={}, type={}] has become unbind from EC during updating",
                        ecEntity.getSourceEntityId(), ecEntity.getGenericType());
            } else {
                if (ecEntity instanceof ECEnvironment) {
                    removeSystemWithoutItfParameters((ECEnvironment) ecEntity);
                    fillEcProjectIdHierarchically((ECEnvironment) ecEntity);
                }
                convertAndStoreEntity(ecEntity, ecEntity.getGenericType(),
                        CoreObjectManager.getInstance().getManager(ecEntity.getParentClass())
                                .getById(ecEntity.getSourceEntityParentId()));
            }
        }
    }

    private void convertAndStoreEntity(ECEntity<? extends EciConfigurable> ecEntity,
                                       Class<? extends EciConfigurable> entityClass, Storable parent) {
        UUID eciSessionId = UUID.randomUUID();
        EciConfigurable convert = ConverterFactory.getConverter(entityClass).convert(ecEntity, parent, eciSessionId);
        if (convert != null) {
            convert.store();
        }
        logUploadingResult(ecEntity.getName(), eciSessionId);
    }

    /**
     * Update entity from service atp-environments by project.
     *
     * @param ecProjectId project id from atp-environment
     * @param projectUuid project UUID
     * @return {@link UpdateEntitiesByEcProjectStructure} updated entitys.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @GetMapping(value = "/get/byproject/{ecProjectId}")
    @AuditAction(auditAction = "Update Entities By EcProject")
    public UpdateEntitiesByEcProjectStructure updateEntitiesByEcProject(
            @PathVariable(value = "ecProjectId") String ecProjectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<Environment> environments =
                ((EnvConfigurationManager<Environment>) CoreObjectManager.getInstance()
                        .getManager(Environment.class)).getByEcProjectId(ecProjectId);
        Collection<System> systems = ((EnvConfigurationManager<System>) CoreObjectManager.getInstance()
                .getManager(System.class)).getByEcProjectId(ecProjectId);
        Collection<TransportConfiguration> transports =
                ((EnvConfigurationManager<TransportConfiguration>) CoreObjectManager.getInstance()
                        .getManager(TransportConfiguration.class)).getByEcProjectId(ecProjectId);
        List<System> systemsInEnvs = Lists.newArrayList();
        List<TransportConfiguration> transportInSystem = Lists.newArrayList();
        for (Environment environment : environments) {
            for (System system : systems) {
                if (environment.getInbound().get(system) != null || environment.getOutbound().get(system) != null) {
                    systemsInEnvs.add(system);
                }
                Set<TransportConfiguration> systemTransports = system.getTransports();
                for (TransportConfiguration transport : transports) {
                    if (systemTransports.stream().filter(systemTransport -> systemTransport.getID()
                            .equals(transport.getID())).findAny().orElse(null) != null) {
                        transportInSystem.add(transport);
                    }
                }
                if (!transportInSystem.isEmpty()) {
                    transports.removeAll(transportInSystem);
                    transportInSystem.clear();
                }
            }
            if (!systemsInEnvs.isEmpty()) {
                systems.removeAll(systemsInEnvs);
                systemsInEnvs.clear();
            }
        }
        Map<String, Set<EciRequestObject>> entitiesForUpdate = Maps.newHashMap();
        entitiesForUpdate.put("environments", environments.stream().map(EciRequestObject::new)
                .collect(Collectors.toSet()));
        entitiesForUpdate.put("systems", systems.stream().map(EciRequestObject::new).collect(Collectors.toSet()));
        entitiesForUpdate.put("transports", transports.stream().map(EciRequestObject::new)
                .collect(Collectors.toSet()));
        return new UpdateEntitiesByEcProjectStructure(UUID.fromString(ecProjectId), entitiesForUpdate);
    }

    /**
     * Unbind entity from service atp-environments by project.
     *
     * @param ecProjectId project id from atp-environment
     * @param projectUuid project UUID
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/unbind/byproject/{ecProjectId}", method = RequestMethod.GET)
    @AuditAction(auditAction = "Unbind Entities By EcProject")
    public void unbindEntitiesByEcProject(@PathVariable("ecProjectId") String ecProjectId,
                                          @RequestParam(value = "projectUuid") UUID projectUuid) {
        ((EnvConfigurationManager<Environment>) CoreObjectManager.getInstance()
                .getManager(Environment.class)).unbindByEcProject(ecProjectId);
        ((EnvConfigurationManager<System>) CoreObjectManager.getInstance()
                .getManager(System.class)).unbindByEcProject(ecProjectId);
        ((EnvConfigurationManager<TransportConfiguration>) CoreObjectManager.getInstance()
                .getManager(TransportConfiguration.class)).unbindByEcProject(ecProjectId);
        ((EnvConfigurationManager<OutboundTransportConfiguration>) CoreObjectManager.getInstance()
                .getManager(OutboundTransportConfiguration.class)).unbindByEcProject(ecProjectId);
        ((EnvConfigurationManager<InboundTransportConfiguration>) CoreObjectManager.getInstance()
                .getManager(InboundTransportConfiguration.class)).unbindByEcProject(ecProjectId);
        ((EnvConfigurationManager<Server>) CoreObjectManager.getInstance().getManager(Server.class))
                .unbindByEcProject(ecProjectId);
        log.info("Entities under EC-project have become unbind: {}", ecProjectId);
    }

    private void logUploadingResult(String entityName, UUID eciSessionId) {
        List<ECIValidationError> errorsList = errorsCache.get(eciSessionId);
        if (errorsList != null) {
            for (ECIValidationError eciValidationError : errorsList) {
                if (eciValidationError.getValidationLevel().equals(ValidationLevel.ERROR)) {
                    log.error("{} was uploaded with next error: {}", entityName, eciValidationError.getError());
                } else {
                    log.warn("{} was uploaded with next warning: {}", entityName, eciValidationError.getError());
                }
            }
            errorsCache.remove(eciSessionId);
        } else {
            log.info("Uploading of {} from Environment Configurator has been completed successfully.", entityName);
        }
    }

    private void fillEcProjectIdHierarchically(ECEnvironment ecEnvironment) {
        String ecProjectId = ecEnvironment.getEcProjectId();
        for (ECSystem ecSystem : ecEnvironment.getSystems()) {
            ecSystem.setEcProjectId(ecProjectId);
            for (ECConnection ecConnection : ecSystem.getConnections()) {
                ecConnection.setEcProjectId(ecProjectId);
            }
        }
    }

    private class UpdateEntitiesByEcProjectStructure {

        private UUID ecProjectId;
        private Map<String, Set<EciRequestObject>> entities;

        public UpdateEntitiesByEcProjectStructure(UUID ecProjectId, Map<String, Set<EciRequestObject>> entities) {
            this.ecProjectId = ecProjectId;
            this.entities = entities;
        }

        public UUID getEcProjectId() {
            return ecProjectId;
        }

        public void setEcProjectId(UUID ecProjectId) {
            this.ecProjectId = ecProjectId;
        }

        public Map<String, Set<EciRequestObject>> getEntities() {
            return entities;
        }

        public void setEntities(Map<String, Set<EciRequestObject>> entities) {
            this.entities = entities;
        }
    }

    private class EciRequestObject {

        private UUID id;
        private String parentId;
        private String sourceEntityId;

        public EciRequestObject(EciConfigurable eciConfigurable) {
            this.id = UUID.fromString(eciConfigurable.getEcId());
            this.parentId = eciConfigurable.getParent().getID().toString();
            this.sourceEntityId = eciConfigurable.getID().toString();
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getSourceEntityId() {
            return sourceEntityId;
        }

        public void setSourceEntityId(String sourceEntityId) {
            this.sourceEntityId = sourceEntityId;
        }
    }
}
