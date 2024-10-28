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

package com.bytechef.component.zoho.util;

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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luka Ljubić
 */
class ZohoCrmUtilTest {

    private final List<Option<String>> expectedOptions = List.of(option("option", "123"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    public void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetProfileOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("profiles", List.of(Map.of("name", "option", "id", "123"))));

        assertEquals(
            expectedOptions,
            ZohoCrmUtils.getProfileOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetRoleOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("roles", List.of(Map.of("name", "option", "id", "123"))));

        assertEquals(
            expectedOptions,
            ZohoCrmUtils.getRoleOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }
}
