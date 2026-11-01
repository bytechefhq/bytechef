/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.service;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Test-only Liquibase change that records, in the schema being upgraded, the tenant id active in {@link TenantContext}
 * while the changelog runs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TenantChangelogProbeChange implements CustomTaskChange {

    private ResourceAccessor resourceAccessor;
    private boolean setUp;

    @Override
    @SuppressFBWarnings({
        "SQL_INJECTION_JDBC", "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"
    })
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection jdbcConnection = (JdbcConnection) database.getConnection();

        Connection connection = jdbcConnection.getUnderlyingConnection();

        String tableName = database.escapeTableName(null, database.getDefaultSchemaName(), "tenant_changelog_probe");

        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "INSERT INTO " + tableName + " (tenant_id) VALUES (?)")) {

            preparedStatement.setString(1, TenantContext.getCurrentTenantId());

            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new CustomChangeException(exception);
        }
    }

    @Override
    public String getConfirmationMessage() {
        if (setUp && resourceAccessor != null) {
            return "Recorded the current tenant id";
        }

        return "Recorded the current tenant id without set-up";
    }

    @Override
    public void setUp() {
        this.setUp = true;
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        this.resourceAccessor = resourceAccessor;
    }

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
