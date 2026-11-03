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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
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
class ViberNewMessageTriggerTest {

    private static final String WEBHOOK_URL = "https://bytechef.example.com/webhooks/abc";

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters connectionParameters = MockParametersFactory.create(Map.of(AUTH_TOKEN, "token"));
    private final Parameters inputParameters = MockParametersFactory.create(Map.of());
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookEnableRegistersWithNoOptionalEvents(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0), Map.of("status", 0));

        WebhookEnableOutput webhookEnableOutput = ViberNewMessageTrigger.webhookEnable(
            inputParameters, connectionParameters, WEBHOOK_URL, "workflowExecutionId", mockedContext);

        assertEquals(Map.of("webhookUrl", WEBHOOK_URL), webhookEnableOutput.parameters());
        assertEquals(List.of("/get_account_info", "/set_webhook"), stringArgumentCaptor.getAllValues());
        assertEquals(
            Body.of(
                Map.of("url", WEBHOOK_URL, "event_types", List.of(), "send_name", true, "send_photo", true),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisableClearsWebhook(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", 0, "webhook", WEBHOOK_URL), Map.of("status", 0));

        ViberNewMessageTrigger.webhookDisable(
            inputParameters, connectionParameters, MockParametersFactory.create(Map.of("webhookUrl", WEBHOOK_URL)),
            "workflowExecutionId", mockedContext);

        assertEquals(List.of("/get_account_info", "/set_webhook"), stringArgumentCaptor.getAllValues());
        assertEquals(Body.of(Map.of("url", ""), BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisableSkipsViberWithoutWebhookUrl(TriggerContext mockedContext, Http mockedHttp) {
        ViberNewMessageTrigger.webhookDisable(
            inputParameters, connectionParameters, MockParametersFactory.create(Map.of()), "workflowExecutionId",
            mockedContext);

        verifyNoInteractions(mockedHttp);
    }

    @Test
    void testWebhookRequestEmitsMessage(TriggerContext mockedContext) {
        Object result = ViberNewMessageTrigger.webhookRequest(
            inputParameters, connectionParameters, null, null,
            mockBody(
                Map.of(
                    "event", "message", "timestamp", 1L, "message_token", 2L,
                    "sender", Map.of("id", "user-1", "name", "Ann"),
                    "message", Map.of("type", "text", "text", "Hi"))),
            null, null, mockedContext);

        List<?> outputs = (List<?>) result;

        assertEquals(1, outputs.size());

        Map<?, ?> output = (Map<?, ?>) outputs.getFirst();

        assertEquals("message", output.get("event"));
        assertEquals(Map.of("type", "text", "text", "Hi"), output.get("message"));
    }

    @Test
    void testWebhookRequestReadsPlatformParsedJsonBody(TriggerContext mockedContext) {
        String rawBody = "{\"event\":\"message\",\"timestamp\":1,\"message_token\":2," +
            "\"sender\":{\"id\":\"user-1\",\"name\":\"Ann\"},\"message\":{\"type\":\"text\",\"text\":\"Hi\"}}";

        Object result = ViberNewMessageTrigger.webhookRequest(
            inputParameters, connectionParameters, null, null,
            PlatformWebhookBody.of(rawBody, ViberNewMessageTrigger.TRIGGER_DEFINITION), null, null, mockedContext);

        List<?> outputs = (List<?>) result;

        assertEquals(1, outputs.size());

        Map<?, ?> output = (Map<?, ?>) outputs.getFirst();

        assertEquals("message", output.get("event"));
        assertEquals(Map.of("type", "text", "text", "Hi"), output.get("message"));
    }

    @Test
    void testWebhookRequestDropsSubscribed(TriggerContext mockedContext) {
        Object result = ViberNewMessageTrigger.webhookRequest(
            inputParameters, connectionParameters, null, null,
            mockBody(Map.of("event", "subscribed", "user", Map.of("id", "user-1"))), null, null, mockedContext);

        assertTrue(((List<?>) result).isEmpty());
    }

    private static WebhookBody mockBody(Map<String, Object> content) {
        WebhookBody webhookBody = mock(WebhookBody.class);

        when(webhookBody.getContent(any(TypeReference.class))).thenReturn(content);

        return webhookBody;
    }
}
