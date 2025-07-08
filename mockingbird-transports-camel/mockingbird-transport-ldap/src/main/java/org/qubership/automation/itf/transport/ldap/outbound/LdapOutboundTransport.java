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

package org.qubership.automation.itf.transport.ldap.outbound;

import static org.qubership.automation.itf.transport.camel.Helper.GSON;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.ADDITIONAL_JNDI_PROPERTIES;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.AUTHENTICATION;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.CREDENTIALS;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.DEFAULT_OUTPUT_FORMAT;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.INITIAL_CONTEXT_FACTORY;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.LDAP_DATASOURCE;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.OUTPUT_FORMAT;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.PRINCIPAL;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.PROVIDER_URL;
import static org.qubership.automation.itf.transport.ldap.outbound.LdapConstants.RESPONSE_CODE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.transport.base.AbstractTransportImpl;
import org.qubership.automation.itf.core.util.transport.base.OutboundTransport;

public class LdapOutboundTransport extends AbstractTransportImpl implements OutboundTransport {

    @Parameter(shortName = INITIAL_CONTEXT_FACTORY,
            longName = "Initial Context Factory",
            description = "For example, com.sun.jndi.ldap.LdapCtxFactory",
            isRedefined = true)
    private String initialContextFactory;

    // It's discussable... When fromServer = true, the property is configurable only at the Environment
    @Parameter(shortName = PROVIDER_URL,
            longName = "Provider URL",
            description = "For example, ldap://some.ldap.server:389",
            fromServer = true)
    private String providerUrl;

    @Parameter(shortName = AUTHENTICATION,
            longName = "Authentication",
            description =
                    "Variants are: 'none' (=anonymous), 'simple' (weak authentication) or a space-separated " + "list"
                            + " of SASL mechanism names",
            optional = true)
    private String authentication;

    @Parameter(shortName = PRINCIPAL,
            longName = "Security Principal",
            description = "For example, cn=Manager; can be empty if Authentication=none",
            optional = true)
    private String principal;

    @Parameter(shortName = CREDENTIALS,
            longName = "Security Credentials",
            description = "Password; can be empty",
            optional = true)
    private String credentials;

    @Parameter(shortName = ADDITIONAL_JNDI_PROPERTIES,
            longName = "Additional JNDI Properties",
            description = "Extra JNDI properties, format is name=value, on the separate row each",
            optional = true)
    private Map<String, String> addJndiProps;

    @Parameter(shortName = OUTPUT_FORMAT,
            longName = "Search results formatting",
            description = "Response Format (for search requests only); LDIF (default) or JSON formats are supported",
            optional = true)
    @Options(value = {"LDIF", "JSON"})
    private String outputFormat;

    @Parameter(shortName = RESPONSE_CODE,
            longName = "Allow Status Code",
            description = "Regexp to test LDAP server response; please leave blank if only success is allowed",
            forTemplate = true,
            optional = true)
    private String allowStatus;

