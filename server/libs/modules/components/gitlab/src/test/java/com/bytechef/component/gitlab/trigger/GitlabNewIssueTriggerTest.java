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

package com.bytechef.component.gitlab.trigger;

import static com.bytechef.component.gitlab.constant.GitlabConstants.ID;
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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class GitlabNewIssueTriggerTest {

    protected Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    protected WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    protected HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    protected HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    protected WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected String workflowExecutionId = "testWorkflowExecutionId";
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    @Test
    void testDynamicWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, 123));

        WebhookEnableOutput webhookEnableOutput = GitlabNewIssueTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = webhookEnableOutput.parameters();
        Instant webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, 123);

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("url", webhookUrl, "issues_events", true, "push_events", false), body.getContent());
    }

    @Test
    void testDynamicWebhookDisable() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        GitlabNewIssueTrigger.dynamicWebhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testDynamicWebhookRequest() {
        Map<String, Object> map = Map.of("key", Map.of(ID, "123"), "action", "open");

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of("object_attributes", map));

        Object result = GitlabNewIssueTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(map, result);
    }
}
