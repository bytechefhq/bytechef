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

package com.bytechef.component.github.trigger;

import static com.bytechef.component.github.constant.GithubConstants.EVENTS;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
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
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GithubEventsTriggerTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(REPOSITORY, "repo", EVENTS, List.of("event1", "event2")));
    private final Parameters mockedOutputParameters = MockParametersFactory.create(Map.of(ID, 12));
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookEnable(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("testOwner");

            String webhookUrl = "testWebhookUrl";

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of(ID, 123));

            WebhookEnableOutput webhookEnableOutput = GithubEventsTrigger.webhookEnable(
                mockedInputParameters, mockedInputParameters, webhookUrl, "testWorkflowExecutionId",
                mockedTriggerContext);

            WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, 123), null);

            assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals("/repos/testOwner/repo/hooks", stringArgumentCaptor.getValue());

            Map<String, Object> expectedBody = Map.of(
                EVENTS, List.of("event1", "event2"),
                "config", Map.of("url", webhookUrl, "content_type", "json"));

            assertEquals(Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable(
        TriggerContext mockedTriggerContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("testOwner");

            when(mockedHttp.delete(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);

            GithubEventsTrigger.webhookDisable(
                mockedInputParameters, null, mockedOutputParameters, "testWorkflowExecutionId", mockedTriggerContext);

            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/repos/testOwner/repo/hooks/12", stringArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookRequest() {
        Map<String, Object> content = Map.of("id", 123);

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        Map<String, Object> result = GithubEventsTrigger.webhookRequest(
            mockedInputParameters, mockedInputParameters, mock(HttpHeaders.class), mockedHttpParameters,
            mockedWebhookBody, mock(WebhookMethod.class), mockedInputParameters, mock(TriggerContext.class));

        assertEquals(content, result);
    }
}
