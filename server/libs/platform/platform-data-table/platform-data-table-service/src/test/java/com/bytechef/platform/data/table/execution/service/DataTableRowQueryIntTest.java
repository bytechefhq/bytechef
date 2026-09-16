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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Filters run against a real database because that is the only place a mis-ordered bind parameter shows up: the SQL
 * concatenates as filters then pagination, and setting them in any other order returns the wrong page rather than
 * failing.
 *
 * <p>
 * The fixture table is built with raw DDL rather than through {@code DataTableService.createTable}, which additionally
 * requires a {@code data_table} registry row.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowQueryIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final String BASE_NAME = "messages";

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_messages\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_messages\" (\"id\" BIGSERIAL PRIMARY KEY, \"title\" TEXT, " +
                "\"score\" BIGINT)");
    }

    @Test
    void testAFilterNarrowsTheResult() {
        insert("alpha", 1);
        insert("beta", 2);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
                List.of(new RowFilter("title", RowFilter.Operator.EQ, "alpha")), List.of());

        assertEquals(1, dataTableRows.size());

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals("alpha", values.get("title"));
    }

    @Test
    void testFiltersAreAndedTogether() {
        insert("alpha", 1);
        insert("alpha", 9);
        insert("beta", 9);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
                List.of(
                    new RowFilter("title", RowFilter.Operator.EQ, "alpha"),
                    new RowFilter("score", RowFilter.Operator.GT, "5")),
                List.of());

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testAStringValueIsCoercedToTheColumnType() {
        insert("alpha", 42);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
                List.of(new RowFilter("score", RowFilter.Operator.EQ, "42")), List.of());

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testContainsMatchesASubstring() {
        insert("hello world", 1);
        insert("goodbye", 2);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
                List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "lo wo")), List.of());

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testAWildcardInTheValueIsMatchedLiterally() {
        insert("100% sure", 1);
        insert("100 sure", 2);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
                List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "100%")), List.of());

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testInMatchesAnyOfItsValues() {
        insert("alpha", 1);
        insert("beta", 2);
        insert("gamma", 3);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
            List.of(new RowFilter("title", RowFilter.Operator.IN, List.of("alpha", "gamma"))), List.of());

        assertEquals(2, dataTableRows.size());
    }

    @Test
    void testBetweenIsInclusive() {
        insert("a", 1);
        insert("b", 2);
        insert("c", 3);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
            List.of(new RowFilter("score", RowFilter.Operator.BETWEEN, List.of("1", "2"))), List.of());

        assertEquals(2, dataTableRows.size());
    }

    @Test
    void testTheParameterOrderSurvivesFilteringAndPagination() {
        for (int index = 0; index < 5; index++) {
            insert("row", index);
        }

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 2, 1,
                List.of(new RowFilter("score", RowFilter.Operator.GTE, "1")), List.of());

        assertEquals(2, dataTableRows.size());

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals(2L, values.get("score"));
    }

    @Test
    void testSortingDescendingReturnsTheNewestFirst() {
        insert("a", 1);
        insert("b", 2);
        insert("c", 3);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 2, 0,
                List.of(), List.of(new RowSort("score", RowSort.Direction.DESC)));

        assertEquals(2, dataTableRows.size());

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals("c", values.get("title"));
    }

    @Test
    void testSortingOnIdDescendingNeedsNoTimestampColumn() {
        insert("first", 1);
        insert("second", 1);
        insert("third", 1);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 1, 0,
                List.of(), List.of(new RowSort("id", RowSort.Direction.DESC)));

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals("third", values.get("title"));
    }

    @Test
    void testATiedSortStillPaginatesWithoutRepeatingOrLosingARow() {
        for (int index = 0; index < 6; index++) {
            insert("row" + index, 1);
        }

        List<RowSort> rowSorts = List.of(new RowSort("score", RowSort.Direction.DESC));

        List<Object> seen = new ArrayList<>();

        for (int offset = 0; offset < 6; offset += 2) {
            List<DataTableRow> page =
                dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 2,
                    offset, List.of(), rowSorts);

            for (DataTableRow dataTableRow : page) {
                Map<String, Object> values = dataTableRow.values();

                seen.add(values.get("title"));
            }
        }

        assertEquals(6, seen.size());
        assertEquals(6, Set.copyOf(seen)
            .size());
    }

    @Test
    void testSortingComposesWithFiltering() {
        insert("keep", 1);
        insert("keep", 3);
        insert("drop", 9);

        List<DataTableRow> dataTableRows =
            dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 10, 0,
                List.of(new RowFilter("title", RowFilter.Operator.EQ, "keep")),
                List.of(new RowSort("score", RowSort.Direction.DESC)));

        assertEquals(2, dataTableRows.size());

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals(3L, values.get("score"));
    }

    @Test
    void testFilteringOnAnUnknownColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.listRows(dataTableRef(BASE_NAME, ENVIRONMENT_ID), 100, 0,
                List.of(new RowFilter("nope", RowFilter.Operator.EQ, "x")), List.of()));
    }

    private void insert(String title, int score) {
        dataTableRowService.insertRow(dataTableRef(BASE_NAME, ENVIRONMENT_ID),
            Map.of("title", title, "score", score));
    }

    /**
     * These tables are all created shared, so naming the shared physical form here states a fact about the fixture
     * rather than skipping resolution.
     */
    private static DataTableRef dataTableRef(String baseName, long environmentId) {
        return new DataTableRef(baseName, environmentId);
    }
}
