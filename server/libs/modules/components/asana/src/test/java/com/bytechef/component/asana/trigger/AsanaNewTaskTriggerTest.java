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

package com.bytechef.component.asana.trigger;

import static com.bytechef.component.asana.constant.AsanaConstants.GID;
import static com.bytechef.component.asana.constant.AsanaConstants.RESOURCE;
import static com.bytechef.component.asana.constant.AsanaConstants.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivona Pavela
 */
@ExtendWith(MockContextSetupExtension.class)
class AsanaNewTaskTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(
        RESOURCE, "resource"));
    private final Parameters mockedParameters = mock(Parameters.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookEnable(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String webhookUrl = "testWebhookUrl";

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "data", Map.of(GID, "123")));

        WebhookEnableOutput result = AsanaNewTaskTrigger.webhookEnable(
            mockedInputParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId", mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(GID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/webhooks", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            Body.of(
                Map.of("data",
                    Map.of(RESOURCE, "resource", TARGET, webhookUrl, "filters",
                        List.of(Map.of("action", "added", "resource_type", "task")))),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable(
        TriggerContext mockedTriggerContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        Parameters mockedOutputParameters = MockParametersFactory.create(Map.of(GID, "xy"));

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        AsanaNewTaskTrigger.webhookDisable(
            mockedInputParameters, mockedParameters, mockedOutputParameters, "testWorkflowExecutionId",
            mockedTriggerContext);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/webhooks/xy", stringArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of("events", List.of(mockedObject)));

        Object result = AsanaNewTaskTrigger.webhookRequest(
            mockedInputParameters, mockedParameters, mock(HttpHeaders.class), mock(HttpParameters.class),
            mockedWebhookBody, mock(WebhookMethod.class), mock(Parameters.class), mock(TriggerContext.class));

        assertEquals(Map.of("events", List.of(mockedObject)), result);
    }

    @Test
    void testWebhookValidateOnEnable() {
        HttpHeaders headersWithSecret = mock(HttpHeaders.class);
        when(headersWithSecret.firstValue("X-Hook-Secret")).thenReturn(java.util.Optional.of("secret"));

        WebhookValidateResponse response = AsanaNewTaskTrigger.webhookValidateOnEnable(
            mockedInputParameters, headersWithSecret, mock(HttpParameters.class),
            mock(WebhookBody.class), mock(WebhookMethod.class), mock(TriggerContext.class));

        assertEquals(200, response.status());
        assertEquals(List.of("secret"), response.headers()
            .get("X-Hook-Secret"));

        HttpHeaders headersWithoutSecret = mock(HttpHeaders.class);
        when(headersWithoutSecret.firstValue("X-Hook-Secret")).thenReturn(java.util.Optional.empty());

        WebhookValidateResponse responseWithoutSecret = AsanaNewTaskTrigger.webhookValidateOnEnable(
            mockedInputParameters, headersWithoutSecret, mock(HttpParameters.class),
            mock(WebhookBody.class), mock(WebhookMethod.class), mock(TriggerContext.class));

        assertEquals(400, responseWithoutSecret.status());
    }
}
