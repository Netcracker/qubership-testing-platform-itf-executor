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

package org.qubership.automation.itf.core.template.velocity.directives;

import static org.qubership.automation.itf.core.template.velocity.VelocityTemplateEngine.extractProjectIdFromContextAdapter;
import static org.qubership.automation.itf.core.util.converter.IdConverter.toBigInt;

import java.io.Writer;
import java.math.BigInteger;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadPart extends Directive {

    @Override
    public String getName() {
        return "load_part";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node)
            throws ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        BigInteger projectId = extractProjectIdFromContextAdapter(internalContextAdapter);
        if (projectId == null) {
            log.warn("Project ID is null! Template can't be found");
            return true;
        }
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) != null) {
                String identifier = String.valueOf(node.jjtGetChild(i).value(internalContextAdapter));
                if (StringUtils.isEmpty(identifier)) {
                    log.warn("#load_part directive: Template name[{}] parameter should be not empty!", i);
                    continue;
                }
                Template<? extends TemplateProvider> template = loadTemplate(identifier, projectId);
                if (template != null) {
                    rsvc.evaluate(internalContextAdapter, writer, identifier, template.getText());
                } else {
                    throw new VelocityException("Unable to load part of template by identifier '" + identifier + "', "
                            + "template isn't found.");
                }
            } else {
                log.warn("#load_part directive: name[{}] parameter is null", i);
            }
        }
        return true;
    }

    private Template<? extends TemplateProvider> loadTemplate(String templateIdentifier, BigInteger projectId) {
        if (StringUtils.isEmpty(templateIdentifier)) {
            return null;
        } else if (StringUtils.isNumeric(templateIdentifier)) {
            BigInteger id;
            try {
                id = toBigInt(templateIdentifier);
            } catch (IllegalArgumentException e) {
                // Conversion isn't successful. Searching by name
                return findTemplateByName(templateIdentifier, projectId);
            }
            return TemplateHelper.getById(id);
        } else {
            return findTemplateByName(templateIdentifier, projectId);
        }
    }

    private Template<? extends TemplateProvider> findTemplateByName(String templateIdentifier, BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = TemplateHelper
                .getFirstPartByNameAndProjectId(templateIdentifier, projectId);
        if (templates.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("More than one template found by name '%s' in the project id %s",
                            templateIdentifier, projectId));
        } else if (templates.isEmpty()) {
            log.error("Template isn't found by name '{}' in project id {}", templateIdentifier, projectId);
            return null;
        }
        return templates.iterator().next();
    }
}
