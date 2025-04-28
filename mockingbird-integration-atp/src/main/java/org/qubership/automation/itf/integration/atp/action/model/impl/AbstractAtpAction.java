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

package org.qubership.automation.itf.integration.atp.action.model.impl;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;

import org.qubership.automation.itf.integration.atp.model.ArgumentValue;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;

public abstract class AbstractAtpAction implements CommonAtpAction {
    private String name;
    private String description;
    private String mask;
    private boolean deprecated;
    private String template;

    public AbstractAtpAction(String name, String description, String mask, String template) {
        setName(name);
        setDescription(description);
        setMask(mask);
        setDeprecated(false);
        setTemplate(template);
    }

    public abstract List<ArgumentValue> getAvailableValues(BigInteger projectId);

    public abstract void setTestRunInfoParams(TestRunInfo testRunInfo, Matcher matcher);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
