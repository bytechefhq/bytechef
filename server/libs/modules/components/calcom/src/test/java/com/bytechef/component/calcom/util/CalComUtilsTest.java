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

package com.bytechef.component.calcom.util;

import static com.bytechef.component.calcom.constant.CalComConstants.DATA;
import static com.bytechef.component.calcom.constant.CalComConstants.ID;
import static com.bytechef.component.calcom.constant.CalComConstants.PAYLOAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class CalComUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);

    @Test
    void testSubscribeWebhook(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, "123")));

        String testEvent = "testEvent";
        String testWebhookUrl = "testWebhookUrl";

        String webhookId = CalComUtils.subscribeWebhook(testEvent, mockedTriggerContext, testWebhookUrl);

        assertEquals("123", webhookId);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "subscriberUrl", testWebhookUrl,
            "triggers", List.of(testEvent),
            "active", true);

        assertEquals(expectedBody, body.getContent());

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/webhooks", stringArgumentCaptor.getValue());
    }

    @Test
    void testUnsubscribeWebhook(
        TriggerContext mockedTriggerContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        CalComUtils.unsubscribeWebhook(mockedTriggerContext, "123");

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/webhooks/123", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetContent() {
        Map<String, Object> content = Map.of(PAYLOAD, Map.of());

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        assertEquals(Map.of(), CalComUtils.getContent(mockedWebhookBody));
    }
}
