/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.sql;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Multi-tenant DataSource wrapper that sets the PostgreSQL search_path based on tenant context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantDataSource implements DataSource {

    private final DataSource dataSource;

    @SuppressFBWarnings("EI")
    public MultiTenantDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
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

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return dataSource.createConnectionBuilder();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    /**
     * Sets the PostgreSQL search_path to the current tenant's schema.
     *
     * <p>
     * <b>Security Note:</b> The SQL injection suppression is for schema name injection which cannot be parameterized in
     * PostgreSQL. Schema names must be included as identifiers, not as parameterized values. The tenant ID is validated
     * to contain only alphanumeric characters, underscores, and hyphens to prevent SQL injection. This is a PostgreSQL
     * limitation, not a security vulnerability.
     */
    @SuppressFBWarnings("SQL_INJECTION_JDBC")
    private static void setSearchPath(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + TenantContext.getCurrentDatabaseSchema());
        }
    }
}