    @Override
    public String getShortName() {
        return "Ldap Outbound";
    }

    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                (String) connectionProperties.get(INITIAL_CONTEXT_FACTORY)); // "com.sun.jndi.ldap.LdapCtxFactory"
        props.setProperty(Context.PROVIDER_URL, (String) connectionProperties.get(PROVIDER_URL)); // "ldap
        // ://localhost:389"
        props.setProperty(Context.SECURITY_AUTHENTICATION,
                (String) connectionProperties.getOrDefault(AUTHENTICATION, "none")); // "simple"
        props.setProperty(Context.SECURITY_PRINCIPAL, (String) connectionProperties.get(PRINCIPAL)); // "cn=Manager"
        props.setProperty(Context.SECURITY_CREDENTIALS, (String) connectionProperties.get(CREDENTIALS)); // "secret"
        if (connectionProperties.get(ADDITIONAL_JNDI_PROPERTIES) != null) {
            props.putAll((Map<?, ?>) connectionProperties.get(ADDITIONAL_JNDI_PROPERTIES));
        }
        // May be, these two properties must be set by default?
        /*
        props.setProperty(Context.URL_PKG_PREFIXES, "com.sun.jndi.url");
        props.setProperty(Context.REFERRAL, "ignore");
        */

        /*  Check template content. It may be:
                1. Ldif change request (requests?) in JSON format
                2. Otherwise - search condition e.g. "(uid=ssd)"
            So, we try to parse message.getText() as Json.
            If it's JsonObject AND there is changeType property
                with the possible values ["add", "delete", "modify", "modrdn", "moddn"],
                then we invoke specific processing instead of Camel standard magic.
            Otherwise - we send search request via Camel.

            Why did we implement specific processing for LDAP change requests?
            - Because LDAP change requests are implemented in the separate Camel module camel-ldif,
            - AND this module is from Camel 2.20.x
            - AND I could not force it to work.
        */
        boolean isSearchRequest;
        Object jsonMessage = null;
        String changeType = null;
        try {
            JSONParser parser = new JSONParser();
            jsonMessage = parser.parse(message.getText());
            if (jsonMessage instanceof JSONObject) {
                changeType = (String) ((JSONObject) jsonMessage).get("changetype");
                isSearchRequest = (changeType == null);
            } else {
                isSearchRequest = true;
            }
        } catch (ParseException e) {
            // So it should be search condition
            isSearchRequest = true;
        }
        if (!isSearchRequest) {
            return sendChangeRequest((JSONObject) jsonMessage, props, changeType,
                    (String) connectionProperties.getOrDefault(RESPONSE_CODE, ""));
        }
        String outputFormat = (String) connectionProperties.getOrDefault(OUTPUT_FORMAT, DEFAULT_OUTPUT_FORMAT);
        String base = (String) props.getOrDefault("base", "ou=system");
        SimpleRegistry registry = new SimpleRegistry();
        registry.put(LDAP_DATASOURCE, new InitialLdapContext(props, null));
        CamelContext context = new DefaultCamelContext(registry);
        context.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start").to("ldap:" + LDAP_DATASOURCE + "?base=" + base); // The base DN for searches.
                // Default: ou=system
            }
        });
        context.start();
        ProducerTemplate template = context.createProducerTemplate();
        Endpoint endpoint = context.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();
        exchange.getIn().setBody(message.getText()); // body is a LDAP query
        Exchange out;
        try {
            out = template.send(endpoint, exchange);
            if (out.isFailed()) {
                throw out.getException();
            }
        } catch (Exception e) {
            context.stop();
            throw new Exception("Error sending/processing of LDAP search request", e);
        }
        Collection<SearchResult> data = out.getOut().getBody(Collection.class);
        Message response = new Message(dataToString(data, outputFormat)); // will be changed
        response.convertAndSetHeaders(exchange.getOut().getHeaders());
        context.stop();
        return response;
    }

    private Message sendChangeRequest(JSONObject jsonMessage, Properties props, String changeType, String allowStatus)
            throws Exception {
        DirContext ctx = new InitialDirContext(props);
        try {
            String name = (String) jsonMessage.get("dn");
            BasicAttributes attrs;
            switch (changeType) {
                case "modify":
                    ModificationItem[] mods = populateModificationItems(jsonMessage);
                    if (mods != null && mods.length > 0) {
                        ctx.modifyAttributes(name, mods);
                    } else {
                        throw new Exception(
                                "Invalid '" + changeType + "' LDAP request: no add/replace/delete actions are found");
                    }
                    break;
                case "delete":
                    ctx.destroySubcontext(name);
                    break;
                case "add":
                    attrs = populateAttributes(jsonMessage);
                    ctx.createSubcontext(name, attrs); // results are returned but they contain no useful information
                    break;
                case "moddn":
                    attrs = populateAttributes(jsonMessage);
                    moveToAnotherSuperior(ctx, name, (String) (attrs.get("newsuperior").get()), "newsuperior");
                    break;
                case "modrdn":
                    attrs = populateAttributes(jsonMessage);
                    renameUnderTheSameSuperior(ctx, name, (String) (attrs.get("newrdn").get()), "newrdn");
                    break;
                default:
                    throw new Exception("Unknown type of LDAP modification request: " + changeType);
            }
            return checkAllowedStatusCode(true, null, allowStatus);
        } catch (NamingException ex) {
            return checkAllowedStatusCode(false, ex, allowStatus);
        } catch (Exception e) {
            throw new Exception("Error sending/processing of LDAP change request", e);
        } finally {
            ctx.close();
        }
    }

    private Message checkAllowedStatusCode(boolean success, NamingException ex, String allowStatus) throws Exception {
        String successMessage = "Changes are applied successfully";
        if (allowStatus.isEmpty()) {
            // No status code checking is configured
            if (success) {
                return new Message(successMessage);
            } else {
                throw new Exception("NamingException while processing of LDAP change request", ex);
            }
        } else {
            if (success) {
                if (successMessage.matches(allowStatus)) {
                    return new Message(
                            successMessage + "\nSuccess because matches 'Allow status code' configured: " + allowStatus);
                } else {
                    throw new Exception(
                            successMessage + "\nFail because doesn't match 'Allow status code' " + "configured: " + allowStatus);
                }
            } else {
                // javax.naming.NameAlreadyBoundException: [LDAP: error code 68 - Entry Already Exists]; remaining
                // name 'uid=ssd2,ou=fixedline,ou=profiles,ou=exampleMobileProvider,c=org,o=exampleMobileProvider'
                /*
                 .replace('\n',' ') - is added because for some errors the explanation contains line-breaks what can
                 change .matches(allowStatus) result to false
                    For example: "javax.naming.directory.SchemaViolationException: [LDAP: error code 65 -
                    single-valued attribute "uid" has multiple values\n]; remaining name 'uid=ssd2,ou=fixedline,
                    ou=profiles,ou=exampleMobileProvider,c=org,o=exampleMobileProvider'"
                 .replace('\n',' ') - is removed. A user should (and definitely can) configure regexp properly
                */
                if (ex.getExplanation().matches(allowStatus)) {
                    return new Message(
                            "LDAP exception explanation: " + ex.getExplanation() + "\nSuccess because matches " +
                                    "'Allow status code' configured: " + allowStatus + "\nFull message: " + ex);
                } else {
                    throw new Exception(
                            "LDAP exception explanation: " + ex.getExplanation() + "Fail because doesn't match "
                                    + "'Allow" + " status code' configured: " + allowStatus, ex);
                }
            }
        }
    }

    private void moveToAnotherSuperior(DirContext ctx, String name, String newSuperior, String propName)
            throws Exception {
        LdapName oldLdapName = new LdapName(name);
        List<Rdn> oldRdns = oldLdapName.getRdns();
        int oldRdnsCount = oldRdns.size();
        if (oldRdnsCount == 0) {
            throw new Exception("moddn request: invalid parsing results for 'dn' LDAP name: " + name);
        }
        if (StringUtils.isBlank(newSuperior)) {
            throw new Exception("moddn request: '" + propName + "' value is null or empty");
        }
        LdapName newLdapSuperior = new LdapName(newSuperior);
        List<Rdn> newSuperiorRdns = newLdapSuperior.getRdns();
        int newRdnsCount = newSuperiorRdns.size();
        if (newRdnsCount == 0) {
            throw new Exception(
                    "moddn request: invalid parsing results for '" + propName + "' LDAP name: " + newSuperior);
        }
        List<Rdn> newRdns = new ArrayList<>(newSuperiorRdns);
        newRdns.add(oldRdns.get(oldRdnsCount - 1));
        LdapName newLdapName = new LdapName(newRdns);
        ctx.rename(oldLdapName, newLdapName);
    }

    private void renameUnderTheSameSuperior(DirContext ctx, String name, String newRdn, String propName)
            throws Exception {
        LdapName oldLdapName = new LdapName(name);
        List<Rdn> oldRdns = oldLdapName.getRdns();
        int rdnsCount = oldRdns.size();
        if (rdnsCount == 0) {
            throw new Exception("modrdn request: invalid parsing results for 'dn' LDAP name: " + name);
        }
        if (StringUtils.isBlank(newRdn)) {
            throw new Exception("modrdn request: '" + propName + "' value is null or empty");
        }
        Rdn rdn = new Rdn(newRdn);
        List<Rdn> newRdns = new ArrayList<>(oldRdns.subList(0, rdnsCount - 1));
        newRdns.add(rdn);
        LdapName newLdapName = new LdapName(newRdns);
        ctx.rename(oldLdapName, newLdapName);
    }

    /*  Example json for "modify" request:
        {
            "dn": "uid=ssd2, ou=fixedline,ou=profiles,ou=exampleMobileProvider,c=org,o=exampleMobileProvider",
            "changetype": "modify",
            "uid": "ssd2",
            "add":
            [
                "fqdn", "inet-subscriptionid"
            ],
            "fqdn": "example@example.com",
            "inet-subscriptionid": "0123456_inetsubscription",
            "replace":
            [
                "uid"
            ],
            "uid": "ssd20123465",
            "delete":
            [
                "description"
            ]
        }
    * */
    private ModificationItem[] populateModificationItems(JSONObject jsonMessage) {
        BasicAttributes attrs = populateAttributes(jsonMessage);
        if (attrs.size() == 0) return null;
        List<ModificationItem> listItems = populateModificationItems(attrs, attrs.get("add"), DirContext.ADD_ATTRIBUTE);
        listItems.addAll(populateModificationItems(attrs, attrs.get("replace"), DirContext.REPLACE_ATTRIBUTE));
        listItems.addAll(populateModificationItems(attrs, attrs.get("delete"), DirContext.REMOVE_ATTRIBUTE));
        ModificationItem[] items = new ModificationItem[listItems.size()];
        return listItems.toArray(items);
    }

    private List<ModificationItem> populateModificationItems(BasicAttributes attrs, Attribute namesList, int action) {
        List<ModificationItem> listItems = new ArrayList<>();
        if (namesList != null) {
            for (int i = 0; i < namesList.size(); i++) {
                try {
                    String curname = (String) namesList.get(i);
                    Attribute attr = attrs.get(curname);
                    if (attr != null) {
                        listItems.add(new ModificationItem(action, attr));
                    }
                } catch (NamingException e) {
                    // ignore it. I can not imagine what the exception can be at the moment
                }
            }
        }
        return listItems;
    }

    private BasicAttributes populateAttributes(JSONObject jsonMessage) {
        BasicAttributes attrs = new BasicAttributes();
        jsonMessage.forEach((key, value) -> {
            // I don't know why, but "add" and "modify" requests operating with "inet-bandwidth" attribute throw an
            // "invalid value" exception.
            // So, I commented this attribute temporarily
            if (!(key.equals("changetype") || key.equals("dn") || key.equals("inet-bandwidth"))) {
                BasicAttribute attr;
                if (value instanceof List) {
                    attr = new BasicAttribute((String) key);
                    ((List) value).forEach(attr::add);
                } else {
                    attr = new BasicAttribute((String) key, value);
                }
                attrs.put(attr);
            }
        });
        return attrs;
    }

    private String dataToString(Collection<SearchResult> data, String outputFormat) throws NamingException {
        if (outputFormat.equals(DEFAULT_OUTPUT_FORMAT)) {
            return data.toString();
        }
        // Construct simplified json representation of LDIF-objects
        List<Object> dataList = new ArrayList<>();
        for (SearchResult item : data) {
            Map<String, Object> mapItem = new HashMap<>();
            mapItem.put("boundObj", item.getObject());
            mapItem.put("name", item.getName());
            mapItem.put("className", item.getClassName());
            mapItem.put("fullName", item.getNameInNamespace());
            mapItem.put("isRel", item.isRelative());
            mapItem.put("attrs_ignoreCase", item.getAttributes().isCaseIgnored());
            Map<String, Object> itemAttrs = new HashMap<>();
            Attributes attributes = item.getAttributes();
            NamingEnumeration<String> attrIdEnum = attributes.getIDs();
            while (attrIdEnum.hasMoreElements()) {
                String attrId = attrIdEnum.next();
                Attribute attr = attributes.get(attrId);
                NamingEnumeration<?> attrValuesEnum = attr.getAll();
                List<String> values = new ArrayList<>();
                while (attrValuesEnum.hasMore()) {
                    values.add(attrValuesEnum.next().toString());
                }
                if (!values.isEmpty()) {
                    if (values.size() == 1) {
                        itemAttrs.put(attrId, values.get(0));
                    } else {
                        itemAttrs.put(attrId, values);
                    }
                }
            }
            mapItem.put("attrs", itemAttrs);
            dataList.add(mapItem);
        }
        return String.valueOf(GSON.toJsonTree(dataList));
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return null;
    }
}
