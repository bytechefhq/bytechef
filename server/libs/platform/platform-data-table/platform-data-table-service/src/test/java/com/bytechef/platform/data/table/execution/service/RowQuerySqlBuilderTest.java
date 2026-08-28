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

import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RowQuerySqlBuilderTest {

    private static final Map<String, ColumnType> COLUMN_TYPES = Map.of(
        "title", ColumnType.STRING,
        "score", ColumnType.INTEGER,
        "created", ColumnType.DATE_TIME);

    @Test
    void testNoFiltersProduceNoSql() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(List.of(), COLUMN_TYPES);

        assertEquals("", fragment.sql());
        assertTrue(fragment.bindings()
            .isEmpty());
    }

    @Test
    void testEqualsBindsOneParameter() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "hello")), COLUMN_TYPES);

        assertEquals(" AND \"title\" = ?", fragment.sql());
        assertEquals(1, fragment.bindings()
            .size());
    }

    @Test
    void testEveryComparisonOperatorRendersItsSymbol() {
        assertEquals(" AND \"score\" <> ?", sqlFor(RowFilter.Operator.NEQ));
        assertEquals(" AND \"score\" > ?", sqlFor(RowFilter.Operator.GT));
        assertEquals(" AND \"score\" >= ?", sqlFor(RowFilter.Operator.GTE));
        assertEquals(" AND \"score\" < ?", sqlFor(RowFilter.Operator.LT));
        assertEquals(" AND \"score\" <= ?", sqlFor(RowFilter.Operator.LTE));
    }

    @Test
    void testValuesAreCoercedToTheColumnType() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("score", RowFilter.Operator.EQ, "42")), COLUMN_TYPES);

        assertEquals(42L, fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testANullValueBecomesAnIsNullTestWithNoParameter() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("title", RowFilter.Operator.EQ, null)), COLUMN_TYPES);

        assertEquals(" AND \"title\" IS NULL", fragment.sql());
        assertTrue(fragment.bindings()
            .isEmpty());
    }

    @Test
    void testANullValueWithNotEqualsBecomesIsNotNull() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("title", RowFilter.Operator.NEQ, null)), COLUMN_TYPES);

        assertEquals(" AND \"title\" IS NOT NULL", fragment.sql());
    }

    @Test
    void testInRendersOnePlaceholderPerValue() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("score", RowFilter.Operator.IN, List.of("1", "2", "3"))), COLUMN_TYPES);

        assertEquals(" AND \"score\" IN (?, ?, ?)", fragment.sql());
        assertEquals(3, fragment.bindings()
            .size());
    }

    @Test
    void testBetweenRendersTwoPlaceholders() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("score", RowFilter.Operator.BETWEEN, List.of("1", "9"))), COLUMN_TYPES);

        assertEquals(" AND \"score\" BETWEEN ? AND ?", fragment.sql());
        assertEquals(2, fragment.bindings()
            .size());
    }

    @Test
    void testContainsWrapsTheValueInWildcards() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "ell")), COLUMN_TYPES);

        assertEquals(" AND \"title\" LIKE ? ESCAPE '\\'", fragment.sql());
        assertEquals("%ell%", fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testStartsWithAnchorsTheWildcardAtTheEnd() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("title", RowFilter.Operator.STARTS_WITH, "he")), COLUMN_TYPES);

        assertEquals("he%", fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testLikeMetacharactersInTheValueAreEscaped() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "100%_of\\it")), COLUMN_TYPES);

        assertEquals("%100\\%\\_of\\\\it%", fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testFiltersAreAndedTogether() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(
                new RowFilter("title", RowFilter.Operator.EQ, "a"),
                new RowFilter("score", RowFilter.Operator.GT, "1")),
            COLUMN_TYPES);

        assertEquals(" AND \"title\" = ? AND \"score\" > ?", fragment.sql());
    }

    @Test
    void testAnUnknownColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("nope", RowFilter.Operator.EQ, "x")), COLUMN_TYPES));
    }

    @Test
    void testAnOwnerColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("owner_id", RowFilter.Operator.EQ, 1L)), COLUMN_TYPES));

        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("owner_type", RowFilter.Operator.EQ, 1L)), COLUMN_TYPES));
    }

    @Test
    void testIdIsFilterableEvenThoughItIsReserved() {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("id", RowFilter.Operator.GT, "100")), COLUMN_TYPES);

        assertEquals(" AND \"id\" > ?", fragment.sql());
        assertEquals(100L, fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testNoSortsStillOrderByIdSoPaginationIsStable() {
        assertEquals(" ORDER BY \"id\"", RowQuerySqlBuilder.orderBy(List.of(), COLUMN_TYPES));
    }

    @Test
    void testASortRendersItsDirection() {
        assertEquals(
            " ORDER BY \"score\" DESC, \"id\"",
            RowQuerySqlBuilder.orderBy(List.of(new RowSort("score", RowSort.Direction.DESC)), COLUMN_TYPES));

        assertEquals(
            " ORDER BY \"score\" ASC, \"id\"",
            RowQuerySqlBuilder.orderBy(List.of(new RowSort("score", RowSort.Direction.ASC)), COLUMN_TYPES));
    }

    @Test
    void testEverySortStillEndsOnTheIdTiebreaker() {
        assertEquals(
            " ORDER BY \"title\" ASC, \"score\" DESC, \"id\"",
            RowQuerySqlBuilder.orderBy(
                List.of(
                    new RowSort("title", RowSort.Direction.ASC),
                    new RowSort("score", RowSort.Direction.DESC)),
                COLUMN_TYPES));
    }

    @Test
    void testSortingOnIdDescendingIsHowNewestFirstIsExpressed() {
        assertEquals(
            " ORDER BY \"id\" DESC, \"id\"",
            RowQuerySqlBuilder.orderBy(List.of(new RowSort("id", RowSort.Direction.DESC)), COLUMN_TYPES));
    }

    @Test
    void testSortingOnAnOwnerColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.orderBy(
                List.of(new RowSort("owner_id", RowSort.Direction.ASC)), COLUMN_TYPES));
    }

    @Test
    void testSortingOnAnUnknownColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.orderBy(List.of(new RowSort("nope", RowSort.Direction.ASC)), COLUMN_TYPES));
    }

    @Test
    void testContainsOnANonStringColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("score", RowFilter.Operator.CONTAINS, "1")), COLUMN_TYPES));
    }

    @Test
    void testInWithAnEmptyListIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("score", RowFilter.Operator.IN, List.of())), COLUMN_TYPES));
    }

    @Test
    void testBetweenWithTheWrongNumberOfBoundsIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("score", RowFilter.Operator.BETWEEN, List.of("1"))), COLUMN_TYPES));
    }

    @Test
    void testAComparisonAgainstAListIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowQuerySqlBuilder.filters(
                List.of(new RowFilter("score", RowFilter.Operator.GT, List.of("1", "2"))), COLUMN_TYPES));
    }

    private static String sqlFor(RowFilter.Operator operator) {
        RowQuerySqlBuilder.Fragment fragment = RowQuerySqlBuilder.filters(
            List.of(new RowFilter("score", operator, "1")), COLUMN_TYPES);

        return fragment.sql();
    }
}
