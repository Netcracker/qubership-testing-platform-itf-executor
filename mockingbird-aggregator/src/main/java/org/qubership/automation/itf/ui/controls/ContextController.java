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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.execution.DebugExecutor;
import org.qubership.automation.itf.core.message.TcContextOperationMessage;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.integration.reports.ReportsService;
import org.qubership.automation.itf.ui.messages.objects.UIReportItem;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIAbstractCallChainStep;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UISituationStep;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;

@Transactional
@RestController
@Tags({@Tag(name = SwaggerConstants.CONTEXT_QUERY_API, description = SwaggerConstants.CONTEXT_QUERY_API_DESCR),
        @Tag(name = SwaggerConstants.CONTEXT_COMMAND_API, description = SwaggerConstants.CONTEXT_COMMAND_API_DESCR)})
@RequiredArgsConstructor
public class ContextController {

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final List<String> FINISHED_STATUS = new ArrayList<>(
            Arrays.asList("PASSED", "FAILED", "STOPPED", "Passed", "Failed", "Stopped", "FAILED_BY_TIMEOUT",
                    "Failed by timeout"));
    private final DebugExecutor debugExecutor;
    private final ReportsService reportsService;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final ProjectSettingsService projectSettingsService;
    // The following 4 methods are used by NTT (REST API). Do NOT delete

