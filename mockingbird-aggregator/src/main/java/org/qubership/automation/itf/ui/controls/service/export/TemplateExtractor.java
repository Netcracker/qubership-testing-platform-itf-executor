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

package org.qubership.automation.itf.ui.controls.service.export;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TemplateExtractor {

    public static final String LOAD_PART_PATTERN = "#load_part\\S\"+(.[^\"]*)\"";

    /**
     * Extract template identifiers (template ids or names) with regex help from load_part construction.
     *
     * @param content template content or some string which you need check to load_part construction.
     * @return Set strings with template identifiers (template ids or names).
     */
    public Set<String> findLoadPartTemplates(String content) {
        Pattern pattern = Pattern.compile(LOAD_PART_PATTERN);
        Matcher matcher = pattern.matcher(content);
        Set<String> result = new HashSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * Get template from db by identifier (id or name + project id).
     *
     * @param templateIdentifier template id or name.
     * @param projectId          project id.
     * @return template object.
     */
    public Template<? extends TemplateProvider> getTemplateObject(String templateIdentifier, BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = TemplateHelper.getByNameAndProjectId(
                templateIdentifier, projectId);
        if (templates.size() != 0) {
            return templates.iterator().next();
        } else {
            log.warn("Template not found by name '{}' in project id='{}'. Trying to search by ID.",
                    templateIdentifier, projectId);
            try {
                Template<? extends TemplateProvider> template = TemplateHelper.getById(templateIdentifier);
                if (template == null) {
                    log.warn("Template is not found by id '{}'", templateIdentifier);
                    return null;
                }
                if (!projectId.equals(template.getProjectId())) {
                    log.warn("Template is not found by id '{}' in project id='{}'", templateIdentifier, projectId);
                    return null;
                }
                return template;
            } catch (IllegalArgumentException e) {
                log.warn("Can't find template by ID because identifier is not numeric value: {}.", templateIdentifier);
            }
        }
        return null;
    }
}
