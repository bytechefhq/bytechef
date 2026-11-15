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

package com.bytechef.platform.data.table.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DataTableIntTestUtils;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableLinkColumn;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.List;
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
@Import(H2DataSourceConfiguration.class)
class DataTableLinkColumnH2IntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long STAGE_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long customersId;
    private long ordersId;

    @BeforeEach
    void beforeEach() {
        DataTableIntTestUtils.dropAll(jdbcTemplate);

        customersId = createTable(WORKSPACE_ID, "customers", DEV_ENVIRONMENT_ID);
        ordersId = createTable(WORKSPACE_ID, "orders", DEV_ENVIRONMENT_ID);
    }

    @Test
    void testAddLinkColumnReportsLinkSpec() {
        ColumnSpec linkColumnSpec = customerLink(LinkDeleteRule.BLOCK, "name");

        dataTableService.addColumn(ordersId, linkColumnSpec, DEV_ENVIRONMENT_ID);

        assertThat(getColumnSpecs(ordersId, DEV_ENVIRONMENT_ID)).contains(linkColumnSpec);
    }

    @Test
    void testAddLinkColumnRejectsTargetInAnotherWorkspace() {
        long foreignId = createTable(2L, "foreign_table", DEV_ENVIRONMENT_ID);

        assertErrorType(
            DataTableErrorType.LINK_TARGET_INVALID,
            () -> dataTableService.addColumn(
                ordersId,
                new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(foreignId, LinkDeleteRule.BLOCK, null)),
                DEV_ENVIRONMENT_ID));

        assertThat(dataTableService.getLinksTo(foreignId, DEV_ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testAddLinkColumnRejectsTargetMissingInEnvironment() {
        dataTableService.createTable(
            WORKSPACE_ID, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), STAGE_ENVIRONMENT_ID);

        assertErrorType(
            DataTableErrorType.LINK_TARGET_INVALID,
            () -> dataTableService.addColumn(
                ordersId, customerLink(LinkDeleteRule.BLOCK, null), STAGE_ENVIRONMENT_ID));

        assertThat(getColumnSpecs(ordersId, STAGE_ENVIRONMENT_ID))
            .extracting(ColumnSpec::name)
            .doesNotContain("customer");
        assertThat(dataTableService.getLinksTo(customersId, STAGE_ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testAddLinkColumnRejectsSelfLink() {
        assertErrorType(
            DataTableErrorType.LINK_TARGET_INVALID,
            () -> dataTableService.addColumn(
                ordersId, new ColumnSpec("parent", ColumnType.LINK, new LinkSpec(ordersId, LinkDeleteRule.BLOCK, null)),
                DEV_ENVIRONMENT_ID));
    }

    @Test
    void testAddLinkColumnRejectsUnknownDisplayColumn() {
        assertErrorType(
            DataTableErrorType.LINK_DISPLAY_COLUMN_INVALID,
            () -> dataTableService.addColumn(
                ordersId, customerLink(LinkDeleteRule.BLOCK, "missing"), DEV_ENVIRONMENT_ID));

        assertThat(getColumnSpecs(ordersId, DEV_ENVIRONMENT_ID))
            .extracting(ColumnSpec::name)
            .doesNotContain("customer");
        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testAddLinkColumnRejectsLinkColumnAsDisplayColumn() {
        dataTableService.addColumn(
            customersId,
            new ColumnSpec("referrer", ColumnType.LINK, new LinkSpec(ordersId, LinkDeleteRule.BLOCK, null)),
            DEV_ENVIRONMENT_ID);

        assertErrorType(
            DataTableErrorType.LINK_DISPLAY_COLUMN_INVALID,
            () -> dataTableService.addColumn(
                ordersId, customerLink(LinkDeleteRule.BLOCK, "referrer"), DEV_ENVIRONMENT_ID));

        assertThat(getColumnSpecs(ordersId, DEV_ENVIRONMENT_ID))
            .extracting(ColumnSpec::name)
            .doesNotContain("customer");
    }

    @Test
    void testLinksInBothDirectionsAreAllowed() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);
        dataTableService.addColumn(
            customersId,
            new ColumnSpec("last_order", ColumnType.LINK, new LinkSpec(ordersId, LinkDeleteRule.CLEAR, null)),
            DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID)).hasSize(1);
        assertThat(dataTableService.getLinksTo(ordersId, DEV_ENVIRONMENT_ID)).hasSize(1);
    }

    @Test
    void testRenameLinkColumnKeepsLink() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.CLEAR, "name"), DEV_ENVIRONMENT_ID);

        dataTableService.renameColumn(ordersId, "customer", "client", DEV_ENVIRONMENT_ID);

        assertThat(getColumnSpecs(ordersId, DEV_ENVIRONMENT_ID))
            .contains(
                new ColumnSpec(
                    "client", ColumnType.LINK, new LinkSpec(customersId, LinkDeleteRule.CLEAR, "name")));
    }

    @Test
    void testRemoveLinkColumnDeletesRegistryRow() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);

        dataTableService.removeColumn(ordersId, "customer", DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID)).isEmpty();
        assertThat(getColumnSpecs(ordersId, DEV_ENVIRONMENT_ID))
            .extracting(ColumnSpec::name)
            .doesNotContain("customer");
    }

    @Test
    void testRenameDisplayColumnUpdatesLinks() {
        createTable(WORKSPACE_ID, "customers", STAGE_ENVIRONMENT_ID, "name");
        createTable(WORKSPACE_ID, "orders", STAGE_ENVIRONMENT_ID, "title");

        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), STAGE_ENVIRONMENT_ID);

        dataTableService.renameColumn(customersId, "name", "full_name", DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID))
            .extracting(DataTableLinkColumn::getDisplayColumnName)
            .containsExactly("full_name");
        assertThat(dataTableService.getLinksTo(customersId, STAGE_ENVIRONMENT_ID))
            .extracting(DataTableLinkColumn::getDisplayColumnName)
            .containsExactly("name");
    }

    @Test
    void testRemoveDisplayColumnClearsDisplayColumn() {
        createTable(WORKSPACE_ID, "customers", STAGE_ENVIRONMENT_ID, "name");
        createTable(WORKSPACE_ID, "orders", STAGE_ENVIRONMENT_ID, "title");

        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), STAGE_ENVIRONMENT_ID);

        dataTableService.removeColumn(customersId, "name", DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID))
            .extracting(DataTableLinkColumn::getDisplayColumnName)
            .containsExactly((String) null);
        assertThat(dataTableService.getLinksTo(customersId, STAGE_ENVIRONMENT_ID))
            .extracting(DataTableLinkColumn::getDisplayColumnName)
            .containsExactly("name");
    }

    @Test
    void testDropTargetTableIsBlockedWhileLinked() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);

        assertErrorType(
            DataTableErrorType.ROW_STILL_LINKED, () -> dataTableService.dropTable(customersId, DEV_ENVIRONMENT_ID));

        assertThat(dataTableService.fetchDataTableInfo(customersId, DEV_ENVIRONMENT_ID)).isPresent();
    }

    @Test
    void testDropSourceTableDeletesItsLinks() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);

        dataTableService.dropTable(ordersId, DEV_ENVIRONMENT_ID);

        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID)).isEmpty();
        assertThat(dataTableService.dropTable(customersId, DEV_ENVIRONMENT_ID)).isTrue();
    }

    @Test
    void testDuplicateSourceTableCopiesLinks() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.CLEAR, "name"), DEV_ENVIRONMENT_ID);

        long copyId = dataTableService.duplicateTable(ordersId, "orders_copy", DEV_ENVIRONMENT_ID);

        assertThat(getColumnSpecs(copyId, DEV_ENVIRONMENT_ID))
            .contains(customerLink(LinkDeleteRule.CLEAR, "name"));
        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID)).hasSize(2);
    }

    @Test
    void testCreateTableInNewEnvironmentWithLinkColumn() {
        createTable(WORKSPACE_ID, "customers", STAGE_ENVIRONMENT_ID, "name");

        dataTableService.createTable(
            WORKSPACE_ID, "orders", null, List.of(customerLink(LinkDeleteRule.BLOCK, "name")), STAGE_ENVIRONMENT_ID);

        assertThat(getColumnSpecs(ordersId, STAGE_ENVIRONMENT_ID)).contains(customerLink(LinkDeleteRule.BLOCK, "name"));
        assertThat(dataTableService.getLinksTo(customersId, STAGE_ENVIRONMENT_ID)).hasSize(1);
        assertThat(dataTableService.getLinksTo(customersId, DEV_ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testRenameTargetTableKeepsLinks() {
        dataTableService.addColumn(ordersId, customerLink(LinkDeleteRule.BLOCK, "name"), DEV_ENVIRONMENT_ID);

        dataTableService.renameTable(customersId, "clients");

        assertThat(getColumnSpecs(ordersId, DEV_ENVIRONMENT_ID)).contains(customerLink(LinkDeleteRule.BLOCK, "name"));
    }

    private ColumnSpec customerLink(LinkDeleteRule linkDeleteRule, String displayColumnName) {
        return new ColumnSpec(
            "customer", ColumnType.LINK, new LinkSpec(customersId, linkDeleteRule, displayColumnName));
    }

    private long createTable(Long workspaceId, String name, long environmentId) {
        return createTable(workspaceId, name, environmentId, name.equals("customers") ? "name" : "title");
    }

    private long createTable(Long workspaceId, String name, long environmentId, String columnName) {
        return dataTableService.createTable(
            workspaceId, name, null, List.of(new ColumnSpec(columnName, ColumnType.STRING)), environmentId);
    }

    private List<ColumnSpec> getColumnSpecs(long dataTableId, long environmentId) {
        DataTableInfo dataTableInfo = dataTableService.fetchDataTableInfo(dataTableId, environmentId)
            .orElseThrow();

        return dataTableInfo.columns();
    }

    private static void assertErrorType(DataTableErrorType expectedErrorType, Runnable runnable) {
        DataTableException dataTableException = assertThrows(DataTableException.class, runnable::run);

        assertEquals(expectedErrorType, dataTableException.getErrorType());
    }
}
