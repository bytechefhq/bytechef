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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
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
 * @author Marija Horvat
 */
class BambooHrUpdatedEmployeeTriggerTest {

    private final Parameters mockedWebhookEnableOutputParameters = mock(Parameters.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private Parameters mockedParameters;
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        mockedParameters = MockParametersFactory
            .create(Map.of(MONITOR_FIELDS, List.of("firstName"), POST_FIELDS, List.of("firstName", "lastName")));

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "3"));

        WebhookEnableOutput webhookEnableOutput = BambooHrUpdatedEmployeeTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(new WebhookEnableOutput(Map.of(ID, "3"), null), webhookEnableOutput);
        assertEquals(List.of("accept", "application/json"), stringArgumentCaptor.getAllValues());

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "name", "bambooHRWebhook", MONITOR_FIELDS, List.of("firstName"),
            POST_FIELDS, Map.of("firstName", "firstName", "lastName", "lastName"), "url", webhookUrl,
            "format", "json");

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testWebhookDisable() {
        mockedParameters = MockParametersFactory.create(
            Map.of(MONITOR_FIELDS, List.of("firstName"), POST_FIELDS, List.of("firstName", "lastName")));

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        BambooHrUpdatedEmployeeTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(List.of("accept", "application/json"), stringArgumentCaptor.getAllValues());
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
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutputParameters, mockedTriggerContext);

        assertEquals(employees, result);
    }
}
