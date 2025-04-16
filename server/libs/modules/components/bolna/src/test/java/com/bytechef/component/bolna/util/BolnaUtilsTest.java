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

package com.bytechef.component.bolna.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class BolnaUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetAgentIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of("agent_name", "test1", "id", "123"),
                Map.of("agent_name", "test2", "id", "456")));

        List<Option<String>> result = BolnaUtils.getAgentIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(option("test1", "123"), option("test2", "456"));

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }

    @Test
    void testGetPhoneNumbersOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of("phone_number", "123"),
                Map.of("phone_number", "456")));

        List<Option<String>> result = BolnaUtils.getPhoneNumbersOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(option("123", "123"), option("456", "456"));

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }
}
