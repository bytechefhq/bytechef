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

package com.bytechef.component.rocketchat.trigger;

import static com.bytechef.component.rocketchat.constant.RocketchatConstants.ID;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.NAME;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.USERNAME;
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
 * @author Marija Horvat
 */
class RocketchatNewMessageTriggerTest {

    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "id"));
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Object mockedObject = mock(Object.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        String workflowExecutionId = "testWorkflowExecutionId";

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("integration", Map.of("_id", "123")));

        WebhookEnableOutput webhookEnableOutput = RocketchatNewMessageTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(
                "type", "webhook-outgoing",
                USERNAME, "rocket.cat",
                "channel", "all_public_channels, all_private_groups, all_direct_messages",
                "event", "sendMessage",
                "urls", List.of(webhookUrl),
                "enabled", true,
                NAME, "Message Sent Trigger",
                "scriptEnabled", false),
            body.getContent());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = RocketchatNewMessageTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}
