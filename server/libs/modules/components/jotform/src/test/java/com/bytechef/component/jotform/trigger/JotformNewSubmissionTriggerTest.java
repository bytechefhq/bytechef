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

package com.bytechef.component.jotform.trigger;

import static com.bytechef.component.jotform.trigger.JotformNewSubmissionTrigger.FORM_ID;
import static com.bytechef.component.jotform.trigger.JotformNewSubmissionTrigger.WEBHOOK_URL;
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
import com.bytechef.component.definition.Context.Json;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class JotformNewSubmissionTriggerTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Json, Executor>> jsonFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Json mockedJson = mock(Json.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(FORM_ID, "xy"));
    private final Parameters mockedOutputParameters = MockParametersFactory.create(Map.of(WEBHOOK_URL, "url1"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);

    @Test
    void testWebhookDisable(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("content", Map.of("1", "url1", "2", "url2")));
        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        JotformNewSubmissionTrigger.webhookDisable(mockedInputParameters, mockedInputParameters, mockedOutputParameters,
            "", mockedContext);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals(List.of("/form/xy/webhooks", "/form/xy/webhooks/1"), stringArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testWebhookEnable(
        TriggerContext mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        WebhookEnableOutput webhookEnableOutput = JotformNewSubmissionTrigger.webhookEnable(
            mockedInputParameters, mockedInputParameters, "url", "", mockedContext);

        assertEquals(new WebhookEnableOutput(Map.of(WEBHOOK_URL, "url"), null), webhookEnableOutput);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals(
            List.of("/form/xy/webhooks", "Content-Type", "multipart/form-data"), stringArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            Body.of(Map.of(WEBHOOK_URL, "url"), Http.BodyContentType.FORM_DATA), bodyArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        WebhookBody mockedWebhookBody = mock(WebhookBody.class);
        TriggerContext mockedTriggerContext = mock(TriggerContext.class);

        Map<String, Object> contentMap = new HashMap<>();

        contentMap.put("rawRequest", "{\"name\":\"xy\"}");

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(contentMap);
        when(mockedTriggerContext.json(jsonFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Json, Executor> value = jsonFunctionArgumentCaptor.getValue();

                return value.apply(mockedJson);
            });
        when(mockedJson.read(stringArgumentCaptor.capture()))
            .thenReturn(Map.of("name", "xy"));

        Object result = JotformNewSubmissionTrigger.webhookRequest(
            mockedInputParameters, null, null, null, mockedWebhookBody,
            null, null, mockedTriggerContext);

        assertEquals(Map.of("rawRequest", "{\"name\":\"xy\"}", "parsedRawRequest", Map.of("name", "xy")), result);
        assertEquals("{\"name\":\"xy\"}", stringArgumentCaptor.getValue());
    }
}
