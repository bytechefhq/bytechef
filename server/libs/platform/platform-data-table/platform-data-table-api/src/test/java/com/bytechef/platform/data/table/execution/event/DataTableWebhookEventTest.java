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
    void testPayloadListsAreCopiedAndUnmodifiable() {
        Map<String, Object> nestedMap = new HashMap<>(Map.of("name", "Apple"));
        List<Object> tags = new ArrayList<>(List.of("fruit", nestedMap));

        Map<String, Object> payload = new HashMap<>(Map.of("id", 1L, "tags", tags));

        DataTableWebhookEvent dataTableWebhookEvent = new DataTableWebhookEvent(
            "items", DataTableWebhookType.RECORD_CREATED, payload, 0);

        tags.add("changed");
        nestedMap.put("name", "Banana");

        List<Map<String, Object>> payloads = dataTableWebhookEvent.getPayloads();

        Map<String, Object> copiedPayload = payloads.getFirst();

        List<?> copiedTags = (List<?>) copiedPayload.get("tags");

        assertEquals(List.of("fruit", Map.of("name", "Apple")), copiedTags);
        assertThrows(UnsupportedOperationException.class, () -> copiedTags.add(null));
    }

    @Test
    void testNullPayloadKeysAreRejected() {
        Map<String, Object> payload = new HashMap<>();

        payload.put(null, "value");

        assertThrows(
            NullPointerException.class,
            () -> new DataTableWebhookEvent("items", DataTableWebhookType.RECORD_CREATED, payload, 0));
    }
}
