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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "test fixtures run constant DDL")
@Testcontainers
class BaseDataSourceIntTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

    private static PGSimpleDataSource delegate;

    @BeforeAll
    static void beforeAll() throws SQLException {
        delegate = new PGSimpleDataSource();

        delegate.setUrl(POSTGRESQL_CONTAINER.getJdbcUrl());
        delegate.setUser(POSTGRESQL_CONTAINER.getUsername());
        delegate.setPassword(POSTGRESQL_CONTAINER.getPassword());

        try (Connection connection = delegate.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA bytechef_000004");
            statement.execute("CREATE SCHEMA bytechef_vectorstore_000004");
            statement.execute("CREATE TABLE public.marker (origin VARCHAR(64))");
            statement.execute("INSERT INTO public.marker VALUES ('public')");
            statement.execute("CREATE TABLE bytechef_000004.marker (origin VARCHAR(64))");
            statement.execute("INSERT INTO bytechef_000004.marker VALUES ('tenant')");
            statement.execute("CREATE TABLE bytechef_vectorstore_000004.marker (origin VARCHAR(64))");
            statement.execute("INSERT INTO bytechef_vectorstore_000004.marker VALUES ('vector')");
            statement.execute("CREATE TABLE public.public_only (origin VARCHAR(64))");
            statement.execute("INSERT INTO public.public_only VALUES ('public')");
        }
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testGetConnectionSetsSearchPathToPublicOnlyForDefaultTenant() throws SQLException {
        BaseDataSource dataSource = new TestDataSource(delegate, null);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(querySingleValue(connection, "SHOW search_path")).isEqualTo("public");
            assertThat(querySingleValue(connection, "SELECT origin FROM marker")).isEqualTo("public");
        }
    }

    @Test
    void testGetConnectionAppendsPublicSchemaAsFallbackForTenantSchema() throws SQLException {
        TenantContext.setCurrentTenantId("000004");

        BaseDataSource dataSource = new TestDataSource(delegate, null);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(querySingleValue(connection, "SHOW search_path")).isEqualTo("bytechef_000004, public");
            assertThat(querySingleValue(connection, "SELECT origin FROM marker")).isEqualTo("tenant");
            assertThat(querySingleValue(connection, "SELECT origin FROM public_only")).isEqualTo("public");
        }
    }

    @Test
    void testGetConnectionAppendsPublicSchemaAsFallbackForVectorSchema() throws SQLException {
        TenantContext.setCurrentTenantId("000004");

        BaseDataSource dataSource = new TestDataSource(delegate, "vectorstore");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(querySingleValue(connection, "SHOW search_path"))
                .isEqualTo("bytechef_vectorstore_000004, public");
            assertThat(querySingleValue(connection, "SELECT origin FROM marker")).isEqualTo("vector");
            assertThat(querySingleValue(connection, "SELECT origin FROM public_only")).isEqualTo("public");
        }
    }

    private static String querySingleValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new IllegalStateException("No row returned by " + sql);
            }

            return resultSet.getString(1);
        }
    }

    private static final class TestDataSource extends BaseDataSource {

        private final String vectorSchemaSuffix;

        private TestDataSource(DataSource dataSource, String vectorSchemaSuffix) {
            super(dataSource);

            this.vectorSchemaSuffix = vectorSchemaSuffix;
        }

        @Override
        protected String getVectorSchemaSuffix() {
            return vectorSchemaSuffix;
        }
    }
}
