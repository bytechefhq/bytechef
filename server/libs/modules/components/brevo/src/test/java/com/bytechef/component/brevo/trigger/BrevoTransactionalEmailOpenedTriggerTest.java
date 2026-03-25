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

package com.bytechef.component.brevo.trigger;

import static com.bytechef.component.brevo.constant.BrevoConstants.ID;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class BrevoTransactionalEmailOpenedTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Object mockedObject = mock(Object.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Http mockedHttp = mock(Http.class);
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();

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
            .thenReturn(Map.of(ID, "123"));

        WebhookEnableOutput webhookEnableOutput = BrevoTransactionalEmailOpenedTrigger.webhookEnable(
            null, null, webhookUrl, null, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput =
            new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals("/webhooks", stringArgumentCaptor.getValue());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("url", webhookUrl, "events", List.of("opened")), body.getContent());
    }

    @Test
    void testWebhookDisable() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, 123));

        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        BrevoTransactionalEmailOpenedTrigger.webhookDisable(
            null, null, mockedParameters, null, mockedTriggerContext);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals("/webhooks/123", stringArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = BrevoTransactionalEmailOpenedTrigger.webhookRequest(
            null, null, null, null, mockedWebhookBody,
            null, null, null);

        assertEquals(mockedObject, result);
    }
}
