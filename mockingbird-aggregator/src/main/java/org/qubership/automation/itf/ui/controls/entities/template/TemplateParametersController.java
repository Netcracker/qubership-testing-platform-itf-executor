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

import java.util.Collection;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.NativeManager;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional(readOnly = true)
public class TemplateParametersController extends ControllerHelper {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/configuration", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Configuration on Template with id {{#parent}} in the project {{#projectUuid}}")
    public UIObjectList getConfigurationById(
            @RequestParam(value = "id", defaultValue = "0") String parent,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        //TODO: it looks unused. Should be revised.
        Template<? extends TemplateProvider> template = TemplateHelper.getById(parent);
        Collection children =
                CoreObjectManager.getInstance().getSpecialManager(template.getClass(), NativeManager.class)
                        .getChildrenByClass(
                                template,
                                OutboundTemplateTransportConfiguration.class,
                                "org.qubership.automation.itf.transport.file.ftp.outbound.FileOverSftpOutbound");
        return getObjectList(children);
    }
}
