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

import static org.testng.AssertJUnit.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.util.exception.ContentException;
import org.qubership.automation.itf.core.util.parser.ParsingRuleType;
import org.qubership.automation.itf.core.util.provider.content.XmlContentProvider;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:hibernate-configuration-test-context.xml"})
public class XpathXmlRuleTest {

    public static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<keys>" + "<key>" + "<value>a"
            + "</value>" + "</key>" + "<key>" + "<value>b</value>" + "</key>" + "</keys>";

    @Test
    public void testGetNodeList() throws ContentException {
        ParsingRule xpathXmlRule = new SystemParsingRule();
        xpathXmlRule.setParsingType(ParsingRuleType.XPATH);
        xpathXmlRule.setExpression("//keys");
        Message complexMessage = new Message(XML);
        complexMessage.setContent(new XmlContentProvider().provide(complexMessage));
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        MessageParameter sp = xpathXmlRule.apply(complexMessage, InstanceContext.from(context, null), false);
        assertTrue(sp.getSingleValue().startsWith("<keys>"));
    }

    @Test
    public void testGetText() throws ContentException {
        ParsingRule xpathXmlRule = new SystemParsingRule();
        xpathXmlRule.setParsingType(ParsingRuleType.XPATH);
        xpathXmlRule.setExpression("//value/text()");
        Message complexMessage = new Message(XML);
        complexMessage.setContent(new XmlContentProvider().provide(complexMessage));
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        MessageParameter sp = xpathXmlRule.apply(complexMessage, InstanceContext.from(context, null), false);
        assertTrue(sp.getSingleValue().startsWith("a"));
    }
}
