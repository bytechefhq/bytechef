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

package com.bytechef.component.daytona.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class DaytonaUtilsTest {

    private final Http mockedHttp = mock(Http.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final List<String> requestedUrls = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() throws Exception {
        when(mockedHttp.get(any()))
            .thenAnswer(invocation -> recordUrl("GET " + invocation.getArgument(0)));
        when(mockedHttp.post(any()))
            .thenAnswer(invocation -> recordUrl("POST " + invocation.getArgument(0)));
        when(mockedActionContext.http(any()))
            .thenAnswer(invocation -> {
                ContextFunction<Http, Http.Executor> httpFunction = invocation.getArgument(0);

                return httpFunction.apply(mockedHttp);
            });
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateSandboxWaitsUntilStarted() {
        ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("id", "sandbox-1", "state", "creating"), Map.of("id", "sandbox-1", "state", "starting"),
                Map.of("id", "sandbox-1", "state", "started"));

        String sandboxId = DaytonaUtils.createSandbox(mockedActionContext, "python");

        assertThat(sandboxId).isEqualTo("sandbox-1");
        assertThat(requestedUrls).containsExactly("POST /sandbox", "GET /sandbox/sandbox-1", "GET /sandbox/sandbox-1");

        Http.Body body = bodyArgumentCaptor.getValue();

        assertThat((Map<String, Object>) body.getContent())
            .containsEntry("labels", Map.of("code-toolbox-language", "python"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateSandboxFailsWhenSandboxErrors() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "sandbox-1", "state", "error", "errorReason", "no capacity"));

        assertThatThrownBy(() -> DaytonaUtils.createSandbox(mockedActionContext, "python"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no capacity");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetToolboxUrlAppendsSandboxIdToProxyUrl() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("url", "https://proxy.app.daytona.io/toolbox/"));

        String toolboxUrl = DaytonaUtils.getToolboxUrl(mockedActionContext, "sandbox-1");

        assertThat(toolboxUrl).isEqualTo("https://proxy.app.daytona.io/toolbox/sandbox-1");
        assertThat(requestedUrls).containsExactly("GET /sandbox/sandbox-1/toolbox-proxy-url");

        verify(mockedActionContext, times(1)).http(any());
    }

    private Http.Executor recordUrl(String url) {
        requestedUrls.add(url);

        return mockedExecutor;
    }
}
