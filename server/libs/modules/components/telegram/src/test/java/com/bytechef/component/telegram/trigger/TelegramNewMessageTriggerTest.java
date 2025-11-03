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

package com.bytechef.component.telegram.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookDisableConsumer;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableFunction;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class TelegramNewMessageTriggerTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ModifiableTriggerDefinition triggerDefinition = TelegramNewMessageTrigger.TRIGGER_DEFINITION;

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> httpFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Optional<WebhookEnableFunction> optionalWebhookEnableFunction = triggerDefinition.getWebhookEnable();

        assertTrue(optionalWebhookEnableFunction.isPresent());

        WebhookEnableFunction webhookEnableFunction = optionalWebhookEnableFunction.get();

        WebhookEnableOutput webhookEnableOutput = webhookEnableFunction.apply(
            null, null, webhookUrl, null, mockedTriggerContext);

        assertEquals(new WebhookEnableOutput(null, null), webhookEnableOutput);
        assertEquals("/setWebhook", stringArgumentCaptor.getValue());

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("url", webhookUrl, "allowed_updates", List.of("message")), body.getContent());
        assertEquals(Http.BodyContentType.JSON, body.getContentType());
    }

    @Test
    void testWebhookDisable() {
        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> httpFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Optional<WebhookDisableConsumer> optionalWebhookDisableConsumer = triggerDefinition.getWebhookDisable();

        assertTrue(optionalWebhookDisableConsumer.isPresent());

        WebhookDisableConsumer webhookDisableConsumer = optionalWebhookDisableConsumer.get();

        webhookDisableConsumer.accept(null, null, null, null, mockedTriggerContext);

        assertEquals("/deleteWebhook", stringArgumentCaptor.getValue());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = TelegramNewMessageTrigger.webhookRequest(
            null, null, null, null, mockedWebhookBody, null, null, null);

        assertEquals(mockedObject, result);
    }
}
