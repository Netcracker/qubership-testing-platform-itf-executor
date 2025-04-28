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

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.LOG_APPENDER_DATE_FORMAT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.MONITORING_PAGINATION_SIZE;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.MONITORING_PAGINATION_SIZE_DEFAULT_VALUE_INT;

import java.math.BigInteger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.message.TcContextOperationMessage;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContextBriefInfo;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.monitoring.UIGetReportList;
import org.qubership.automation.itf.ui.messages.objects.UIReportItem;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MonitoringController {

    private static final String DATE_FORMAT = Config.getConfig().getString(LOG_APPENDER_DATE_FORMAT);
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(ZoneId.systemDefault());

    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final ProjectSettingsService projectSettingsService;

    public static UIReportItem buildUIReportItem(TcContext item) {
        return buildUIReportItem(new TcContextBriefInfo(item));
    }

    private static UIReportItem buildUIReportItem(TcContextBriefInfo item) {
        UIReportItem uiReportItem = new UIReportItem();
        uiReportItem.setId(item.getId().toString());
        uiReportItem.setName(item.getName());
        uiReportItem.setEnvironment(item.getEnvironment() != null ? item.getEnvname() : "Environment not set");
        uiReportItem.setInitiator(item.getInitiator() != null ? item.getIniname() : "Initiator not set");
        uiReportItem.setStatus(item.getStatus().toString());
        uiReportItem.setStartTime((item.getStartTime() == null) ? "" :
                item.getStartTime().toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter));
        uiReportItem.setEndTime((item.getEndTime() == null) ? "" :
                item.getEndTime().toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter));
        uiReportItem.setDuration((item.getDuration() == null)
                ? ""
                : DurationFormatUtils.formatDuration(Long.parseLong(item.getDuration().toString()), "HH:mm:ss", true));
        uiReportItem.setClient(item.getClient());
        return uiReportItem;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/monitoring/page/size", method = RequestMethod.POST)
    @AuditAction(auditAction = "Set page size to {{#size}} for project {{#projectId}}/{{#projectUuid}}")
    public void setPageSize(@RequestParam String size,
                            @RequestParam(value = "projectId") BigInteger projectId,
                            @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        projectSettingsService.update(projectId, MONITORING_PAGINATION_SIZE, size, true);
        log.info("Parameter value is changed for project [{}]: '{}'={}",
                projectId, MONITORING_PAGINATION_SIZE, size);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/monitoring/page/size", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get page size for project {{#projectId}}/{{#projectUuid}}")
    public int getPageSize(@RequestParam(value = "projectId") BigInteger projectId,
                           @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        return projectSettingsService.getInt(projectId, MONITORING_PAGINATION_SIZE,
                MONITORING_PAGINATION_SIZE_DEFAULT_VALUE_INT);
    }

    //TODO: Below endpoint used on monolith ITF for failed\stopped context in context popup to re-execute with changed
    // params..
    // now when we have different reporting db - it's not valid logic...
    @Deprecated
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"EXECUTE\")")
    @RequestMapping(value = "/monitoring/setcontext",
            method = RequestMethod.POST,
            consumes = "application/json",
            headers = "Accept=application/json")
    @AuditAction(auditAction = "Merge context into tc-context id {{#contextId}} in the project {{#projectUuid}}")
    public void setContext(
            @RequestParam(value = "contextId") String contextId,
            @RequestBody JSONObject context,
            @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        //  I think this action is allowed only on the contexts initiated by callchains
        //TODO: it's incorrect to do on the itf-executor side! We don't have access to database with reported data
        TcContext tcContext = CoreObjectManager.getInstance().getManager(TcContext.class).getById(contextId);
        if (tcContext.getInitiator() == null || !(tcContext.getInitiator() instanceof CallChainInstance)) {
            throw new IllegalArgumentException(
                    String.format("Context ID=%s: initiator is NOT a callchain;  Update is cancelled", contextId));
        }
        tcContext.merge(context);
        CacheServices.getTcBindingCacheService().bind(tcContext);
        ExecutionServices.getExecutionProcessManagerService().updateContext(tcContext);
        tcContext.store();
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"EXECUTE\")")
    @RequestMapping(value = "/monitoring/terminateContexts", method = RequestMethod.POST)
    @AuditAction(auditAction = "Terminate contexts for project {{#projectUuid}}")
    public void terminate(@RequestBody UIIds ids,
                          @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid,
                          @SuppressWarnings("unused") @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        for (String id : ids.getIds()) {
            ExecutionServices.getTCContextService().stop(new BigInteger(id), tenantId);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"EXECUTE\")")
    @RequestMapping(value = "/monitoring/context/pauseResume", method = RequestMethod.POST)
    @AuditAction(auditAction = "Do '{{#action}}' action for contexts in the project {{#projectUuid}}")
    public UIGetReportList pauseResumeContext(@RequestParam(value = "action") String action,
                                              @RequestBody UIIds uiIds,
                                              @SuppressWarnings("unused") @RequestParam(
                                                      value = "projectUuid") UUID projectUuid,
                                              @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        Status newStatus;
        if (action == null) {
            throw new IllegalArgumentException("Parameter 'action' (values are pause or resume) is null or missed");
        } else if (action.equalsIgnoreCase("pause")) {
            newStatus = Status.PAUSED;
        } else if (action.equalsIgnoreCase("resume")) {
            newStatus = Status.IN_PROGRESS;
        } else {
            throw new IllegalArgumentException(
                    String.format("Parameter 'action' (values are pause or resume) value is incorrect: '%s'", action));
        }
        UIGetReportList result = new UIGetReportList();

        /* Currently we:
             1. Stop at the 1st exception (maybe we should continue processing?)
             2. Silently do nothing on the context if it doesn't suit the conditions (maybe we should log it?)
         */
        for (String id : uiIds.getIds()) {
            executorToMessageBrokerSender.sendMessageToTcContextOperationsTopic(
                    new TcContextOperationMessage(newStatus.name(), new BigInteger(id)), tenantId);
            result.getReportItems().add(buildUIReportItem(CacheServices.getTcContextCacheService()
                    .getById(new BigInteger(id))));
        }
        return result;
    }
}
