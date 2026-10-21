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

package com.bytechef.platform.data.table.execution.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableWebhookEventTest {

    @Test
    void testRowValuesAreCopiedAndUnmodifiable() {
        Map<String, Object> nestedMap = new HashMap<>(Map.of("name", "Apple"));
        List<Object> tags = new ArrayList<>(List.of("fruit", nestedMap));

        Map<String, Object> values = new HashMap<>(Map.of("tags", tags));

        DataTableWebhookEvent dataTableWebhookEvent = new DataTableWebhookEvent(
            "items", DataTableWebhookType.RECORD_CREATED, List.of(new DataTableRow(1L, values)), 0);

        tags.add("changed");
        nestedMap.put("name", "Banana");
        values.put("extra", "value");

        List<DataTableRow> rows = dataTableWebhookEvent.getRows();

        DataTableRow copiedRow = rows.getFirst();

        Map<String, Object> copiedValues = copiedRow.values();

        List<?> copiedTags = (List<?>) copiedValues.get("tags");

        assertEquals(1L, copiedRow.id());
        assertEquals(List.of("fruit", Map.of("name", "Apple")), copiedTags);
        assertEquals(Map.of("tags", List.of("fruit", Map.of("name", "Apple"))), copiedValues);
        assertThrows(UnsupportedOperationException.class, () -> copiedTags.add(null));
        assertThrows(UnsupportedOperationException.class, () -> copiedValues.put("other", "value"));
    }

    @Test
    void testNullValueKeysAreRejected() {
        Map<String, Object> values = new HashMap<>();

        values.put(null, "value");

        List<DataTableRow> rows = List.of(new DataTableRow(1L, values));

        assertThrows(
            NullPointerException.class,
            () -> new DataTableWebhookEvent("items", DataTableWebhookType.RECORD_CREATED, rows, 0));
    }

    @Test
    void testEmptyRowsAreRejected() {
        List<DataTableRow> rows = List.of();

        assertThrows(
            IllegalArgumentException.class,
            () -> new DataTableWebhookEvent("items", DataTableWebhookType.RECORD_CREATED, rows, 0));
    }
}
