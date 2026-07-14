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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings(
    value = {
        "ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION", "SQL_INJECTION_JDBC"
    },
    justification = "connection and preparedStatement are Mockito mocks; prepareStatement() is stubbed/verified, not "
        + "a real resource-returning call")
class BaseDataSourceTest {

    @Mock
    private DataSource delegate;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Test
    void testGetConnectionSetsSearchPathToPublicOnlyForDefaultTenant() throws SQLException {
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        BaseDataSource dataSource = new TestDataSource(delegate, null);

        dataSource.getConnection();

        assertEquals("SET search_path TO public", capturedSearchPathStatement());
    }

    @Test
    void testGetConnectionAppendsPublicSchemaAsFallbackForTenantSchema() throws SQLException {
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        TenantContext.setCurrentTenantId("000004");

        try {
            BaseDataSource dataSource = new TestDataSource(delegate, null);

            dataSource.getConnection();

            assertEquals("SET search_path TO bytechef_000004, public", capturedSearchPathStatement());
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    @Test
    void testGetConnectionAppendsPublicSchemaAsFallbackForVectorSchema() throws SQLException {
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        TenantContext.setCurrentTenantId("000004");

        try {
            BaseDataSource dataSource = new TestDataSource(delegate, "vectorstore");

            dataSource.getConnection();

            assertEquals(
                "SET search_path TO bytechef_vectorstore_000004, public", capturedSearchPathStatement());
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    private String capturedSearchPathStatement() throws SQLException {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(connection).prepareStatement(sqlCaptor.capture());

        return sqlCaptor.getValue();
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
