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

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;

public class ResponseCacheHelper {

    public static void beforeUpdatedForRestAndSoapTransport(Configuration configuration,
                                                            UIConfiguration uiConfiguration,
                                                            BigInteger projectId) {
        Optional<UIProperty> uiCacheResponseForSeconds
                = uiConfiguration.getProperty(PropertyConstants.Http.CACHE_RESPONSE_FOR_SECONDS);
        if (uiCacheResponseForSeconds.isPresent()) {
            UIProperty uiProperty = uiCacheResponseForSeconds.get();
            if (Objects.isNull(uiProperty.getInheritedValue())) {
                return;
            }
            if (!uiProperty.getInheritedValue().equals(uiProperty.getValue())
                    && StringUtils.isNotEmpty(uiProperty.getValue())) {
                String keyCachedResponse = String.format("%s_%s%s", projectId,
                        configuration.get("baseUrl"),
                        configuration.get("endpoint"));
                CacheServices.getResponseCacheService().evict(keyCachedResponse);
            }
        }
    }
}
