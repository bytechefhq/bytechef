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

package com.bytechef.component.typeform.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.typeform.constant.TypeformConstants.ID;
import static com.bytechef.component.typeform.constant.TypeformConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TypeformUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("name", "abc"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters parameters = MockParametersFactory.create(Map.of());
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetFormOptions() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", List.of(Map.of(TITLE, "name", ID, "abc"))));

        assertEquals(
            expectedOptions,
            TypeformUtils.getFormOptions(parameters, parameters, Map.of(), "", mockedTriggerContext));
    }

    @Test
    void testGetWorkspaceOptions() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", List.of(Map.of("name", "name", ID, "abc"))));

        assertEquals(
            expectedOptions,
            TypeformUtils.getWorkspaceOptions(parameters, parameters, Map.of(), "", mockedActionContext));
    }
}
