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

package com.bytechef.component.github.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubic
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GithubUtilsTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> contextFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(OWNER, "testOwner", REPOSITORY, "testRepo"));
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeEach()
    void beforeEach(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetRepositoryOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of(NAME, "taskName", ID, "123")));

        List<Option<String>> result = GithubUtils.getRepositoryOptions(
            mockedParameters, null, Map.of(), "", mockedContext);

        assertEquals(List.of(option("taskName", "taskName")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/user/repos", stringArgumentCaptor.getValue());

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of("per_page", 100, "page", 1), Arrays.asList(query));
    }

    @Test
    void testGetIssueOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("login", "user"),
                List.of(Map.of(TITLE, "taskName", "number", 123)));

        List<Option<String>> result = GithubUtils.getIssueOptions(mockedParameters, null, Map.of(), "", mockedContext);

        assertEquals(List.of(option("123 - taskName", "123")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/repos/testOwner/testRepo/issues", stringArgumentCaptor.getValue());

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of("per_page", 100, "page", 1, "state", "open"), Arrays.asList(query));
    }

    @Test
    void testGetCollaborators(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("login", "user"),
                List.of(Map.of(NAME, "John Doe", "login", "jdTest123")));

        List<Option<String>> result = GithubUtils.getCollaborators(
            mockedParameters, null, Map.of(), "", mockedContext);

        assertEquals(List.of(option("John Doe", "jdTest123")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/repos/user/testRepo/collaborators", stringArgumentCaptor.getValue());

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of("per_page", 100, "page", 1), Arrays.asList(query));
    }

    @Test
    void testGetLabels(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("login", "user"),
                List.of(Map.of("name", "Bug", "id", "12323123")));

        List<Option<String>> result = GithubUtils.getLabels(mockedParameters, null, Map.of(), "", mockedContext);

        assertEquals(List.of(option("Bug", "Bug")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/repos/user/testRepo/labels", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetOwnerName(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("login", "name"));

        String actualOwnerName = GithubUtils.getOwnerName(mockedContext);

        assertEquals("name", actualOwnerName);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/user", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetItemsWithNullResponse(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(null);
        when(mockedResponse.getHeader("link"))
            .thenReturn(null);

        List<Map<String, ?>> result = GithubUtils.getItems(mockedContext, "/test/url", false);

        assertEquals(List.of(), result);
    }

    @Test
    void testGetRepositoryOptionsWithNullResponse(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(null);
        when(mockedResponse.getHeader("link"))
            .thenReturn(null);

        List<Option<String>> result = GithubUtils.getRepositoryOptions(
            mockedParameters, null, Map.of(), "", mockedContext);

        assertEquals(List.of(), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }
}
