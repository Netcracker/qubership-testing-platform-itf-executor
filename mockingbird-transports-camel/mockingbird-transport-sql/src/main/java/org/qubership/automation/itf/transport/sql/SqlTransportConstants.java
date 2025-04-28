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

public interface SqlTransportConstants {

    String TYPE_DB = "typeDB";
    String TYPE_DB_DESCRIPTION = "Type of database";

    String ORACLE = "Oracle";
    String CASSANDRA = "Cassandra";
    String POSTGRESQL = "PostgreSQL";
    String SQLSERVER = "SQLServer";
    String TRINO = "Trino";
    String APACHE_HIVE = "Hive";

    String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";
    String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    String CASANDRA_DRIVER = "com.dbschema.CassandraJdbcDriver";
    String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    String TRINO_DRIVER = "io.trino.jdbc.TrinoDriver";
    String APACHE_HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";

    String USER = "user";
    String USER_DESCRIPTION = "DataBase User";

    String PASSWORD = "pass";
    String PASSWORD_STRING = "DataBase Password";
    String PASSWORD_DESCRIPTION = "DataBase Password, non-empty (for Trino DBs, please use NONE instead of empty "
            + "value)";

    String JDBC_URL = "jdbcUrl";
    String JDBC_URL_STRING = "JDBC URL";
    String JDBC_URL_DESCRIPTION = "jdbc:oracle:thin:host:port:sid or " + "jdbc:cassandra://{host}[:{port}]/{database}"
            + " or " + "jdbc:postgresql://{host}:{port}/{database} or " + "jdbc:sqlserver://[serverName"
            + "[\\instanceName][:portNumber]][;property=value[;property=value]]";

    String OPTIONS = "options";
    String OPTIONS_STRING = "Options";
    String OPTIONS_DESCRIPTION = "SomeOption=SomeValue\nreadSize=50";

    String DATA_SOURCE = "DataSource";
}
