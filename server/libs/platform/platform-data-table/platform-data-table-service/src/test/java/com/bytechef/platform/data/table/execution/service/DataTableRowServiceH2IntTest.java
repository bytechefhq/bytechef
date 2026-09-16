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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
@RecordApplicationEvents
class DataTableRowServiceH2IntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DataTableRef dataTableRef;

    @BeforeEach
    void beforeEach() {
        dataTableService.fetchDataTable(WORKSPACE_ID, "h2rows")
            .ifPresent(dataTable -> dataTableService.dropTable(dataTable.getId(), ENVIRONMENT_ID));

        long dataTableId = dataTableService.createTable(
            WORKSPACE_ID, "h2rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableRef = new DataTableRef(dataTableId, ENVIRONMENT_ID);

        applicationEvents.clear();
    }

    @Test
    void testInsertRowPublishesRecordCreated() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));

        assertThat(dataTableRow.values()).containsEntry("title", "a");
        assertThat(webhookTypes()).containsExactly(DataTableWebhookType.RECORD_CREATED);
    }

    @Test
    void testUpdateRowPublishesRecordUpdated() {
        DataTableRow inserted = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));

        applicationEvents.clear();

        DataTableRow updated = dataTableRowService.updateRow(dataTableRef, inserted.id(), Map.of("title", "b"));

        assertThat(updated.values()).containsEntry("title", "b");
        assertThat(webhookTypes()).containsExactly(DataTableWebhookType.RECORD_UPDATED);
    }

    @Test
    void testUpsertRowCreatesThenMerges() {
        UpsertResult first = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "a"));
        UpsertResult second = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "b"));

        assertThat(first.created()).isTrue();
        assertThat(first.row()
            .externalId()).isEqualTo("ORD-1");
        assertThat(second.created()).isFalse();
        assertThat(second.row()
            .id()).isEqualTo(
                first.row()
                    .id());
        assertThat(second.row()
            .values()).containsEntry("title", "b");
        assertThat(dataTableRowService.listRows(dataTableRef, 10, 0)).hasSize(1);
        assertThat(webhookTypes()).containsExactly(
            DataTableWebhookType.RECORD_CREATED, DataTableWebhookType.RECORD_UPDATED);
    }

    @Test
    void testUpsertRowWithNoValuesReturnsTheExistingRow() {
        UpsertResult first = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "a"));
        UpsertResult second = dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of());

        assertThat(second.created()).isFalse();
        assertThat(second.row()
            .id()).isEqualTo(
                first.row()
                    .id());
        assertThat(second.row()
            .values()).containsEntry("title", "a");
    }

    @Test
    void testInsertRowsWithTheUpsertStrategyCreatesAndMerges() {
        dataTableRowService.upsertRow(dataTableRef, "ORD-1", Map.of("title", "a"));

        applicationEvents.clear();

        List<DataTableRow> dataTableRows = dataTableRowService.insertRows(
            dataTableRef,
            List.of(new NewRow(Map.of("title", "b"), "ORD-1"), new NewRow(Map.of("title", "c"), "ORD-2")),
            CreateStrategy.UPSERT);

        assertThat(dataTableRows).extracting(DataTableRow::externalId)
            .containsExactly("ORD-1", "ORD-2");
        assertThat(dataTableRowService.listRows(dataTableRef, 10, 0)).hasSize(2);
        assertThat(webhookTypes()).containsExactly(
            DataTableWebhookType.RECORD_UPDATED, DataTableWebhookType.RECORD_CREATED);
    }

    @Test
    void testDeleteRowsDeletesOnlyTheExistingListedRows() {
        DataTableRow first = dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));
        DataTableRow second = dataTableRowService.insertRow(dataTableRef, Map.of("title", "b"));
        DataTableRow kept = dataTableRowService.insertRow(dataTableRef, Map.of("title", "c"));

        applicationEvents.clear();

        List<Long> deletedIds = dataTableRowService.deleteRows(
            dataTableRef, List.of(first.id(), second.id(), 999_999L));

        assertThat(deletedIds).containsExactlyInAnyOrder(first.id(), second.id());
        assertThat(dataTableRowService.listRows(dataTableRef, 10, 0)).extracting(DataTableRow::id)
            .containsExactly(kept.id());
        assertThat(webhookTypes()).containsExactly(
            DataTableWebhookType.RECORD_DELETED, DataTableWebhookType.RECORD_DELETED);
    }

    @Test
    void testClearRowsDeletesEveryRow() {
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "a"));
        dataTableRowService.insertRow(dataTableRef, Map.of("title", "b"));

        applicationEvents.clear();

        assertThat(dataTableRowService.clearRows(dataTableRef)).isEqualTo(2);
        assertThat(dataTableRowService.listRows(dataTableRef, 10, 0)).isEmpty();
        assertThat(webhookTypes()).containsExactly(
            DataTableWebhookType.RECORD_DELETED, DataTableWebhookType.RECORD_DELETED);
    }

    private List<DataTableWebhookType> webhookTypes() {
        return applicationEvents.stream(DataTableWebhookEvent.class)
            .map(DataTableWebhookEvent::getType)
            .toList();
    }
}
