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

package org.qubership.automation.itf.integration.bv.utils;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.report.LinkCollectorConfiguration;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.core.util.logger.ItfLogger;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.integration.bv.engine.BvEngineIntegration;
import org.qubership.automation.itf.integration.bv.engine.BvInstance;
import org.qubership.automation.itf.integration.bv.engine.BvInstanceExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public class BvHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BvHelper.class);
    private static final Logger ITF_LOGGER = ItfLogger.getLogger(BvEngineIntegration.class);

    public static URL normalizeUrl(String url, String endpoint) {
        URI uri = URI.create(url.trim());
        try {
            if (uri.getPath().endsWith("/")) {
                uri = uri.resolve(uri.getPath());
            } else {
                uri = uri.resolve(uri.getPath() + "/");
            }
            return uri.resolve(endpoint.trim()).toURL();
        } catch (MalformedURLException | NullPointerException e) {
            throw new EngineIntegrationException("Error building URL for Bulk Validator request: URL = "
                    + ((StringUtils.isBlank(url)) ? "null or empty" : url) + ", Endpoint = "
                    + ((StringUtils.isBlank(endpoint)) ? "null or empty" : endpoint) + "!", e);
        }
    }

    public static void endWithFailed(String errorMessage, BvInstance bvInstance) {
        EngineIntegrationException exception = new EngineIntegrationException(errorMessage);
        bvInstance.setStatus(Status.FAILED);
        bvInstance.setError(exception);
        bvInstance.setEndTime(new Date());
        SpContext spContext = null;
        Report.error(bvInstance, "Validation Phase failed", spContext, exception);
        ITF_LOGGER.debug(errorMessage, exception);
        throw exception;
    }

    public static void completeSuccessfully(CallChainInstance chainInstance, BvInstance bvInstance, String reportLink,
                                            String message, String status) {
        LOGGER.info(message);
        String deleteKey = "Bulk Validator Link : Not validated";
        Map<String, String> rl = chainInstance.getContext().tc().getReportLinks();
        rl.remove(deleteKey);
        rl.put("Bulk Validator Link : " + status, reportLink);
        bvInstance.setStatus(Status.PASSED);
        bvInstance.setEndTime(new Date());
    }

    public static void addOnCaseFinishValidation(CallChainInstance instance, CallChain callChain, String datasetName,
                                                 boolean validateTestcase, String bvAction) {
        String bvCaseId = callChain.getBvCases().get(datasetName);
        if (!Strings.isNullOrEmpty(bvCaseId)) {
            BvInstanceExtension extension = instance.getExtension(BvInstanceExtension.class);
            if (extension == null) {
                extension = new BvInstanceExtension();
                instance.extend(extension);
            }
            extension.validate = validateTestcase && !Strings.isNullOrEmpty(bvCaseId); // Run validation after
            // callchain execution
            extension.bvCaseId = bvCaseId;
            extension.bvAction = bvAction;
        } else {
            LOGGER.info("Bulk validation won't be started after completing the {} callchain, because there is no "
                    + "BV-case for {} dataset", callChain.getName(), datasetName);
        }
    }

    public static void addMessageOnStepValidation(CallChainInstance instance) {
        BvInstanceExtension extension = instance.getExtension(BvInstanceExtension.class);
        if (extension == null) {
            extension = new BvInstanceExtension();
            instance.extend(extension);
        }
        extension.validateMessages = true;
    }

    public static String buildBVConfPath(String confFromIntegrationTab, CallChainInstance callchainInstance) {
        if (confFromIntegrationTab.contains("/") || confFromIntegrationTab.isEmpty()) {
            return confFromIntegrationTab;
        }
        String fromEnv = getBVPathFromEnv(callchainInstance);
        Map<String, Storable> owners = Maps.newHashMap();
        owners.put(TemplateEngine.ENVIRONMENT, callchainInstance.getContext().getTC().getEnvironmentById());
        return TemplateEngineFactory.get().process(owners, fromEnv, callchainInstance.getContext(),
                "BvHelper#buildBVConfPath");
    }

    private static String getBVPathFromEnv(CallChainInstance callchainInstance) {
        Environment env = callchainInstance.getContext().getTC().getEnvironmentById();
        Collection<LinkCollectorConfiguration> reportCollectors = env.getReportCollectors();
        return reportCollectors != null && !reportCollectors.isEmpty()
                ? reportCollectors.iterator().next().get("bv.conf.path") : null;
    }

    public static String getProjectUUID(BigInteger projectId) {
        String uuid;
        synchronized (projectId) {
            UUID uuidFromDB =
                    CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId).getUuid();
            if (Objects.isNull(uuidFromDB)) {
                throw new EngineIntegrationException("BulkValidator Project UUID not found!");
            }
            uuid = uuidFromDB.toString();
        }
        return uuid;
    }
}
