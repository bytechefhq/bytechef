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

package com.bytechef.component.apollo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class ApolloUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("ime", "123"));
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Context mockedContext = mock(Context.class);

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
    void testGetAccountIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("organizations", List.of(Map.of("id", "123", "name", "ime"))));

        List<Option<String>> result = ApolloUtils.getAccountIdOptions(
            mockedParameters, mockedParameters, null, "", mockedContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetOOwnerIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("users", List.of(Map.of("id", "123", "name", "ime"))));

        List<Option<String>> result = ApolloUtils.getOwnerIdOptions(
            mockedParameters, mockedParameters, null, "", mockedContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetOpportunityIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("opportunities", List.of(Map.of("id", "123", "name", "ime"))));

        List<Option<String>> result = ApolloUtils.getOpportunityIdOptions(
            mockedParameters, mockedParameters, null, "", mockedContext);

        assertEquals(expectedOptions, result);
    }
}
