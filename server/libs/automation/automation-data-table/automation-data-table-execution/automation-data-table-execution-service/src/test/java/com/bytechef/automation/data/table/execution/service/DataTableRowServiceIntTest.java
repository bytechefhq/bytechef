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

package com.bytechef.automation.data.table.execution.service;

import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.data.table.execution.config.DataTableRowIntTestConfiguration;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableRowIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class DataTableRowServiceIntTest {

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        cleanupTables();
        createTestTables();
    }

    @AfterEach
    public void afterEach() {
        cleanupTables();
    }

    private void cleanupTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_orders\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_products\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_all_types\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_1_orders\"");
    }

    private void createTestTables() {
        jdbcTemplate.execute("""
            CREATE TABLE "dt_0_orders" (
                "id" BIGSERIAL PRIMARY KEY,
                "name" VARCHAR(256),
                "amount" NUMERIC
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE "dt_0_products" (
                "id" BIGSERIAL PRIMARY KEY,
                "name" VARCHAR(256),
                "price" NUMERIC,
                "quantity" INTEGER
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE "dt_0_all_types" (
                "id" BIGSERIAL PRIMARY KEY,
                "string_col" VARCHAR(256),
                "number_col" NUMERIC,
                "integer_col" BIGINT,
                "date_col" DATE,
                "datetime_col" TIMESTAMP,
                "boolean_col" BOOLEAN
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE "dt_1_orders" (
                "id" BIGSERIAL PRIMARY KEY,
                "name" VARCHAR(256)
            )
            """);
    }

    @Test
    public void testInsertRow() {
        DataTableRow row = dataTableRowService.insertRow(
            "orders",
            Map.of("name", "Order 1", "amount", new BigDecimal("100.50")),
            DEVELOPMENT.ordinal());

        assertThat(row.id()).isPositive();
        assertThat(row.values()).containsEntry("name", "Order 1");
    }

    @Test
    public void testInsertRowWithEmptyValues() {
        DataTableRow row = dataTableRowService.insertRow(
            "orders",
            Map.of(),
            DEVELOPMENT.ordinal());

        assertThat(row.id()).isPositive();
    }

    @Test
    public void testListRows() {
        dataTableRowService.insertRow("orders", Map.of("name", "Order 1"), DEVELOPMENT.ordinal());
        dataTableRowService.insertRow("orders", Map.of("name", "Order 2"), DEVELOPMENT.ordinal());
        dataTableRowService.insertRow("orders", Map.of("name", "Order 3"), DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).hasSize(3);
    }

    @Test
    public void testListRowsWithPagination() {
        for (int i = 1; i <= 10; i++) {
            dataTableRowService.insertRow("orders", Map.of("name", "Order " + i), DEVELOPMENT.ordinal());
        }

        List<DataTableRow> page1 = dataTableRowService.listRows("orders", 3, 0, DEVELOPMENT.ordinal());
        List<DataTableRow> page2 = dataTableRowService.listRows("orders", 3, 3, DEVELOPMENT.ordinal());
        List<DataTableRow> page3 = dataTableRowService.listRows("orders", 3, 6, DEVELOPMENT.ordinal());
        List<DataTableRow> page4 = dataTableRowService.listRows("orders", 3, 9, DEVELOPMENT.ordinal());

        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(3);
        assertThat(page3).hasSize(3);
        assertThat(page4).hasSize(1);
    }

    @Test
    public void testListRowsReturnsEmpty() {
        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).isEmpty();
    }

    @Test
    public void testUpdateRow() {
        DataTableRow inserted = dataTableRowService.insertRow(
            "orders",
            Map.of("name", "Original"),
            DEVELOPMENT.ordinal());

        DataTableRow updated = dataTableRowService.updateRow(
            "orders",
            inserted.id(),
            Map.of("name", "Updated"),
            DEVELOPMENT.ordinal());

        assertThat(updated.values()
            .get("name")).isEqualTo("Updated");
    }

    @Test
    public void testUpdateRowWithPartialValues() {
        DataTableRow inserted = dataTableRowService.insertRow(
            "products",
            Map.of("name", "Product 1", "price", new BigDecimal("10.00"), "quantity", 100),
            DEVELOPMENT.ordinal());

        DataTableRow updated = dataTableRowService.updateRow(
            "products",
            inserted.id(),
            Map.of("price", new BigDecimal("15.00")),
            DEVELOPMENT.ordinal());

        assertThat(updated.values()
            .get("name")).isEqualTo("Product 1");
        assertThat(updated.values()
            .get("quantity")).isEqualTo(100);
    }

    @Test
    public void testUpdateRowThrowsExceptionForNonExistent() {
        assertThatThrownBy(() -> dataTableRowService.updateRow(
            "orders",
            99999L,
            Map.of("name", "Updated"),
            DEVELOPMENT.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Row not found");
    }

    @Test
    public void testDeleteRow() {
        DataTableRow inserted = dataTableRowService.insertRow(
            "orders",
            Map.of("name", "To Delete"),
            DEVELOPMENT.ordinal());

        boolean deleted = dataTableRowService.deleteRow("orders", inserted.id(), DEVELOPMENT.ordinal());

        assertThat(deleted).isTrue();

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).isEmpty();
    }

    @Test
    public void testDeleteRowReturnsFalseForNonExistent() {
        boolean deleted = dataTableRowService.deleteRow("orders", 99999L, DEVELOPMENT.ordinal());

        assertThat(deleted).isFalse();
    }

    @Test
    public void testExportCsv() {
        dataTableRowService.insertRow("orders", Map.of("name", "Order 1", "amount", 100), DEVELOPMENT.ordinal());
        dataTableRowService.insertRow("orders", Map.of("name", "Order 2", "amount", 200), DEVELOPMENT.ordinal());

        String csv = dataTableRowService.exportCsv("orders", DEVELOPMENT.ordinal());

        assertThat(csv).contains("name");
        assertThat(csv).contains("amount");
        assertThat(csv).contains("Order 1");
        assertThat(csv).contains("Order 2");
    }

    @Test
    public void testExportCsvEmptyTable() {
        String csv = dataTableRowService.exportCsv("orders", DEVELOPMENT.ordinal());

        assertThat(csv).contains("name");
        assertThat(csv).contains("amount");
    }

    @Test
    public void testImportCsv() {
        String csv = """
            name,amount
            Imported 1,100
            Imported 2,200
            """;

        dataTableRowService.importCsv("orders", csv, DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)
            .values()
            .get("name")).isEqualTo("Imported 1");
        assertThat(rows.get(1)
            .values()
            .get("name")).isEqualTo("Imported 2");
    }

    @Test
    public void testImportCsvWithNullValue() {
        dataTableRowService.importCsv("orders", null, DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).isEmpty();
    }

    @Test
    public void testImportCsvSkipsEmptyRows() {
        String csv = """
            name,amount
            Row 1,100

            Row 2,200
            """;

        dataTableRowService.importCsv("orders", csv, DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).hasSize(2);
    }

    @Test
    public void testImportCsvSkipsIdColumn() {
        String csv = """
            id,name,amount
            999,Row 1,100
            """;

        dataTableRowService.importCsv("orders", csv, DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()
            .id()).isNotEqualTo(999L);
    }

    @Test
    public void testImportCsvIgnoresUnknownColumns() {
        String csv = """
            name,amount,unknown_col
            Row 1,100,ignored
            """;

        dataTableRowService.importCsv("orders", csv, DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()
            .values()).doesNotContainKey("unknown_col");
    }

    @Test
    public void testRowOperationsAreIsolatedByEnvironment() {
        dataTableRowService.insertRow("orders", Map.of("name", "Dev Order"), DEVELOPMENT.ordinal());

        jdbcTemplate.execute("INSERT INTO \"dt_1_orders\" (\"name\") VALUES ('Staging Order')");

        List<DataTableRow> devRows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());
        List<DataTableRow> stagingRows = dataTableRowService.listRows("orders", 10, 0, 1);

        assertThat(devRows).hasSize(1);
        assertThat(devRows.getFirst()
            .values()
            .get("name")).isEqualTo("Dev Order");

        assertThat(stagingRows).hasSize(1);
        assertThat(stagingRows.getFirst()
            .values()
            .get("name")).isEqualTo("Staging Order");
    }

    @Test
    public void testInvalidBaseNameThrowsException() {
        assertThatThrownBy(() -> dataTableRowService.insertRow(
            "dt_invalid",
            Map.of("name", "test"),
            DEVELOPMENT.ordinal()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testEmptyBaseNameThrowsException() {
        assertThatThrownBy(() -> dataTableRowService.insertRow(
            "",
            Map.of("name", "test"),
            DEVELOPMENT.ordinal()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCaseInsensitiveColumnValues() {
        dataTableRowService.insertRow(
            "orders",
            Map.of("NAME", "Test", "Amount", 100),
            DEVELOPMENT.ordinal());

        List<DataTableRow> rows = dataTableRowService.listRows("orders", 10, 0, DEVELOPMENT.ordinal());

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()
            .values()
            .get("name")).isEqualTo("Test");
    }
}
