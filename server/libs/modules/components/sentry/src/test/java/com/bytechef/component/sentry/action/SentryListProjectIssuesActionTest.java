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

import static com.bytechef.component.sentry.constant.SentryConstants.LIMIT;
import static com.bytechef.component.sentry.constant.SentryConstants.ORGANIZATION;
import static com.bytechef.component.sentry.constant.SentryConstants.PROJECT;
import static com.bytechef.component.sentry.constant.SentryConstants.QUERY;
import static com.bytechef.component.sentry.constant.SentryConstants.STATS_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class SentryListProjectIssuesActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(ORGANIZATION, "my-org"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(PROJECT, "my-project", QUERY, "is:unresolved level:error", STATS_PERIOD, "14d", LIMIT, 25));
    private final ArgumentCaptor<Object[]> queryParametersArgumentCaptor = forClass(Object[].class);
    private final List<Map<String, Object>> responseList = List.of(Map.of("id", "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        List<Map<String, Object>> result = SentryListProjectIssuesAction.perform(
            mockedInputParameters, mockedConnectionParameters, mockedContext);

        assertEquals(responseList, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/projects/my-org/my-project/issues/", stringArgumentCaptor.getValue());
        assertEquals(
            List.of(QUERY, "is:unresolved level:error", STATS_PERIOD, "14d", LIMIT, 25),
            Arrays.asList(queryParametersArgumentCaptor.getValue()));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
