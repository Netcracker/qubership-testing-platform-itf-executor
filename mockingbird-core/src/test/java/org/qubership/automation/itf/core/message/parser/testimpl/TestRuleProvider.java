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

package org.qubership.automation.itf.core.message.parser.testimpl;

import java.util.Set;

import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.parser.ParsingRuleType;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;

import com.google.common.collect.Sets;

public class TestRuleProvider extends AbstractStorable implements ParsingRuleProvider {

    public Set<ParsingRule> returnParsingRules() {
        System system = new System();
        system.setName("test1");

        ParsingRule firstRule = new SystemParsingRule();
        firstRule.setMultiple(false);
        firstRule.setParsingType(ParsingRuleType.REGEX);
        firstRule.setParamName("regex");
        firstRule.setExpression("from>(.*)</from");
        firstRule.setParent(system);

        ParsingRule secondRule = new SystemParsingRule();
        secondRule.setMultiple(true);
        secondRule.setParsingType(ParsingRuleType.XPATH);
        secondRule.setParamName("xpath");
        secondRule.setExpression("//*[text()]/text()");
        secondRule.setParent(system);

        return Sets.newHashSet(firstRule, secondRule);
    }

    @Override
    public void addParsingRule(ParsingRule parsingRule) {
        // empty method
    }

    @Override
    public void removeParsingRule(ParsingRule parsingRule) {
        // empty method
    }
}
