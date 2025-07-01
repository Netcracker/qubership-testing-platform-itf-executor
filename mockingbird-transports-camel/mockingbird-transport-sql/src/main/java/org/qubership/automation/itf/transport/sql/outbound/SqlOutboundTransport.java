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

package org.qubership.automation.itf.transport.sql.outbound;

import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.APACHE_HIVE;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.APACHE_HIVE_DRIVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.CASANDRA_DRIVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.CASSANDRA;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.DATA_SOURCE;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.JDBC_URL;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.JDBC_URL_DESCRIPTION;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.JDBC_URL_STRING;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.OPTIONS;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.OPTIONS_DESCRIPTION;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.OPTIONS_STRING;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.ORACLE;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.ORACLE_DRIVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.PASSWORD;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.PASSWORD_DESCRIPTION;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.PASSWORD_STRING;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.POSTGRESQL;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.POSTGRESQL_DRIVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.SQLSERVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.SQL_SERVER_DRIVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.TRINO;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.TRINO_DRIVER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.TYPE_DB;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.TYPE_DB_DESCRIPTION;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.USER;
import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.USER_DESCRIPTION;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.component.cassandra.CassandraComponent;
import org.apache.camel.component.cassandra.CassandraEndpoint;
import org.apache.camel.component.jdbc.JdbcComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.transport.base.AbstractOutboundTransportImpl;
import org.qubership.automation.itf.transport.sql.CassandraSessionsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import lombok.Getter;

