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

import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OUTPUT_FORMAT;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.REF;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Encoder;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivona Pavela
 */
@ExtendWith(MockContextSetupExtension.class)
class GithubGetRepositoryContentActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, ?>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(OWNER, "testOwner", REPOSITORY, "testRepo", PATH, "test"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);

    @Test
    void testPerformWhenItReturnsFile(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, ?> value = encoderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64Decode(stringArgumentCaptor.capture()))
            .thenReturn(bytes);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("content", "dGVzdA=="));

        Object result = GithubGetRepositoryContentAction.perform(mockedParameters, null, mockedContext);

        assertEquals("test", result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals(List.of("/repos/testOwner/testRepo/contents/test", "dGVzdA=="),
            stringArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertNotNull(encoderFunctionArgumentCaptor.getValue());
    }

    @Test
    void testPerformWhenItReturnsDirectory(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of(NAME, "test1"), Map.of(NAME, "test2")));

        Object result = GithubGetRepositoryContentAction.perform(mockedParameters, null, mockedContext);

        assertEquals("test1\ntest2", result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/repos/testOwner/testRepo/contents/test", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        Object result = GithubGetRepositoryContentAction.perform(mockedParameters, null, mockedContext);

        assertEquals("", result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/repos/testOwner/testRepo/contents/test", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformStructuredFileWithRef(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(
                OWNER, "testOwner", REPOSITORY, "testRepo", PATH, "docs/a.md", REF, "v1.0", OUTPUT_FORMAT,
                GithubGetRepositoryContentAction.STRUCTURED));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(REF, "v1.0"))
            .thenReturn(mockedExecutor);
        when(mockedContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, ?> value = encoderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64Decode("dGVzdA=="))
            .thenReturn(bytes);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "content", "dGVz\ndA==", NAME, "a.md", PATH, "docs/a.md", "sha", "abc", "size", 4, "type",
                    "file"));

        Object result = GithubGetRepositoryContentAction.perform(parameters, null, mockedContext);

        Map<String, Object> expected = new HashMap<>();

        expected.put("content", "test");
        expected.put("download_url", null);
        expected.put("html_url", null);
        expected.put(NAME, "a.md");
        expected.put(PATH, "docs/a.md");
        expected.put("sha", "abc");
        expected.put("size", 4);
        expected.put("type", "file");

        assertEquals(expected, result);
        assertEquals("/repos/testOwner/testRepo/contents/docs/a.md", stringArgumentCaptor.getAllValues()
            .get(0));
        verify(mockedExecutor).queryParameters(REF, "v1.0");
    }

    @Test
    void testPerformStructuredDirectory(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(
                OWNER, "testOwner", REPOSITORY, "testRepo", PATH, "docs", OUTPUT_FORMAT,
                GithubGetRepositoryContentAction.STRUCTURED));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                List.of(
                    Map.of(NAME, "a.md", PATH, "docs/a.md", "type", "file"),
                    Map.of(NAME, "img", PATH, "docs/img", "type", "dir")));

        Object result = GithubGetRepositoryContentAction.perform(parameters, null, mockedContext);

        List<?> entries = (List<?>) result;

        assertEquals(2, entries.size());
        assertEquals("docs/img", ((Map<?, ?>) entries.get(1)).get(PATH));
        assertEquals("dir", ((Map<?, ?>) entries.get(1)).get("type"));
        verify(mockedExecutor, never()).queryParameters(anyString(), any());
    }
}
