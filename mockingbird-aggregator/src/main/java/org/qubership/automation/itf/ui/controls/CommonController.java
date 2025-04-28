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

package org.qubership.automation.itf.ui.controls;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ByProject;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.ui.messages.UIList;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@RestController
public class CommonController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonController.class);
    private static boolean disabled = false;

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/common/ni", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Network Interfaces, project {{#projectUuid}}")
    public UIList<UIObject> getAllNetworkInterfaces(@RequestParam(value = "projectUuid") UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        UIList<UIObject> uiObjects = new UIObjectList();
        if (disabled) {
            return uiObjects;
        }
        try {
            List<PcapNetworkInterface> networkInterfaces = Pcaps.findAllDevs();
            for (PcapNetworkInterface networkInterface : networkInterfaces) {
                UIObject object = new UIObject();
                object.setName(networkInterface.getName());
                StringBuilder sb = new StringBuilder();
                for (PcapAddress address : networkInterface.getAddresses()) {
                    sb.append(address.getAddress().toString());
                }
                object.setDescription(networkInterface.getDescription() + sb);
                uiObjects.addObject(object);
            }
        } catch (PcapNativeException e) {
            LOGGER.warn("Error while searching the network devices: {}", e.toString());
        } catch (NoClassDefFoundError e) {
            LOGGER.error("Error while searching the network devices; may be Pcap software wasn't installed properly: "
                    + "{}", e.toString());
            disabled = true;
        }
        return uiObjects;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/common/filtered", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all objects by name {{#name}} and entityType {{#entityType}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public List<? extends UIObject> getObjectsByFilteredName(
            @RequestParam(value = "name") String name,
            @RequestParam(value = "entityType") String entityType,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        try {
            Collection<? extends Storable> collection = null;
            if ("template".equals(entityType)) {
                collection = TemplateHelper.getByPieceOfNameAndProject(name, projectId);
            } else if ("situation".equals(entityType)) {
                collection = CoreObjectManager.getInstance().getSpecialManager(Situation.class,
                        ByProject.class).getByPieceOfNameAndProject(name, projectId);
            }
            if (collection != null) {
                List<UIObject> filteredList = Lists.newArrayListWithCapacity(collection.size());
                for (Storable object : collection) {
                    filteredList.add(new UIObject(object));
                }
                return filteredList;
            }
        } catch (Exception e) {
            LOGGER.warn("Error while getting filtered name of entity: {}", e.toString());
        }
        return null;
    }
}
