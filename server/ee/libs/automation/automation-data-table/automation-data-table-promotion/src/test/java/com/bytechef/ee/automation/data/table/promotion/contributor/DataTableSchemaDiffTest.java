/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.promotion.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange.Kind;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataTableSchemaDiffTest {

    private static final String TABLE_NAME = "orders";

    @Test
    void testMissingTargetCreatesTableAndAddsEveryColumn() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.NUMBER)));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, null, Kind.CREATE_TABLE, null, null, false),
            new PromotionSchemaChange(TABLE_NAME, "title", Kind.ADD_COLUMN, null, "STRING", false),
            new PromotionSchemaChange(TABLE_NAME, "amount", Kind.ADD_COLUMN, null, "NUMBER", false));
    }

    @Test
    void testExtraTargetColumnIsDropped() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("legacy", ColumnType.DATE)),
            List.of(new ColumnSpec("title", ColumnType.STRING)));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "legacy", Kind.DROP_COLUMN, "DATE", null, true));
    }

    @Test
    void testOnlyDroppedColumnsAreDestructive() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(new ColumnSpec("legacy", ColumnType.DATE)),
            List.of(new ColumnSpec("title", ColumnType.STRING)));

        assertThat(schemaChanges)
            .extracting(PromotionSchemaChange::kind, PromotionSchemaChange::destructive)
            .containsExactlyInAnyOrder(tuple(Kind.DROP_COLUMN, true), tuple(Kind.ADD_COLUMN, false));
    }

    @Test
    void testDifferentTypeChangesColumnType() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(new ColumnSpec("count", ColumnType.STRING)),
            List.of(new ColumnSpec("count", ColumnType.INTEGER)));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "count", Kind.CHANGE_COLUMN_TYPE, "STRING", "INTEGER", true));
    }

    @Test
    void testColumnsAreMatchedByNameIgnoringCase() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(new ColumnSpec("Title", ColumnType.STRING)),
            List.of(new ColumnSpec("title", ColumnType.STRING)));

        assertThat(schemaChanges).isEmpty();
    }

    @Test
    void testSourceColumnsDifferingOnlyByCaseAreRefused() {
        List<ColumnSpec> sourceColumns = List.of(
            new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("Title", ColumnType.INTEGER));

        assertThatThrownBy(() -> DataTableSchemaDiff.diff(TABLE_NAME, null, sourceColumns))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("'title'")
            .hasMessageContaining("'Title'")
            .satisfies(throwable -> assertThat(((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED.getErrorKey()));
    }

    @Test
    void testRenamedColumnIsDroppedAndAdded() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(new ColumnSpec("name", ColumnType.STRING)),
            List.of(new ColumnSpec("full_name", ColumnType.STRING)));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "name", Kind.DROP_COLUMN, "STRING", null, true),
            new PromotionSchemaChange(TABLE_NAME, "full_name", Kind.ADD_COLUMN, null, "STRING", false));
    }

    @Test
    void testLinkWithDifferentTargetTableChangesLink() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(linkColumn("customer", 1L, LinkDeleteRule.BLOCK, "name")),
            List.of(linkColumn("customer", 2L, LinkDeleteRule.BLOCK, "name")));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "customer", Kind.CHANGE_LINK, "LINK", "LINK", true));
    }

    @Test
    void testLinkWithDifferentDeleteRuleOrDisplayColumnChangesLink() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME,
            List.of(
                linkColumn("customer", 1L, LinkDeleteRule.BLOCK, "name"),
                linkColumn("supplier", 1L, LinkDeleteRule.BLOCK, "name")),
            List.of(
                linkColumn("customer", 1L, LinkDeleteRule.CLEAR, "name"),
                linkColumn("supplier", 1L, LinkDeleteRule.BLOCK, "code")));

        assertThat(schemaChanges)
            .extracting(PromotionSchemaChange::columnName, PromotionSchemaChange::kind)
            .containsExactly(
                tuple("customer", Kind.CHANGE_LINK),
                tuple("supplier", Kind.CHANGE_LINK));
    }

    @Test
    void testSameLinkIsUnchanged() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(linkColumn("customer", 1L, LinkDeleteRule.BLOCK, "Name")),
            List.of(linkColumn("customer", 1L, LinkDeleteRule.BLOCK, "name")));

        assertThat(schemaChanges).isEmpty();
    }

    @Test
    void testLinkReplacingScalarIsDroppedAndAdded() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(new ColumnSpec("customer", ColumnType.INTEGER)),
            List.of(linkColumn("customer", 1L, LinkDeleteRule.BLOCK, null)));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "customer", Kind.DROP_COLUMN, "INTEGER", null, true),
            new PromotionSchemaChange(TABLE_NAME, "customer", Kind.ADD_COLUMN, null, "LINK", false));
    }

    @Test
    void testScalarReplacingLinkIsDroppedAndAdded() {
        List<PromotionSchemaChange> schemaChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, List.of(linkColumn("customer", 1L, LinkDeleteRule.BLOCK, null)),
            List.of(new ColumnSpec("customer", ColumnType.STRING)));

        assertThat(schemaChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "customer", Kind.DROP_COLUMN, "LINK", null, true),
            new PromotionSchemaChange(TABLE_NAME, "customer", Kind.ADD_COLUMN, null, "STRING", false));
    }

    @Test
    void testReservedColumnsNeverAppear() {
        List<PromotionSchemaChange> createChanges = DataTableSchemaDiff.diff(
            TABLE_NAME, null,
            List.of(
                new ColumnSpec("id", ColumnType.INTEGER), new ColumnSpec("external_id", ColumnType.STRING),
                new ColumnSpec("title", ColumnType.STRING)));

        List<PromotionSchemaChange> updateChanges = DataTableSchemaDiff.diff(
            TABLE_NAME,
            List.of(new ColumnSpec("id", ColumnType.INTEGER), new ColumnSpec("EXTERNAL_ID", ColumnType.STRING)),
            List.of(new ColumnSpec("id", ColumnType.STRING), new ColumnSpec("title", ColumnType.STRING)));

        assertThat(createChanges)
            .extracting(PromotionSchemaChange::columnName)
            .containsExactly(null, "title");
        assertThat(updateChanges).containsExactly(
            new PromotionSchemaChange(TABLE_NAME, "title", Kind.ADD_COLUMN, null, "STRING", false));
    }

    private static ColumnSpec linkColumn(
        String name, long targetDataTableId, LinkDeleteRule onDelete, String displayColumnName) {

        return new ColumnSpec(
            name, ColumnType.LINK, new LinkSpec(targetDataTableId, onDelete, displayColumnName));
    }
}
