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

package com.bytechef.component.typeform.trigger;

import static com.bytechef.component.typeform.constant.TypeformConstants.FORM_ID;
import static com.bytechef.component.typeform.constant.TypeformConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
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
class TypeformNewSubmissionTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(FORM_ID, "abc"));
    private final Parameters mockedOutputParameters = MockParametersFactory.create(Map.of(ID, "xy"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookEnable(
        TriggerContext mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String webhookUrl = "testWebhookUrl";
        UUID uuid = UUID.randomUUID();

        try (MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {
            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            when(mockedHttp.put(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);

            WebhookEnableOutput webhookEnableOutput = TypeformNewSubmissionTrigger.webhookEnable(
                mockedInputParameters, null, webhookUrl, "", mockedContext);

            WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
                Map.of(ID, uuid.toString()), null);

            assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals("/forms/abc/webhooks/" + uuid, stringArgumentCaptor.getValue());
            assertEquals(
                Body.of("enabled", true, "url", webhookUrl, "event_types", Map.of("form_response", true)),
                bodyArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable(
        TriggerContext mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        TypeformNewSubmissionTrigger.webhookDisable(
            mockedInputParameters, null, mockedOutputParameters, "", mockedContext);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/forms/abc/webhooks/xy", stringArgumentCaptor.getValue());
    }
}
