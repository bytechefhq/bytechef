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

package com.bytechef.component.vbout.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.vbout.constant.VboutConstants.CHANNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class VboutUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("One", "1"), option("Two", "2"));
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);

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
    void testGetListsIdOptions() {
        Map<String, Object> mockResponse = Map.of(
            "lists",
            Map.of("items",
                List.of(
                    Map.of("name", "One", "id", "1"),
                    Map.of("name", "Two", "id", "2"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = VboutUtils.getListsIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetContactIdOptions() {
        Map<String, Object> mockResponse = Map.of(
            "contacts",
            Map.of("items",
                List.of(
                    Map.of("email", "One", "id", "1"),
                    Map.of("email", "Two", "id", "2"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = VboutUtils.getContactIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetChannelIdOptions() {
        Map<String, Object> mockResponse = Map.of(
            "channels",
            Map.of("Facebook", Map.of(
                "pages", List.of(
                    Map.of("name", "One", "id", "1"),
                    Map.of("name", "Two", "id", "2")))));

        when(mockedParameters.getRequiredString(CHANNEL))
            .thenReturn("Facebook");
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = VboutUtils.getChannelIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
    }
}
