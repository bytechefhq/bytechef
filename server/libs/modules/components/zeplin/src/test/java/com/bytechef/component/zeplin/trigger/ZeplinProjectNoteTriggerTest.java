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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class ZeplinProjectNoteTriggerTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final Object mockedObject = mock(Object.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private static final String TEST_WORKFLOW_EXECUTION_ID = "testWorkflowExecutionId";

    @Test
    void testWebhookEnable() {
        UUID uuid = UUID.randomUUID();

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "abc"));

        try (MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {

            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            WebhookEnableOutput result = ZeplinProjectNoteTrigger.webhookEnable(
                mockedParameters, mockedParameters, "testWebhookUrl", TEST_WORKFLOW_EXECUTION_ID, mockedTriggerContext);

            WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
                Map.of(ID, "abc"), null);

            assertEquals(expectedWebhookEnableOutput, result);

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(
                Map.of("url", "testWebhookUrl",
                    "secret", uuid,
                    "events", List.of("project.note")),
                body.getContent());
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

        ZeplinProjectNoteTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, TEST_WORKFLOW_EXECUTION_ID, mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = ZeplinProjectNoteTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}
