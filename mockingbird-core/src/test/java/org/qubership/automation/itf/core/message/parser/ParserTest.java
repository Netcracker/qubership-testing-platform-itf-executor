/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.core.message.parser;

import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.automation.itf.core.message.parser.testimpl.TestRuleProvider;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

@SpringJUnitConfig(classes = {ProjectSettingsServiceTestConfig.class, CoreServices.class})
public class ParserTest {

    @Autowired
    ProjectSettingsServiceTest projectSettingsServiceTest;

    static BigInteger projectId;

    @BeforeAll
    public static void setUp() {
        projectId = new BigInteger("123");
        Config config = new Config();
        config.setInstanceName("test");
        Hazelcast.newHazelcastInstance(config);
    }

    @Test
    public void testParse() {
        Message message = new Message("""
                <note>
                <to>Tove</to>
                <from>Jani</from>
                <heading>Reminder</heading>
                <body>Don't forget me this weekend!</body>
                </note>""");
        Parser parser = new Parser();
        ParsingRuleProvider provider = new TestRuleProvider();
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        Map<String, MessageParameter> parse = parser.parse(projectId, message, InstanceContext.from(context, null),
                provider);
        Assertions.assertEquals(2, parse.size());
        Assertions.assertEquals("Jani", parse.get("regex").getSingleValue());
        Assertions.assertTrue(parse.get("xpath").isMultiple());
        Assertions.assertEquals(4, parse.get("xpath").getMultipleValue().size());
        Assertions.assertEquals("Tove", parse.get("xpath").getMultipleValue().get(0));
        Assertions.assertEquals("Jani", parse.get("xpath").getMultipleValue().get(1));
        Assertions.assertEquals("Reminder", parse.get("xpath").getMultipleValue().get(2));
        Assertions.assertEquals("Don't forget me this weekend!", parse.get("xpath").getMultipleValue().get(3));
    }
}
