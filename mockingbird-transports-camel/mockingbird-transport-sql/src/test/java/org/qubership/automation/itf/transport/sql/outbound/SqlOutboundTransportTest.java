package org.qubership.automation.itf.transport.sql.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;

@ExtendWith(MockitoExtension.class)
class SqlOutboundTransportTest {

    private ConnectionProperties connectionProperties;

    @BeforeEach
    void setUp() {
        connectionProperties = new ConnectionProperties();
        connectionProperties.put("typeDB", "postgresql");
        connectionProperties.put("jdbcUrl", "jdbc:postgresql://localhost:5432/test");
        connectionProperties.put("user", "testuser");
        connectionProperties.put("password", "testpass");
    }

    // ==================== 1. determineType() ====================

    @Test
    void determineType_Select_Returns1() throws Exception {
        int expectedType = 1; // select -> 1
        Method method = getPrivateStaticMethod("determineType", String.class);
        assertEquals(expectedType, method.invoke(null, "SELECT * FROM users"));
        assertEquals(expectedType, method.invoke(null, "select * from users"));
        assertEquals(expectedType, method.invoke(null, "  SELECT * FROM users"));
    }

    @Test
    void determineType_InsertUpdateDelete_Returns2() throws Exception {
        int expectedType = 2; // DML: insert, update, delete -> 2
        Method method = getPrivateStaticMethod("determineType", String.class);
        assertEquals(expectedType, method.invoke(null, "INSERT INTO users VALUES (1)"));
        assertEquals(expectedType, method.invoke(null, "UPDATE users SET name='x'"));
        assertEquals(expectedType, method.invoke(null, "DELETE FROM users WHERE id=1"));
    }

    @Test
    void determineType_CreateAlterDropCall_Returns3() throws Exception {
        int expectedType = 3; // DDL: create, alter, drop -> 3. Call -> 3 too.
        Method method = getPrivateStaticMethod("determineType", String.class);
        assertEquals(expectedType, method.invoke(null, "CREATE TABLE test (id INT)"));
        assertEquals(expectedType, method.invoke(null, "ALTER TABLE test ADD COLUMN x"));
        assertEquals(expectedType, method.invoke(null, "DROP TABLE test"));
        assertEquals(expectedType, method.invoke(null, "CALL some_proc()"));
    }

    @Test
    void determineType_Unknown_ReturnsMinus1() throws Exception {
        int expectedType = -1; // others -> -1.
        Method method = getPrivateStaticMethod("determineType", String.class);
        assertEquals(expectedType, method.invoke(null, "WITH cte AS (...) SELECT"));
        assertEquals(expectedType, method.invoke(null, "BEGIN TRANSACTION"));
        assertEquals(expectedType, method.invoke(null, ""));
    }

    // ==================== 2. selectDataBaseDriver() ====================

    @Test
    void selectDataBaseDriver_SupportedTypes_ReturnsCorrectDriver() throws Exception {
        Method method = getPrivateStaticMethod("selectDataBaseDriver", String.class);
        assertEquals("oracle.jdbc.driver.OracleDriver", method.invoke(null, "Oracle"));
        assertEquals("org.postgresql.Driver", method.invoke(null, "PostgreSQL"));
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", method.invoke(null, "SQLServer"));
        assertEquals("io.trino.jdbc.TrinoDriver", method.invoke(null, "Trino"));
        assertEquals("org.apache.hive.jdbc.HiveDriver", method.invoke(null, "Hive"));
        assertEquals("com.dbschema.CassandraJdbcDriver", method.invoke(null, "Cassandra"));
    }

    @Test
    void selectDataBaseDriver_Unknown_ReturnsNull() throws Exception {
        Method method = getPrivateStaticMethod("selectDataBaseDriver", String.class);
        assertNull(method.invoke(null, "mysql"));
        assertNull(method.invoke(null, "unknown"));
    }

    // ==================== 3. getAndCheckRequiredProperty() ====================

    @Test
    void getAndCheckRequiredProperty_PropertyExists_ReturnsValue() throws Exception {
        Method method = getPrivateStaticMethod("getAndCheckRequiredProperty",
                ConnectionProperties.class, String.class, String.class);
        String result = (String) method.invoke(null, connectionProperties, "user", "User");
        assertEquals("testuser", result);
    }

    @Test
    void getAndCheckRequiredProperty_PropertyMissing_ThrowsException() throws Exception {
        Method method = getPrivateStaticMethod("getAndCheckRequiredProperty",
                ConnectionProperties.class, String.class, String.class);
        ConnectionProperties empty = new ConnectionProperties();

        Exception exception = assertThrows(Exception.class,
                () -> method.invoke(null, empty, "missing", "RequiredField"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("RequiredField"));
    }

    // ==================== 4. convertToJson() ====================

    @Test
    void convertToJson_ValidObject_ReturnsPrettyJson() throws Exception {
        Method method = getPrivateMethod("convertToJson", Object.class);
        SqlOutboundTransport transport = new SqlOutboundTransport();

        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "John"),
                Map.of("id", 2, "name", "Jane")
        );

        String result = (String) method.invoke(transport, data);

        assertNotNull(result);
        assertTrue(result.contains("John"));
        assertTrue(result.contains("Jane"));
        assertTrue(result.contains("\"id\" : 1") || result.contains("\"id\":1"));
    }

    @Test
    void convertToJson_Null_ReturnsNullString() throws Exception {
        Method method = getPrivateMethod("convertToJson", Object.class);
        SqlOutboundTransport transport = new SqlOutboundTransport();

        String result = (String) method.invoke(transport, (Object) null);
        assertEquals("null", result);
    }

    // ==================== 6. closeDataSourceSafely() ====================

    @Test
    void closeDataSourceSafely_NullConfig_DoesNothing() throws Exception {
        Method method = getPrivateStaticMethod("closeDataSourceSafely",
                Class.forName("org.qubership.automation.itf.transport.sql.outbound.SqlOutboundTransport$SqlConfig"));
        // Should NOT throw an exception
        method.invoke(null, (Object) null);
    }

    // ==================== 7. warnIfTooSlow() ====================

    @Test
    void warnIfTooSlow_SlowExecution_LogsWarning() throws Exception {
        // Just check that method didn't fail
        Method method = getPrivateStaticMethod("warnIfTooSlow", long.class, long.class, String.class);
        long startTime = System.currentTimeMillis() - 60000; // 60 seconds before

        // Should NOT throw an exception
        method.invoke(null, startTime, 1000L, "Test message");
    }

    // ==================== Service methods with reflection ====================

    private Method getPrivateStaticMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = SqlOutboundTransport.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private Method getPrivateMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = SqlOutboundTransport.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}