    /*  Based on native 'InstanceContextRepository#getTcContextInfo' query:
     *       select ctx.id, ctx.name, ctx.initiator_id, ctx.enviroment_id, ctx.status, ctx.start_time, ctx.end_time,
     * ctx.json_string
     *       from mb_context ctx where ctx.id = cast(:tc_context_id as int8)
     *
     *   TcContext status itself is [4] element of resulting array
     *   Please check if the query is changed.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/context/state", method = RequestMethod.GET)
    @Operation(summary = "GetContextStatusById",
            description = "Retrieve status of context",
            tags = {SwaggerConstants.CONTEXT_QUERY_API})
    @AuditAction(auditAction = "Get status of tc-context id {{#contextId}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public Properties status(
            @RequestParam(value = "id") String contextId,
            @RequestParam(value = "projectId", required = false) BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        String status;
        TcContext tc = CacheServices.getTcContextCacheService().getById((new BigInteger(contextId)));
        if (tc == null) {
            Object[] props = getContextProperties(contextId, projectUuid, false);
            status = props[4].toString();
        } else {
            status = tc.getStatus().toString().toUpperCase();
        }
        Properties properties = new Properties();
        properties.put("status", status);
        if (isContextFinished(status)) {
            properties.put("isFinished", "true");
        } else {
            properties.put("isFinished", "false");
        }
        return properties;
    }

    /*  Based on native 'InstanceContextRepository#getTcContextInfo' query:
     *       select ctx.id, ctx.name, ctx.initiator_id, ctx.enviroment_id, ctx.status, ctx.start_time, ctx.end_time,
     * ctx.json_string
     *       from mb_context ctx where ctx.id = cast(:tc_context_id as int8)
     *
     *   TcContext variables are [7] element of resulting array
     *   Please check if the query is changed.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/context/get",
            method = RequestMethod.GET,
            produces = {"application/json; charset=UTF-8"})
    @Operation(summary = "GetContextById",
            description = "Retrieve context by id",
            tags = {SwaggerConstants.CONTEXT_QUERY_API})
    @AuditAction(auditAction = "Get tc-context by id {{#contextId}} in the project {{#projectUuid}}")
    public String get(@RequestParam(value = "id") String contextId,
                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        TcContext tc = CacheServices.getTcContextCacheService().getById(new BigInteger(contextId));
        if (tc == null) {
            Object[] props = getContextProperties(contextId, projectUuid, true);
            return (props[7]).toString();
        } else {
            return tc.toJSONString();
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/context/keys", method = RequestMethod.GET)
    @Operation(summary = "GetKeysById",
            description = "Retrieve context keys by id",
            tags = {SwaggerConstants.CONTEXT_QUERY_API})
    @AuditAction(auditAction = "Get Context Keys of tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public Set<String> getKeys(
            @RequestParam(value = "id") String contextId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        TcContext tc = CacheServices.getTcContextCacheService().getById(new BigInteger(contextId));
        if (tc == null) {
            return reportsService.getKeys(contextId, projectUuid);
        } else {
            return tc.getBindingKeys();
        }
    }

    @Transactional(readOnly = true)
    @RequestMapping(value = "/context/info", method = RequestMethod.GET, produces = {"application/json; charset=UTF-8"})
    @Operation(summary = "GetInfoById",
            description = "Retrieve context info by id",
            tags = {SwaggerConstants.CONTEXT_QUERY_API})
    @AuditAction(auditAction = "Get Context Info of tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public Properties getInfo(
            @RequestParam(value = "id") String contextId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Properties properties = new Properties();
        String status;
        TcContext tc = CacheServices.getTcContextCacheService().getById(new BigInteger(contextId));
        if (tc == null) {
            Object[] props = getContextProperties(contextId, projectUuid, true);
            properties.put("id", (props[0]).toString());
            properties.put("name", props[1]);
            properties.put("start_time", (props[5]).toString());
            properties.put("end_time", (props[6]).toString());
            status = props[4].toString();
        } else {
            properties.put("id", tc.getID().toString());
            properties.put("name", tc.getName());
            properties.put("start_time", (tc.getStartTime() == null) ? "" : tc.getStartTime().toString());
            properties.put("end_time", (tc.getEndTime() == null) ? "" : tc.getEndTime().toString());
            status = tc.getStatus().toString().toUpperCase();
        }
        properties.put("status", status);
        properties.put("isFinished", String.valueOf(isContextFinished(status)));
        return properties;
    }
    //END - The following 4 methods are used by NTT (REST API). Do NOT delete, and check contract in case changes

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"EXECUTE\")")
    @RequestMapping(value = "/context/fail", method = RequestMethod.POST)
    @Operation(summary = "FailTC",
            description = "Force context to be failed.",
            tags = {SwaggerConstants.CONTEXT_COMMAND_API})
    @AuditAction(auditAction = "Fail tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public void fail(@RequestBody Properties request,
                     @RequestParam(value = "projectUuid") UUID projectUuid) {
        String contextId = request.getProperty("id");
        if (contextId == null) {
            throw new IllegalArgumentException("Parameter 'id' (Context id) is null or missed");
        }
        TcContext tcContext = CacheServices.getTcContextCacheService().getById(contextId);
        if (tcContext == null) {
            throw new IllegalArgumentException(String.format("Context isn't found by id '%s'", contextId));
        }
        failTc(request, tcContext);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"EXECUTE\")")
    @RequestMapping(value = "/context/setstate", method = RequestMethod.POST)
    @Operation(summary = "SetState", description = "Set state of context", tags =
            {SwaggerConstants.CONTEXT_COMMAND_API})
    @AuditAction(auditAction = "Set state of tc-contexts selected in the project {{#projectUuid}}")
    public void setState(@RequestBody Properties request,
                         @RequestParam(value = "projectUuid") UUID projectUuid,
                         @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        executorToMessageBrokerSender.sendMessageToTcContextOperationsTopic(
                new TcContextOperationMessage(request.getProperty("state"),
                        new BigInteger(request.getProperty("contextId"))), tenantId);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"EXECUTE\")")
    @RequestMapping(value = "/context/continue", method = RequestMethod.GET)
    @Operation(summary = "PushCallAndContinueTC",
            description = "Continue paused context",
            tags = {SwaggerConstants.CONTEXT_COMMAND_API})
    @AuditAction(auditAction = "Push call and continue tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public UIReportItem pushCallAndContinueTC(
            @RequestParam(value = "contextId") String contextId,
            @RequestParam(value = "stepId") String stepId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        TcContext tcContext = searchAndGetValidTcContext(contextId);
        Step step = searchAndGetValidStep(stepId);
        prepareTcContextForContinue(tcContext);
        debugExecutor.executeCallChainBeginStep(tcContext, step);
        return MonitoringController.buildUIReportItem(tcContext);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'EXECUTE')")
    @RequestMapping(value = "/callchain/run/disablestepbystep", method = RequestMethod.GET)
    @AuditAction(auditAction = "Disable StepByStep mode during execution of TC context {{#contextId}}")
    public void disableStepByStep(@RequestParam(value = "contextId") BigInteger contextId,
                                  @RequestParam(value = "projectUuid") UUID projectUuid,
                                  @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        executorToMessageBrokerSender.sendMessageToDisableStepByStepTopic(contextId, tenantId);
    }

    @Deprecated
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/getallstepsituation", method = RequestMethod.GET)
    @Operation(summary = "GetAllSituationStep",
            description = "Retrieve all situation steps of context",
            tags = {SwaggerConstants.CONTEXT_QUERY_API})
    @AuditAction(auditAction = "Get all situation steps of CallChain id {{#id}} in the project {{#projectUuid}}")
    public UIWrapper<List<UIAbstractCallChainStep>> getAllSituationStep(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        // Commented, currently is unused.
        /*
        CallChain chainObject = CoreObjectManager.getInstance().getManager(CallChain.class).getById(id);
        throwExceptionIfNull(chainObject, "", id, CallChain.class);
        */
        UIWrapper<List<UIAbstractCallChainStep>> wrapper = new UIWrapper<>();
        // Commented, currently is unused.
        /*
        String counterToString = "";
        Set<Object> chainIds = new HashSet<>();
        wrapper.setData(getAllSituationStepForCallChain(chainObject, Lists.newArrayList(), counterToString, chainIds));
        */
        return wrapper;
    }

    private void failTc(@RequestBody Properties request, TcContext tcContext) {
        String title = request.getProperty(TITLE, "");
        String message = request.getProperty(MESSAGE, "");
        List<AbstractContainerInstance> instances = tcContext.getInstances();
        AbstractContainerInstance lastInstance = instances.get(instances.size() - 1);
        Report.terminated(lastInstance, title, message, null);
        ExecutionServices.getExecutionProcessManagerService().fail(tcContext);
    }

    private List<UIAbstractCallChainStep> getAllSituationStepForCallChain(CallChain chain,
                                                                          List<UIAbstractCallChainStep> steps,
                                                                          String counterToString,
                                                                          Set<Object> chainIds) {
        String beginString = new StringBuilder(counterToString).toString(); //FIXME what is going on?
        chainIds.add(chain.getID());
        List<Step> chainSteps = chain.getSteps();
        for (int i = 0; i < chainSteps.size(); i++) {
            Step step = chainSteps.get(i);
            if (step == null || !step.isEnabled()) {
                continue;
            }
            counterToString = counterToString + String.valueOf(i + 1); //FIXME use StringBuilder instead of concat.
            if (step instanceof SituationStep) {
                steps.add(new UISituationStep((SituationStep) step, counterToString, false));
            } else if (step instanceof EmbeddedStep) {
                if (((EmbeddedStep) step).getChain() != null
                        && !(chainIds.contains(((EmbeddedStep) step).getChain().getID()))) {
                    steps = getAllSituationStepForCallChain(
                            ((EmbeddedStep) step).getChain(),
                            steps,
                            counterToString + ".",
                            chainIds
                    );
                }
            }
            counterToString = beginString;
        }
        return steps;
    }

    private TcContext searchAndGetValidTcContext(String contextId) {
        TcContext tcContext = CacheServices.getTcContextCacheService().getById(new BigInteger(contextId));
        if (tcContext == null) {
            throw new IllegalArgumentException(String.format("Context isn't found by id '%s'", contextId));
        }
        if (Objects.isNull(tcContext.getInitiator()) || !(tcContext.getInitiator() instanceof CallChainInstance)) {
            throw new IllegalArgumentException(String.format("Initiator isn't found by context id '%s'", contextId));
        }
        tcContext.setStartedFrom(StartedFrom.ITF_UI);
        return tcContext;
    }

    private Step searchAndGetValidStep(String stepId) {
        Step step = CoreObjectManager.getInstance().getManager(Step.class).getById(stepId);
        if (!(step instanceof SituationStep)) {
            throw new IllegalArgumentException(String.format("Step isn't found by id '%s'", stepId));
        }
        return step;
    }

    private void prepareTcContextForContinue(TcContext tcContext) {
        InstanceContext instanceContext = tcContext.getInitiator().getContext();
        if (instanceContext == null) {
            instanceContext = new InstanceContext();
            tcContext.getInitiator().setContext(instanceContext);
        }
        instanceContext.setTC(tcContext);
        tcContext.setStatus(Status.IN_PROGRESS);
        tcContext.setEndTime(null);
    }

    /*  Get tc-context properties by id.
     *   There is one more variant: get info not from database but from tcCache, like this:
     *      CoreObjectManager.getInstance().getSpecialManager(TcContext.class, ContextManager.class)
     *          .getByIdInCache(new BigInteger(contextId))
     *   But it's not so good because it entails a necessity of checking after the context is finished.
     *   Moreover, it would be not a decision for multy-pod execution.
     */
    private Object[] getContextProperties(String contextId, UUID projectUuid, boolean failIfNotFound) {
        List<Object[]> contextProperties = reportsService.getContextProperties(contextId, projectUuid);
        if (contextProperties == null || contextProperties.isEmpty()) {
            if (failIfNotFound) {
                throw new IllegalArgumentException(String.format("Context isn't found by id '%s'", contextId));
            } else {
                return new String[]{contextId, "", "", "", "Unknown"};
            }
        }
        return contextProperties.get(0);
    }

    private boolean isContextFinished(String status) {
        return FINISHED_STATUS.contains(status);
    }
}
