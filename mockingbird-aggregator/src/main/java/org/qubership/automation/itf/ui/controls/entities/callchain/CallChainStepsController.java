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

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIAbstractCallChainStep;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIEmbeddedChainStep;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UISituationStep;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@Transactional(readOnly = true)
@RestController
public class CallChainStepsController extends AbstractController<UIAbstractCallChainStep, Step> {

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/steps", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get CallChain Steps of CallChain {{#id}} in the project {{#projectUuid}}")
    public UIWrapper<List<UIAbstractCallChainStep>> getSteps(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain chainObject = getManager(CallChain.class).getById(id);
        ControllerHelper.throwExceptionIfNull(chainObject, "", id, CallChain.class, "get CallChain by id");
        UIWrapper<List<UIAbstractCallChainStep>> wrapper = new UIWrapper<>();
        wrapper.setData(Lists.newArrayList());
        for (Step step : chainObject.getSteps()) {
            if (step instanceof SituationStep) {
                SituationStep situationStep = (SituationStep) step;
                wrapper.getData().add(new UISituationStep(situationStep, false));
            } else if (step instanceof EmbeddedStep) {
                EmbeddedStep embeddedStep = (EmbeddedStep) step;
                wrapper.getData().add(new UIEmbeddedChainStep(embeddedStep, false));
            }
        }
        for (int i = 0; i < wrapper.getData().size(); i++) {
            wrapper.getData().get(i).setOrder(i);
        }
        return wrapper;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/callchain/steps", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create CallChain Step of type {{#type}} under CallChain id {{#parentId}} in the "
            + "project {{#projectUuid}}")
    public UIAbstractCallChainStep create(
            @RequestParam(value = "id", defaultValue = "0") String parentId,
            @RequestParam(value = "type", defaultValue = "embeddedChainStep") String type,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        UIAbstractCallChainStep step = super.create(parentId, type);
        step.setName("Step[" + step.getId() + "]");
        return step;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/callchain/steps", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Steps from CallChain id {{#parentId}} in the project {{#projectUuid}}")
    public Map<String, Object> delete(@RequestParam(value = "parentId") String parentId,
                                      @RequestBody Collection<UIAbstractCallChainStep> objectsToDelete,
                                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, Object> result = new HashMap<>();
        CallChain callChain = CoreObjectManager.getInstance().getManager(CallChain.class).getById(parentId);
        ControllerHelper.throwExceptionIfNull(callChain, "", parentId, CallChain.class, "get CallChain by id");
        super.delete(objectsToDelete);
        callChain.store();
        callChain.flush();
        result.put("parentVersion", callChain.getVersion());
        return result;
    }

    @Override
    protected Class<Step> _getGenericUClass() {
        return Step.class;
    }

    @Override
    protected UIAbstractCallChainStep _newInstanceTClass(Step step) {
        if (step instanceof SituationStep) {
            return new UISituationStep((SituationStep) step, false);
        } else if (step instanceof EmbeddedStep) {
            return new UIEmbeddedChainStep((EmbeddedStep) step, false);
        } else {
            throw new IllegalArgumentException("Illegal step was created: " + step);
        }
    }

    @Override
    protected CallChain _getParent(String parentId) {
        return getManager(CallChain.class).getById(parentId);
    }

    @Transactional
    @RequestMapping(value = "/callchain/step/prescript", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Save pre-script on the Step by id {{#stepId}} [Deprecated]")
    public void saveStepPreScript(@RequestParam(value = "stepId", defaultValue = "0") String stepId,
                                  @RequestBody(required = false) String preScript) {
        //deprecated, but the method still exists in the callchain.data.service.js (saveStepPreScript())
    }
}
