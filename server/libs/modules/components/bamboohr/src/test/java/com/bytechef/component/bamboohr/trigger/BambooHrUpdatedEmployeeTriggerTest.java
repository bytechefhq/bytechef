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

package com.bytechef.component.bamboohr.trigger;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.MONITOR_FIELDS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.POST_FIELDS;
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
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class BambooHrUpdatedEmployeeTriggerTest {

    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private Parameters mockedParameters;
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);

    @Test
    void testWebhookEnable(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String webhookUrl = "testWebhookUrl";
        mockedParameters = MockParametersFactory
            .create(Map.of(MONITOR_FIELDS, List.of("firstName"), POST_FIELDS, List.of("firstName", "lastName")));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "3"));

        WebhookEnableOutput webhookEnableOutput = BambooHrUpdatedEmployeeTrigger.webhookEnable(
            mockedParameters, null, webhookUrl, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(new WebhookEnableOutput(Map.of(ID, "3"), null), webhookEnableOutput);
        assertEquals(List.of("/webhooks", "accept", "application/json"), stringArgumentCaptor.getAllValues());

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "name", "bambooHRWebhook", MONITOR_FIELDS, List.of("firstName"),
            POST_FIELDS, Map.of("firstName", "firstName", "lastName", "lastName"), "url", webhookUrl,
            "format", "json");

        assertEquals(expectedBody, body.getContent());

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.Type.JSON, responseType.getType());
    }

    @Test
    void testWebhookDisable(
        TriggerContext mockedTriggerContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(ID, 1));

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        BambooHrUpdatedEmployeeTrigger.webhookDisable(
            null, null, mockedParameters, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(List.of("/webhooks/1", "accept", "application/json"), stringArgumentCaptor.getAllValues());
        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
    }

    @Test
    void testWebhookRequest() {

        List<Map<String, Map<String, Map<String, String>>>> employees = List.of(
            Map.of(
                "fields", Map.of(
                    "firstName", Map.of("value", "test"),
                    "lastName", Map.of("value", "test"),
                    "employeeNumber", Map.of("value", "1"))));

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of("employees", employees));

        Object result = BambooHrUpdatedEmployeeTrigger.webhookRequest(
            null, null, null, null, mockedWebhookBody,
            null, null, null);

        assertEquals(employees, result);
    }
}
