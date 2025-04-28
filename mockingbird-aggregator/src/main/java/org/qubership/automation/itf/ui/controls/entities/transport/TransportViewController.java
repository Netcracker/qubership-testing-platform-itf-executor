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

package org.qubership.automation.itf.ui.controls.entities.transport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;

@RestController
public class TransportViewController {

    public static final String APPLICATION_JAVASCRIPT = "application/javascript";
    private static final Logger LOGGER_FOR_CHANGELOG = LoggerFactory.getLogger(TransportViewController.class);

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/registry.js", method = RequestMethod.GET, produces = APPLICATION_JAVASCRIPT)
    @AuditAction(auditAction = "Get /transport/registry.js, project {{#projectUuid}}")
    public ResponseEntity<InputStreamResource> getTransportsJs(@RequestParam(value = "projectUuid") UUID projectUuid) {
        StringBuilder sb = new StringBuilder("function getTransportViews() {");
        sb.append("var modules = [\n");
        try {
            for (Map.Entry<String, Pair<String, String>> entry :
                    TransportRegistryManager.getInstance().getViews().entrySet()) {
                sb.append("            {\n"
                        + "                name: '").append(entry.getKey()).append("',\n"
                        + "                script: '").append(getView(entry.getValue().getLeft())).append("'\n"
                        + "            },\n");
            }
        } catch (Exception e) {
            LOGGER_FOR_CHANGELOG.error("Error computing JS", e);
        }
        sb.append("        ];" + "return modules;");
        sb.append("}");
        return getResponseEntity(sb, APPLICATION_JAVASCRIPT);
    }

    private ResponseEntity<InputStreamResource> getResponseEntity(StringBuilder sb, String type) {
        String content = sb.toString();
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        return ResponseEntity
                .ok()
                .contentLength(content.getBytes().length)
                .contentType(MediaType.parseMediaType(type))
                .body(new InputStreamResource(stream));
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/required.js", method = RequestMethod.GET, produces = APPLICATION_JAVASCRIPT)
    @AuditAction(auditAction = "Get /transport/required.js, project {{#projectUuid}}")
    public ResponseEntity<InputStreamResource> getRequired(@RequestParam(value = "projectUuid") UUID projectUuid)
            throws TransportException {
        StringBuilder builder = new StringBuilder("define(\n[");
        for (Map.Entry<String, Pair<String, String>> entry :
                TransportRegistryManager.getInstance().getViews().entrySet()) {
            builder.append('\'').append(getView(entry.getValue().getLeft())).append('\'').append(",\n");
        }
        builder.append("],\n" + "    function () {})");
        return getResponseEntity(builder, APPLICATION_JAVASCRIPT);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/views", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get /transport/views, project {{#projectUuid}}")
    public HashMap<String, String> getDeclaredViews(@RequestParam(value = "projectUuid") UUID projectUuid)
            throws TransportException {
        HashMap<String, String> viewBinding = Maps.newHashMapWithExpectedSize(50);
        for (Map.Entry<String, Pair<String, String>> entry :
                TransportRegistryManager.getInstance().getViews().entrySet()) {
            String htmlView = entry.getValue().getRight();
            if (StringUtils.isBlank(htmlView)) {
                continue;
            }
            String view = getView(htmlView);
            viewBinding.put(entry.getKey(), view);
        }
        return viewBinding;
    }

    private String getView(String view) {
        switch (view) {
            case "__defaultInboundView__":
            case "__defaultView__":
                return StringUtils.EMPTY;
            default:
                return view;
        }
    }
}
