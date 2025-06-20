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

package org.qubership.automation.itf.core.template.velocity;

import static org.qubership.automation.itf.Constants.ENV_INFO_KEY;
import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.VELOCITY_CONFIG;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.Data;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.apache.velocity.tools.config.ToolConfiguration;
import org.apache.velocity.tools.config.ToolboxConfiguration;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.helper.ClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;

public class VelocityTemplateEngine implements TemplateEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(VelocityTemplateEngine.class);
    private final VelocityEngine engine;
    private final ToolManager toolManager;

    @Inject
    public VelocityTemplateEngine() {
        String velocityConfig = Config.getConfig().getString(VELOCITY_CONFIG);
        if (!Strings.isNullOrEmpty(velocityConfig)) {
            LOGGER.info("Init Apache Velocity engine with settings {}", velocityConfig);
            engine = new VelocityEngine(velocityConfig);
        } else {
            LOGGER.info("Init Apache Velocity engine with default settings");
            engine = new VelocityEngine();
        }
        engine.setProperty(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, "true");
        engine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.qubership.automation.itf.core.template.velocity.log.Slf4jLogChute");
        engine.setProperty("console.logsystem.max.level", "WARN");
        engine.setProperty("runtime.log.logsystem.max.level", "WARN");
        engine.init();
        for (Class directiveClass : ClassResolver.getInstance().getSubtypesOf(Directive.class)) {
            engine.loadDirective(directiveClass.getName());
        }

        FactoryConfiguration factoryConfiguration = makeGenericFactoryConfig();
        factoryConfiguration.addConfiguration(makeStrutsFactoryConfig());
        factoryConfiguration.addConfiguration(makeViewFactoryConfig());

        toolManager = new ToolManager();

        toolManager.getToolboxFactory().configure(factoryConfiguration);

        toolManager.setVelocityEngine(engine);
    }

    private static Data fillData(String type, String key, Object value) {
        Data data = new Data();
        data.setType(type);
        data.setKey(key);
        data.setValue(value);
        return data;
    }

    private static List<ToolConfiguration> makeToolsList(String... args) {
        List<ToolConfiguration> list = new ArrayList<>();
        for(String arg : args) {
            ToolConfiguration cfg = new ToolConfiguration();
            cfg.setClassname(arg);
            list.add(cfg);
        }
        return list;
    }

    private static FactoryConfiguration makeGenericFactoryConfig() {
        FactoryConfiguration factoryConfiguration = new FactoryConfiguration();
        factoryConfiguration.addData(fillData("number", "TOOLS_VERSION", "2.0"));
        factoryConfiguration.addData(fillData("boolean", "GENERIC_TOOLS_AVAILABLE", "true"));

        ToolboxConfiguration applicationToolboxConfiguration = new ToolboxConfiguration();
        applicationToolboxConfiguration.setScope("application");
        applicationToolboxConfiguration.setTools(makeToolsList(
                "org.apache.velocity.tools.generic.AlternatorTool",
                "org.apache.velocity.tools.generic.ClassTool",
                "org.apache.velocity.tools.generic.ComparisonDateTool",
                "org.apache.velocity.tools.generic.ConversionTool",
                "org.apache.velocity.tools.generic.DisplayTool",
                "org.apache.velocity.tools.generic.EscapeTool",
                "org.apache.velocity.tools.generic.FieldTool",
                "org.apache.velocity.tools.generic.MathTool",
                "org.apache.velocity.tools.generic.NumberTool",
                "org.apache.velocity.tools.generic.ResourceTool",
                "org.apache.velocity.tools.generic.SortTool"));
        factoryConfiguration.addToolbox(applicationToolboxConfiguration);

        ToolboxConfiguration requestToolboxConfiguration = new ToolboxConfiguration();
        requestToolboxConfiguration.setScope("request");
        requestToolboxConfiguration.setTools(makeToolsList(
                "org.apache.velocity.tools.generic.ContextTool",
                "org.apache.velocity.tools.generic.LinkTool",
                "org.apache.velocity.tools.generic.LoopTool",
                "org.apache.velocity.tools.generic.RenderTool"));
        factoryConfiguration.addToolbox(requestToolboxConfiguration);
        return factoryConfiguration;
    }

    private static FactoryConfiguration makeStrutsFactoryConfig() {
        FactoryConfiguration factoryConfiguration = new FactoryConfiguration();
        factoryConfiguration.addData(fillData("boolean", "STRUTS_TOOLS_AVAILABLE", "true"));

        ToolboxConfiguration requestToolboxConfiguration = new ToolboxConfiguration();
        requestToolboxConfiguration.setScope("request");
        requestToolboxConfiguration.setTools(makeToolsList(
                "org.apache.velocity.tools.struts.ActionMessagesTool",
                "org.apache.velocity.tools.struts.ErrorsTool",
                "org.apache.velocity.tools.struts.FormTool",
                "org.apache.velocity.tools.struts.MessageTool",
                "org.apache.velocity.tools.struts.StrutsLinkTool",
                "org.apache.velocity.tools.struts.TilesTool",
                "org.apache.velocity.tools.struts.ValidatorTool"));
        factoryConfiguration.addToolbox(requestToolboxConfiguration);
        return factoryConfiguration;
    }

    private static FactoryConfiguration makeViewFactoryConfig() {
        FactoryConfiguration factoryConfiguration = new FactoryConfiguration();
        factoryConfiguration.addData(fillData("boolean", "VIEW_TOOLS_AVAILABLE", "true"));

        ToolboxConfiguration requestToolboxConfiguration = new ToolboxConfiguration();
        requestToolboxConfiguration.setScope("request");
        requestToolboxConfiguration.setTools(makeToolsList(
                "org.apache.velocity.tools.view.CookieTool",
                "org.apache.velocity.tools.view.ImportTool",
                "org.apache.velocity.tools.view.IncludeTool",
                "org.apache.velocity.tools.view.LinkTool",
                "org.apache.velocity.tools.view.PagerTool",
                "org.apache.velocity.tools.view.ParameterTool",
                "org.apache.velocity.tools.view.ViewContextTool",
                "org.apache.velocity.tools.generic.ResourceTool"));
        factoryConfiguration.addToolbox(requestToolboxConfiguration);

        ToolboxConfiguration sessionToolboxConfiguration = new ToolboxConfiguration();
        sessionToolboxConfiguration.setScope("session");
        sessionToolboxConfiguration.setProperty("createSession", "false");
        sessionToolboxConfiguration.setTools(makeToolsList(
                "org.apache.velocity.tools.view.BrowserTool"));
        factoryConfiguration.addToolbox(sessionToolboxConfiguration);
        return factoryConfiguration;
    }

    public static BigInteger extractProjectIdFromContextAdapter(InternalContextAdapter internalContextAdapter) {
        if (Objects.nonNull(internalContextAdapter.get("tc"))) {
            return ((TcContext) internalContextAdapter.get("tc")).getProjectId();
        }
        Object owner = internalContextAdapter.get(TemplateEngine.OWNER);
        if (Objects.nonNull(owner)) {
            if (owner instanceof System) {
                return ((System) owner).getProjectId();
            } else if (owner instanceof Template) {
                Object templateParent = ((Template) owner).getParent();
                if (templateParent instanceof Operation) {
                    return ((Operation) templateParent).getProjectId();
                } else if (templateParent instanceof System) {
                    return ((System) templateParent).getProjectId();
                }
            }
        }
        return null;
    }

    @Override
    public String process(Storable owner, String someString, JsonContext context) {
        return process(owner, someString, context, "");
    }

    @Override
    public String process(Map<String, Storable> storables, String someString, JsonContext context) {
        return process(storables, someString, context, "");
    }

    @Override
    public String process(Storable owner, String someString, JsonContext context, String coords) {
        if (StringUtils.isBlank(someString)) {
            return "";
        }
        return processing(owner, someString, context, toolManager.createContext(), coords);
    }

    @Override
    public String process(Map<String, Storable> storables, String someString, JsonContext context, String coords) {
        if (StringUtils.isBlank(someString)) {
            return "";
        }
        Context velocityContext = toolManager.createContext();
        putIfPresent(velocityContext, OPERATION, storables.get(OPERATION));
        putIfPresent(velocityContext, ENVIRONMENT, storables.get(ENVIRONMENT));
        putIfPresent(velocityContext, INITIATOR, storables.get(INITIATOR));
        Storable owner = storables.get(OWNER);
        return processing(owner, someString, context, velocityContext, coords);
    }

    private void putIfPresent(Context velocityContext, String key, Storable obj) {
        if (obj != null) {
            velocityContext.put(key, obj);
        }
    }

    private String processing(Storable owner, String someString, JsonContext context, Context velocityContext,
                              String coords) {
        if (StringUtils.isBlank(someString)) {
            return "";
        }
        LOGGER.trace("Processing string with Velocity: {}\nContext: {}", someString, context);
        putIfPresent(velocityContext, OWNER, owner);
        velocityContext.put(HOST_NAME, Config.getConfig().getString("runningHostname"));
        for (Object o : context.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            velocityContext.put(String.valueOf(entry.getKey()), entry.getValue());
            if (entry.getValue() instanceof TcContext) {
                AbstractContainerInstance initiator = ((TcContext) entry.getValue()).getInitiator();
                if (initiator instanceof CallChainInstance && initiator.getContext() != null) {
                    velocityContext.put(ENV_INFO_KEY, initiator.getContext().get(ENV_INFO_KEY));
                }
            }
        }
        boolean isOwnerNameNotNull = owner != null && owner.getName() != null;
        try {
            StringWriter stringWriter = new StringWriter(someString.length());
            engine.evaluate(velocityContext, stringWriter, isOwnerNameNotNull ? owner.getName() : LOG_TAG, someString);
            String string = stringWriter.toString();
            LOGGER.trace("String processed, result is: {}", string);
            return string;
        } catch (Exception e) {
            throw new VelocityException(String.format("Error occurred while processing of %s: %s",
                    StringUtils.isBlank(coords)
                            ? "template '" + (isOwnerNameNotNull ? owner.getName() : LOG_TAG) + "'" : coords,
                    e.getMessage()), e);
        }
    }
}
