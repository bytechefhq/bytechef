/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.promotion.contributor;

import static com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions.assertPreAuthorize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.promotion.config.DataTablePromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange.Kind;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.test.config.PromotionIntTestSharedMocks;
import com.bytechef.ee.automation.promotion.test.config.RecordingPermissionService;
import com.bytechef.ee.automation.promotion.test.config.RecordingPermissionService.ResourceScopeCheck;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTablePromotionIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@PromotionIntTestSharedMocks
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class DataTablePromotionContributorIntTest {

    private static final String PERMISSION_EXPRESSION = "hasPermission(#sourceId, 'DataTable', 'DATA_TABLE_EDIT')";
    private static final long SOURCE_ENVIRONMENT_ID = Environment.DEVELOPMENT.ordinal();
    private static final long TARGET_ENVIRONMENT_ID = Environment.STAGING.ordinal();
    private static final long WORKSPACE_ID = 9301;

    @Autowired
    private EnvironmentPromotionContributor dataTablePromotionContributor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingPermissionService permissionService;

    @Autowired
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void beforeEach() {
        permissionService.reset();

        deleteTestTables();

        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", WORKSPACE_ID);
        jdbcTemplate.update(
            "INSERT INTO workspace (id, name, created_date, created_by, last_modified_date, last_modified_by, " +
                "version) VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
            WORKSPACE_ID, "data-table-promotion-workspace");
    }

    @AfterEach
    void afterEach() {
        deleteTestTables();

        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", WORKSPACE_ID);

        permissionService.reset();
    }

    @Test
    void testPromoteCreatesEmptyTableWithSameColumns() {
        long dataTableId = createTable(
            "orders", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("amount", ColumnType.NUMBER), new ColumnSpec("paid", ColumnType.BOOLEAN));

        workspaceDataTableFacade.insertRow(
            dataTableId, Map.of("title", "first", "amount", 10, "paid", true), SOURCE_ENVIRONMENT_ID);
        workspaceDataTableFacade.insertRow(
            dataTableId, Map.of("title", "second", "amount", 20, "paid", false), SOURCE_ENVIRONMENT_ID);

        EnvironmentPromotionResult environmentPromotionResult = promote(dataTableId);

        assertThat(environmentPromotionResult.targetId()).isEqualTo(dataTableId);
        assertThat(environmentPromotionResult.created()).isTrue();
        assertThat(environmentPromotionResult.unresolvedConnectionIds()).isEmpty();
        assertThat(targetColumns(dataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.NUMBER),
            new ColumnSpec("paid", ColumnType.BOOLEAN));
        assertThat(countRows(dataTableId, TARGET_ENVIRONMENT_ID)).isZero();
        assertThat(countRows(dataTableId, SOURCE_ENVIRONMENT_ID)).isEqualTo(2);
        assertThat(permissionService.getResourceScopeChecks())
            .contains(new ResourceScopeCheck(dataTableId, "DataTable", "DATA_TABLE_EDIT"));
    }

    @Test
    void testPromoteFullSyncAddsDropsAndRetypes() {
        long dataTableId = createTable(
            "invoices", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("quantity", ColumnType.INTEGER), new ColumnSpec("approved", ColumnType.BOOLEAN));

        createTable(
            "invoices", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("quantity", ColumnType.STRING), new ColumnSpec("legacy", ColumnType.DATE));

        workspaceDataTableFacade.insertRow(
            dataTableId, Map.of("title", "kept", "quantity", "5"), TARGET_ENVIRONMENT_ID);

        EnvironmentPromotionResult environmentPromotionResult = promote(dataTableId);

        assertThat(environmentPromotionResult.targetId()).isEqualTo(dataTableId);
        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(targetColumns(dataTableId)).containsExactlyInAnyOrder(
            new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("quantity", ColumnType.INTEGER),
            new ColumnSpec("approved", ColumnType.BOOLEAN));

        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT title, quantity FROM " + physicalName(dataTableId, TARGET_ENVIRONMENT_ID));

        assertThat(row).containsEntry("title", "kept");
        assertThat(((Number) row.get("quantity")).intValue()).isEqualTo(5);
    }

    @Test
    void testPromoteUpdateKeepsTargetRowsAndCopiesNoSourceRows() {
        long dataTableId = createTable(
            "receipts", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("total", ColumnType.NUMBER));

        createTable("receipts", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING));

        workspaceDataTableFacade.insertRow(
            dataTableId, Map.of("title", "source-first", "total", 1), SOURCE_ENVIRONMENT_ID);
        workspaceDataTableFacade.insertRow(
            dataTableId, Map.of("title", "source-second", "total", 2), SOURCE_ENVIRONMENT_ID);
        workspaceDataTableFacade.insertRow(dataTableId, Map.of("title", "target-own"), TARGET_ENVIRONMENT_ID);

        EnvironmentPromotionResult environmentPromotionResult = promote(dataTableId);

        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(countRows(dataTableId, TARGET_ENVIRONMENT_ID)).isEqualTo(1);
        assertThat(selectString("title", dataTableId)).isEqualTo("target-own");
        assertThat(countRows(dataTableId, SOURCE_ENVIRONMENT_ID)).isEqualTo(2);
    }

    @Test
    void testPromoteRefusesUnsupportedColumnTypeChange() {
        long dataTableId = createTable("parcels", SOURCE_ENVIRONMENT_ID, new ColumnSpec("sent", ColumnType.BOOLEAN));

        createTable("parcels", TARGET_ENVIRONMENT_ID, new ColumnSpec("sent", ColumnType.DATE));

        assertThatThrownBy(() -> promote(dataTableId))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("'sent'")
            .hasMessageContaining("not supported")
            .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED.getErrorKey()));

        assertThat(targetColumns(dataTableId)).containsExactly(new ColumnSpec("sent", ColumnType.DATE));
    }

    @Test
    void testPromoteRefusesSourceTableWithoutColumns() {
        long dataTableId = createTable("archive", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));

        workspaceDataTableFacade.removeColumn(dataTableId, "name", SOURCE_ENVIRONMENT_ID);

        assertThatThrownBy(() -> promote(dataTableId))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("has no columns")
            .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED.getErrorKey()));

        assertThat(tableExists(dataTableId, TARGET_ENVIRONMENT_ID)).isFalse();
    }

    @Test
    void testFailedRetypeRollsBackEverything() {
        long dataTableId = createTable("shipments", SOURCE_ENVIRONMENT_ID, new ColumnSpec("b", ColumnType.INTEGER));

        createTable(
            "shipments", TARGET_ENVIRONMENT_ID, new ColumnSpec("a", ColumnType.STRING),
            new ColumnSpec("b", ColumnType.STRING));

        workspaceDataTableFacade.insertRow(dataTableId, Map.of("a", "kept", "b", "x"), TARGET_ENVIRONMENT_ID);

        assertThatThrownBy(() -> promote(dataTableId))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("shipments")
            .hasMessageContaining("'b'")
            .hasCauseExactlyInstanceOf(DataIntegrityViolationException.class)
            .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED.getErrorKey()));

        assertThat(targetColumns(dataTableId)).containsExactly(
            new ColumnSpec("a", ColumnType.STRING), new ColumnSpec("b", ColumnType.STRING));

        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT a, b FROM " + physicalName(dataTableId, TARGET_ENVIRONMENT_ID));

        assertThat(row).containsEntry("a", "kept");
        assertThat(row).containsEntry("b", "x");
    }

    @Test
    void testPromotesLinkedTablesAndCycles() {
        long firstDataTableId =
            createTable("authors", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long secondDataTableId = createTable(
            "books", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("author", firstDataTableId, LinkDeleteRule.CLEAR, "name"));

        workspaceDataTableFacade.addColumn(
            firstDataTableId, linkColumn("favorite_book", secondDataTableId, LinkDeleteRule.BLOCK, "title"),
            SOURCE_ENVIRONMENT_ID);

        EnvironmentPromotionResult environmentPromotionResult = promote(firstDataTableId);

        assertThat(environmentPromotionResult.targetId()).isEqualTo(firstDataTableId);
        assertThat(environmentPromotionResult.created()).isTrue();
        assertThat(targetColumns(firstDataTableId)).containsExactly(
            new ColumnSpec("name", ColumnType.STRING),
            linkColumn("favorite_book", secondDataTableId, LinkDeleteRule.BLOCK, "title"));
        assertThat(targetColumns(secondDataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING),
            linkColumn("author", firstDataTableId, LinkDeleteRule.CLEAR, "name"));
    }

    @Test
    void testPromoteCreatesPulledInLinkedTablesEmpty() {
        long authorDataTableId =
            createTable("writers", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long bookDataTableId = createTable(
            "novels", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("writer", authorDataTableId, LinkDeleteRule.CLEAR, "name"));

        DataTableRow authorRow = workspaceDataTableFacade.insertRow(
            authorDataTableId, Map.of("name", "ada"), SOURCE_ENVIRONMENT_ID);

        workspaceDataTableFacade.insertRow(
            bookDataTableId, Map.of("title", "notes", "writer", authorRow.id()), SOURCE_ENVIRONMENT_ID);

        promote(bookDataTableId);

        assertThat(countRows(bookDataTableId, TARGET_ENVIRONMENT_ID)).isZero();
        assertThat(countRows(authorDataTableId, TARGET_ENVIRONMENT_ID)).isZero();
        assertThat(countRows(bookDataTableId, SOURCE_ENVIRONMENT_ID)).isEqualTo(1);
        assertThat(countRows(authorDataTableId, SOURCE_ENVIRONMENT_ID)).isEqualTo(1);
    }

    @Test
    void testSyncsExistingLinkedTableMissingDisplayColumn() {
        long customerDataTableId = createTable(
            "customers", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING),
            new ColumnSpec("code", ColumnType.STRING));
        long orderDataTableId = createTable(
            "purchases", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("customer", customerDataTableId, LinkDeleteRule.BLOCK, "code"));

        createTable("customers", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));

        EnvironmentPromotionResult environmentPromotionResult = promote(orderDataTableId);

        assertThat(environmentPromotionResult.created()).isTrue();
        assertThat(targetColumns(customerDataTableId)).containsExactly(
            new ColumnSpec("name", ColumnType.STRING), new ColumnSpec("code", ColumnType.STRING));
        assertThat(targetColumns(orderDataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING),
            linkColumn("customer", customerDataTableId, LinkDeleteRule.BLOCK, "code"));
    }

    @Test
    void testPromoteDeleteRuleChangeKeepsLinkValues() {
        long customerDataTableId = createTable(
            "patrons", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long orderDataTableId = createTable(
            "loans", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("patron", customerDataTableId, LinkDeleteRule.CLEAR, "name"));

        createTable("patrons", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        createTable(
            "loans", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("patron", customerDataTableId, LinkDeleteRule.BLOCK, null));

        DataTableRow customerRow = workspaceDataTableFacade.insertRow(
            customerDataTableId, Map.of("name", "ada"), TARGET_ENVIRONMENT_ID);

        workspaceDataTableFacade.insertRow(
            orderDataTableId, Map.of("title", "loan", "patron", customerRow.id()), TARGET_ENVIRONMENT_ID);

        EnvironmentPromotionPreview environmentPromotionPreview = preview(orderDataTableId);

        assertThat(environmentPromotionPreview.schemaChanges()).containsExactly(
            new PromotionSchemaChange("loans", "patron", Kind.CHANGE_LINK, "LINK", "LINK", false));
        assertThat(environmentPromotionPreview.warnings()).noneSatisfy(
            warning -> assertThat(warning).contains("patron"));

        promote(orderDataTableId);

        assertThat(targetColumns(orderDataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING),
            linkColumn("patron", customerDataTableId, LinkDeleteRule.CLEAR, "name"));
        assertThat(selectLong("patron", orderDataTableId)).isEqualTo(customerRow.id());
    }

    @Test
    void testPromoteLinkTargetChangeClearsLinkValues() {
        long firstDataTableId =
            createTable("shelves", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long secondDataTableId = createTable("rooms", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long itemDataTableId = createTable(
            "items", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("place", secondDataTableId, LinkDeleteRule.BLOCK, null));

        createTable("shelves", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        createTable("rooms", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        createTable(
            "items", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("place", firstDataTableId, LinkDeleteRule.BLOCK, null));

        DataTableRow shelfRow = workspaceDataTableFacade.insertRow(
            firstDataTableId, Map.of("name", "top"), TARGET_ENVIRONMENT_ID);

        workspaceDataTableFacade.insertRow(
            itemDataTableId, Map.of("title", "book", "place", shelfRow.id()), TARGET_ENVIRONMENT_ID);

        EnvironmentPromotionPreview environmentPromotionPreview = preview(itemDataTableId);

        assertThat(environmentPromotionPreview.warnings()).anySatisfy(
            warning -> assertThat(warning).contains("'place'", "'items'", "cleared"));

        promote(itemDataTableId);

        assertThat(targetColumns(itemDataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING),
            linkColumn("place", secondDataTableId, LinkDeleteRule.BLOCK, null));
        assertThat(selectLong("place", itemDataTableId)).isNull();
        assertThat(selectString("title", itemDataTableId)).isEqualTo("book");
    }

    @Test
    void testPromoteSwapsScalarAndLinkColumns() {
        long customerDataTableId = createTable(
            "members", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long orderDataTableId = createTable(
            "bookings", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("member", customerDataTableId, LinkDeleteRule.BLOCK, "name"),
            new ColumnSpec("referrer", ColumnType.STRING));

        createTable("members", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        createTable(
            "bookings", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("member", ColumnType.INTEGER),
            linkColumn("referrer", customerDataTableId, LinkDeleteRule.CLEAR, null));

        EnvironmentPromotionResult environmentPromotionResult = promote(orderDataTableId);

        assertThat(environmentPromotionResult.created()).isFalse();
        assertThat(targetColumns(orderDataTableId)).containsExactlyInAnyOrder(
            new ColumnSpec("title", ColumnType.STRING),
            linkColumn("member", customerDataTableId, LinkDeleteRule.BLOCK, "name"),
            new ColumnSpec("referrer", ColumnType.STRING));
    }

    @Test
    void testPreviewListsChangesPerTablePromotedTableFirst() {
        long customerDataTableId = createTable(
            "clients", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long orderDataTableId = createTable(
            "tickets", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("client", customerDataTableId, LinkDeleteRule.BLOCK, "name"));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(orderDataTableId);

        assertThat(environmentPromotionPreview.resourceType()).isEqualTo(DataTablePromotionContributor.RESOURCE_TYPE);
        assertThat(environmentPromotionPreview.existingTargetId()).isNull();
        assertThat(environmentPromotionPreview.connections()).isEmpty();
        assertThat(environmentPromotionPreview.schemaChanges())
            .extracting(PromotionSchemaChange::tableName, PromotionSchemaChange::columnName,
                PromotionSchemaChange::kind)
            .containsExactly(
                tuple("tickets", null, Kind.CREATE_TABLE),
                tuple("tickets", "title", Kind.ADD_COLUMN),
                tuple("tickets", "client", Kind.ADD_COLUMN),
                tuple("clients", null, Kind.CREATE_TABLE),
                tuple("clients", "name", Kind.ADD_COLUMN));
        assertThat(environmentPromotionPreview.warnings()).contains(
            "The data table is created empty in Staging; rows are not promoted.",
            "Linked data table 'clients' does not exist in Staging and will be created empty.");
    }

    @Test
    void testPreviewReportsExistingTarget() {
        long dataTableId = createTable("notes", SOURCE_ENVIRONMENT_ID, new ColumnSpec("body", ColumnType.STRING));

        createTable("notes", TARGET_ENVIRONMENT_ID, new ColumnSpec("body", ColumnType.INTEGER));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        assertThat(environmentPromotionPreview.existingTargetId()).isEqualTo(dataTableId);
        assertThat(environmentPromotionPreview.existingTargetName()).isEqualTo("notes");
        assertThat(environmentPromotionPreview.schemaChanges()).containsExactly(
            new PromotionSchemaChange("notes", "body", Kind.CHANGE_COLUMN_TYPE, "INTEGER", "STRING", false));
    }

    @Test
    void testPreviewWarnsAboutLossyColumnTypeChange() {
        long dataTableId = createTable("prices", SOURCE_ENVIRONMENT_ID, new ColumnSpec("amount", ColumnType.INTEGER));

        createTable("prices", TARGET_ENVIRONMENT_ID, new ColumnSpec("amount", ColumnType.NUMBER));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        assertThat(environmentPromotionPreview.warnings()).anySatisfy(
            warning -> assertThat(warning).contains("'amount'", "'prices'", "NUMBER", "INTEGER", "truncate"));
    }

    @Test
    void testPreviewWarnsAboutUnsupportedColumnTypeChange() {
        long dataTableId = createTable("events", SOURCE_ENVIRONMENT_ID, new ColumnSpec("held", ColumnType.BOOLEAN));

        createTable("events", TARGET_ENVIRONMENT_ID, new ColumnSpec("held", ColumnType.DATE));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        assertThat(environmentPromotionPreview.warnings()).anySatisfy(
            warning -> assertThat(warning).contains("'held'", "'events'", "DATE", "BOOLEAN", "will fail"));
    }

    @Test
    void testPreviewDoesNotWarnAboutLosslessColumnTypeChange() {
        long dataTableId = createTable("memos", SOURCE_ENVIRONMENT_ID, new ColumnSpec("body", ColumnType.STRING));

        createTable("memos", TARGET_ENVIRONMENT_ID, new ColumnSpec("body", ColumnType.INTEGER));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        assertThat(environmentPromotionPreview.warnings()).noneSatisfy(
            warning -> assertThat(warning).contains("'body'"));
    }

    @Test
    void testRefusesSameSourceAndTargetEnvironment() {
        long dataTableId = createTable("labels", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));

        assertThatThrownBy(
            () -> dataTablePromotionContributor.preview(dataTableId, Environment.DEVELOPMENT, Environment.DEVELOPMENT))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey()));

        assertThatThrownBy(
            () -> dataTablePromotionContributor.promote(
                dataTableId, Environment.DEVELOPMENT, Environment.DEVELOPMENT, Map.of(), null))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey()));
    }

    @Test
    void testRefusesSourceMissingInSourceEnvironment() {
        long dataTableId = createTable("drafts", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));

        assertThatThrownBy(() -> preview(dataTableId))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SOURCE_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testRefusesConnectionMappings() {
        long dataTableId = createTable("tasks", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));

        assertThatThrownBy(
            () -> dataTablePromotionContributor.promote(
                dataTableId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(1L, 2L), null))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey()));

        assertThat(tableExists(dataTableId, TARGET_ENVIRONMENT_ID)).isFalse();
    }

    @Test
    void testPreviewChangesNothing() {
        long customerDataTableId = createTable(
            "vendors", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING),
            new ColumnSpec("code", ColumnType.STRING));
        long orderDataTableId = createTable(
            "deliveries", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.INTEGER),
            linkColumn("vendor", customerDataTableId, LinkDeleteRule.BLOCK, "code"));

        createTable("deliveries", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("legacy", ColumnType.DATE));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(orderDataTableId);

        assertThat(environmentPromotionPreview.schemaChanges()).isNotEmpty();
        assertThat(targetColumns(orderDataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("legacy", ColumnType.DATE));
        assertThat(tableExists(customerDataTableId, TARGET_ENVIRONMENT_ID)).isFalse();
    }

    @Test
    void testPreviewAndPromoteRequireDataTableEditPermission() {
        assertPreAuthorize(
            DataTablePromotionContributor.class, "preview",
            new Class<?>[] {
                long.class, Environment.class, Environment.class
            },
            PERMISSION_EXPRESSION);
        assertPreAuthorize(
            DataTablePromotionContributor.class, "promote",
            new Class<?>[] {
                long.class, Environment.class, Environment.class, Map.class, String.class
            },
            PERMISSION_EXPRESSION);
    }

    @Test
    void testDropColumnChangesHash() {
        long dataTableId = createTable("ledgers", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING));

        createTable("ledgers", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING));

        EnvironmentPromotionPreview withoutDrop = preview(dataTableId);

        assertThat(withoutDrop.destructiveChangesHash())
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of()));

        workspaceDataTableFacade.addColumn(
            dataTableId, new ColumnSpec("legacy", ColumnType.DATE), TARGET_ENVIRONMENT_ID);

        EnvironmentPromotionPreview withDrop = preview(dataTableId);

        PromotionChange droppedColumn = new PromotionChange(
            PromotionChange.Kind.DELETE, "COLUMN", "legacy", "ledgers", "legacy", true);

        assertThat(withDrop.destructiveChangesHash())
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of(droppedColumn)))
            .isNotEqualTo(withoutDrop.destructiveChangesHash());
        assertThat(withDrop.changes()).containsExactly(
            new PromotionChange(PromotionChange.Kind.UPDATE, "DATA_TABLE", "ledgers", null, "ledgers", false));
    }

    @Test
    void testLinkRetargetIsDestructive() {
        long firstDataTableId =
            createTable("racks", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long secondDataTableId = createTable("bins", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long itemDataTableId = createTable(
            "parts", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("place", secondDataTableId, LinkDeleteRule.BLOCK, null));

        createTable("racks", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        createTable("bins", TARGET_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        createTable(
            "parts", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("place", firstDataTableId, LinkDeleteRule.BLOCK, null));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(itemDataTableId);

        PromotionChange retargetedColumn = new PromotionChange(
            PromotionChange.Kind.UPDATE, "COLUMN", "place", "parts", "place", true);

        assertThat(environmentPromotionPreview.destructiveChangesHash())
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of(retargetedColumn)));
        assertThat(environmentPromotionPreview.schemaChanges()).containsExactly(
            new PromotionSchemaChange("parts", "place", Kind.CHANGE_LINK, "LINK", "LINK", true));
    }

    @Test
    void testLosslessTypeChangeIsNotDestructive() {
        long dataTableId = createTable("quotes", SOURCE_ENVIRONMENT_ID, new ColumnSpec("body", ColumnType.STRING));

        createTable("quotes", TARGET_ENVIRONMENT_ID, new ColumnSpec("body", ColumnType.INTEGER));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        assertThat(environmentPromotionPreview.destructiveChangesHash())
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of()));
        assertThat(environmentPromotionPreview.schemaChanges()).containsExactly(
            new PromotionSchemaChange("quotes", "body", Kind.CHANGE_COLUMN_TYPE, "INTEGER", "STRING", false));
    }

    @Test
    void testLossyTypeChangeIsDestructive() {
        long dataTableId = createTable("rates", SOURCE_ENVIRONMENT_ID, new ColumnSpec("amount", ColumnType.INTEGER));

        createTable("rates", TARGET_ENVIRONMENT_ID, new ColumnSpec("amount", ColumnType.NUMBER));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        PromotionChange truncatedColumn = new PromotionChange(
            PromotionChange.Kind.UPDATE, "COLUMN", "amount", "rates", "amount", true);

        assertThat(environmentPromotionPreview.destructiveChangesHash())
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of(truncatedColumn)));
        assertThat(environmentPromotionPreview.schemaChanges()).containsExactly(
            new PromotionSchemaChange("rates", "amount", Kind.CHANGE_COLUMN_TYPE, "NUMBER", "INTEGER", true));
    }

    @Test
    void testLinkedTableCreateIsListed() {
        long customerDataTableId = createTable(
            "buyers", SOURCE_ENVIRONMENT_ID, new ColumnSpec("name", ColumnType.STRING));
        long orderDataTableId = createTable(
            "baskets", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            linkColumn("buyer", customerDataTableId, LinkDeleteRule.BLOCK, "name"));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(orderDataTableId);

        assertThat(environmentPromotionPreview.changes()).containsExactly(
            new PromotionChange(PromotionChange.Kind.CREATE, "DATA_TABLE", "baskets", null, "baskets", false),
            new PromotionChange(PromotionChange.Kind.CREATE, "DATA_TABLE", "buyers", null, "buyers", false));
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        long dataTableId = createTable("stock", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.INTEGER));

        createTable("stock", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        String previewedHash = environmentPromotionPreview.destructiveChangesHash();

        workspaceDataTableFacade.addColumn(
            dataTableId, new ColumnSpec("legacy", ColumnType.DATE), TARGET_ENVIRONMENT_ID);

        assertThatThrownBy(
            () -> dataTablePromotionContributor.promote(
                dataTableId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), previewedHash))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                        .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey()));

        assertThat(targetColumns(dataTableId)).containsExactly(
            new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("legacy", ColumnType.DATE));
    }

    @Test
    void testPromoteWithCurrentHashSucceeds() {
        long dataTableId = createTable("slots", SOURCE_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING));

        createTable(
            "slots", TARGET_ENVIRONMENT_ID, new ColumnSpec("title", ColumnType.STRING),
            new ColumnSpec("legacy", ColumnType.DATE));

        EnvironmentPromotionPreview environmentPromotionPreview = preview(dataTableId);

        String previewedHash = environmentPromotionPreview.destructiveChangesHash();

        dataTablePromotionContributor.promote(
            dataTableId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), previewedHash);

        assertThat(targetColumns(dataTableId)).containsExactly(new ColumnSpec("title", ColumnType.STRING));
    }

    private int countRows(long dataTableId, long environmentId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + physicalName(dataTableId, environmentId), Integer.class);

        return Objects.requireNonNull(count);
    }

    private long createTable(String name, long environmentId, ColumnSpec... columnSpecs) {
        return workspaceDataTableFacade.createTable(name, name, List.of(columnSpecs), WORKSPACE_ID, environmentId);
    }

    private void deleteTestTables() {
        List<Long> dataTableIds = jdbcTemplate.queryForList(
            "SELECT id FROM data_table WHERE workspace_id = ?", Long.class, WORKSPACE_ID);

        for (long dataTableId : dataTableIds) {
            for (Environment environment : Environment.values()) {
                jdbcTemplate.execute(
                    "DROP TABLE IF EXISTS " + physicalName(dataTableId, environment.ordinal()) + " CASCADE");
            }

            jdbcTemplate.update("DELETE FROM data_table_link_column WHERE data_table_id = ?", dataTableId);
            jdbcTemplate.update("DELETE FROM data_table_link_column WHERE target_data_table_id = ?", dataTableId);
        }

        jdbcTemplate.update("DELETE FROM data_table WHERE workspace_id = ?", WORKSPACE_ID);
    }

    private static ColumnSpec linkColumn(
        String name, long targetDataTableId, LinkDeleteRule onDelete, String displayColumnName) {

        return new ColumnSpec(name, ColumnType.LINK, new LinkSpec(targetDataTableId, onDelete, displayColumnName));
    }

    private static String physicalName(long dataTableId, long environmentId) {
        return "dt_" + environmentId + "_" + dataTableId;
    }

    private EnvironmentPromotionPreview preview(long dataTableId) {
        return dataTablePromotionContributor.preview(dataTableId, Environment.DEVELOPMENT, Environment.STAGING);
    }

    private EnvironmentPromotionResult promote(long dataTableId) {
        return dataTablePromotionContributor.promote(
            dataTableId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);
    }

    private boolean tableExists(long dataTableId, long environmentId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND " +
                "table_name = ?",
            Integer.class, physicalName(dataTableId, environmentId));

        return Objects.requireNonNull(count) > 0;
    }

    private @Nullable Long selectLong(String columnName, long dataTableId) {
        return jdbcTemplate.queryForObject(
            "SELECT " + columnName + " FROM " + physicalName(dataTableId, TARGET_ENVIRONMENT_ID), Long.class);
    }

    private @Nullable String selectString(String columnName, long dataTableId) {
        return jdbcTemplate.queryForObject(
            "SELECT " + columnName + " FROM " + physicalName(dataTableId, TARGET_ENVIRONMENT_ID), String.class);
    }

    private List<ColumnSpec> targetColumns(long dataTableId) {
        DataTableInfo dataTableInfo = workspaceDataTableFacade.getTable(dataTableId, TARGET_ENVIRONMENT_ID);

        return dataTableInfo.columns();
    }
}
