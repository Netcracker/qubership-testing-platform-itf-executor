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

package org.qubership.automation.itf.ui.controls.entities.callchain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UISituationStep;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

/**
 * Please, don't use the controller. In Next version I will try to delete this interface. It's overhead.
 */
@Deprecated
@RestController
public class CallChainUtilController extends ControllerHelper {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/preview", method = RequestMethod.GET)
    public List<UISituationStep> previewChain(@RequestParam(value = "id", defaultValue = "0") String id,
                                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain chainObject = getManager(CallChain.class).getById(id);
        throwExceptionIfNull(chainObject, "", id, CallChain.class, "preview CallChain");
        return previewChain(chainObject);
    }

    private List<UISituationStep> previewChain(CallChain chain) {
        ArrayList<UISituationStep> result = Lists.newArrayListWithExpectedSize(chain.getSteps().size() * 10);
        for (Step step : chain.getSteps()) {
            if (step instanceof SituationStep) {
                result.add(new UISituationStep((SituationStep) step, false));
            } else if (step instanceof EmbeddedStep && step.isEnabled()) {
                result.addAll(previewChain(((EmbeddedStep) step).getChain()));
            }
        }
        return result;
    }
}
