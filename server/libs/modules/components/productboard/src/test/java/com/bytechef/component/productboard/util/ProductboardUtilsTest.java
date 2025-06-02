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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
public class ProductboardUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetNoteIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, List.of(Map.of("title", "name", "id", "abc"))));

        assertEquals(
            List.of(option("name", "abc")),
            ProductboardUtils.getNoteIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testCreateSubscription() {
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

        WebhookEnableOutput result =
            ProductboardUtils.createSubscription(webhookUrl, workflowExecutionId, mockedTriggerContext, "event");

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, result);
        assertEquals(List.of("X-Version", "1"), stringArgumentCaptor.getAllValues());

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(
            DATA, Map.of(
                "name", "Webhook for " + workflowExecutionId,
                "events", List.of(Map.of("eventType", "event")),
                "notification", Map.of("url", webhookUrl, "version", 1))),
            body.getContent());
    }

    @Test
    void testDeleteSubscription() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        ProductboardUtils.deleteSubscription(mockedTriggerContext, "webhookId");

        assertEquals(List.of("X-Version", "1"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testWebhookValidateOnEnable() {
        when(mockedHttpParameters.toMap())
            .thenReturn(Map.of("validationToken", List.of("token")));

        WebhookValidateResponse webhookValidateResponse =
            ProductboardUtils.webhookValidateOnEnable(mockedParameters, mock(HttpHeaders.class), mockedHttpParameters,
                mock(WebhookBody.class), mock(WebhookMethod.class), mockedTriggerContext);

        assertEquals(new WebhookValidateResponse("token", 200), webhookValidateResponse);
    }
}
