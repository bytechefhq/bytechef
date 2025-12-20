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

package com.bytechef.component.zendesk.trigger;

import static com.bytechef.component.zendesk.constant.ZendeskConstants.ID;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.NAME;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.WEBHOOK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class ZendeskNewTicketTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Map<String, Object> mockedMap = Map.of("detail", Map.of());
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "123", NAME, "name"));
    private final Response mockedResponse = mock(Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);

    @Test
    void testWebhookDisable() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        ZendeskNewTicketTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, "testWorkflowExecutionId",
            mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testWebhookEnable() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(WEBHOOK, Map.of(ID, "123")));

        String webhookUrl = "testWebhookUrl";
        WebhookEnableOutput webhookEnableOutput = ZendeskNewTicketTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId",
            mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            WEBHOOK, Map.of(
                NAME, "name",
                "status", "active",
                "endpoint", webhookUrl,
                "http_method", "POST",
                "request_format", "json",
                "subscriptions", List.of("zen:event-type:ticket.created")));

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(mockedMap);

        Object result = ZendeskNewTicketTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(Map.of(), result);
    }
}
