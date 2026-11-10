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

package com.bytechef.component.github.action;

import static com.bytechef.component.github.constant.GithubConstants.ORDER;
import static com.bytechef.component.github.constant.GithubConstants.PAGE;
import static com.bytechef.component.github.constant.GithubConstants.PER_PAGE;
import static com.bytechef.component.github.constant.GithubConstants.QUERY;
import static com.bytechef.component.github.constant.GithubConstants.SORT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
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
class GithubSearchIssuesAndPullRequestsActionTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final Map<String, Object> responseMap = Map.of("total_count", 1);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(
                QUERY, "repo:bytechefhq/bytechef is:pr is:open", SORT, "updated", ORDER, "asc", PAGE, 2,
                PER_PAGE, 50));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = GithubSearchIssuesAndPullRequestsAction.perform(parameters, null, mockedContext);

        assertEquals(responseMap, result);
        assertEquals("/search/issues", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "q", "repo:bytechefhq/bytechef is:pr is:open",
            SORT, "updated",
            ORDER, "asc",
            PAGE, 2,
            PER_PAGE, 50
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(ResponseType.JSON, configurationBuilder.build()
            .getResponseType());
    }

    @Test
    void testPerformWithQueryOnly(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters parameters = MockParametersFactory.create(Map.of(QUERY, "is:issue label:bug"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = GithubSearchIssuesAndPullRequestsAction.perform(parameters, null, mockedContext);

        assertEquals(responseMap, result);

        Object[] expectedQueryParameters = {
            "q", "is:issue label:bug",
            SORT, null,
            ORDER, null,
            PAGE, null,
            PER_PAGE, null
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }
}
