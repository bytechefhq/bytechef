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

package com.bytechef.component.datatable.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableUtilsTest {

    @Test
    void testFlattenRowLiftsColumnValuesNextToId() {
        Map<String, Object> values = new HashMap<>();

        values.put("phone", "385916039814");
        values.put("staff_reply", "on my way");

        Map<String, Object> flattenedRow = DataTableUtils.flattenRow(new DataTableRow(7, values));

        assertEquals(Map.of("id", 7L, "phone", "385916039814", "staff_reply", "on my way"), flattenedRow);
    }

    @Test
    void testFlattenRowKeepsNullColumnsAsPresentKeys() {
        Map<String, Object> values = new HashMap<>();

        values.put("staff_reply", null);

        Map<String, Object> flattenedRow = DataTableUtils.flattenRow(new DataTableRow(7, values));

        assertTrue(flattenedRow.containsKey("staff_reply"));
        assertNull(flattenedRow.get("staff_reply"));
    }

    @Test
    void testFlattenRowsFlattensEveryRow() {
        List<Map<String, Object>> flattenedRows = DataTableUtils.flattenRows(
            List.of(new DataTableRow(1, Map.of("status", "BOT")), new DataTableRow(2, Map.of("status", "CLOSED"))));

        assertEquals(
            List.of(Map.of("id", 1L, "status", "BOT"), Map.of("id", 2L, "status", "CLOSED")), flattenedRows);
    }

    @Test
    void testFlattenPayloadFlattensWebhookRowPayload() {
        Map<String, Object> payload = Map.of("id", 7, "values", Map.of("staff_reply", "on my way"));

        assertEquals(Map.of("id", 7, "staff_reply", "on my way"), DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testFlattenPayloadWithoutValuesIsReturnedAsIs() {
        Map<String, Object> payload = Map.of("id", 7);

        assertEquals(Map.of("id", 7), DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testFlattenPayloadOfUnknownShapeIsReturnedUnchanged() {
        Object payload = List.of("unexpected");

        assertSame(payload, DataTableUtils.flattenPayload(payload));
    }
}
