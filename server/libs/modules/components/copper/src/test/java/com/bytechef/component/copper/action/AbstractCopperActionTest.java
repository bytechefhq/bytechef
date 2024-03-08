/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.copper.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.copper.util.CopperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
public abstract class AbstractCopperActionTest {

    protected ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Context.Http.Body.class);
    protected MockedStatic<CopperUtils> copperUtilsMockedStatic;
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    Map<String, Object> responeseMap = Map.of("key", "value");

    @BeforeEach
    public void beforeEach() {
        copperUtilsMockedStatic = mockStatic(CopperUtils.class);

        copperUtilsMockedStatic.when(() -> CopperUtils.getHeaders(mockedParameters))
            .thenReturn(Map.of());

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(anyMap()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);
    }

    @AfterEach
    public void afterEach() {
        copperUtilsMockedStatic.close();
    }
}
