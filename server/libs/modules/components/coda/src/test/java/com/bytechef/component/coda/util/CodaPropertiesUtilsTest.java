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

package com.bytechef.component.coda.util;

import static com.bytechef.component.coda.constant.CodaConstants.DOC_ID;
import static com.bytechef.component.coda.constant.CodaConstants.ROW_VALUES;
import static com.bytechef.component.coda.constant.CodaConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
class CodaPropertiesUtilsTest {

    private static final String TEST_NAME = "testName";
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testCreatePropertiesForRowValues() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(DOC_ID, "1", TABLE_ID, "2", ROW_VALUES, Map.of("3", "test")));

        Map<String, Object> mockedResponseBody = Map.of("items", createItems());

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedResponseBody);

        List<ValueProperty<?>> result = CodaPropertiesUtils.createPropertiesForRowValues(
            mockedParameters, mockedParameters, new HashMap<>(), mockedActionContext);

        assertEquals(getExpectedProperties(), result);
    }

    private static List<Map<String, Object>> createItems() {
        return List.of(
            Map.of("name", TEST_NAME, "format", Map.of("type", "duration")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "slider")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "scale")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "lookup")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "select")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "text")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "canvas")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "link")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "image")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "checkbox")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "email")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "number")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "percent")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "currency")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "date")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "dateTime")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "time")),
            Map.of("name", TEST_NAME, "format", Map.of("type", "person")));
    }

    private static List<ValueProperty<?>> getExpectedProperties() {
        return List.of(
            integer(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            integer(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            integer(TEST_NAME)
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
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .controlType(ControlType.URL)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .controlType(ControlType.URL)
                .required(false),
            bool(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .controlType(ControlType.EMAIL)
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
            date(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            dateTime(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            time(TEST_NAME)
                .label(TEST_NAME)
                .required(false),
            string(TEST_NAME)
                .label(TEST_NAME)
                .description("Use email address to insert person.")
                .required(false));
    }
}
