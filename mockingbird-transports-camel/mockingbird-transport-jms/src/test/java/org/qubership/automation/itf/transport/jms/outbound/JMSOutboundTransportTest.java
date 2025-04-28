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

package org.qubership.automation.itf.transport.jms.outbound;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.InitialContext;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Ignore;
import org.junit.Test;
import org.qubership.automation.itf.transport.jms.InitialContextBuilder;

public class JMSOutboundTransportTest {
    private final InitialContextBuilder initialContextBuilder = new InitialContextBuilder();

    /*
    This class added for testing connection to WebLogic and send messages to JMSDestination.
    It DOST'T TEST any functional
     */

    @Ignore
    @Test
    public void testSendJMSMessage() throws Exception {
        InitialContext initialContext = initialContextBuilder.createContext();
        ConnectionFactory factory = (ConnectionFactory) initialContext.lookup("MB_Connection_Factory_Out");
        Destination destination = (Destination) initialContext.lookup("MB_Out_Queue");
        DefaultCamelContext context = new DefaultCamelContext();
        JmsComponent component = JmsComponent.jmsComponent(factory);
        context.start();
        context.addComponent("jms", component);

        ProducerTemplate producer = context.createProducerTemplate();
        producer.start();


        JmsEndpoint endpoint = JmsEndpoint.newInstance(destination, component);
        Exchange passed = producer.send(endpoint, exchange -> exchange.getIn().setBody("Passed"));
        System.out.println(passed.isFailed());


    }
}
