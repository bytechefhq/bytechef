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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class CalComUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);

    @Test
    void testSubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
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
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        CalComUtils.unsubscribeWebhook(mockedTriggerContext, "123");

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testGetContent() {
        Map<String, Object> content = Map.of(PAYLOAD, Map.of());

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        assertEquals(Map.of(), CalComUtils.getContent(mockedWebhookBody));
    }
}
