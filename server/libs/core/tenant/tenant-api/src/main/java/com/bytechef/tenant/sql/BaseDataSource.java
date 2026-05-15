/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.tenant.sql;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.DataSource;

/**
 * @author Igor Beslic
 */
public abstract class BaseDataSource implements DataSource {

    private final DataSource dataSource;
    private static final String SET_SEARCH_PATH_STATEMENT = "SET search_path TO ";

    @SuppressFBWarnings("EI")
    public BaseDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return dataSource.createConnectionBuilder();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();

        setSearchPath(connection);

        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = dataSource.getConnection(username, password);

        setSearchPath(connection);

        return connection;
    }

    /**
     * Sets the search_path to the current tenant's schema. Method handles setting the search path for both vector and
     * classic schema.
     *
     * <p>
     * <b>Note:</b> This method is responsible for setting the database tenant in multitenant environments. If one
     * relies on this API class and does not depend on the multitenant database, this method can be safely ignored.
     * </p>
     * <p>
     * <b>Security Note:</b> The SQL injection suppression is for schema name configuration statement that cannot be
     * parameterized with PreparedStatement in PostgreSQL. Schema names must be included as identifiers, not as
     * parameterized values. The tenant ID is validated to contain only alphanumeric characters, underscores, and
     * hyphens to prevent SQL injection. This is a PostgreSQL limitation, not a security vulnerability.
     * </p>
     */
    @SuppressFBWarnings("SQL_INJECTION_JDBC")
    protected void setSearchPath(Connection connection) throws SQLException {
        String currentDatabaseSchema = TenantContext.getCurrentDatabaseSchema(getVectorSchemaSuffix());

        try (PreparedStatement statement =
            connection.prepareStatement(SET_SEARCH_PATH_STATEMENT + currentDatabaseSchema)) {

            statement.execute();
        }
    }

    /**
     * Each tenant might have both vector and classic schema. Vector schema suffix designates the schema where vector
     * data is stored.
     *
     * @return prefix for vector schema
     */
    protected String getVectorSchemaSuffix() {
        return null;
    }

}