@UserName("Outbound SQL Synchronous")
public class SqlOutboundTransport extends AbstractOutboundTransportImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlOutboundTransport.class);
    private static final ObjectMapper MAPPER;

    private static final Integer defaultQueryTimeout;
    private static final Integer initialSize;
    private static final Integer maxTotal;
    private static final Integer maxIdle;
    private static final Integer minIdle;
    private static final boolean testWhileIdle;
    private static final boolean fastFailValidation;
    private static final boolean removeAbandonedOnMaintenance;
    private static final boolean removeAbandonedOnBorrow;
    private static final Integer maxWaitMillis;
    private static final Integer minEvictableIdleTimeMillis;
    private static final Integer timeBetweenEvictionRunsMillis;
    private static final Integer maxConnLifetimeMillis;

    private static final Integer ojdbcReadTimeout;
    private static final Integer ojdbcConnectTimeout;
    private static final Integer ojdbcOutboundConnectTimeout;

    private static final boolean dataSourcesCacheEnable;
    private static final int dataSourcesCacheExpireMinutes;
    private static final boolean dataSourcesCacheRecordStats;

    private static final LoadingCache<ConnectionProperties, SqlConfig> configCache;
    private static final ScheduledExecutorService configCacheMaintenanceService =
            Executors.newSingleThreadScheduledExecutor();
    private static boolean isCacheCleanupScheduled = false;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);

        defaultQueryTimeout = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.defaultQueryTimeout", 360);
        initialSize = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.initialSize", 2);
        maxTotal = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.maxTotal", 100);
        maxIdle = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.maxIdle", 10);
        minIdle = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.minIdle", 0);

        testWhileIdle = Boolean.parseBoolean(Config.getConfig()
                .getStringOrDefault("sql.transport.dataSource.testWhileIdle", "false"));
        fastFailValidation = Boolean.parseBoolean(Config.getConfig()
                .getStringOrDefault("sql.transport.dataSource.fastFailValidation", "false"));
        removeAbandonedOnMaintenance = Boolean.parseBoolean(Config.getConfig()
                .getStringOrDefault("sql.transport.dataSource.removeAbandonedOnMaintenance", "false"));
        removeAbandonedOnBorrow = Boolean.parseBoolean(Config.getConfig()
                .getStringOrDefault("sql.transport.dataSource.removeAbandonedOnBorrow", "false"));

        maxWaitMillis = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.maxWaitMillis", 10000);
        minEvictableIdleTimeMillis = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.minEvictableIdleTimeMillis", 900000);
        timeBetweenEvictionRunsMillis = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.timeBetweenEvictionRunsMillis", 600000);
        maxConnLifetimeMillis = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.maxConnLifetimeMillis", 1800000);

        ojdbcReadTimeout = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.ojdbc.ReadTimeout", 600);
        ojdbcConnectTimeout = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.ojdbc.ConnectTimeout", 5);
        ojdbcOutboundConnectTimeout = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSource.ojdbc.OutboundConnectTimeout", 10);

        dataSourcesCacheEnable = Boolean.parseBoolean(Config.getConfig()
                .getStringOrDefault("sql.transport.dataSourcesCache.enable", "true"));
        dataSourcesCacheExpireMinutes = Config.getConfig()
                .getIntOrDefault("sql.transport.dataSourcesCache.expireMinutes", 1380);
        dataSourcesCacheRecordStats = Boolean.parseBoolean(Config.getConfig()
                .getStringOrDefault("sql.transport.dataSourcesCache.recordStats", "false"));

        if (dataSourcesCacheEnable) {
            CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
            if (dataSourcesCacheExpireMinutes != -1) {
                builder.expireAfterAccess(dataSourcesCacheExpireMinutes, TimeUnit.MINUTES);
            }
            if (dataSourcesCacheRecordStats) {
                builder.recordStats();
            }
            configCache = builder
                    .removalListener((RemovalListener<ConnectionProperties, SqlConfig>) removalNotification -> {
                        if (removalNotification.getCause().equals(RemovalCause.EXPIRED)) {
                            SqlConfig sqlConfig = removalNotification.getValue();
                            if (sqlConfig != null) {
                                BasicDataSource ds = sqlConfig.getDataSource();
                                if (ds != null && !ds.isClosed()) {
                                    try {
                                        ds.close();
                                    } catch (SQLException e) {
                                        // Silently do nothing
                                    }
                                }
                            }
                        }
                    })
                    .build(new CacheLoader<ConnectionProperties, SqlConfig>() {
                        @Override
                        public SqlConfig load(@Nonnull ConnectionProperties id) {
                            BasicDataSource dataSource = (BasicDataSource) setupDataSource(id);
                            return new SqlConfig(dataSource);
                        }
                    });
        } else {
            configCache = null;
        }
    }

    @Parameter(shortName = TYPE_DB, longName = TYPE_DB_DESCRIPTION,
            description = TYPE_DB_DESCRIPTION)
    @Options({ORACLE, CASSANDRA, POSTGRESQL, SQLSERVER, TRINO, APACHE_HIVE})
    private String typeDB;
    @Parameter(shortName = JDBC_URL, longName = JDBC_URL_STRING,
            description = JDBC_URL_DESCRIPTION, isDynamic = true)
    private String jdbcUrl;
    @Parameter(shortName = USER, longName = USER_DESCRIPTION,
            description = USER_DESCRIPTION, isDynamic = true)
    private String user;
    @Parameter(shortName = PASSWORD, longName = PASSWORD_STRING,
            description = PASSWORD_DESCRIPTION, isDynamic = true)
    private String pass;
    @Parameter(shortName = OPTIONS, longName = OPTIONS_STRING,
            description = OPTIONS_DESCRIPTION, optional = true, forTemplate = true)
    private Map<String, String> options = new HashMap<>();

    private static DataSource setupDataSource(ConnectionProperties connectionProperties) {
        BasicDataSource dataSource = new BasicDataSource();
        String typeDataBase = getAndCheckRequiredProperty(connectionProperties, TYPE_DB, TYPE_DB_DESCRIPTION);
        String driver = selectDataBaseDriver(typeDataBase);
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(getAndCheckRequiredProperty(connectionProperties, USER, USER_DESCRIPTION));
        dataSource.setPassword(getAndCheckRequiredProperty(connectionProperties, PASSWORD, PASSWORD_DESCRIPTION));
        dataSource.setUrl(getAndCheckRequiredProperty(connectionProperties, JDBC_URL, JDBC_URL_STRING));

        if (ORACLE.equals(typeDataBase)) {
            if (ojdbcReadTimeout != -1) {
                dataSource.addConnectionProperty("oracle.net.READ_TIMEOUT", ojdbcReadTimeout.toString());
                dataSource.addConnectionProperty("oracle.jdbc.ReadTimeout", ojdbcReadTimeout.toString());
            }
            if (ojdbcConnectTimeout != -1) {
                dataSource.addConnectionProperty("oracle.net.CONNECT_TIMEOUT", ojdbcConnectTimeout.toString());
            }
            if (ojdbcOutboundConnectTimeout != -1) {
                dataSource.addConnectionProperty("oracle.net.OUTBOUND_CONNECT_TIMEOUT",
                        ojdbcOutboundConnectTimeout.toString());
            }
        }

        dataSource.setDefaultQueryTimeout(defaultQueryTimeout);
        dataSource.setInitialSize(initialSize);
        dataSource.setMaxTotal(maxTotal);
        dataSource.setMaxIdle(maxIdle);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxWaitMillis(maxWaitMillis);

        dataSource.setTestWhileIdle(testWhileIdle);
        dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        dataSource.setMaxConnLifetimeMillis(maxConnLifetimeMillis);
        dataSource.setFastFailValidation(fastFailValidation);
        dataSource.setRemoveAbandonedOnMaintenance(removeAbandonedOnMaintenance);
        dataSource.setRemoveAbandonedOnBorrow(removeAbandonedOnBorrow);
        return dataSource;
    }

    private static String selectDataBaseDriver(String typeDataBase) {
        switch (typeDataBase) {
            case (ORACLE):
                return ORACLE_DRIVER;
            case (POSTGRESQL):
                return POSTGRESQL_DRIVER;
            case (CASSANDRA):
                return CASANDRA_DRIVER;
            case (SQLSERVER):
                return SQL_SERVER_DRIVER;
            case (TRINO):
                return TRINO_DRIVER;
            case (APACHE_HIVE):
                return APACHE_HIVE_DRIVER;
            default:
                break;
        }
        return null;
    }

    private static String getAndCheckRequiredProperty(ConnectionProperties connectionProperties, String field,
                                                      String fieldName) {
        String reqProperty;
        try {
            reqProperty = connectionProperties.get(field).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Required property '%s' can't be empty", fieldName));
        }
        return reqProperty;
    }

    @Override
    public String getShortName() {
        return "SQL Outbound";
    }

    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        String typeDataBase = getAndCheckRequiredProperty(connectionProperties, TYPE_DB, TYPE_DB_DESCRIPTION);
        if (typeDataBase.equals(CASSANDRA)) {
            return sendReceiveSyncCassandra(message, connectionProperties);
        } else if (typeDataBase.equals(TRINO)) {
            String pwd = connectionProperties.getOrDefault(PASSWORD, StringUtils.EMPTY).toString();
            /*
                A special case for Trino, to avoid the following problem:
                    - Trino Databases, currently used by project,
                    require TLS/SSL verification when user/pass authentication is used.
                    So, user/pass authentication fails with an exception:
                    Cannot create PoolableConnectionFactory
                    (TLS/SSL is required for authentication with username and password)
                    - Without password (with empty password) connection is established successfully.
                    - But, Password property is required non-empty in SqlOutbound transport...
             */
            if (pwd.equals("NONE")) {
                connectionProperties.replace(PASSWORD, StringUtils.EMPTY);
            }
        }

        /* If we are able to simply determine sql command type,
            we use fix for SVT (speed up SQL performance via dataSources and connections reusing).
            Otherwise - we use legacy Camel-based implementation.
            Since 2025-06-30: explicit enabling/disabling of dataSources Cache is added.
            So, we use legacy Camel-based implementation also if dataSources Cache is disabled.
         */
        String sqlCommand = message.getText().trim();
        int sqlCommandType = determineType(sqlCommand);
        if (sqlCommandType == -1 || !dataSourcesCacheEnable) {
            return sendReceiveSyncLegacy(message, connectionProperties);
        }
        // ~ Camel-less implementation for SVT
        connectionProperties.remove("ContextId");
        connectionProperties.remove("transportId");
        SqlConfig sqlConfig = configCache.get(connectionProperties);
        Message response;
        JdbcTemplate jdbcTemplate = sqlConfig.getJdbcTemplate();
        switch (sqlCommandType) {
            case 1:
                List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlCommand);
                response = new Message(convertToJson(result));
                break;
            case 2:
                int processedCount = jdbcTemplate.update(sqlCommand);
                response = new Message("[{\"result\":\"Executed successfully\",\"rowsProcessed\":"
                        + processedCount + "}]");
                break;
            case 3:
                jdbcTemplate.execute(sqlCommand);
                response = new Message("[{\"result\":\"Executed successfully\"}]");
                break;
            default:
                response = new Message("[{\"result\":\"Unknown sql command; rejected\"}]");
        }
        scheduleCacheCleanupIfNeeded();
        return response;
    }

    private static synchronized void scheduleCacheCleanupIfNeeded() {
        if (!dataSourcesCacheEnable) {
            return;
        }
        if (!isCacheCleanupScheduled) {
            if (configCache.size() > 0) {
                configCacheMaintenanceService.scheduleWithFixedDelay(() -> {
                    try {
                        if (dataSourcesCacheRecordStats) {
                            CacheStats cacheStats = configCache.stats();
                            LOGGER.info("DataSources Cache Statistics: {}", cacheStats);
                        }
                        configCache.cleanUp();
                    } catch (Throwable t) {
                        LOGGER.error("Error while SqlOutboundTransport cache cleanUp: {}", t.toString());
                    }
                }, 61L, 20L, TimeUnit.MINUTES);
                isCacheCleanupScheduled = true;
            }
        }
    }

    private static int determineType(String sqlCommand) {
        String startString = sqlCommand.substring(0, 6).toLowerCase();
        return ((startString.equals("select")) ? 1
                : (startString.equals("insert") || startString.equals("update") || startString.equals("delete")) ? 2
                : (startString.equals("create") || startString.startsWith("alter")
                || startString.startsWith("drop") || startString.startsWith("call")) ? 3
                : -1);
    }

    private Message sendReceiveSyncLegacy(Message message, ConnectionProperties connectionProperties) throws Exception {
        DataSource dataSource = setupDataSource(connectionProperties);
        SimpleRegistry registry = new SimpleRegistry();
        registry.put(DATA_SOURCE, dataSource);
        CamelContext context = new DefaultCamelContext(registry);
        if (!context.getComponentNames().contains("jdbc")) {
            JdbcComponent jdbcComponent = new JdbcComponent(context);
            jdbcComponent.setDataSource(dataSource);
            context.addComponent("jdbc", jdbcComponent);
        }
        ProducerTemplate template = context.createProducerTemplate();
        String options = getStringOptionsForRouteBuilder(connectionProperties);
        SqlRouteBuilder sqlRoute = new SqlRouteBuilder(options);
        sqlRoute.setContext(context);
        context.addRoutes(sqlRoute);
        Endpoint endpoint = context.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();
        exchange.getIn().setBody(message.getText()); // SQL Query text is here
        Exchange out;
        context.start();
        try {
            out = template.send(endpoint, exchange);
            if (out.isFailed()) {
                throw out.getException();
            }
        } catch (Exception e) {
            context.stop();
            throw new Exception("Error sending SQL Message. Stacktrace: " + e);
        }
        Message response = new Message(convertToJson(out.getOut().getBody()));
        exchange.getOut().getHeaders().put(OPTIONS_STRING, options);
        response.convertAndSetHeaders(exchange.getOut().getHeaders());
        context.stop();
        return response;
    }

    private Message sendReceiveSyncCassandra(Message message, ConnectionProperties connectionProperties) throws Exception {
        String username = getAndCheckRequiredProperty(connectionProperties, USER, USER_DESCRIPTION);
        String password = getAndCheckRequiredProperty(connectionProperties, PASSWORD, PASSWORD_DESCRIPTION);

        Session session = CassandraSessionsHolder.getInstance().getSession(
                getAndCheckRequiredProperty(connectionProperties, JDBC_URL, JDBC_URL_STRING), username, password);
        CassandraComponent component = new CassandraComponent();
        CassandraEndpoint endpoint = new CassandraEndpoint("",
                component,
                null, // Please be aware: if cluster parameter != null - a new session is created while endpoint
                // .start()!!!
                session, session.getLoggedKeyspace());
        endpoint.setUsername(username);
        endpoint.setPassword(password);
        endpoint.setCql(message.getText()); // SQL Query text is here
        endpoint.start();
        CamelContext context = new DefaultCamelContext();
        context.setErrorHandlerBuilder(new LoggingErrorHandlerBuilder(LOGGER));
        ProducerTemplate template = context.createProducerTemplate();
        endpoint.setCamelContext(context);
        Exchange exchange = endpoint.createExchange();
        Exchange out;
        context.start();
        try {
            out = template.send(endpoint, exchange);
            if (out.isFailed()) {
                throw out.getException();
            }
        } catch (Exception e) {
            stop(context, session, session.getCluster());
            throw new Exception("Error sending SQL Message. Stacktrace: " + e);
        }
        Message response = new Message(convertToJson(processCassandraResponse(out.getOut().getBody())));
        exchange.getOut().getHeaders().put(OPTIONS_STRING, options);
        response.convertAndSetHeaders(exchange.getOut().getHeaders());
        stop(context, session, session.getCluster());
        return response;
    }

    private void stop(CamelContext context, Session session, Cluster cluster) throws Exception {
        context.stop();
        // Session and Cluster remain opened until ITF is stopped
        /*
        if (!session.isClosed()) session.close();
        if (!cluster.isClosed()) cluster.close();
        */
    }

    private String getStringOptionsForRouteBuilder(ConnectionProperties connectionProperties) {
        if (!connectionProperties.containsKey(OPTIONS)) {
            return "";
        }
        Map<String, Object> mapOfOptions = new HashMap<>((Map<String, Object>) connectionProperties.get(OPTIONS));
        StringBuilder st = new StringBuilder();
        st.append("?");
        int numberOptions = mapOfOptions.size();
        for (Map.Entry<String, Object> entry : mapOfOptions.entrySet()) {
            st.append(entry);
            numberOptions--;
            if (numberOptions > 0) {
                st.append('&');
            }
        }
        return st.toString();
    }

    private String convertToJson(Object responseBody) throws Exception {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            throw new Exception("Error while converting query response to Json. Stacktrace: " + e);
        }
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

    /**
     * Iterates through all columns in the {@code row}. For each column, returns its value as Java type
     * that matches the CQL type in switch part. Otherwise returns the value as bytes composing the value.
     *
     * @param row the row returned by the ResultSet
     * @return list of values in {@code row}
     * <p>
     * From: https://www.javatips.net/.../cassandra/translator-cassandra/src/main/java/org
     * /teiid/translator/cassandra/CassandraQueryExecution.java
     * <p>
     * Modified and tested by Alexander Kapustin.
     * Duplicated (by sense) processing is in the 'getByName' method below.
     * It can't be combined due to different data types: Row vs. UDTValue
     */
    private Object getColumnValue(Row row, int i, DataType.Name columnType) {
        if (row == null || row.isNull(i)) {
            return null;
        }
        switch (columnType) {
            case ASCII:
            case VARCHAR:
            case TEXT:
                return row.getString(i);
            case BIGINT:
            case COUNTER:
                return row.getLong(i);
            case BOOLEAN:
                return row.getBool(i);
            case DECIMAL:
                return row.getDecimal(i);
            case DOUBLE:
                return row.getDouble(i);
            case FLOAT:
                return row.getFloat(i);
            case INET:
                return row.getInet(i);
            case INT:
                return row.getInt(i);
            case SMALLINT:
                return (int) row.getShort(i);
            case TINYINT:
                return (int) row.getByte(i);
            case UDT:
                return udtValue2Object(row.getUDTValue(i));
            case LIST:
                try {
                    return row.getList(i, String.class);
                } catch (CodecNotFoundException ex) {
                    try {
                        List<UDTValue> udtList = row.getList(i, UDTValue.class);
                        List<Object> list = new ArrayList<>();
                        for (UDTValue udtValue : udtList) {
                            list.add(udtValue2Object(udtValue));
                        }
                        return list;
                    } catch (CodecNotFoundException ex1) {
                        return row.getObject(i);
                    }
                }
            case MAP:
                try {
                    return row.getMap(i, String.class, String.class);
                } catch (CodecNotFoundException ex) {
                    Map<String, UDTValue> udtMap = row.getMap(i, String.class, UDTValue.class);
                    Map<String, Object> map = new HashMap<>();
                    for (Map.Entry<String, UDTValue> entry : udtMap.entrySet()) {
                        map.put(entry.getKey(), udtValue2Object(entry.getValue()));
                    }
                    return map;
                }
            case SET:
                try {
                    return row.getSet(i, String.class);
                } catch (CodecNotFoundException ex) {
                    try {
                        Set<UDTValue> udtSet = row.getSet(i, UDTValue.class);
                        List<Object> list = new ArrayList<>();
                        for (UDTValue value : udtSet) {
                            list.add(udtValue2Object(value));
                        }
                        return list;
                    } catch (CodecNotFoundException ex1) {
                        return row.getObject(i);
                    }
                }
            case TIMESTAMP:
                return row.getTimestamp(i); //row.getDate(i); // is not working at 3.7.1
            case TIMEUUID:
            case UUID:
                return row.getUUID(i);
            case VARINT:
                return row.getVarint(i);
            default:
                //read as a varbinary
                ByteBuffer bytesUnsafe = row.getBytesUnsafe(i);
                byte[] b = new byte[bytesUnsafe.remaining()];
                bytesUnsafe.get(b);
                return b;
        }
    }

    private Object udtValue2Object(UDTValue udtValue) {
        if (udtValue == null) {
            return null;
        }
        Map<String, Object> udtMap = new HashMap<>();
        for (String fieldName : udtValue.getType().getFieldNames()) {
            udtMap.put(fieldName, getByName(udtValue, fieldName));
        }
        return udtMap;
    }

    private Object getByName(UDTValue udtValue, String fieldName) {
        if (udtValue.isNull(fieldName)) {
            return null;
        }
        switch (udtValue.getType().getFieldType(fieldName).getName()) {
            case ASCII:
            case VARCHAR:
            case TEXT:
                return udtValue.getString(fieldName);
            case BIGINT:
            case COUNTER:
                return udtValue.getLong(fieldName);
            case BOOLEAN:
                return udtValue.getBool(fieldName);
            case DECIMAL:
                return udtValue.getDecimal(fieldName);
            case DOUBLE:
                return udtValue.getDouble(fieldName);
            case FLOAT:
                return udtValue.getFloat(fieldName);
            case INET:
                return udtValue.getInet(fieldName);
            case INT:
                return udtValue.getInt(fieldName);
            case SMALLINT:
                return (int) udtValue.getShort(fieldName);
            case TINYINT:
                return (int) udtValue.getByte(fieldName);
            case UDT:
                return udtValue2Object(udtValue.getUDTValue(fieldName));
            case LIST:
                try {
                    return udtValue.getList(fieldName, String.class);
                } catch (CodecNotFoundException ex) {
                    try {
                        List<UDTValue> udtList = udtValue.getList(fieldName, UDTValue.class);
                        List<Object> list = new ArrayList<>();
                        for (UDTValue value : udtList) {
                            list.add(udtValue2Object(value));
                        }
                        return list;
                    } catch (CodecNotFoundException ex1) {
                        return udtValue.getObject(fieldName);
                    }
                }
            case MAP:
                try {
                    return udtValue.getMap(fieldName, String.class, String.class);
                } catch (CodecNotFoundException ex) {
                    Map<String, UDTValue> udtMap = udtValue.getMap(fieldName, String.class, UDTValue.class);
                    Map<String, Object> map = new HashMap<>();
                    for (Map.Entry<String, UDTValue> entry : udtMap.entrySet()) {
                        map.put(entry.getKey(), udtValue2Object(entry.getValue()));
                    }
                    return map;
                }
            case SET:
                try {
                    return udtValue.getSet(fieldName, String.class);
                } catch (CodecNotFoundException ex) {
                    try {
                        Set<UDTValue> udtSet = udtValue.getSet(fieldName, UDTValue.class);
                        List<Object> list = new ArrayList<>();
                        for (UDTValue value : udtSet) {
                            list.add(udtValue2Object(value));
                        }
                        return list;
                    } catch (CodecNotFoundException ex1) {
                        return udtValue.getObject(fieldName);
                    }
                }
            case TIMESTAMP:
                return udtValue.getTimestamp(fieldName);
            case TIMEUUID:
            case UUID:
                return udtValue.getUUID(fieldName);
            case VARINT:
                return udtValue.getVarint(fieldName);
            default:
                //read as a varbinary
                ByteBuffer bytesUnsafe = udtValue.getBytesUnsafe(fieldName);
                byte[] b = new byte[bytesUnsafe.remaining()];
                bytesUnsafe.get(b);
                return b;
        }
    }

    private Object processCassandraResponse(Object responseBody) {
        if (responseBody == null) {
            return null;
        } else if (responseBody instanceof ArrayList) {
            List<Object> array = new ArrayList<>(((ArrayList) responseBody).size());
            for (Object row : (ArrayList) responseBody) {
                if (row instanceof Row) {
                    ColumnDefinitions columnDefinitions = ((Row) row).getColumnDefinitions();
                    LinkedHashMap<String, Object> rowValuesMap = new LinkedHashMap<>(columnDefinitions.size());
                    for (int i = 0; i < columnDefinitions.size(); i++) {
                        Object value = getColumnValue((Row) row, i, columnDefinitions.getType(i).getName());
                        rowValuesMap.put(columnDefinitions.getName(i), value);
                    }
                    array.add(rowValuesMap);
                }
            }
            return array;
        } else {
            return responseBody;
        }
    }

    @Getter
    private static class SqlConfig {
        BasicDataSource dataSource;
        JdbcTemplate jdbcTemplate;

        public SqlConfig(BasicDataSource dataSource) {
            this.dataSource = dataSource;
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }

    }
}
