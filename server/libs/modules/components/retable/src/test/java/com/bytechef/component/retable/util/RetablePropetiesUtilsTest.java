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
import static com.bytechef.component.retable.constant.RetableConstants.COLUMN_ID;
import static com.bytechef.component.retable.constant.RetableConstants.PROJECT_ID;
import static com.bytechef.component.retable.constant.RetableConstants.RETABLE_ID;
import static com.bytechef.component.retable.constant.RetableConstants.ROWS_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class RetablePropetiesUtilsTest {

    private static final String TEST_ID = "testId";
    private static final String TEST_NAME = "testName";
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreatePropertiesForRowValues(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(RETABLE_ID, "xy", PROJECT_ID, "1", ROWS_IDS, Map.of("2", "test")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", Map.of("columns", createItems())));

        List<ValueProperty<?>> result = RetablePropertiesUtils.createPropertiesForRowValues(
            mockedParameters, mockedParameters, new HashMap<>(), mockedActionContext);

        assertEquals(getExpectedProperties(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/retable/xy", stringArgumentCaptor.getValue());
    }

    private static List<Map<String, Object>> createItems() {
        return List.of(
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "text"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "select"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "color"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "phone_number"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "email"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "checkbox"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "number"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "percent"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "currency"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "rating"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "duration"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "calendar"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "attachment"),
            Map.of(COLUMN_ID, TEST_ID, "title", TEST_NAME, "type", "image"));
    }

    private static List<ValueProperty<?>> getExpectedProperties() {
        return List.of(
            string(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            string(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            string(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            string(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            string(TEST_ID)
                .label(TEST_NAME)
                .controlType(ControlType.EMAIL)
                .required(false),
            bool(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            number(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            number(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            number(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            integer(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            integer(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            dateTime(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            fileEntry(TEST_ID)
                .label(TEST_NAME)
                .required(false),
            fileEntry(TEST_ID)
                .label(TEST_NAME)
                .required(false));
    }
}
