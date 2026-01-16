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

package com.bytechef.component.trello.trigger;

import static com.bytechef.component.trello.constant.TrelloConstants.ID;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class TrelloNewCardTriggerTest {

    private final Parameters mockedDynamicWebhookEnableOutput = mock(Parameters.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private static final String TEST_WORKFLOW_EXECUTION_ID = "testWorkflowExecutionId";

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getString(ID_LIST))
            .thenReturn("listId");
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "abc"));

        WebhookEnableOutput webhookEnableOutput = TrelloNewCardTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, TEST_WORKFLOW_EXECUTION_ID, mockedTriggerContext);

        Map<String, ?> parameters = webhookEnableOutput.parameters();
        Instant webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

        assertEquals(Map.of(ID, "abc"), parameters);
        assertNull(webhookExpirationDate);

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of("callbackURL", webhookUrl, "idModel", "listId"), Arrays.asList(query));
    }

    @Test
    void testDynamicWebhookDisable() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        TrelloNewCardTrigger.dynamicWebhookDisable(
            mockedParameters, mockedParameters, mockedParameters, TEST_WORKFLOW_EXECUTION_ID, mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testDynamicWebhookRequest() {
        Map<String, Object> map = Map.of(ID, "123");

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of("action", Map.of("type", "createCard", "data", Map.of("card", Map.of(ID, "abc")))));

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Object result = TrelloNewCardTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

        assertEquals(map, result);
    }
}
