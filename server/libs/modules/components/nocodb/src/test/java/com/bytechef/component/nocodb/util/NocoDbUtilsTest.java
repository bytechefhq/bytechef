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

package com.bytechef.component.nocodb.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.FIELDS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.RECORDS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_COLUMNS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class NocoDbUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FIELDS, List.of("firstName", "lastName")));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> tableColumns = Map.of("columns", List.of(
        Map.of("uidt", "Checkbox", TITLE, "name"),
        Map.of("uidt", "SingleLineText", TITLE, "name"),
        Map.of("uidt", "PhoneNumber", TITLE, "name"),
        Map.of("uidt", "Email", TITLE, "name"),
        Map.of("uidt", "URL", TITLE, "name"),
        Map.of("uidt", "LongText", TITLE, "name"),
        Map.of("uidt", "Number", TITLE, "name"),
        Map.of("uidt", "Decimal", TITLE, "name"),
        Map.of("uidt", "Percent", TITLE, "name"),
        Map.of("uidt", "Rating", TITLE, "name"),
        Map.of("uidt", "Currency", TITLE, "name"),
        Map.of("uidt", "Year", TITLE, "name"),
        Map.of("uidt", "MultiSelect", TITLE, "name", "colOptions",
            Map.of("options", List.of(Map.of(TITLE, "option1")))),
        Map.of("uidt", "SingleSelect", TITLE, "name", "colOptions",
            Map.of("options", List.of(Map.of(TITLE, "option1")))),
        Map.of("uidt", "Date", TITLE, "name"),
        Map.of("uidt", "Time", TITLE, "name"),
        Map.of("uidt", "DateTime", TITLE, "name")));

    @Test
    void testCreatePropertiesForRecordWhenAddingNewRecord() throws Exception {
        mockHttpParemeters();

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(tableColumns);

        List<? extends ValueProperty<?>> properties = NocoDbUtils.createPropertiesForRecord(true)
            .apply(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        assertEquals(
            List.of(
                array(RECORDS)
                    .label("Records")
                    .description("Records to be created/updated.")
                    .items(
                        object()
                            .properties(
                                bool("name")
                                    .label("name")
                                    .required(false),
                                string("name")
                                    .label("name")
                                    .required(false),
                                string("name")
                                    .label("name")
                                    .required(false),
                                string("name")
                                    .label("name")
                                    .required(false),
                                string("name")
                                    .label("name")
                                    .required(false),
                                string("name")
                                    .label("name")
                                    .controlType(ControlType.TEXT_AREA)
                                    .required(false),
                                number("name")
                                    .label("name")
                                    .required(false),
                                number("name")
                                    .label("name")
                                    .required(false),
                                number("name")
                                    .label("name")
                                    .required(false),
                                number("name")
                                    .label("name")
                                    .required(false),
                                number("name")
                                    .label("name")
                                    .required(false),
                                number("name")
                                    .label("name")
                                    .required(false),
                                array("name")
                                    .label("name")
                                    .items(string())
                                    .options(List.of(option("option1", "option1")))
                                    .required(false),
                                string("name")
                                    .label("name")
                                    .options(List.of(option("option1", "option1")))
                                    .required(false),
                                date("name")
                                    .label("name")
                                    .required(false),
                                time("name")
                                    .label("name")
                                    .required(false),
                                dateTime("name")
                                    .label("name")
                                    .required(false)))
                    .minItems(1)
                    .required(true)),
            properties);
    }

    @Test
    void testGetBaseIdOptions() {
        mockHttpParemeters();

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("list", List.of(Map.of(TITLE, "abc", "id", "123"))));

        assertEquals(
            expectedOptions,
            NocoDbUtils.getBaseIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetTableIdOptions() {
        mockHttpParemeters();

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("list", List.of(Map.of(TITLE, "abc", "id", "123"))));

        assertEquals(
            expectedOptions,
            NocoDbUtils.getTableIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetWorkspaceIdOptions() {
        mockHttpParemeters();

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("list", List.of(Map.of(TITLE, "abc", "id", "123"))));

        assertEquals(
            expectedOptions,
            NocoDbUtils.getWorkspaceIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void transformRecordsForInsertion() {
        mockedParameters = MockParametersFactory.create(Map.of(TABLE_COLUMNS, Map.of(RECORDS,
            List.of(Map.of("firstName", "John", "lastName", "Doe", "options", List.of("option1", "option2"))))));

        List<Map<String, Object>> transformRecords = NocoDbUtils.transformRecordsForInsertion(mockedParameters);

        assertEquals(List.of(Map.of("firstName", "John", "lastName", "Doe", "options", "option1,option2")),
            transformRecords);
    }

    private void mockHttpParemeters() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }
}
