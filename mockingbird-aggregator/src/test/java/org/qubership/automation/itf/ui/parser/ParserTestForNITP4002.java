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

package org.qubership.automation.itf.ui.parser;

import java.rmi.RemoteException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.automation.itf.core.message.parser.ProducerMessageHelper;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.util.exception.ExportException;
import org.qubership.automation.itf.core.util.exception.FindRegistryException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = {"classpath*:*ui-test-context.xml"})
public class ParserTestForNITP4002 {

    private static Template template;
    private static InstanceContext context;
    private static String text = "$!tc.ggg.Test";

    @BeforeAll
    public static void prepare() throws ExportException, RemoteException, FindRegistryException {
        template = new SystemTemplate();
        template.setName("template");
        template.setText(text);
        context = new InstanceContext();
        TcContext tcContext = new TcContext();
        JsonContext jsonContext = new JsonContext();
        jsonContext.put("Test", "999");
        tcContext.put("ggg", jsonContext);
        context.setTC(tcContext);
    }

    @Test
    public void testProduceMessage() throws RemoteException {
        Message message = ProducerMessageHelper.getInstance().produceMessage(template, context, "org.qubership"
                + ".automation.itf.transport.file.ftp.outbound.FileOverFtpOutbound");
        Assertions.assertNotNull(message);
        Assertions.assertEquals("999", message.getText(), "Message is bad parsed");
    }
//    @Test
//    public void testValidateName() throws Exception {
//
//        Assert.assertEquals("new message", "JSONContextName1", ((JsonStorable) context.getTC().get("ggg")).getName());
//
//    }
}
