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

package com.bytechef.component.productboard.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.productboard.constant.ProductboardConstants.DATA;
import static com.bytechef.component.productboard.constant.ProductboardConstants.ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class ProductboardUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testCreateSubscription() {
        String webhookUrl = "testWebhookUrl";
        String workflowExecutionId = "testWorkflowExecutionId";

        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(ID, "123")));

        WebhookEnableOutput result = ProductboardUtils.createSubscription(
            webhookUrl, workflowExecutionId, mockedTriggerContext, "event");

        assertEquals(new WebhookEnableOutput(Map.of(ID, "123"), null), result);
        assertEquals(Http.Body.of(
            Map.of(
                DATA, Map.of(
                    "name", "Webhook for " + workflowExecutionId,
                    "events", List.of(Map.of("eventType", "event")),
                    "notification", Map.of("url", webhookUrl, "version", 1))),
            Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/webhooks", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetNoteIdOptions() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(DATA, List.of(Map.of("title", "123", "id", "abc")), "pageCursor", "pg"),
                Map.of(DATA, List.of(Map.of("title", "234", "id", "def"))));

        assertEquals(
            List.of(option("123", "abc"), option("234", "def")),
            ProductboardUtils.getNoteIdOptions(null, null, null, null, mockedContext));

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/notes", "/notes"), stringArgumentCaptor.getAllValues());

        List<Object[]> objectsArgumentCaptorAllValues = objectsArgumentCaptor.getAllValues();

        assertEquals(2, objectsArgumentCaptorAllValues.size());

        Object[] objects1 = new Object[] {
            "pageLimit", 2000, "pageCursor", null
        };
        Object[] objects2 = new Object[] {
            "pageLimit", 2000, "pageCursor", "pg"
        };

        assertArrayEquals(objects1, objectsArgumentCaptorAllValues.getFirst());
        assertArrayEquals(objects2, objectsArgumentCaptorAllValues.getLast());
    }

    @Test
    void testGetFeatureIdOptions() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    DATA, List.of(Map.of("name", "Feature A", ID, "fa")),
                    "links", Map.of("next", "/features?page=2")),
                Map.of(
                    DATA, List.of(Map.of("name", "Feature B", ID, "fb"))));

        List<Option<String>> options = ProductboardUtils.getFeatureIdOptions(
            null, null, null, null, mockedContext);

        assertEquals(List.of(option("Feature A", "fa"), option("Feature B", "fb")), options);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/features", "/features?page=2"), stringArgumentCaptor.getAllValues());

        List<Object[]> objectsArgumentCaptorAllValues = objectsArgumentCaptor.getAllValues();

        assertEquals(1, objectsArgumentCaptorAllValues.size());

        Object[] objects1 = new Object[] {
            "pageLimit", 1000
        };

        assertArrayEquals(objects1, objectsArgumentCaptorAllValues.getFirst());
    }

    @Test
    void testDeleteSubscription() {
        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        ProductboardUtils.deleteSubscription(mockedTriggerContext, "webhookId");

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals("/webhooks/webhookId", stringArgumentCaptor.getValue());
    }

    @Test
    void testWebhookValidateOnEnable() {
        when(mockedHttpParameters.toMap())
            .thenReturn(Map.of("validationToken", List.of("token")));

        WebhookValidateResponse webhookValidateResponse = ProductboardUtils.webhookValidateOnEnable(
            null, null, mockedHttpParameters, null, null, null);

        assertEquals(new WebhookValidateResponse("token", 200), webhookValidateResponse);
    }
}
