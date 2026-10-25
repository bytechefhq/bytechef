/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RowValuesValidatorTest {

    private static final List<ColumnSpec> COLUMNS = List.of(
        new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("score", ColumnType.INTEGER),
        new ColumnSpec("price", ColumnType.NUMBER), new ColumnSpec("done", ColumnType.BOOLEAN),
        new ColumnSpec("day", ColumnType.DATE), new ColumnSpec("at", ColumnType.DATE_TIME));

    @Test
    void testNaturalTypesAndStringFormsPass() {
        Map<String, Object> values = new HashMap<>();

        values.put("title", "x");
        values.put("score", 3);
        values.put("price", "9.5");
        values.put("done", "true");
        values.put("day", "2026-09-02");
        values.put("at", "2026-09-02T10:00:00Z");
        values.put("nullable", null);

        assertThrows(DataTableException.class, () -> RowValuesValidator.validate(values, COLUMNS), "unknown column");

        values.remove("nullable");

        assertDoesNotThrow(() -> RowValuesValidator.validate(values, COLUMNS));
    }

    @Test
    void testUncoercibleValuesNameTheColumn() {
        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> RowValuesValidator.validate(Map.of("score", "ten"), COLUMNS));

        assertEquals(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey(), dataTableException.getErrorKey());
        assertTrue(dataTableException.getMessage()
            .contains("score"));
        assertThrows(DataTableException.class, () -> RowValuesValidator.validate(Map.of("done", "yes"), COLUMNS));
        assertThrows(DataTableException.class, () -> RowValuesValidator.validate(Map.of("day", "tomorrow"), COLUMNS));
        assertThrows(DataTableException.class, () -> RowValuesValidator.validate(Map.of("id", 1), COLUMNS));
    }

    @Test
    void testExternalIdBounds() {
        assertThrows(DataTableException.class, () -> RowValuesValidator.validateExternalId(" "));
        assertThrows(DataTableException.class, () -> RowValuesValidator.validateExternalId("x".repeat(256)));
        assertDoesNotThrow(() -> RowValuesValidator.validateExternalId("x".repeat(255)));
    }
}
