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

package org.qubership.automation.itf.transport.jms.inbound;

import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.ADDITIONAL_JNDI_PROPERTIES;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.AUTHENTICATION;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.CONNECTION_FACTORY;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.CREDENTIALS;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.DESTINATION;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.DESTINATION_TYPE;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.INITIAL_CONTEXT_FACTORY;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.MAX_ATTEMTPS;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.MESSAGE_SELECTOR;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.PRINCIPAL;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.PROVIDER_URL;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.RECOVERY_INTERVAL;

import java.util.Map;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UserName("Inbound JMS Asynchronous")
public class JMSInboundTransport extends AbstractInboundTransportImpl {
    public static final Logger LOGGER = LoggerFactory.getLogger(JMSInboundTransport.class);
    @Parameter(shortName = PROVIDER_URL, longName = "Provider URL",
            description = PropertyConstants.Commons.PROVIDER_URL_DESCRIPTION, fromServer = true)
    protected String providerUrl;

    @Parameter(shortName = MESSAGE_SELECTOR, longName = "Message selector",
            description = PropertyConstants.Jms.MESSAGE_SELECTOR_DESCRIPTION, fromServer = true, optional = true)
    protected String messageSelector;

    @Parameter(shortName = DESTINATION, longName = "In Data Destination",
            description = PropertyConstants.Jms.DESTINATION_DESCRIPTION)
    protected String destination;

    @Parameter(shortName = PRINCIPAL, longName = "Principal",
            description = PropertyConstants.Commons.PRINCIPAL_DESCRIPTION, optional = true)
    protected String principal;

    @Parameter(shortName = AUTHENTICATION, longName = "Authentication",
            description = PropertyConstants.Commons.AUTHENTICATION_DESCRIPTION, optional = true)
    protected String authentication;

    @Parameter(shortName = CREDENTIALS, longName = "Credentials",
            description = PropertyConstants.Commons.CREDENTIALS_DESCRIPTION, optional = true)
    protected String credentials;

    @Parameter(shortName = CONNECTION_FACTORY, longName = "Connection Factory Name",
            description = PropertyConstants.Jms.CONNECTION_FACTORY_DESCRIPTION)
    protected String connectionFactoryName;

    @Parameter(shortName = INITIAL_CONTEXT_FACTORY, longName = "Initial Context Factory",
            description = PropertyConstants.Jms.INITIAL_CONTEXT_FACTORY_DESCRIPTION)
    protected String initialContextFactory;

    @Parameter(shortName = ADDITIONAL_JNDI_PROPERTIES, longName = "Additional JNDI Properties",
            description = PropertyConstants.Jms.ADDITIONAL_JNDI_PROPERTIES_DESCRIPTION, optional = true)
    protected Map<String, String> addJndiProps;

    @Parameter(shortName = DESTINATION_TYPE, longName = "JMS destination type. Queue/Topic",
            description = PropertyConstants.Jms.DESTINATION_TYPE_DESCRIPTION)
    @Options({"Queue", "Topic"})
    protected String destinationType;

    @Parameter(shortName = RECOVERY_INTERVAL, longName = "Connection Recovery Interval",
            description = PropertyConstants.Jms.RECOVERY_INTERVAL_DESCRIPTION, optional = true)
    protected String recoveryInterval;

    @Parameter(shortName = MAX_ATTEMTPS, longName = "Connection Max Attempts",
            description = PropertyConstants.Jms.MAX_ATTEMPTS_DESCRIPTION, optional = true)
    protected String maxAttempts;

    public JMSInboundTransport() {
    }

    @Override
    public String getShortName() {
        return "JMS Inbound";
    }

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_ASYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-jms";
    }
}
