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

package org.qubership.automation.itf.transport.sql;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qubership.automation.itf.transport.sql.outbound.SqlOutboundTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;

public class CassandraClientURI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlOutboundTransport.class);

    private static final String PREFIX = "jdbc:cassandra://";
    static Set<String> allKeys = new HashSet<>();

    static {
        allKeys.add("javax.net.ssl.trustStore");
        allKeys.add("javax.net.ssl.trustStorePassword");
        allKeys.add("javax.net.ssl.keyStore");
        allKeys.add("javax.net.ssl.keyStorePassword");
    }

    private final List<String> hosts;
    private final String database;
    private final String collection;
    private final String uri;
    private final String userName;
    private final String password;

    public CassandraClientURI(String uri, String username, String password) {
        this.uri = uri;
        this.userName = username;
        this.password = password;

        if (!uri.startsWith(PREFIX)) {
            throw new IllegalArgumentException("URI must start with " + PREFIX + ", but configured: " + uri);
        }

        uri = uri.substring(PREFIX.length());

        String serverPart;
        String nsPart;
        String optionsPart;
        {
            int idx = uri.lastIndexOf("/");
            if (idx < 0) {
                if (uri.contains("?")) {
                    throw new IllegalArgumentException("URI contains options without trailing slash: " + uri);
                }
                serverPart = uri;
                nsPart = null;
                optionsPart = "";
            } else {
                serverPart = uri.substring(0, idx);
                nsPart = uri.substring(idx + 1);
                idx = nsPart.indexOf("?");
                if (idx >= 0) {
                    optionsPart = nsPart.substring(idx + 1);
                    nsPart = nsPart.substring(0, idx);
                } else {
                    optionsPart = "";
                }
            }
        }

        { // userName,password,hosts
            List<String> all = new LinkedList<>();
            Collections.addAll(all, serverPart.split(","));
            hosts = Collections.unmodifiableList(all);
        }

        if (nsPart != null && nsPart.length() != 0) { // database,_collection
            int idx = nsPart.indexOf(".");
            if (idx < 0) {
                database = nsPart;
                collection = null;
            } else {
                database = nsPart.substring(0, idx);
                collection = nsPart.substring(idx + 1);
            }
        } else {
            database = null;
            collection = null;
        }
        Map<String, List<String>> optionsMap = parseOptions(optionsPart);
        warnOnUnsupportedOptions(optionsMap);
    }

    public Cluster createBuilder() throws java.net.UnknownHostException {
        return createBuilder(new ConstantReconnectionPolicy(100L));
    }

    public Cluster createBuilder(ReconnectionPolicy reconnectionPolicy) throws java.net.UnknownHostException {
        Cluster.Builder builder = Cluster.builder();
        if (System.getProperty("javax.net.ssl.trustStore") != null) {
            builder = builder.withSSL();
        }
        int port = -1;
        for (String host : hosts) {
            int idx = host.indexOf(":");
            if (idx > 0) {
                port = Integer.parseInt(host.substring(idx + 1).trim());
                host = host.substring(0, idx).trim();
            }
            builder.addContactPoints(InetAddress.getByName(host));
        }
        if (port > -1) {
            builder.withPort(port);
        }
        builder.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
                .withReconnectionPolicy((reconnectionPolicy == null)
                        ? new ConstantReconnectionPolicy(100L)
                        : reconnectionPolicy);
        if (userName != null) {
            builder.withCredentials(userName, password);
            LOGGER.info("URI: {} - Using authentication as user '{}'", uri, userName);
        }
        return builder.build();
    }

    private void warnOnUnsupportedOptions(Map<String, List<String>> optionsMap) {
        for (String key : optionsMap.keySet()) {
            if (key.startsWith("javax.net.ssl")) {
                System.setProperty(key, optionsMap.get(key).get(0));
            } else if (!allKeys.contains(key)) {
                LOGGER.warn("Unknown or Unsupported Option '{}'", key);
            }
        }
    }

    private String getLastValue(final Map<String, List<String>> optionsMap, final String key) {
        List<String> valueList = optionsMap.get(key);
        return (valueList == null) ? null : valueList.get(valueList.size() - 1);
    }

    private Map<String, List<String>> parseOptions(String optionsPart) {
        Map<String, List<String>> optionsMap = new HashMap<>();

        for (String _part : optionsPart.split("&|;")) {
            int idx = _part.indexOf("=");
            if (idx >= 0) {
                String key = _part.substring(0, idx).toLowerCase();
                String value = _part.substring(idx + 1);
                List<String> valueList = optionsMap.get(key);
                if (valueList == null) {
                    valueList = new ArrayList<>(1);
                }
                valueList.add(value);
                optionsMap.put(key, valueList);
            }
        }
        return optionsMap;
    }

    boolean _parseBoolean(String _in) {
        String in = _in.trim();
        return in != null && in.length() > 0 && (in.equals("1")
                || in.toLowerCase().equals("true")
                || in.toLowerCase().equals("yes"));
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return userName;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public char[] getPassword() {
        return password != null ? password.toCharArray() : null;
    }

    /**
     * Gets the list of hosts.
     *
     * @return the host list
     */
    public List<String> getHosts() {
        return hosts;
    }

    /**
     * Gets the database name.
     *
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Gets the collection name.
     *
     * @return the collection name
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Get the unparsed URI.
     *
     * @return the URI
     */
    public String getURI() {
        return uri;
    }

    @Override
    public String toString() {
        return uri;
    }
}
