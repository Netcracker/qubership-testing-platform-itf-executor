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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.JDBCException;
import org.json.simple.parser.ParseException;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.integration.reports.ReportsService;
import org.qubership.automation.itf.ui.messages.objects.ResponseObject;
import org.qubership.automation.itf.ui.messages.objects.UIVelocityRequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VelocityController {

    private final ReportsService reportsService;

    @Autowired
    public VelocityController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    /*
     * Parse template content.
     * projectId parameter is made non-mandatory, because this endpoint is used from ITF Lite too.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @PutMapping(value = "/velocity", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Parse velocity content, project {{#projectId}}/{{#projectUuid}}")
    public ResponseObject parseContent(@RequestBody UIVelocityRequestBody requestBody,
                                       @RequestParam(value = "projectId", required = false) BigInteger projectId,
                                       @RequestParam(value = "projectUuid") UUID projectUuid)
            throws ParseException, IllegalAccessException, InstantiationException {
        if (projectId == null) {
            //noinspection unchecked
            projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class, SearchManager.class)
                    .getEntityInternalIdByUuid(projectUuid);
            if (projectId == null) {
                ResponseObject responseObject = new ResponseObject();
                responseObject.setResponse(String.format("Can't find project by uuid = %s", projectUuid));
                return responseObject;
            }
        }
        String context = requestBody.getContext();
        String message = requestBody.getMessage();
        TcContext tcContext;
        String process = "";
        try {
            if (StringUtils.isBlank(context)) {
                tcContext = new TcContext();
            } else {
                List<Object[]> contextProperties = reportsService.getContextProperties(context, projectUuid);
                if (contextProperties == null || contextProperties.isEmpty()) {
                    tcContext = CacheServices.getTcBindingCacheService()
                            .findByKey(context, false, projectId, projectUuid);
                    if (tcContext == null) {
                        tcContext = new TcContext();
                    }
                } else {
                    tcContext = JsonContext.fromJson(contextProperties.get(0)[7].toString(), TcContext.class);
                }
            }
            if (tcContext.getProjectId() == null) {
                tcContext.setProjectId(projectId);
                tcContext.setProjectUuid(projectUuid);
            }
            SpContext sp = new SpContext();
            Object saved = tcContext.get("saved");
            if (saved instanceof Map) {
                sp.putAll((Map) saved);
            }
            InstanceContext instanceContext = InstanceContext.from(tcContext, sp);
            process += TemplateEngineFactory.process(null, message, instanceContext);
        } catch (Exception e) {
            process = processException(e);
        }
        ResponseObject responseObject = new ResponseObject();
        responseObject.setResponse(process);
        return responseObject;
    }

    /*
     * Parse template content.
     * It's used from src/main/angular/src/app/modules/template/template.view.debug.js only.
     * So, projectId parameter remains mandatory, contrary to "/velocity" endpoint above,
     * which is used from ITF Lite too.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/velocity/processTemplate", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Process velocity template, project {{#projectId}}/{{#projectUuid}}")
    public ResponseObject processTemplate(@RequestBody UIVelocityRequestBody requestBody,
                                          @RequestParam(value = "projectId") BigInteger projectId,
                                          @RequestParam(value = "projectUuid") UUID projectUuid)
            throws ParseException, IllegalAccessException, InstantiationException {
        String tcContextStr = requestBody.getTc();
        String spContextStr = requestBody.getSp();
        if (tcContextStr == null && spContextStr == null) {
            return parseContent(requestBody, projectId, projectUuid);
        }
        String templateBody = requestBody.getMessage();
        String result;
        try {
            InstanceContext context = new InstanceContext();
            TcContext tcContext = null;
            if (tcContextStr != null) {
                tcContext = TcContext.fromJson(tcContextStr, TcContext.class);
                if (projectId != null) {
                    tcContext.setProjectId(projectId);
                    tcContext.setProjectUuid(projectUuid);
                }
                context.setTC(tcContext);
            }
            if (spContextStr != null) {
                SpContext spContext = SpContext.fromJson(spContextStr, SpContext.class);
                if (tcContext != null) {
                    Object saved = tcContext.get("saved");
                    if (saved instanceof Map) {
                        spContext.putAll((Map) saved);
                    }
                }
                context.setSP(spContext);
            }
            result = TemplateEngineFactory.process(null, templateBody, context);
        } catch (Exception e) {
            result = processException(e);
        }
        ResponseObject response = new ResponseObject();
        response.setResponse(result);
        return response;
    }

    private String processException(Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return ExceptionUtils.getMessage(ex) + (ex instanceof ParseException ? ex.toString() : "");
        } else if (cause instanceof JDBCException) {
            throw new RuntimeException(ex.getMessage()
                    + "\nCaused by: " + ((JDBCException) cause).getSQLException().getMessage());
        } else if (cause instanceof DataAccessException) {
            throw new RuntimeException(ex.getMessage()
                    + "\nCaused by: " + ((DataAccessException) cause).getMessage()
                    + ((cause.getCause() != null && cause.getCause() instanceof JDBCException)
                    ? "\nCaused by: " + ((JDBCException) cause.getCause()).getSQLException().getMessage()
                    : "")
            );
        } else if (cause instanceof RuntimeException) {
            return processException(cause);
        } else {
            return ExceptionUtils.getMessage(ex);
        }
    }
}
