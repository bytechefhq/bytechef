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

package com.bytechef.platform.data.table.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
class DataTableRowServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("rows", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
    }

    @AfterEach
    void afterEach() {
        dataTableService.dropTable("rows", DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertRowReturnsTheStoredValues() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertNotNull(dataTableRow);
        assertEquals("first", dataTableRow.values()
            .get("title"));
    }

    @Test
    void testInsertRowInsertsAndReadsInASingleStatement() {
        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertStoredRow(insertedDataTableRow, "first");
    }

    @Test
    void testInsertRowReadsAfterInsertingWhenReturningIsUnsupported() throws SQLException {
        DataTableRowService returningUnsupportedDataTableRowService = createReturningUnsupportedDataTableRowService();

        DataTableRow insertedDataTableRow = returningUnsupportedDataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertStoredRow(insertedDataTableRow, "first");
    }

    @Test
    void testUpdateRowReturnsTheUpdatedValues() {
        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            "rows", insertedDataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertEquals(insertedDataTableRow.id(), updatedDataTableRow.id());
        assertEquals("second", updatedDataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = dataTableRowService.getRow(
            "rows", insertedDataTableRow.id(), DEV_ENVIRONMENT_ID);

        assertEquals("second", fetchedDataTableRow.values()
            .get("title"));
    }

    @Test
    void testUpdateRowUpdatesAndReadsInASingleStatement() {
        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        DataTableRow updatedDataTableRow = dataTableRowService.updateRow(
            "rows", insertedDataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertEquals(insertedDataTableRow.id(), updatedDataTableRow.id());
        assertStoredRow(updatedDataTableRow, "second");
    }

    @Test
    void testUpdateRowReadsAfterUpdatingWhenReturningIsUnsupported() throws SQLException {
        DataTableRowService returningUnsupportedDataTableRowService = createReturningUnsupportedDataTableRowService();

        DataTableRow insertedDataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        DataTableRow updatedDataTableRow = returningUnsupportedDataTableRowService.updateRow(
            "rows", insertedDataTableRow.id(), Map.of("title", "second"), DEV_ENVIRONMENT_ID);

        assertEquals(insertedDataTableRow.id(), updatedDataTableRow.id());
        assertStoredRow(updatedDataTableRow, "second");
    }

    @Test
    void testDeleteRowRemovesTheRow() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "first"), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowService.deleteRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertNull(dataTableRowService.getRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertTrue(dataTableRowService.listRows("rows", 10, 0, DEV_ENVIRONMENT_ID)
            .isEmpty());
    }

    @Test
    void testDeleteRowPublishesTheDeletedRowValues() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "closed"), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowService.deleteRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertEquals(Map.of("id", dataTableRow.id(), "values", Map.of("title", "closed")), deletedPayload());
    }

    @Test
    void testDeleteRowReadsAndDeletesInASingleStatement() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "closed"), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableRowService.deleteRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertNull(dataTableRowService.getRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));
        assertEquals(Map.of("id", dataTableRow.id(), "values", Map.of("title", "closed")), deletedPayload());
    }

    @Test
    void testDeleteRowReadsBeforeDeletingWhenReturningIsUnsupported() throws SQLException {
        DataTableRowService returningUnsupportedDataTableRowService = createReturningUnsupportedDataTableRowService();

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "rows", Map.of("title", "closed"), DEV_ENVIRONMENT_ID);

        assertTrue(returningUnsupportedDataTableRowService.deleteRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        assertNull(dataTableRowService.getRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID));
        assertEquals(Map.of("id", dataTableRow.id(), "values", Map.of("title", "closed")), deletedPayload());
    }

    @Test
    void testDeleteRowOfMissingRowReturnsFalse() {
        assertFalse(dataTableRowService.deleteRow("rows", Long.MAX_VALUE, DEV_ENVIRONMENT_ID));
    }

    @Test
    void testDeleteRowOfMissingRowPublishesNothing() {
        assertFalse(dataTableRowService.deleteRow("rows", Long.MAX_VALUE, DEV_ENVIRONMENT_ID));

        assertEquals(0, applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(dataTableWebhookEvent -> dataTableWebhookEvent.getType() == DataTableWebhookType.RECORD_DELETED)
            .count());
    }

    private void assertStoredRow(DataTableRow dataTableRow, String expectedTitle) {
        assertNotNull(dataTableRow);
        assertEquals(expectedTitle, dataTableRow.values()
            .get("title"));

        DataTableRow fetchedDataTableRow = dataTableRowService.getRow("rows", dataTableRow.id(), DEV_ENVIRONMENT_ID);

        assertEquals(fetchedDataTableRow, dataTableRow);
    }

    /**
     * Builds a service over the real Postgres data source whose connection metadata reports H2, so the code path used
     * for databases without RETURNING runs against the real database.
     */
    private DataTableRowService createReturningUnsupportedDataTableRowService() {
        DataSource h2ReportingDataSource = new DelegatingDataSource(dataSource) {

            @Override
            public Connection getConnection() throws SQLException {
                return reportH2(super.getConnection());
            }
        };

        return new DataTableRowServiceImpl(applicationEventPublisher, new JdbcTemplate(h2ReportingDataSource));
    }

    private Map<String, Object> deletedPayload() {
        List<DataTableWebhookEvent> deletedEvents = applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(dataTableWebhookEvent -> dataTableWebhookEvent.getType() == DataTableWebhookType.RECORD_DELETED)
            .toList();

        assertEquals(1, deletedEvents.size());

        DataTableWebhookEvent deletedEvent = deletedEvents.getFirst();

        return deletedEvent.getPayload();
    }

    @SuppressWarnings("PMD.UnusedReturnValue")
    private static Connection reportH2(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();

        DatabaseMetaData h2DatabaseMetaData = mock(DatabaseMetaData.class, delegatesTo(databaseMetaData));

        doReturn("H2").when(h2DatabaseMetaData)
            .getDatabaseProductName();

        Connection h2Connection = mock(Connection.class, delegatesTo(connection));

        doReturn(h2DatabaseMetaData).when(h2Connection)
            .getMetaData();

        return h2Connection;
    }
}
