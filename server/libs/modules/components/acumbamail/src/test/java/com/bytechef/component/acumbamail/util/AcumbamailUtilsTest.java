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

package com.bytechef.component.acumbamail.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class AcumbamailUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of("access_token", "test-token"));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    private final List<Option<String>> expectedOptions = List.of(
        option("List 1", "list1"), option("List 2", "list2"));
    private final Map<String, Object> responseMap = Map.of(
        "list1", Map.of("name", "List 1"),
        "list2", Map.of("name", "List 2"));
    private final ArgumentCaptor<String> queryKeyCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Object> queryValueCaptor = ArgumentCaptor.forClass(Object.class);

    @BeforeEach
    void beforeEach() {

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryKeyCaptor.capture(), queryValueCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);
    }

    @Test
    void getListsIdOptions() {
        List<Option<String>> result = AcumbamailUtils.getListsIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(new HashSet<>(expectedOptions), new HashSet<>(result));
        assertEquals("auth_token", queryKeyCaptor.getValue());
        assertEquals("test-token", queryValueCaptor.getValue());
    }

    @Test
    void getSubscriberOptions() {
        List<Option<String>> result = AcumbamailUtils.getSubscriberOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(new HashSet<>(expectedOptions), new HashSet<>(result));
        assertEquals("auth_token", queryKeyCaptor.getValue());
        assertEquals("test-token", queryValueCaptor.getValue());
    }
}
