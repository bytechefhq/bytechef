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

import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ChatNewRequestTriggerTest {

    /**
     * This trigger's output IS the agent channel request contract, so it must be built from the one place that contract
     * is spelled rather than re-spelled here out of the component's own constants. Two spellings of the same three
     * fields drift by construction — the contract gained property descriptions and this trigger did not notice.
     */
    @Test
    void testOutputSchemaIsTheSharedAgentChannelRequestContract() {
        OutputResponse outputResponse = ChatNewRequestTrigger.TRIGGER_DEFINITION.getOutputDefinition()
            .flatMap(OutputDefinition::getOutputResponse)
            .orElseThrow();

        assertEquals(agentChannelRequest(), outputResponse.getOutputSchema());
    }

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
