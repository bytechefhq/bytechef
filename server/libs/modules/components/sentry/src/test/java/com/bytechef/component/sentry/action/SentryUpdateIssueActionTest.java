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

package com.bytechef.component.sentry.action;

import static com.bytechef.component.sentry.constant.SentryConstants.ASSIGNED_TO;
import static com.bytechef.component.sentry.constant.SentryConstants.ISSUE_ID;
import static com.bytechef.component.sentry.constant.SentryConstants.ORGANIZATION;
import static com.bytechef.component.sentry.constant.SentryConstants.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
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
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class SentryUpdateIssueActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(ORGANIZATION, "my-org"));
    private final Map<String, Object> responseMap = Map.of("id", "123", STATUS, "resolved");
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(ISSUE_ID, "123", STATUS, "resolved", ASSIGNED_TO, "user:1"));

        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = SentryUpdateIssueAction.perform(
            mockedInputParameters, mockedConnectionParameters, mockedContext);

        assertEquals(responseMap, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/organizations/my-org/issues/123/", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of(STATUS, "resolved", ASSIGNED_TO, "user:1"), BodyContentType.JSON),
            bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformSendsOnlyTheFieldsThatAreSet(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedInputParameters = MockParametersFactory.create(Map.of(ISSUE_ID, "123", STATUS, "ignored"));

        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        SentryUpdateIssueAction.perform(mockedInputParameters, mockedConnectionParameters, mockedContext);

        assertEquals(Body.of(Map.of(STATUS, "ignored"), BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}
