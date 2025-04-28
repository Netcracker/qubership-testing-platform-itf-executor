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

package org.qubership.automation.itf.report.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.qubership.automation.itf.core.model.container.StepContainer;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.config.Config;

public class ATPReportUtil {

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("\\w+://");

    public static String getExecutorHostName() {
        return Config.getConfig().getRunningUrl(); // Changed from getRunningHostname() after consultations with
        // Nikolay Diyakov (ATP Support)
    }

    public static String getTestingServerUrl(AbstractContainerInstance startedBy) {
        StepContainer stepContainer = startedBy.getStepContainer();
        if (stepContainer instanceof CallChain) {
            String server = getServerFromStarter(startedBy, stepContainer);
            if (server != null) {
                return server;
            }
        }
        return null;
    }

    /*  This method recursively determines Url of the server to report to ATP.
        It takes the 1st enabled situation/integration step
     */
    private static String getServerFromStarter(AbstractContainerInstance startedBy, StepContainer stepContainer) {
        if (stepContainer == null) {
            return null;
        }
        for (Step step : stepContainer.getSteps()) {
            if (step == null) {
                continue;
            }
            if (!step.isEnabled()) {
                continue;
            }
            if (step instanceof IntegrationStep) {
                return extractUrl(step, startedBy);
            } else if (step instanceof SituationStep) {
                return extractUrl(((SituationStep) step).getSituation().getIntegrationStep(), startedBy);
            } else if (step instanceof EmbeddedStep) {
                String result = getServerFromStarter(startedBy, ((EmbeddedStep) step).getChain());
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static String extractUrl(Step step, AbstractContainerInstance startedBy) {
        System receiver = ((IntegrationStep) step).getReceiver();
        Environment environment = startedBy.getContext().tc().getEnvironmentById();
        if (environment == null) {
            return "unknown server";
        }
        Server server = environment.getOutbound().get(receiver);
        if (server == null) {
            return String.format("Server is not defined for system '%s', check your environment '%s'",
                    receiver.getName(), environment.getName());
        }
        return server.getUrl();
    }

    public static String getSolutionBuild(String urlString) {
        Matcher matcher = PROTOCOL_PATTERN.matcher(urlString);
        if (matcher.find()) {
            urlString = matcher.replaceFirst("http://");
        }
        try {
            URL url = new URL(urlString + "/version.txt");
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            return IOUtils.toString(in, encoding);
        } catch (IOException e) {
            return "Unknown solution build";
        }
    }

    public static URL parseToUrl(String stringUrl) {
        try {
            return new URL(stringUrl);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Error while parsing ATP URL...", e);
        }
    }

    public static String extractPathFromUrl(URL url) {
        return url.getPath() + "?" + url.getQuery();
    }

    public static String extractAddressFromUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
    }

    public static String buildRunParamsInfo(CallChainInstance instance) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div>").append(String.format("Call chain link: <a href=\"%s#/callchain/%s\">%s</a>",
                        Config.getConfig().getRunningUrl(), instance.getStepContainer().getID(),
                        instance.getStepContainer().getName()))
                .append("</div><div>")
                .append(String.format("Host Name: %s", Config.getConfig().getRunningUrl()))
                .append("</div><div>")
                .append(String.format("DataSet: %s",
                        instance.getDatasetName() != null
                                ? instance.getDatasetName()
                                : "not set"))
                .append("</div><div>")
                .append(
                        String.format("Environment: %s", instance.getContext().tc() != null
                                ? instance.getContext().tc().getEnvironmentById().getName()
                                : "not set"))
                .append("</div>");
        return sb.toString();
    }
}
