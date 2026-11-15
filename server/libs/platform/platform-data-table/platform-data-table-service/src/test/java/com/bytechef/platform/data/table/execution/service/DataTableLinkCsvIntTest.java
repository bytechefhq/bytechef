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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DataTableIntTestUtils;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableLinkCsvIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long OTHER_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long customersId;
    private long firstCustomerId;
    private long ordersId;

    @BeforeEach
    void beforeEach() {
        DataTableIntTestUtils.dropAll(jdbcTemplate);

        long[] tableIds = createTables(ENVIRONMENT_ID);

        customersId = tableIds[0];
        ordersId = tableIds[1];

        firstCustomerId = dataTableRowService.insertRow(customersId, Map.of("name", "Ann"), "c-1", ENVIRONMENT_ID)
            .id();

        dataTableRowService.insertRow(customersId, Map.of("name", "Bob"), "c-2", ENVIRONMENT_ID);
    }

    @Test
    void testExportWritesIdAndExternalIdColumns() {
        dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", firstCustomerId), "o-1", ENVIRONMENT_ID);
        dataTableRowService.insertRow(ordersId, Map.of("title", "o2"), "o-2", ENVIRONMENT_ID);

        List<String> lines = dataTableRowService.exportCsv(ordersId, ENVIRONMENT_ID)
            .lines()
            .toList();

        assertEquals("external_id,title,customer,customer.externalId", lines.get(0));
        assertEquals("o-1,o1," + firstCustomerId + ",c-1", lines.get(1));
        assertEquals("o-2,o2,,", lines.get(2));
    }

    @Test
    void testRoundTripIntoAnotherEnvironmentLinksByExternalId() {
        dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", firstCustomerId), "o-1", ENVIRONMENT_ID);

        String csv = dataTableRowService.exportCsv(ordersId, ENVIRONMENT_ID);

        long[] otherTableIds = createTables(OTHER_ENVIRONMENT_ID);

        long otherCustomersId = otherTableIds[0];
        long otherOrdersId = otherTableIds[1];

        dataTableRowService.insertRow(
            otherCustomersId, Map.of("name", "Padding"), "padding", OTHER_ENVIRONMENT_ID);

        long otherFirstCustomerId = dataTableRowService.insertRow(
            otherCustomersId, Map.of("name", "Ann"), "c-1", OTHER_ENVIRONMENT_ID)
            .id();

        assertEquals(1, dataTableRowService.importCsv(otherOrdersId, csv, OTHER_ENVIRONMENT_ID));

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            otherOrdersId, 10, 0, OTHER_ENVIRONMENT_ID);

        assertThat(dataTableRows).hasSize(1);
        assertEquals(otherFirstCustomerId, dataTableRows.getFirst()
            .values()
            .get("customer"));
    }

    @Test
    void testImportWithUnknownExternalIdFailsWholeImport() {
        String csv = "title,customer.externalId\no1,c-1\no2,c-unknown\n";

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> dataTableRowService.importCsv(ordersId, csv, ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.LINKED_ROW_NOT_FOUND, dataTableException.getErrorType());
        assertThat(dataTableException.getMessage()).contains("CSV line 3");
        assertEquals(0, countOrders());
    }

    @Test
    void testImportWithIdsOnly() {
        String csv = "title,customer\no1," + firstCustomerId + "\no2,\n";

        assertEquals(2, dataTableRowService.importCsv(ordersId, csv, ENVIRONMENT_ID));

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(ordersId, 10, 0, ENVIRONMENT_ID);

        assertEquals(firstCustomerId, dataTableRows.get(0)
            .values()
            .get("customer"));
        assertEquals(null, dataTableRows.get(1)
            .values()
            .get("customer"));
    }

    @Test
    void testImportWithIdsOnlyAndMissingTargetFailsWholeImport() {
        String csv = "title,customer\no1," + firstCustomerId + "\no2,987654321\n";

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> dataTableRowService.importCsv(ordersId, csv, ENVIRONMENT_ID));

        assertEquals(DataTableErrorType.LINKED_ROW_NOT_FOUND, dataTableException.getErrorType());
        assertEquals(0, countOrders());
    }

    @Test
    void testImportPrefersExternalIdColumnAndEmptyMeansNoLink() {
        String csv = "title,customer,customer.externalId\no1,987654321,c-1\no2," + firstCustomerId + ",\n";

        assertEquals(2, dataTableRowService.importCsv(ordersId, csv, ENVIRONMENT_ID));

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(ordersId, 10, 0, ENVIRONMENT_ID);

        assertEquals(firstCustomerId, dataTableRows.get(0)
            .values()
            .get("customer"));
        assertEquals(null, dataTableRows.get(1)
            .values()
            .get("customer"));
    }

    @Test
    void testImportIgnoresExternalIdHeaderOfNonLinkColumn() {
        String csv = "title,title.externalId\no1,ignored\n";

        assertEquals(1, dataTableRowService.importCsv(ordersId, csv, ENVIRONMENT_ID));
        assertEquals(1, countOrders());
    }

    private long[] createTables(long environmentId) {
        long environmentCustomersId = dataTableService.createTable(
            WORKSPACE_ID, "customers" + environmentId, null, List.of(new ColumnSpec("name", ColumnType.STRING)),
            environmentId);
        long environmentOrdersId = dataTableService.createTable(
            WORKSPACE_ID, "orders" + environmentId, null, List.of(new ColumnSpec("title", ColumnType.STRING)),
            environmentId);

        dataTableService.addColumn(
            environmentOrdersId,
            new ColumnSpec(
                "customer", ColumnType.LINK, new LinkSpec(environmentCustomersId, LinkDeleteRule.BLOCK, "name")),
            environmentId);

        return new long[] {
            environmentCustomersId, environmentOrdersId
        };
    }

    private long countOrders() {
        return dataTableRowService.countRows(ordersId, DataTableRowConditions.matchAll(), ENVIRONMENT_ID);
    }
}
