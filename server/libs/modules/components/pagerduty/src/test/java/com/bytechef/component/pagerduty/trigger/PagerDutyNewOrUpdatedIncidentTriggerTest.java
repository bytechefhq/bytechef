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

package com.bytechef.component.pagerduty.trigger;

import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ID;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
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
class PagerDutyNewOrUpdatedIncidentTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Map<String, Object> mockedMap = Map.of("event", Map.of());
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ID, "123", SERVICE, "service"));
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testWebhookDisable(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        PagerDutyNewOrUpdatedIncidentTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, "testWorkflowExecutionId",
            mockedContext);

        verify(mockedContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();

        assertEquals("/webhook_subscriptions/123", stringArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());
    }

    @Test
    void testWebhookEnable(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("webhook_subscription", Map.of(ID, "123")));

        String webhookUrl = "testWebhookUrl";
        WebhookEnableOutput webhookEnableOutput = PagerDutyNewOrUpdatedIncidentTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId", mockedContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/webhook_subscriptions", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Map<String, Object> expectedBody = Map.of(
            "webhook_subscription", Map.of(
                "delivery_method", Map.of("type", "http_delivery_method", "url", webhookUrl),
                "events", List.of("incident.acknowledged",
                    "incident.annotated",
                    "incident.delegated",
                    "incident.escalated",
                    "incident.priority_updated",
                    "incident.reassigned",
                    "incident.reopened",
                    "incident.resolved",
                    "incident.service_updated",
                    "incident.triggered",
                    "incident.unacknowledged"),
                "filter", Map.of(
                    "id", "service",
                    "type", "service_reference"),
                "type", "webhook_subscription"));

        assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(mockedMap);

        Object result = PagerDutyNewOrUpdatedIncidentTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, null);

        assertEquals(Map.of(), result);
    }
}
