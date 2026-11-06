/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class RowQueryParserTest {

    private static final Set<String> COLUMNS = Set.of("id", "external_id", "title", "score");

    @Test
    void testParsesOperatorAndKeepsColonsInTheValue() {
        List<DataTableRowFilter> rowFilters = filters(List.of("title:EQ:a:b:c", "score:GTE:10"));

        assertEquals(2, rowFilters.size());
        assertEquals("title", rowFilters.getFirst()
            .column());
        assertEquals(DataTableRowFilter.Operation.EQUALS, rowFilters.getFirst()
            .operation());
        assertEquals(DataTableRowFilter.Operation.GREATER_EQUALS, rowFilters.get(1)
            .operation());
        assertEquals("a:b:c", rowFilters.getFirst()
            .value());
    }

    @Test
    void testInAndBetweenSplitOnCommas() {
        List<DataTableRowFilter> rowFilters = filters(List.of("score:IN:1,2,3", "score:BETWEEN:1,9"));

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
        assertEquals("external_id", filters(List.of("externalId:EQ:k"))
            .getFirst()
            .column());
    }

    @Test
    void testIdAndExternalIdAreFilterableAndSortableByTheirLiteralNames() {
        List<DataTableRowFilter> rowFilters = filters(List.of("id:EQ:5", "external_id:EQ:k"));

        assertEquals("id", rowFilters.get(0)
            .column());
        assertEquals("external_id", rowFilters.get(1)
            .column());

        List<DataTableRowSort> rowSorts = RowQueryParser.parseSorts(List.of("id:ASC", "external_id:DESC"), COLUMNS);

        assertEquals("id", rowSorts.get(0)
            .column());
        assertEquals("external_id", rowSorts.get(1)
            .column());
    }

    @Test
    void testNoFiltersMatchesAll() {
        assertTrue(RowQueryParser.parseFilters(null, COLUMNS)
            .isMatchAll());
        assertTrue(RowQueryParser.parseFilters(List.of(), COLUMNS)
            .isMatchAll());
    }

    @Test
    void testParsesSorts() {
        List<DataTableRowSort> rowSorts = RowQueryParser.parseSorts(List.of("score:DESC", "title:ASC"), COLUMNS);

        assertEquals(DataTableRowSort.Direction.DESC, rowSorts.getFirst()
            .direction());
        assertThrows(DataTableException.class, () -> RowQueryParser.parseSorts(List.of("score:UP"), COLUMNS));
        assertThrows(DataTableException.class, () -> RowQueryParser.parseSorts(List.of("nosuch:ASC"), COLUMNS));
    }

    private static List<DataTableRowFilter> filters(List<String> raw) {
        return RowQueryParser.parseFilters(raw, COLUMNS)
            .groups()
            .getFirst();
    }
}
