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

package com.bytechef.platform.data.table.configuration.migration;

import java.sql.Connection;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Runs {@link DataTableOwnerColumnMigrator} as a Liquibase {@code customChange}.
 *
 * <p>
 * The backfill has to be Java because the tables it alters are created at runtime and so cannot be named in a
 * changeset. Running it through Liquibase anyway buys the two things a startup listener would not: it executes once per
 * schema and is recorded in {@code databasechangelog} instead of rescanning {@code information_schema} on every boot,
 * and {@code MultiTenantSpringLiquibase} already runs the changelog against each tenant schema in turn.
 *
 * @author Ivica Cardic
 */
public class DataTableOwnerColumnChange implements CustomTaskChange {

    private int alteredTableCount;

    @Override
    public void execute(Database database) throws CustomChangeException {
        if (!(database.getConnection() instanceof JdbcConnection jdbcConnection)) {
            throw new CustomChangeException(
                "Expected a JDBC connection, got " + database.getConnection());
        }

        Connection connection = jdbcConnection.getUnderlyingConnection();

        // suppressClose, because the connection belongs to Liquibase and is reused for the rest of the changelog
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(connection, true);

        DataTableOwnerColumnMigrator dataTableOwnerColumnMigrator = new DataTableOwnerColumnMigrator(
            new JdbcTemplate(dataSource));

        alteredTableCount = dataTableOwnerColumnMigrator.migrate(database.getDefaultSchemaName());
    }

    @Override
    public String getConfirmationMessage() {
        return "Added owner columns to " + alteredTableCount + " data tables";
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // the change reads no changelog resources
        alteredTableCount = 0;
    }

    @Override
    public void setUp() {
        alteredTableCount = 0;
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
