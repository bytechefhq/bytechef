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

package com.bytechef.component.bambooHR.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class BambooHRUtilsTest {

    protected Context mockedContext = mock(Context.class);
    protected Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    protected Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    protected ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Context.Http.Body.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetLocationOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of(
                    "name", "Location",
                    "options", List.of(
                        Map.of("name", "London, UK")))));

        List<Option<String>> result =
            BambooHRUtils.getLocationOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals("London, UK", result.get(0)
            .getValue());
    }

    @Test
    void testGetJobTitleOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of(
                    "name", "Job Title",
                    "options", List.of(
                        Map.of("name", "Software Engineer")))));

        List<Option<String>> result =
            BambooHRUtils.getJobTitleOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals("Software Engineer", result.get(0)
            .getLabel());
    }

    @Test
    void testGetEmploymentStatusOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of(
                    "name", "Employment Status",
                    "options", List.of(
                        Map.of("name", "Full-Time")))));

        List<Option<String>> result =
            BambooHRUtils.getEmploymentStatusOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals("Full-Time", result.get(0)
            .getLabel());
    }
}
