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

package org.qubership.automation.itf.ui.controls.entities.template;

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.OperationTemplateObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemTemplateObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.TransportConfigurationObjectManager;
import org.qubership.automation.itf.core.model.IdNamePair;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironment;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironmentItem;
import org.qubership.automation.itf.ui.messages.objects.template.UITemplate;
import org.qubership.automation.itf.ui.messages.objects.template.UITransportPropsForTemplate;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Transactional(readOnly = true)
public class TemplateController extends AbstractController<UITemplate, Template> {

    /**
     * Get all Templates under the Project.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Templates in the project {{#projectId}}/{{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "projectUuid") UUID projectUuid,
                                           @RequestParam(value = "projectId") BigInteger projectId) {
        return TemplateHelper.getByProjectId(projectId).stream().map(UIObject::new).collect(Collectors.toList());
    }

    /**
     * Get {id, name} list of SystemTemplates under the parent System (for drop-down lists in diameter transport).
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/allIdAndName", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Templates (briefly) in the project {{#projectId}}/{{#projectUuid}}")
    public List<SystemTemplate> getIdAndName(
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "systemId", required = false) BigInteger systemId,
            @RequestParam(value = "transportId", required = false) BigInteger transportId) {
        if (Objects.nonNull(transportId) && Objects.isNull(systemId)) {
            OutboundTransportConfiguration outboundTransportConfiguration = CoreObjectManager.getInstance()
                    .getManager(OutboundTransportConfiguration.class).getById(transportId);
            systemId = (BigInteger) outboundTransportConfiguration.getSystem().getID();
        }
        return CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, SystemTemplateObjectManager.class)
                .getSimpleSystemTemplatesByParentId(systemId);
    }

    /**
     * Get Template List under parent Operation and its System.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/onOperationAndSystem", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Templates under Operation by id {{#id}} and parent System in the project "
            + "{{#projectUuid}}")
    public List<? extends UIObject> getTemplateOnOperationAndSystem(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        super.setSimple(true);
        Operation operation = CoreObjectManager.getInstance().getManager(Operation.class).getById(id);
        ControllerHelper.throwExceptionIfNull(operation, "", id, Operation.class,
                "get Templates under Operation");

        List<UIObject> uiObjects = (List<UIObject>) asListUIObject(operation.returnTemplates(), true, true);
        uiObjects.addAll(asListUIObject(operation.getParent().returnTemplates(), true, true));

        super.setSimple(false);
        return uiObjects;
    }

    /**
     * Returns operation and system templates filtering by name.
     * Applies if the project setting 'many.objects.ui.mode' is true.
     *
     * @param id          situation id
     * @param name        template name for filtering
     * @param projectId   ITF project id
     * @param projectUuid ATP project UUID
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/onOperationAndSystemFiltered", method = RequestMethod.GET)
    public List<IdNamePair> getTemplateOnOperationAndSystemFiltered(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(id);
        ControllerHelper.throwExceptionIfNull(situation, "", id, Situation.class, "get Situation by id");
        Operation operation = situation.getParent();
        CoreObjectManagerService objectManagerService = CoreObjectManager.getInstance();
        List<IdNamePair> uiObjects = objectManagerService
                .getSpecialManager(SystemTemplate.class, SystemTemplateObjectManager.class)
                .getByPieceOfNameAndParentId(name, (BigInteger) operation.getParent().getID());
        uiObjects.addAll(objectManagerService
                .getSpecialManager(OperationTemplate.class, OperationTemplateObjectManager.class)
                .getByPieceOfNameAndParentId(name, (BigInteger) operation.getID()));
        return uiObjects;
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Template by id {{#id}} in the project {{#projectUuid}}")
    public UITemplate getById(@RequestParam(value = "id", defaultValue = "0") BigInteger id,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        return new UITemplate(TemplateHelper.getById(id));
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/template", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Template under {{#type}} with id {{#parentId}} in the project {{#projectUuid}}")
    public UITemplate create(@RequestParam(value = "selectedId") String parentId,
                             @RequestParam(value = "projectUuid") UUID projectUuid,
                             @RequestParam(value = "type") Class<? extends TemplateProvider> type) {
        TemplateProvider parent;
        if (Template.class.isAssignableFrom(type)) {
            parent = _getParent(parentId);
        } else {
            parent = CoreObjectManager.getInstance().getManager(type).getById(parentId);
        }
        return new UITemplate(TemplateHelper.getManagerByParent(parent).create(parent));
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/template", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Template with id {{#uiTemplate.id}} in the project {{#projectUuid}}")
    public UITemplate update(@RequestBody UITemplate uiTemplate,
                             @RequestParam(value = "projectUuid") UUID projectUuid) {
        ObjectManager<? extends Template<? extends TemplateProvider>>
                objectManager = SystemTemplate.class.getName().equals(uiTemplate.getClassName())
                ? getManager(SystemTemplate.class)
                : getManager(OperationTemplate.class);
        return updateUIObject(objectManager.getById(uiTemplate.getId()), uiTemplate);
    }

    /**
     * Delete Templates by template Ids list given.
     * Check for usages or not, depending on 'ignoreUsages' parameter.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/template", method = RequestMethod.DELETE, produces = "application/json")
    @AuditAction(auditAction = "Delete Templates from project {{#projectUuid}}")
    public List<List<UIObject>> delete(
            @RequestParam(value = "ignoreUsages", defaultValue = "false") Boolean ignoreUsages,
            @RequestBody UIIds uiDeleteObjectReq,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        List<UIObject> deletedUiObjects = new ArrayList<>();
        List<UIObject> usedUiObjects = new ArrayList<>();
        Map<String, Map<String, String>> result = Maps.newHashMap();
        for (String id : uiDeleteObjectReq.getIds()) {
            Template<? extends TemplateProvider> template = TemplateHelper.getById(id);
            if (haveUsages(template, result, ignoreUsages)) {
                usedUiObjects.add(new UIObject(template));
                continue;
            }
            deletedUiObjects.add(new UIObject(template));
            delete(template);
            LOGGER.info("Storable {} is deleted", template);
        }
        List<List<UIObject>> allObjects = new ArrayList<>();
        allObjects.add(deletedUiObjects);
        allObjects.add(usedUiObjects);
        return allObjects;
    }

    /**
     * Find Templates by Name under the project.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/templateByName", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Templates by name {{#name}} in the project {{#projectUuid}}")
    public UITemplate getByName(
            @RequestParam(value = "name") String name,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<Template<? extends TemplateProvider>> templates =
                TemplateHelper.getByNameAndProjectId(name, projectId);
        if (templates.isEmpty()) {
            throw new ObjectNotFoundException(Template.class.getSimpleName(), null, name, "get Templates by name");
        }
        return new UITemplate(templates.iterator().next());
    }

    /**
     * Find Template usages.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/usages", method = RequestMethod.GET, produces = "application/json")
    @AuditAction(auditAction = "Get usages of Template with id {{#id}} in the project {{#projectUuid}}")
    public Map<String, Object> getUsages(@RequestParam(value = "id") BigInteger id,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        Template<? extends TemplateProvider> template = TemplateHelper.getById(id);
        Map<String, Object> usages = new HashMap<>();
        usages.put("situations", findTemplateUsagesOnSituations(template));
        usages.put("transports", findTemplateUsagesOnTransportConfigurations(template));
        usages.put("environments", findTemplateUsagesOnOutboundConfigurations(template));
        return usages;
    }

    private List findTemplateUsagesOnSituations(Template<? extends TemplateProvider> template) {
        Collection<UsageInfo> usageInfo = TemplateHelper.getManagerByParent(template.getParent()).findUsages(template);
        List<Object> situations = new ArrayList<>();
        if (usageInfo != null) {
            for (UsageInfo item : usageInfo) {
                UISituation uiSituation = null;
                if (item.getReferer().getParent() instanceof Situation) {
                    Situation situation = (Situation) item.getReferer().getParent();
                    uiSituation = new UISituation(situation);
                }
                situations.add(uiSituation);
            }
        }
        return situations;
    }

    private List findTemplateUsagesOnTransportConfigurations(Template<? extends TemplateProvider> template) {
        List<Object> transports = new ArrayList<>();
        // Currently, only SystemTemplates (from the parent System) can be used inside diameter transport config.
        if (template instanceof SystemTemplate) {
            Collection<TransportConfiguration> transportConfigurations = CoreObjectManager.getInstance()
                    .getSpecialManager(TransportConfiguration.class, TransportConfigurationObjectManager.class)
                    .findUsagesTemplateOnTransport((BigInteger) template.getID());
            transportConfigurations.forEach(transportConfiguration -> {
                UITransport uiTransport = new UITransport(transportConfiguration);
                transports.add(uiTransport);
            });
        }
        return transports;
    }

    private List findTemplateUsagesOnOutboundConfigurations(Template<? extends TemplateProvider> template) {
        List<Object> environments = new ArrayList<>();
        // Currently, only SystemTemplates (from the parent System) can be used inside diameter transport config.
        if (template instanceof SystemTemplate) {
            List<Map<String, Object>> findUsagesOnConfigurationDiameterOutbound =
                    CoreObjectManager.getInstance()
                            .getSpecialManager(SystemTemplate.class, SystemTemplateObjectManager.class)
                            .findUsagesOnOutboundDiameterConfiguration(template);

            findUsagesOnConfigurationDiameterOutbound.forEach(stringObjectMap -> {
                UIEnvironment uiEnvironment = new UIEnvironment();
                uiEnvironment.setId(stringObjectMap.get("env_id").toString());
                uiEnvironment.setName(stringObjectMap.get("env_name").toString());
                UIEnvironmentItem uiEnvironmentItem = new UIEnvironmentItem();
                UIObject system = new UIObject();
                system.setName(stringObjectMap.get("system_name").toString());
                UIObject server = new UIObject();
                server.setName(stringObjectMap.get("server_name").toString());
                uiEnvironmentItem.setSystem(system);
                uiEnvironmentItem.setServer(server);
                uiEnvironment.setOutbound(new ArrayList<UIEnvironmentItem>() {
                    {
                        add(uiEnvironmentItem);
                    }
                });
                environments.add(uiEnvironment);
            });
        }
        return environments;
    }

    @Override
    protected Template _beforeUpdate(UITemplate editRequest, Template template) {
        template.setText(editRequest.getContent());
        List<UIProperty> uiHeaders = editRequest.getHeaders();
        if (uiHeaders != null) {
            Map<String, String> headers = new HashMap<>();
            for (UIProperty uiHeader : uiHeaders) {
                headers.put(uiHeader.getName(), uiHeader.getValue());
            }
            template.fillHeaders(headers);
        }
        ImmutableList<UITransportPropsForTemplate> uiProperties = editRequest.getTransportProperties();
        if (uiProperties != null) {
            Collection<OutboundTemplateTransportConfiguration> templateProperties = Lists.newArrayList();
            for (UITransportPropsForTemplate uiProperty : uiProperties) {
                if (uiProperty.getTransportProperties().isEmpty()) {
                    continue;
                }
                OutboundTemplateTransportConfiguration properties =
                        new OutboundTemplateTransportConfiguration(uiProperty.getClassName(), template);
                for (UIProperty property : uiProperty.getTransportProperties()) {
                    if (StringUtils.isNotEmpty(property.getValue())) {
                        properties.put(property.getName(), property.getValue());
                    }
                }
                if (!properties.isEmpty()) {
                    templateProperties.add(properties);
                }
            }
            template.fillTransportProperties(templateProperties);
        }
        return super._beforeUpdate(editRequest, template);
    }

    @Override
    protected Class<Template> _getGenericUClass() {
        return Template.class;
    }

    @Override
    protected UITemplate _newInstanceTClass(Template object) {
        return new UITemplate(object);
    }

    @Override
    protected TemplateProvider _getParent(String templateId) {
        Template<? extends TemplateProvider> template = TemplateHelper.getById(templateId);
        if (Objects.nonNull(template)) {
            return template.getParent();
        } else {
            throw new ObjectNotFoundException(Template.class.getSimpleName(), templateId, null, null);
        }
    }

    private TemplateProvider getParentInTransaction(String parentId, Class<? extends TemplateProvider> parentClass)
            throws Exception {
        return TxExecutor.execute(() -> getManager(parentClass).getById(parentId),
                TxExecutor.defaultWritableTransaction());
    }
}
