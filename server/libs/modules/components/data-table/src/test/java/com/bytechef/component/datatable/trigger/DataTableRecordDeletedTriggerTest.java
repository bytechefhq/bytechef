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

package com.bytechef.component.datatable.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableRecordDeletedTriggerTest extends AbstractDataTableTriggerTest {

    @Test
    void testWebhookRequestFlattensRowPayload() throws Exception {
        assertEquals(
            Map.of("id", 7, "status", "CLOSED"),
            webhookRequest(
                DataTableRecordDeletedTrigger.of(null, null, null),
                rowContent("RECORD_DELETED", Map.of("status", "CLOSED"))));
    }

    @Test
    void testWebhookRequestWithoutPayloadReturnsContent() throws Exception {
        Map<String, Object> content = Map.of("type", "RECORD_DELETED", "table", "conversations");

        assertEquals(content, webhookRequest(DataTableRecordDeletedTrigger.of(null, null, null), content));
    }
}
