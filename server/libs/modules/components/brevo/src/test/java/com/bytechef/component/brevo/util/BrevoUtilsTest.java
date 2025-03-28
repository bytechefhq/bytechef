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

package com.bytechef.component.brevo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
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
class BrevoUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(
        option("contact1@test.com", "contact1@test.com"),
        option("contact2@test.com", "contact2@test.com"));
    private final Context mockedContext = mock(Context.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);

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
    void testGetContactsOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "contacts", List.of(
                    Map.of("email", "contact1@test.com"),
                    Map.of("email", "contact2@test.com"))));

        List<Option<String>> result = BrevoUtils.getContactsOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }

    @Test
    void testGetSendersOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "senders", List.of(
                    Map.of("email", "contact1@test.com"),
                    Map.of("email", "contact2@test.com"))));

        List<Option<String>> result = BrevoUtils.getSendersOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expectedOptions.toArray()));
    }
}
