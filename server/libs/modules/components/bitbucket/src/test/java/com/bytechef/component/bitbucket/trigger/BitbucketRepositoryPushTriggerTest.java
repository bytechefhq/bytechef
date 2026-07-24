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

package com.bytechef.component.bitbucket.trigger;

import static com.bytechef.component.bitbucket.constant.BitbucketConstants.ACTIVE;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.EVENTS;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.ID;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.REPOSITORY;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.URL;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.WORKSPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class BitbucketRepositoryPushTriggerTest {
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Map<String, Object> mockedMap = Map.of();
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(WORKSPACE, "workspace", REPOSITORY, "repository", ID, "id"));
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutputParameters = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testWebhookDisable(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        BitbucketRepositoryPushTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, "testWorkflowExecutionId",
            mockedContext);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/repositories/workspace/repository/hooks/id", stringArgumentCaptor.getValue());

        verify(mockedContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testWebhookEnable(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("uuid", "id"));

        String webhookUrl = "testWebhookUrl";
        WebhookEnableOutput webhookEnableOutput = BitbucketRepositoryPushTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId", mockedContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "id"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/repositories/workspace/repository/hooks", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Map<String, Object> expectedBody = Map.of(
            URL, webhookUrl,
            ACTIVE, true,
            EVENTS, List.of("repo:push"));

        assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(mockedMap);

        Object result = BitbucketRepositoryPushTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutputParameters, null);

        assertEquals(Map.of(), result);
    }
}
