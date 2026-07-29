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

package com.bytechef.component.daytona.action;

import static com.bytechef.component.daytona.constant.DaytonaConstants.CODE;
import static com.bytechef.component.daytona.constant.DaytonaConstants.LANGUAGE;
import static com.bytechef.component.daytona.constant.DaytonaConstants.SANDBOX_ID;
import static com.bytechef.component.daytona.constant.DaytonaConstants.TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class DaytonaExecuteCodePerformTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerformCreatesRunsAndDeletesSandbox() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(LANGUAGE, "python", CODE, "print('hi')", TIMEOUT, 30));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "sandbox-1"), Map.of("exitCode", 0, "result", "hi\n"));

        Object result = DaytonaExecuteCodeAction.perform(parameters, parameters, mockedActionContext);

        assertThat((Map<String, Object>) result)
            .containsEntry("exitCode", 0)
            .containsEntry("stdout", "hi\n")
            .containsEntry("success", true)
            .containsEntry("sandboxId", "sandbox-1");

        // create sandbox + run code + delete sandbox
        verify(mockedActionContext, times(3)).http(any());

        List<Http.Body> bodies = bodyArgumentCaptor.getAllValues();

        // create body (language only) and run body (language + code + timeout); delete sends no body
        assertThat(bodies).hasSize(2);
        assertThat((Map<String, Object>) bodies.get(0)
            .getContent()).containsEntry(LANGUAGE, "python");
        assertThat((Map<String, Object>) bodies.get(1)
            .getContent())
                .containsEntry(LANGUAGE, "python")
                .containsEntry(CODE, "print('hi')")
                .containsEntry(TIMEOUT, 30);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformDeletesSandboxEvenWhenRunFails() {
        Parameters parameters = MockParametersFactory.create(Map.of(LANGUAGE, "python", CODE, "boom"));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "sandbox-1"))
            .thenThrow(new RuntimeException("run failed"));

        try {
            DaytonaExecuteCodeAction.perform(parameters, parameters, mockedActionContext);
        } catch (RuntimeException runtimeException) {
            // expected: the run failure propagates
            assertThat(runtimeException).hasMessageContaining("run failed");
        }

        // sandbox must still be created and deleted despite the run failure
        verify(mockedActionContext, times(3)).http(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformReusesProvidedSandboxWithoutCreatingOrDeleting() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(LANGUAGE, "python", CODE, "print('hi')", SANDBOX_ID, "existing-sandbox"));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("exitCode", 0, "result", "hi\n"));

        Object result = DaytonaExecuteCodeAction.perform(parameters, parameters, mockedActionContext);

        assertThat((Map<String, Object>) result).containsEntry("sandboxId", "existing-sandbox");

        // only the code-run call; no create and no delete for a reused sandbox
        verify(mockedActionContext, times(1)).http(any());
    }
}
