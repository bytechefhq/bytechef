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

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookRequestFunction;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableRecordUpdatedTriggerTest {

    @Test
    void testWebhookRequestFlattensRowPayload() throws Exception {
        Map<String, Object> content = Map.of(
            "type", "RECORD_UPDATED", "table", "conversations", "payload",
            Map.of("id", 7, "values", Map.of("phone", "385916039814", "staff_reply", "on my way")));

        assertEquals(
            Map.of("id", 7, "phone", "385916039814", "staff_reply", "on my way"), webhookRequest(content));
    }

    @Test
    void testWebhookRequestWithoutPayloadReturnsContent() throws Exception {
        Map<String, Object> content = Map.of("type", "RECORD_UPDATED", "table", "conversations");

        assertEquals(content, webhookRequest(content));
    }

    private static Object webhookRequest(Map<String, Object> content) throws Exception {
        ModifiableTriggerDefinition triggerDefinition = DataTableRecordUpdatedTrigger.of(null, null, null);

        WebhookRequestFunction webhookRequestFunction = triggerDefinition.getWebhookRequest()
            .orElseThrow();

        return webhookRequestFunction.apply(null, null, null, null, new TestWebhookBody(content), null, null, null);
    }

    private record TestWebhookBody(Map<String, Object> content) implements WebhookBody {

        @Override
        public Object getContent() {
            return content;
        }

        @Override
        public <T> T getContent(Class<T> valueType) {
            return valueType.cast(content);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getContent(TypeReference<T> valueTypeRef) {
            return (T) content;
        }

        @Override
        public ContentType getContentType() {
            return ContentType.JSON;
        }

        @Override
        public String getMimeType() {
            return "application/json";
        }

        @Override
        public String getRawContent() {
            return String.valueOf(content);
        }
    }
}
