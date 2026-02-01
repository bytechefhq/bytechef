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
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Encoder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivona Pavela
 */
class GithubGetRepositoryContentActionTest extends AbstractGithubActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, byte[]>> encoderFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(OWNER, "testOwner", REPOSITORY, "testRepo", PATH, "test"));

    @BeforeEach
    void setUp() {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());
        when(mockedActionContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> encoderFunctionArgumentCaptor.getValue()
                .apply(mockedEncoder));
        when(mockedEncoder.base64Decode(any(String.class)))
            .thenAnswer(invocation -> Base64.getDecoder()
                .decode(invocation.getArgument(0, String.class)));
    }

    @Test
    void testPerformWhenItReturnsFile() throws Exception {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("content", "dGVzdA=="));

        String result = (String) executePerformFunction(
            GithubGetRepositoryContentAction.ACTION_DEFINITION, mockedParameters);

        assertEquals("test", result);
    }

    @Test
    void testPerformWhenItReturnsDirectory() throws Exception {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of(NAME, "test1"),
                Map.of(NAME, "test2")));

        String result = (String) executePerformFunction(
            GithubGetRepositoryContentAction.ACTION_DEFINITION, mockedParameters);

        assertEquals("test1\ntest2", result);
    }

    @Test
    void testPerform() throws Exception {

        String result = (String) executePerformFunction(
            GithubGetRepositoryContentAction.ACTION_DEFINITION, mockedParameters);

        assertEquals("", result);
        assertEquals("/repos/testOwner/testRepo/contents/test", stringArgumentCaptor.getValue());
    }
}
