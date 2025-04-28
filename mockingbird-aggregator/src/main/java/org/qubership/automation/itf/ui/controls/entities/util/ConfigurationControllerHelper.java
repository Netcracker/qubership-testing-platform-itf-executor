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

package org.qubership.automation.itf.ui.controls.entities.util;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.util.eds.service.FileManagementService;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationControllerHelper {
    private static final String INPUT_TYPE_REFERENCE = "reference";
    @Autowired
    private FileManagementService fileManagementService;

    public void setProperty(Configuration entry, UIProperty property, UUID projectUuid) {
        if (INPUT_TYPE_REFERENCE.equals(property.getInputType())) {
            if (property.getReferenceValue() != null) {
                entry.put(property.getName(), property.getReferenceValue().getId());
            } else {
                entry.put(property.getName(), StringUtils.EMPTY);
            }
        } else {
            String propertyValue =
                    StringUtils.isNotEmpty(property.getValue())
                            && StringUtils.isNotEmpty(property.getFilePathDirectoryType())
                            ? fileManagementService.getDirectory(property.getFilePathDirectoryType(), projectUuid,
                            property.getValue())
                            : property.getValue();
            entry.put(property.getName(), propertyValue);
        }
    }
}
