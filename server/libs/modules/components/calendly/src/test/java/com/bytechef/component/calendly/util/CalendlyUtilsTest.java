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

package com.bytechef.component.calendly.util;

import static com.bytechef.component.calendly.constant.CalendlyConstants.RESOURCE;
import static com.bytechef.component.calendly.constant.CalendlyConstants.SCOPE;
import static com.bytechef.component.calendly.constant.CalendlyConstants.URI;
import static com.bytechef.component.calendly.constant.CalendlyConstants.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class CalendlyUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testSubscribeWebhok() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(RESOURCE, Map.of("current_organization", "organization", URI, "user_uri")),
                Map.of(RESOURCE, Map.of("uri", "uri/AAAAA")));

        WebhookEnableOutput result = CalendlyUtils.subscribeWebhook(mockedContext, "webhookUrl", "scope", "event");

        assertEquals(new WebhookEnableOutput(Map.of(UUID, "AAAAA"), null), result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Object content = body.getContent();

        Map<String, Object> expectedBody = Map.of(
            "url", "webhookUrl",
            "organization", "organization",
            "user", "user_uri",
            SCOPE, "scope",
            "events", List.of("event"));

        assertEquals(expectedBody, content);
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        CalendlyUtils.unsubscribeWebhook(mockedContext, "uuid");

        verify(mockedContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
