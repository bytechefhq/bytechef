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

package com.bytechef.component.chat.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ChatNewRequestTriggerTest {

    @Test
    void getWebhookResultPreservesEmptyAttachmentsFromJsonPayload() {
        // A JSON client sending `attachments: []` must not crash the trigger. Java 21's
        // List.getFirst() throws on empty lists, so checkMap has to short-circuit first.
        Map<String, Object> content = new LinkedHashMap<>();

        content.put("conversationId", "abc123");
        content.put("message", "Hi");
        content.put("attachments", List.of());

        WebhookBody body = mock(WebhookBody.class);

        when(body.getContent()).thenReturn(content);

        @SuppressWarnings("unchecked")
        Map<String, Object> result =
            (Map<String, Object>) ChatNewRequestTrigger.getWebhookResult(null, null, null, null, body, null, null,
                null);

        assertThat(result)
            .containsEntry("message", "Hi")
            .containsEntry("conversationId", "abc123")
            .containsEntry("attachments", List.of());
    }

    @Test
    void getWebhookResultUnwrapsSingletonNonFileEntryList() {
        // Form-data clients submit each field as a singleton list; checkMap unwraps those
        // back to scalars so downstream ${trigger.field} resolves to the value directly.
        Map<String, Object> content = new LinkedHashMap<>();

        content.put("conversationId", List.of("abc123"));
        content.put("message", List.of("Hi"));

        WebhookBody body = mock(WebhookBody.class);

        when(body.getContent()).thenReturn(content);

        @SuppressWarnings("unchecked")
        Map<String, Object> result =
            (Map<String, Object>) ChatNewRequestTrigger.getWebhookResult(null, null, null, null, body, null, null,
                null);

        assertThat(result)
            .containsEntry("conversationId", "abc123")
            .containsEntry("message", "Hi")
            .containsEntry("attachments", List.of());
    }

    @Test
    void getWebhookResultDefaultsAttachmentsWhenAbsent() {
        Map<String, Object> content = new LinkedHashMap<>();

        content.put("conversationId", "abc123");
        content.put("message", "Hi");

        WebhookBody body = mock(WebhookBody.class);

        when(body.getContent()).thenReturn(content);

        @SuppressWarnings("unchecked")
        Map<String, Object> result =
            (Map<String, Object>) ChatNewRequestTrigger.getWebhookResult(null, null, null, null, body, null, null,
                null);

        assertThat(result)
            .containsEntry("attachments", List.of());
    }
}
