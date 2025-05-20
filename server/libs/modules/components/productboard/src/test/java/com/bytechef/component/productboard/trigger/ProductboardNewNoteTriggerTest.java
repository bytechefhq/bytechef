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

package com.bytechef.component.productboard.trigger;

import static com.bytechef.component.productboard.constant.ProductboardConstants.DATA;
import static com.bytechef.component.productboard.constant.ProductboardConstants.ID;
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
 * @author Monika Kušter
 */
class ProductboardNewNoteTriggerTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        String workflowExecutionId = "testWorkflowExecutionId";

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
            .thenReturn(Map.of(DATA, Map.of(ID, "123")));

        WebhookEnableOutput result = ProductboardNewNoteTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, result);
        assertEquals(List.of("X-Version", "1"), stringArgumentCaptor.getAllValues());

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(
            DATA, Map.of(
                "name", "Webhook for " + workflowExecutionId,
                "events", List.of(Map.of("eventType", "note.created")),
                "notification", Map.of("url", webhookUrl, "version", 1))),
            body.getContent());
    }

    @Test
    void testWebhookDisable() {
        String workflowExecutionId = "testWorkflowExecutionId";

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        ProductboardNewNoteTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        assertEquals(List.of("X-Version", "1"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = ProductboardNewNoteTrigger.webhookRequest(
            mockedParameters, mockedParameters, mock(HttpHeaders.class), mock(HttpParameters.class), mockedWebhookBody,
            mock(WebhookMethod.class), mock(WebhookEnableOutput.class), mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}
