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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
abstract class AbstractGithubActionTest {

    protected final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> contextFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    protected final ActionContext mockedActionContext = mock(ActionContext.class);
    protected final Http.Executor mockedExecutor = mock(Http.Executor.class);
    protected final Http mockedHttp = mock(Http.class);
    protected final Http.Response mockedResponse = mock(Http.Response.class);
    protected final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    protected Object executePerformFunction(
        ModifiableActionDefinition modifiableActionDefinition, Parameters mockedParameters) throws Exception {

        Optional<PerformFunction> performFunction = modifiableActionDefinition.getPerform();

        assertTrue(performFunction.isPresent());

        SingleConnectionPerformFunction singleConnectionPerformFunction =
            (SingleConnectionPerformFunction) performFunction.get();

        when(mockedActionContext.http(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> contextFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        return singleConnectionPerformFunction.apply(mockedParameters, null, mockedActionContext);
    }
}
