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

package org.qubership.automation.itf.core.message.parser;

import java.math.BigInteger;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.automation.itf.core.message.parser.testimpl.TestRuleProvider;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testng.Assert;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ProjectSettingsServiceTestConfig.class, CoreServices.class})
public class ParserTest {

    @Autowired
    ProjectSettingsServiceTest projectSettingsServiceTest;

    BigInteger projectId;

    @Before
    public void setUp() {
        projectId = new BigInteger("123");
        Config config = new Config();
        config.setInstanceName("test");
        Hazelcast.newHazelcastInstance(config);
    }

    @Test
    public void testParse() {
        Message message = new Message("<note>\n<to>Tove</to>\n<from>Jani</from>\n<heading>Reminder</heading>\n" +
                "<body>Don't forget me this weekend!</body>\n</note>");
        Parser parser = new Parser();
        ParsingRuleProvider provider = new TestRuleProvider();
        TcContext context = new TcContext();
        context.put("aaa", "bbb");
        Map<String, MessageParameter> parse = parser.parse(projectId, message, InstanceContext.from(context, null),
                provider);
        Assert.assertEquals(parse.size(), 2);
        Assert.assertEquals(parse.get("regex").getSingleValue(), "Jani");
        Assert.assertTrue(parse.get("xpath").isMultiple());
        Assert.assertEquals(parse.get("xpath").getMultipleValue().size(), 4);
        Assert.assertEquals(parse.get("xpath").getMultipleValue().get(0), "Tove");
        Assert.assertEquals(parse.get("xpath").getMultipleValue().get(1), "Jani");
        Assert.assertEquals(parse.get("xpath").getMultipleValue().get(2), "Reminder");
        Assert.assertEquals(parse.get("xpath").getMultipleValue().get(3), "Don't forget me this weekend!");
    }
}
