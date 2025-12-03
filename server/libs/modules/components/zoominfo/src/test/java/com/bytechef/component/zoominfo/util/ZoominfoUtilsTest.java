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

package com.bytechef.component.zoominfo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
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
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class ZoominfoUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testGetCompanyFieldOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of("fieldName", "companyName", "accessGranted", true),
                Map.of("fieldName", "companyId", "accessGranted", true)));

        List<? extends Option<String>> options = ZoominfoUtils
            .getCompanyFieldOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(
            List.of(
                option("companyName", "companyName"), option("companyId", "companyId")),
            options);
    }

    @Test
    void testGetContactFieldOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of("fieldName", "firstName", "accessGranted", true),
                Map.of("fieldName", "lastName", "accessGranted", true)));

        List<? extends Option<String>> options = ZoominfoUtils
            .getContactFieldOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(
            List.of(
                option("firstName", "firstName"), option("lastName", "lastName")),
            options);
    }
}
