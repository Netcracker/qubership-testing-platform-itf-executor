package org.qubership.automation.itf.transport.sql.outbound;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;

@ExtendWith(MockitoExtension.class)
class SqlOutboundTransportSetupDataSourceTest {

    private ConnectionProperties validProperties;
    private static final List<BasicDataSource> dataSourcesToClose = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        dataSourcesToClose.clear();
    }

    @BeforeEach
    void setUp() {
        validProperties = new ConnectionProperties();
        validProperties.put("typeDB", "PostgreSQL");
        validProperties.put("jdbcUrl", "jdbc:postgresql://localhost:5432/testdb");
        validProperties.put("user", "testuser");
        validProperties.put("pass", "testpass");
    }

    @AfterAll
    static void closeResources() {
        for (BasicDataSource ds : dataSourcesToClose) {
            try {
                if (ds != null && !ds.isClosed()) {
                    ds.close();
                }
            } catch (SQLException e) {
                // Don't fail test(s) in case errors during dataSource closing
                //e.printStackTrace();
            }
        }
        dataSourcesToClose.clear();
    }

    // ==================== 1. Basic tests: successful DataSource creation ====================

    @Test
    void setupDataSource_WithValidPostgresProperties_ReturnsConfiguredDataSource() throws Exception {
        DataSource ds = invokeSetupDataSourceAndTrack(validProperties);

        assertNotNull(ds);
        assertInstanceOf(BasicDataSource.class, ds);
        BasicDataSource bds = (BasicDataSource) ds;

        assertEquals("org.postgresql.Driver", bds.getDriverClassName());
        assertEquals("testuser", bds.getUsername());
        assertEquals("testpass", bds.getPassword());
        assertEquals("jdbc:postgresql://localhost:5432/testdb", bds.getUrl());
    }

    @Test
    void setupDataSource_WithValidOracleProperties_ReturnsConfiguredDataSource() throws Exception {
        ConnectionProperties oracleProps = new ConnectionProperties();
        oracleProps.put("typeDB", "Oracle");
        oracleProps.put("jdbcUrl", "jdbc:oracle:thin:@localhost:1521:XE");
        oracleProps.put("user", "oracle_user");
        oracleProps.put("pass", "oracle_pass");

        DataSource ds = invokeSetupDataSourceAndTrack(oracleProps);

        assertNotNull(ds);
        BasicDataSource bds = (BasicDataSource) ds;
        assertEquals("oracle.jdbc.driver.OracleDriver", bds.getDriverClassName());
        assertEquals("oracle_user", bds.getUsername());
        assertEquals("oracle_pass", bds.getPassword());
    }

    @Test
    void setupDataSource_WithValidSqlServerProperties_ReturnsConfiguredDataSource() throws Exception {
        ConnectionProperties sqlserverProps = new ConnectionProperties();
        sqlserverProps.put("typeDB", "SQLServer");
        sqlserverProps.put("jdbcUrl", "jdbc:sqlserver://localhost:1433;databaseName=test");
        sqlserverProps.put("user", "sa");
        sqlserverProps.put("pass", "sa_pass");

        DataSource ds = invokeSetupDataSourceAndTrack(sqlserverProps);

        assertNotNull(ds);
        BasicDataSource bds = (BasicDataSource) ds;
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", bds.getDriverClassName());
    }

    @Test
    void setupDataSource_WithValidTrinoProperties_ReturnsConfiguredDataSource() throws Exception {
        ConnectionProperties trinoProps = new ConnectionProperties();
        trinoProps.put("typeDB", "Trino");
        trinoProps.put("jdbcUrl", "jdbc:trino://localhost:8080/hive");
        trinoProps.put("user", "trino_user");
        trinoProps.put("pass", "trino_pass");

        DataSource ds = invokeSetupDataSourceAndTrack(trinoProps);

        assertNotNull(ds);
        BasicDataSource bds = (BasicDataSource) ds;
        assertEquals("io.trino.jdbc.TrinoDriver", bds.getDriverClassName());
    }

    @Test
    void setupDataSource_WithValidApacheHiveProperties_ReturnsConfiguredDataSource() throws Exception {
        ConnectionProperties hiveProps = new ConnectionProperties();
        hiveProps.put("typeDB", "Hive");
        hiveProps.put("jdbcUrl", "jdbc:hive2://localhost:10000/default");
        hiveProps.put("user", "hive_user");
        hiveProps.put("pass", "hive_pass");

        DataSource ds = invokeSetupDataSourceAndTrack(hiveProps);

        assertNotNull(ds);
        BasicDataSource bds = (BasicDataSource) ds;
        assertEquals("org.apache.hive.jdbc.HiveDriver", bds.getDriverClassName());
    }

    @Test
    void setupDataSource_WithValidCassandraProperties_ReturnsConfiguredDataSource() throws Exception {
        ConnectionProperties cassandraProps = new ConnectionProperties();
        cassandraProps.put("typeDB", "Cassandra");
        cassandraProps.put("jdbcUrl", "jdbc:cassandra://localhost:9042/keyspace");
        cassandraProps.put("user", "cassandra_user");
        cassandraProps.put("pass", "cassandra_pass");

        DataSource ds = invokeSetupDataSourceAndTrack(cassandraProps);

        assertNotNull(ds);
        BasicDataSource bds = (BasicDataSource) ds;
        assertEquals("com.dbschema.CassandraJdbcDriver", bds.getDriverClassName());
    }

    // ==================== 2. Tests of mandatory fields validation ====================

    @Test
    void setupDataSource_MissingTypeDB_ThrowsException() {
        ConnectionProperties props = new ConnectionProperties();
        props.put("jdbcUrl", "jdbc:postgresql://localhost:5432/testdb");
        props.put("user", "testuser");
        props.put("pass", "testpass");

        Exception exception = assertThrows(Exception.class, () -> invokeSetupDataSourceAndTrack(props));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Required property 'Type of database' can't be empty",
                exception.getCause().getMessage());
    }

    @Test
    void setupDataSource_MissingJdbcUrl_ThrowsException() {
        ConnectionProperties props = new ConnectionProperties();
        props.put("typeDB", "PostgreSQL");
        props.put("user", "testuser");
        props.put("pass", "testpass");

        Exception exception = assertThrows(Exception.class, () -> invokeSetupDataSourceAndTrack(props));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Required property 'JDBC URL' can't be empty",
                exception.getCause().getMessage());
    }

    @Test
    void setupDataSource_MissingUser_ThrowsException() {
        ConnectionProperties props = new ConnectionProperties();
        props.put("typeDB", "PostgreSQL");
        props.put("jdbcUrl", "jdbc:postgresql://localhost:5432/testdb");
        props.put("pass", "testpass");

        Exception exception = assertThrows(Exception.class, () -> invokeSetupDataSourceAndTrack(props));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Required property 'DataBase User' can't be empty",
                exception.getCause().getMessage());
    }

    @Test
    void setupDataSource_MissingPassword_ThrowsException() {
        ConnectionProperties props = new ConnectionProperties();
        props.put("typeDB", "PostgreSQL");
        props.put("jdbcUrl", "jdbc:postgresql://localhost:5432/testdb");
        props.put("user", "testuser");

        Exception exception = assertThrows(Exception.class, () -> invokeSetupDataSourceAndTrack(props));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage()
                .startsWith("Required property 'DataBase Password"));
    }

    @ParameterizedTest
    @CsvSource({
            "Oracle, oracle.jdbc.driver.OracleDriver",
            "PostgreSQL, org.postgresql.Driver",
            "SQLServer, com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "Trino, io.trino.jdbc.TrinoDriver",
            "Hive, org.apache.hive.jdbc.HiveDriver",
            "Cassandra, com.dbschema.CassandraJdbcDriver"
    })
    void setupDataSource_AllSupportedTypes_ConfigureCorrectDriver(String dbType, String expectedDriver) throws Exception {
        ConnectionProperties props = new ConnectionProperties();
        props.put("typeDB", dbType);
        props.put("jdbcUrl", "jdbc:test://localhost/test");
        props.put("user", "user");
        props.put("pass", "pass");

        DataSource ds = invokeSetupDataSourceAndTrack(props);
        BasicDataSource bds = (BasicDataSource) ds;

        assertEquals(expectedDriver, bds.getDriverClassName());
    }

    // ==================== 4. Tests of connection pool settings ====================

    @Test
    void setupDataSource_SetsAllPoolConfigurationParameters() throws Exception {
        DataSource ds = invokeSetupDataSourceAndTrack(validProperties);
        BasicDataSource bds = (BasicDataSource) ds;

        // Test that config values are set (real values are from Config)
        // These tests check that 'setXXX' methods are invoked (values are arbitrary)
        assertNotNull(bds.getDefaultQueryTimeout());
        assertTrue(bds.getInitialSize() >= 0);
        assertTrue(bds.getMaxTotal() >= 0);
        assertTrue(bds.getMaxIdle() >= 0);
        assertTrue(bds.getMinIdle() >= 0);
        assertTrue(bds.getMaxWaitMillis() >= 0);

        // eviction parameters
        assertNotNull(bds.getMinEvictableIdleTimeMillis());
        assertNotNull(bds.getTimeBetweenEvictionRunsMillis());
        assertNotNull(bds.getMaxConnLifetimeMillis());
    }

    // ==================== 5. Tests of unknown DB type ====================

    @ParameterizedTest
    @ValueSource(strings = {"mysql", "mongodb", "unknown", ""})
    void setupDataSource_UnknownDatabaseType_SetsNullDriver(String unknownType) throws Exception {
        ConnectionProperties props = new ConnectionProperties();
        props.put("typeDB", unknownType);
        props.put("jdbcUrl", "jdbc:unknown://localhost/test");
        props.put("user", "user");
        props.put("pass", "pass");

        DataSource ds = invokeSetupDataSourceAndTrack(props);
        BasicDataSource bds = (BasicDataSource) ds;

        // selectDataBaseDriver returns null in case unknown DB type
        assertNull(bds.getDriverClassName());
    }

    // ==================== 6. Safety test: masking of special characters ====================

    @Test
    void setupDataSource_WithSpecialCharactersInCredentials_HandlesCorrectly() throws Exception {
        ConnectionProperties props = new ConnectionProperties();
        props.put("typeDB", "PostgreSQL");
        props.put("jdbcUrl", "jdbc:postgresql://localhost:5432/testdb");
        props.put("user", "user@domain.com");
        props.put("pass", "p@ssw0rd!;'\"");

        DataSource ds = invokeSetupDataSourceAndTrack(props);
        BasicDataSource bds = (BasicDataSource) ds;

        assertEquals("user@domain.com", bds.getUsername());
        assertEquals("p@ssw0rd!;'\"", bds.getPassword());
    }

    // ==================== 7. Test to identify libraries versions mismatch ====================

    @Test
    void setupDataSource_ShouldNotThrowNoSuchFieldError() throws Exception {
        // Check that there is no error thrown while DataSource creation.
        // NoSuchFieldError could be due to libraries versions mismatch.
        // Especially it's about commons-dbcp2 and commons-pool2 versions.

        assertDoesNotThrow(() -> {
            DataSource ds = invokeSetupDataSourceAndTrack(validProperties);
            assertNotNull(ds);
        });
    }

    // ==================== Service methods with reflection ====================

    private DataSource invokeSetupDataSource(ConnectionProperties props) throws Exception {
        Method method = SqlOutboundTransport.class.getDeclaredMethod("setupDataSource", ConnectionProperties.class);
        method.setAccessible(true);
        return (DataSource) method.invoke(null, props);
    }

    private DataSource invokeSetupDataSourceAndTrack(ConnectionProperties props) throws Exception {
        DataSource ds = invokeSetupDataSource(props);
        if (ds instanceof BasicDataSource) {
            dataSourcesToClose.add((BasicDataSource) ds);
        }
        return ds;
    }
}
