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

package com.bytechef.component.viber.trigger;

import static com.bytechef.component.viber.constant.ViberConstants.AUTH_TOKEN;
import static com.bytechef.component.viber.constant.ViberConstants.CONVERSATION_STARTED;
import static com.bytechef.component.viber.constant.ViberConstants.EVENTS;
import static com.bytechef.component.viber.constant.ViberConstants.MESSAGE;
import static com.bytechef.component.viber.constant.ViberConstants.SUBSCRIBED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.component.viber.PlatformWebhookBody;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockContextSetupExtension.class, ObjectMapperSetupExtension.class
})
class ViberNewEventTriggerTest {

    private static final String WEBHOOK_URL = "https://bytechef.example.com/webhooks/abc";

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters connectionParameters = MockParametersFactory.create(Map.of(AUTH_TOKEN, "token"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookEnableRequestsConversationStartedWhenSelected(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubSetWebhook(mockedResponse, mockedExecutor, mockedHttp);

        WebhookEnableOutput webhookEnableOutput = ViberNewEventTrigger.webhookEnable(
            MockParametersFactory.create(Map.of(EVENTS, List.of(MESSAGE, CONVERSATION_STARTED))), connectionParameters,
            WEBHOOK_URL, "workflowExecutionId", mockedContext);

        assertEquals(Map.of("webhookUrl", WEBHOOK_URL), webhookEnableOutput.parameters());
        assertEquals(
            List.of(CONVERSATION_STARTED), ((Map<?, ?>) ((Body) bodyArgumentCaptor.getValue()).getContent())
                .get("event_types"));
    }

    @Test
    void testWebhookEnableOmitsConversationStartedWhenNotSelected(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubSetWebhook(mockedResponse, mockedExecutor, mockedHttp);

        ViberNewEventTrigger.webhookEnable(
            MockParametersFactory.create(Map.of(EVENTS, List.of(MESSAGE))), connectionParameters, WEBHOOK_URL,
            "workflowExecutionId", mockedContext);

        assertEquals(
            List.of(), ((Map<?, ?>) ((Body) bodyArgumentCaptor.getValue()).getContent()).get("event_types"));
    }

    @Test
    void testWebhookDisableSkipsViberWithoutWebhookUrl(TriggerContext mockedContext, Http mockedHttp) {
        ViberNewEventTrigger.webhookDisable(
            MockParametersFactory.create(Map.of()), connectionParameters, MockParametersFactory.create(Map.of()),
            "workflowExecutionId",
            mockedContext);

        verifyNoInteractions(mockedHttp);
    }

    @Test
    void testWebhookRequestEmitsSelectedEventOnly(TriggerContext mockedContext) {
        Parameters inputParameters = MockParametersFactory.create(Map.of(EVENTS, List.of(SUBSCRIBED)));

        Object messageResult = ViberNewEventTrigger.webhookRequest(
            inputParameters, connectionParameters, null, null,
            mockBody(
                Map.of(
                    "event", "message", "timestamp", 1L, "message_token", 2L,
                    "sender", Map.of("id", "user-1", "name", "Ann"),
                    "message", Map.of("type", "text", "text", "Hi"))),
            null, null, mockedContext);

        assertTrue(((List<?>) messageResult).isEmpty());

        Object subscribedResult = ViberNewEventTrigger.webhookRequest(
            inputParameters, connectionParameters, null, null,
            mockBody(Map.of("event", "subscribed", "user", Map.of("id", "user-1"))), null, null, mockedContext);

        assertEquals(1, ((List<?>) subscribedResult).size());
    }

    @Test
    void testWebhookRequestReadsPlatformParsedJsonBody(TriggerContext mockedContext) {
        String rawBody = "{\"event\":\"subscribed\",\"timestamp\":1,\"user\":{\"id\":\"user-1\"}}";

        Object result = ViberNewEventTrigger.webhookRequest(
            MockParametersFactory.create(Map.of(EVENTS, List.of(SUBSCRIBED))), connectionParameters, null, null,
            PlatformWebhookBody.of(rawBody, ViberNewEventTrigger.TRIGGER_DEFINITION), null, null, mockedContext);

        List<?> outputs = (List<?>) result;

        assertEquals(1, outputs.size());
        assertEquals("subscribed", ((Map<?, ?>) outputs.getFirst()).get("event"));
    }

    private void stubSetWebhook(Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0), Map.of("status", 0));
    }

    private static WebhookBody mockBody(Map<String, Object> content) {
        WebhookBody webhookBody = mock(WebhookBody.class);

        when(webhookBody.getContent(any(TypeReference.class))).thenReturn(content);

        return webhookBody;
    }
}
