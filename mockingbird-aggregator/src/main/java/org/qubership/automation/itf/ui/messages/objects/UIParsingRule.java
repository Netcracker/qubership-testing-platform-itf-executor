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

package org.qubership.automation.itf.ui.messages.objects;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;

public class UIParsingRule extends UIObject {

    private String multiple;
    private String type;
    private String expression;
    private String paramName;
    private String autosave;

    public UIParsingRule() {
    }

    public UIParsingRule(Storable parsingRule) {
        this((ParsingRule<? extends ParsingRuleProvider>) parsingRule);
    }

    public UIParsingRule(ParsingRule<? extends ParsingRuleProvider> parsingRule) {
        super(parsingRule);
        defineObjectParam(parsingRule);
    }

    public UIParsingRule(ParsingRule<? extends ParsingRuleProvider> parsingRule, boolean isFullWithParent) {
        super(parsingRule, isFullWithParent);
        defineObjectParam(parsingRule);
    }

    private void defineObjectParam(ParsingRule<? extends ParsingRuleProvider> parsingRule) {
        this.multiple = String.valueOf(parsingRule.getMultiple());
        if (parsingRule.getParsingType() != null) {
            this.type = parsingRule.getParsingType().toString();
        }
        this.expression = parsingRule.getExpression();
        this.paramName = parsingRule.getParamName();
        this.autosave = String.valueOf(parsingRule.getAutosave());
    }

    public String getMultiple() {
        return multiple;
    }

    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getAutosave() {
        return autosave;
    }

    public void setAutosave(String autosave) {
        this.autosave = autosave;
    }
}
