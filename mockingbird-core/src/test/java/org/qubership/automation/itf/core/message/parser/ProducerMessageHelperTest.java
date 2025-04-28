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

import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.util.exception.ExportException;
import org.qubership.automation.itf.core.util.transport.base.Transport;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.executor.transports.registry.TransportRegistryLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:*core-test-context-no-broker-bean.xml"})
public class ProducerMessageHelperTest {

    private static final Set<Transport> transports = Sets.newConcurrentHashSet();
    private static Template template;
    private static InstanceContext context;

    @BeforeClass
    public static void prepare() throws ExportException {
        TransportRegistryManager.getInstance().init(new TransportRegistryLoader());
        transports.add(new TestRestOverHttpOutbound());
        transports.add(new TestFileOverFtpOutbound());
        template = new OperationTemplate();
        template.setName("template");
        String text = "##load_part(\"activateService Start part\")\n" + "#load_part(\"activateService " +
                "Start part Entry point\")\n" + "\n" + "\n" + "#$tc.dynamic_param_one = \"ONE\" " + "#$tc" +
                ".dynamic_param_two = \"TWO\" " + "\n" + "\n" + "\n" + "#$tc.filename = \"newFileName-\" + " +
                "#next_index" + "(\"NNN\")" + "\n" + "#load_part(\"activateService Finish part\")";
        template.setText(text);
        OutboundTemplateTransportConfiguration configurationRest = new OutboundTemplateTransportConfiguration(
                TestRestOverHttpOutbound.class.getName(), template);
        OutboundTemplateTransportConfiguration configurationFile = new OutboundTemplateTransportConfiguration(
                TestFileOverFtpOutbound.class.getName(), template);
        configurationRest.put("headers", "some_one=/one/$tc.dynamic_param_one\nsome_two=/two/$tc.dynamic_param_two");
        configurationFile.put("destinationFileName", "$tc.filename");
        template.getTransportProperties().add(configurationRest);
        template.getTransportProperties().add(configurationFile);
        context = new InstanceContext();
    }

    @Test
    public void testProduceMessage() {
        for (Transport transport : transports) {
            Message message = ProducerMessageHelper.getInstance().produceMessage(template, context,
                    transport.getClass().getTypeName());
            Assert.assertNotNull(message);
            Assert.assertFalse(message.getHeaders().isEmpty());
        }
    }
}
