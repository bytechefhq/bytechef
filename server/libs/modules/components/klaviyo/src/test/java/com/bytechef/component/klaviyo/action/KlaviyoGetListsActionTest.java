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

package com.bytechef.component.klaviyo.action;

import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LIST_FIELDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class KlaviyoGetListsActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Map<String, List<String>>> headerArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final ArgumentCaptor<String> queryArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(LIST_FIELDS, List.of("name", "created", "updated", "opt_in_process")));
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(headerArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(queryArgumentCaptor.capture(), queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = KlaviyoGetListsAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        assertEquals(
            List.of(Map.of(
                "accept", List.of("application/vnd.api+json"),
                "revision", List.of("2025-04-15"))),
            headerArgumentCaptor.getAllValues());

        Object queryArguments = queryArgumentCaptor.getAllValues();

        assertEquals(List.of("fields[list]", "name,created,updated,opt_in_process"), queryArguments);
    }
}
