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

package org.qubership.automation.itf.ui.controls.entities.environment;

import java.math.BigInteger;
import java.util.Collection;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByProjectIdManager;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Please, don't use the controller. In Next version I will try to delete this interface. It's overhead.
 */
@Deprecated
@RestController
public class EnvironmentUtilController extends ControllerHelper {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment/simplified", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Environments (simplified) for project {{#projectId}}/{{#projectUuid}}")
    public JSONArray getAllSimplifiedEnvironments(@RequestParam(value = "projectId") BigInteger projectId,
                                                  @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<? extends Environment> environments =
                CoreObjectManager.getInstance().getSpecialManager(Environment.class, SearchByProjectIdManager.class)
                        .getByProjectId(projectId);
        return makeJson(environments);
    }

    private JSONArray makeJson(Collection<? extends Environment> environments) {
        if (environments != null) {
            JSONArray result = new JSONArray();
            for (Environment environment : environments) {
                JSONObject uiEnvironment = new JSONObject();
                uiEnvironment.put("id", environment.getID().toString());
                uiEnvironment.put("name", environment.getName());
                if (environment.getEnvironmentState() != null) {
                    uiEnvironment.put("inboundState", environment.getEnvironmentState().toString());
                }
                result.add(uiEnvironment);
            }
            return result;
        }
        return null;
    }
}
