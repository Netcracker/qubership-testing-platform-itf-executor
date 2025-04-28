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

package org.qubership.automation.itf.core.report;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.constants.SituationLevelValidation;
import org.qubership.automation.itf.core.util.engine.EngineAfterIntegration;
import org.qubership.automation.itf.core.util.engine.EngineIntegration;
import org.qubership.automation.itf.core.util.engine.EngineOnStepIntegration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.core.util.transport.service.report.Report;

public class ReportIntegration {

    private static ReportIntegration instance = new ReportIntegration();

    private ReportIntegration() {
    }

    public static ReportIntegration getInstance() {
        return instance;
    }

    private void runAfterIntegrations(CallChainInstance instance) {
        Set<IntegrationConfig> integrationConfs =
                CoreObjectManager.getInstance().getManager(StubProject.class).getById(instance.getContext().tc().getProjectId()).getIntegrationConfs();
        for (Map.Entry<String, EngineIntegration> item :
                EngineIntegrationRegistry.getInstance().getAvailableEngines().entrySet()) {
            if (item.getValue() instanceof EngineAfterIntegration) {
                for (IntegrationConfig config : integrationConfs) {
                    if (config.getTypeName().equals(item.getKey())) {
                        try {
                            ((EngineAfterIntegration) item.getValue()).executeAfter(instance, config);
                        } catch (Exception ex) {
                            addInstanceException(instance, "After-Callchain-Finished Integration", ex);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * This method was made public because
     * when executing the case it is necessary to stop the execution of the case
     * and throw an exception if it need.
     * LogSubscriber is not suitable for this.
     * If you have any ideas, please call.
     *
     * @param instance  is current situation instance.
     * @param spContext is step context.
     * @param initiator is starter for current case.
     * @return is validation in Integration.
     * TRUE if the message  correct.
     * FALSE if message doesn't correct.
     * @throws Exception then will happen some problem on connection BV or etc.
     */

    public boolean runOnStepIntegrations(SituationInstance instance,
                                         SpContext spContext,
                                         AbstractContainerInstance initiator,
                                         Situation situation) throws Exception {
        boolean passed = true;
        Set<IntegrationConfig> integrationConfs =
                CoreObjectManager.getInstance().getManager(StubProject.class).getById(instance.getContext().tc().getProjectId()).getIntegrationConfs();
        if (integrationConfs.isEmpty()) {
            return passed;
        }
        for (Map.Entry<String, EngineIntegration> item :
                EngineIntegrationRegistry.getInstance().getAvailableEngines().entrySet()) {
            if (item.getValue() instanceof EngineOnStepIntegration) {
                for (IntegrationConfig config : integrationConfs) {
                    if (config.getTypeName().equals(item.getKey())) {
                        try {
                            passed = passed && ((EngineOnStepIntegration) item.getValue())
                                    .executeOnStep(instance, spContext, initiator, config, situation);
                        } catch (Exception ex) {
                            addInstanceException(instance,
                                    item.getKey() + ": exception on the step '" + instance.getName() + "'", ex);
                            if (!situation.getValidateIncoming().equals(SituationLevelValidation.IGNORE)) {
                                throw new Exception(item.getKey() + ": exception on the step '" + instance.getName() + "' with config " + config.getName() + " and engine " + item.getValue().toString(), ex);
                            }
                        }
                        break;
                    }
                }
            }
        }
        return passed;
    }

    void runAndReportAfterIntegrations(TcContext tcContext) {
        AbstractContainerInstance initiator = tcContext.getInitiator();
        if (initiator instanceof CallChainInstance) {
            runAfterIntegrations((CallChainInstance) initiator);
            reportReportLinks((CallChainInstance) initiator);
        }
    }

    private void addInstanceException(AbstractContainerInstance initiator, String where, Throwable exception) {
        String errName = initiator.getErrorName();
        String errMessage = initiator.getErrorMessage();
        initiator.setErrorName(((StringUtils.isBlank(errName)) ? "" : errName + " ...One more exception(s): ") + "[" + where + " Exception]" + exception.getMessage());
        initiator.setErrorMessage(((StringUtils.isBlank(errMessage)) ? "" : errMessage + " ...One more exception(s): "
        ) + "[" + where + " Exception]" + ((exception.getCause() != null) ? exception.getCause().toString() :
                ExceptionUtils.getStackTrace(exception)));
    }

    private void reportReportLinks(CallChainInstance chainInstance) {
        Report.info(chainInstance, "Report links after execution",
                ReportLinkCreator.getInstance().buildReportLinks(chainInstance.getContext().tc()));
    }
}
