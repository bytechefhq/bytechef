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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowOwnerFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.owner.Owner;
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
 * concatenates as owner, filters, pagination, and setting them in any other order returns the wrong page rather than
 * failing.
 *
 * <p>
 * The table is built with raw DDL rather than through {@code DataTableService.createTable}, which additionally requires
 * a {@code data_table} registry row, matching {@link DataTableRowOwnerScopingIntTest}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowQueryIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final String BASE_NAME = "messages";

    private static final RowOwnerFilter ACCOUNT_A = RowOwnerFilter.ownedBy(Owner.connectedUser(1L));
    private static final RowOwnerFilter ACCOUNT_B = RowOwnerFilter.ownedBy(Owner.connectedUser(2L));

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_messages\"");
        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_messages\" (\"id\" BIGSERIAL PRIMARY KEY, \"owner_id\" BIGINT, " +
                "\"owner_type\" INT, \"title\" TEXT, \"score\" BIGINT)");
    }

    @Test
    void testAFilterNarrowsTheResult() {
        insert("alpha", 1);
        insert("beta", 2);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "alpha")));

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

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(
                new RowFilter("title", RowFilter.Operator.EQ, "alpha"),
                new RowFilter("score", RowFilter.Operator.GT, "5")));

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testAStringValueIsCoercedToTheColumnType() {
        insert("alpha", 42);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("score", RowFilter.Operator.EQ, "42")));

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testContainsMatchesASubstring() {
        insert("hello world", 1);
        insert("goodbye", 2);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "lo wo")));

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testAWildcardInTheValueIsMatchedLiterally() {
        insert("100% sure", 1);
        insert("100 sure", 2);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "100%")));

        assertEquals(1, dataTableRows.size());
    }

    @Test
    void testInMatchesAnyOfItsValues() {
        insert("alpha", 1);
        insert("beta", 2);
        insert("gamma", 3);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("title", RowFilter.Operator.IN, List.of("alpha", "gamma"))));

        assertEquals(2, dataTableRows.size());
    }

    @Test
    void testBetweenIsInclusive() {
        insert("a", 1);
        insert("b", 2);
        insert("c", 3);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("score", RowFilter.Operator.BETWEEN, List.of("1", "2"))));

        assertEquals(2, dataTableRows.size());
    }

    @Test
    void testAFilterCannotReachAnotherAccountsRow() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "secret", "score", 1), ENVIRONMENT_ID, ACCOUNT_B);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, ACCOUNT_A,
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "secret")));

        assertTrue(dataTableRows.isEmpty());
    }

    @Test
    void testTheParameterOrderSurvivesAnOwnerFilterAndPagination() {
        for (int index = 0; index < 5; index++) {
            dataTableRowService.insertRow(
                BASE_NAME, Map.of("title", "row", "score", index), ENVIRONMENT_ID, ACCOUNT_A);
        }

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 2, 1, ENVIRONMENT_ID, ACCOUNT_A,
            List.of(new RowFilter("score", RowFilter.Operator.GTE, "1")));

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

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 2, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(), List.of(),
            List.of(new RowSort("score", RowSort.Direction.DESC)));

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

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 1, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(), List.of(),
            List.of(new RowSort("id", RowSort.Direction.DESC)));

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
            List<DataTableRow> page = dataTableRowService.listRows(
                BASE_NAME, 2, offset, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(), List.of(), rowSorts);

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
    void testSortingComposesWithFilteringAndOwnership() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "keep", "score", 1), ENVIRONMENT_ID, ACCOUNT_A);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "keep", "score", 3), ENVIRONMENT_ID, ACCOUNT_A);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "keep", "score", 9), ENVIRONMENT_ID, ACCOUNT_B);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            BASE_NAME, 10, 0, ENVIRONMENT_ID, ACCOUNT_A,
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "keep")),
            List.of(new RowSort("score", RowSort.Direction.DESC)));

        assertEquals(2, dataTableRows.size());

        DataTableRow dataTableRow = dataTableRows.getFirst();

        Map<String, Object> values = dataTableRow.values();

        assertEquals(3L, values.get("score"));
    }

    @Test
    void testSortingOnAnOwnerColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.listRows(
                BASE_NAME, 100, 0, ENVIRONMENT_ID, ACCOUNT_A, List.of(),
                List.of(new RowSort("owner_id", RowSort.Direction.ASC))));
    }

    @Test
    void testFilteringOnAnOwnerColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.listRows(
                BASE_NAME, 100, 0, ENVIRONMENT_ID, ACCOUNT_A,
                List.of(new RowFilter("owner_id", RowFilter.Operator.EQ, "2"))));
    }

    @Test
    void testFilteringOnAnUnknownColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.listRows(
                BASE_NAME, 100, 0, ENVIRONMENT_ID, ACCOUNT_A,
                List.of(new RowFilter("nope", RowFilter.Operator.EQ, "x"))));
    }

    private void insert(String title, int score) {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", title, "score", score), ENVIRONMENT_ID);
    }
}
