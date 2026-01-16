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

package com.bytechef.component.http.client.action;

import static com.bytechef.component.http.client.constant.HttpClientConstants.URI;
import static com.bytechef.component.test.definition.MockParametersFactory.create;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.BasePerformFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
public class HttpClientActionTest {

    protected final ArgumentCaptor<ContextFunction<Http, Executor>> contextFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    protected final ActionContext mockedActionContext = mock(ActionContext.class);
    protected final Executor mockedExecutor = mock(Executor.class);
    protected final Http mockedHttp = mock(Http.class);
    protected final Parameters mockedParameters = create(Map.of(URI, "/test"));
    protected final Response mockedResponse = mock(Response.class);
    protected final ArgumentCaptor<RequestMethod> requestMethodArgumentCaptor = forClass(RequestMethod.class);
    protected final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    protected Object executePerformFunction(ModifiableActionDefinition action) throws Exception {
        Optional<? extends BasePerformFunction> basePerformFunction = action.getPerform();

        assertTrue(basePerformFunction.isPresent());

        PerformFunction performFunction = (PerformFunction) basePerformFunction.get();

        when(mockedActionContext.http(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> contextFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.exchange(stringArgumentCaptor.capture(), requestMethodArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(anyMap()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(anyMap()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        return performFunction.apply(mockedParameters, null, mockedActionContext);
    }
}
