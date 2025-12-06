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

import static com.bytechef.component.typeform.constant.TypeformConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class TypeformNewSubmissionTriggerTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters parameters = MockParametersFactory.create(Map.of());

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        UUID uuid = UUID.randomUUID();

        try (MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {

            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            when(mockedTriggerContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);

            WebhookEnableOutput webhookEnableOutput = TypeformNewSubmissionTrigger.webhookEnable(parameters, parameters,
                webhookUrl, "", mockedTriggerContext);

            Http.Body body = bodyArgumentCaptor.getValue();

            Object content = body.getContent();

            assertEquals(Map.of(
                "enabled", true,
                "url", webhookUrl,
                "event_types", List.of(Map.of("form_response", true))), content);

            Map<String, ?> webhookParameters = webhookEnableOutput.parameters();
            Instant webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

            Map<String, Object> expectedParameters = Map.of(ID, uuid.toString());

            assertEquals(expectedParameters, webhookParameters);
            assertNull(webhookExpirationDate);
        }
    }

    @Test
    void testWebhookDisable() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        TypeformNewSubmissionTrigger.webhookDisable(parameters, parameters, parameters, "", mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
