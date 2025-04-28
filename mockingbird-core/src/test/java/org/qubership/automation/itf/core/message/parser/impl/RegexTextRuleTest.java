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

package org.qubership.automation.itf.core.message.parser.impl;

import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.util.parser.ParsingRuleType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RegexTextRuleTest {

    @Test
    public void testApplySingle() throws Exception {
        Message message = new Message("to test regexp");
        ParsingRule rule = new SystemParsingRule();
        rule.setParsingType(ParsingRuleType.REGEX);
        rule.setParamName("test");
        rule.setMultiple(false);
        rule.setExpression(".*es");
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        MessageParameter apply = rule.apply(message, InstanceContext.from(context, null), false);
        Assert.assertEquals(apply.getSingleValue(), "to tes");
        Assert.assertEquals(apply.getParamName(), "test");
    }

    @Test
    public void testApplyMulti() throws Exception {
        Message message = new Message("to test regexp");
        ParsingRule rule = new SystemParsingRule();
        rule.setParsingType(ParsingRuleType.REGEX);
        rule.setParamName("test");
        rule.setMultiple(true);
        rule.setExpression("t");
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        MessageParameter apply = rule.apply(message, InstanceContext.from(context, null), false);
        Assert.assertEquals(apply.getMultipleValue().size(), 3);
        Assert.assertEquals(apply.getParamName(), "test");
    }
}
