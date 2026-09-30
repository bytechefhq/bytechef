/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RowQueryParserTest {

    private static final Set<String> COLUMNS = Set.of("id", "external_id", "title", "score");

    @Test
    void testParsesOperatorAndKeepsColonsInTheValue() {
        List<RowFilter> rowFilters = RowQueryParser.parseFilters(
            List.of("title:EQ:a:b:c", "score:GTE:10"), COLUMNS);

        assertEquals(2, rowFilters.size());
        assertEquals("title", rowFilters.getFirst()
            .field());
        assertEquals(RowFilter.Operator.EQ, rowFilters.getFirst()
            .operator());
        assertEquals("a:b:c", rowFilters.getFirst()
            .value());
    }

    @Test
    void testInAndBetweenSplitOnCommas() {
        List<RowFilter> rowFilters = RowQueryParser.parseFilters(
            List.of("score:IN:1,2,3", "score:BETWEEN:1,9"), COLUMNS);

        assertEquals(List.of("1", "2", "3"), rowFilters.get(0)
            .value());
        assertEquals(List.of("1", "9"), rowFilters.get(1)
            .value());
    }

    @Test
    void testBetweenNeedsExactlyTwoValues() {
        for (String bad : List.of("score:BETWEEN:1", "score:BETWEEN:1,2,3")) {
            DataTableException dataTableException = assertThrows(
                DataTableException.class, () -> RowQueryParser.parseFilters(List.of(bad), COLUMNS), bad);

            assertEquals(DataTableErrorType.FILTER_INVALID.getErrorKey(), dataTableException.getErrorKey());
        }
    }

    @Test
    void testInWithNoValuesIsInvalid() {
        for (String bad : List.of("score:IN:", "score:IN:,")) {
            DataTableException dataTableException = assertThrows(
                DataTableException.class, () -> RowQueryParser.parseFilters(List.of(bad), COLUMNS), bad);

            assertEquals(DataTableErrorType.FILTER_INVALID.getErrorKey(), dataTableException.getErrorKey());
        }
    }

    @Test
    void testUnknownOperatorColumnOrShapeIsInvalid() {
        for (String bad : List.of("score:LIKE:1", "nosuch:EQ:1", "score", "score:EQ")) {
            assertThrows(DataTableException.class, () -> RowQueryParser.parseFilters(List.of(bad), COLUMNS), bad);
        }
    }

    @Test
    void testExternalIdIsAddressableAsExternalIdAndExternalUnderscoreId() {
        assertEquals("external_id", RowQueryParser.parseFilters(List.of("externalId:EQ:k"), COLUMNS)
            .getFirst()
            .field());
    }

    /**
     * {@code id} and {@code external_id} are reserved but still readable, so both must remain filterable and sortable
     * by their literal names. A regression that rejected a reserved name outright, or that mis-special-cased
     * {@code external_id} the way {@code externalId} is special-cased, would slip past every other test in this class.
     */
    @Test
    void testIdAndExternalIdAreFilterableAndSortableByTheirLiteralNames() {
        List<RowFilter> rowFilters = RowQueryParser.parseFilters(List.of("id:EQ:5", "external_id:EQ:k"), COLUMNS);

        assertEquals("id", rowFilters.get(0)
            .field());
        assertEquals("external_id", rowFilters.get(1)
            .field());

        List<RowSort> rowSorts = RowQueryParser.parseSorts(List.of("id:ASC", "external_id:DESC"), COLUMNS);

        assertEquals("id", rowSorts.get(0)
            .field());
        assertEquals("external_id", rowSorts.get(1)
            .field());
    }

    @Test
    void testParsesSorts() {
        List<RowSort> rowSorts = RowQueryParser.parseSorts(List.of("score:DESC", "title:ASC"), COLUMNS);

        assertEquals(RowSort.Direction.DESC, rowSorts.getFirst()
            .direction());
        assertThrows(DataTableException.class, () -> RowQueryParser.parseSorts(List.of("score:UP"), COLUMNS));
        assertThrows(DataTableException.class, () -> RowQueryParser.parseSorts(List.of("nosuch:ASC"), COLUMNS));
    }
}
