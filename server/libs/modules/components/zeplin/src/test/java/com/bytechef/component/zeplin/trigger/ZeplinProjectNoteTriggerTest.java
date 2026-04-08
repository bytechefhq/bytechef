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

package com.bytechef.component.zeplin.trigger;

import static com.bytechef.component.zeplin.constant.ZeplinConstants.ID;
import static com.bytechef.component.zeplin.constant.ZeplinConstants.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class ZeplinProjectNoteTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(PROJECT_ID, "xy"));
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedOutputParameters = MockParametersFactory.create(Map.of(ID, "123"));
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookEnable(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        UUID uuid = UUID.randomUUID();

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "abc"));

        try (MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {
            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            WebhookEnableOutput result = ZeplinProjectNoteTrigger.webhookEnable(
                mockedInputParameters, mockedInputParameters, "testWebhookUrl", "testWorkflowExecutionId",
                mockedContext);

            WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
                Map.of(ID, "abc"), null);

            assertEquals(expectedWebhookEnableOutput, result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals("/projects/xy/webhooks", stringArgumentCaptor.getValue());
            assertEquals(
                Body.of(
                    Map.of("url", "testWebhookUrl", "secret", uuid, "events", List.of("project.note")),
                    BodyContentType.JSON),
                bodyArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable(
        TriggerContext mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        ZeplinProjectNoteTrigger.webhookDisable(
            mockedInputParameters, null, mockedOutputParameters, "testWorkflowExecutionId", mockedContext);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/projects/xy/webhooks/123", stringArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = ZeplinProjectNoteTrigger.webhookRequest(
            mockedInputParameters, null, null, null, mockedWebhookBody,
            null, null, null);

        assertEquals(mockedObject, result);
    }
}
