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

package com.bytechef.component.retable.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.retable.constant.RetableConstants.PROJECT_ID;
import static com.bytechef.component.retable.constant.RetableConstants.ROWS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class RetablePropetiesUtilsTest {

    private static final String TEST_NAME = "testName";
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);

    @Test
    void testCreatePropertiesForRowValues() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(PROJECT_ID, "1", ROWS, Map.of("2", "test")));

        Map<String, Object> mockedResponseBody = Map.of("data", Map.of("columns", createItems()));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedResponseBody);

        List<Property.ValueProperty<?>> result = RetablePropertiesUtils.createPropertiesForRowValues(
            mockedParameters, mockedParameters, new HashMap<>(), mockedActionContext);

        assertEquals(getExpectedProperties(), result);
    }

    private static List<Map<String, Object>> createItems() {
        return List.of(
            Map.of("title", TEST_NAME, "type", "text"),
            Map.of("title", TEST_NAME, "type", "select"),
            Map.of("title", TEST_NAME, "type", "color"),
            Map.of("title", TEST_NAME, "type", "phone_number"),
            Map.of("title", TEST_NAME, "type", "email"),
            Map.of("title", TEST_NAME, "type", "checkbox"),
            Map.of("title", TEST_NAME, "type", "number"),
            Map.of("title", TEST_NAME, "type", "percent"),
            Map.of("title", TEST_NAME, "type", "currency"),
            Map.of("title", TEST_NAME, "type", "rating"),
            Map.of("title", TEST_NAME, "type", "duration"),
            Map.of("title", TEST_NAME, "type", "calendar"),
            Map.of("title", TEST_NAME, "type", "attachment"),
            Map.of("title", TEST_NAME, "type", "image"));
    }

    private static List<Property.ValueProperty<?>> getExpectedProperties() {
        return List.of(
            string(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .controlType(Property.ControlType.EMAIL)
                .required(false),
            bool(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            number(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            number(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            number(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            integer(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            integer(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            dateTime(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            fileEntry(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            fileEntry(TEST_NAME)
                .label(TEST_NAME)
                .required(false));
    }
}
