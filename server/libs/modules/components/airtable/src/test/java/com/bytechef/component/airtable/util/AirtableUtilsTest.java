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

package com.bytechef.component.airtable.util;

import static com.bytechef.component.airtable.constant.AirtableConstants.BASE_ID;
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.airtable.util.AirtableUtils.AirtableChoice;
import com.bytechef.component.airtable.util.AirtableUtils.AirtableField;
import com.bytechef.component.airtable.util.AirtableUtils.AirtableOptions;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Context;
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
class AirtableUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetBaseIdOptions() {
        Parameters parameters = MockParametersFactory.create(Map.of());

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("bases", List.of(Map.of("name", "abc", "id", "123"))));

        List<Option<String>> result =
            AirtableUtils.getBaseIdOptions(parameters, parameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("abc", "123")), result);
    }

    @Test
    void testGetFieldsProperties() throws Exception {
        Parameters parameters = MockParametersFactory.create(Map.of(BASE_ID, "123", TABLE_ID, "table123"));

        String fieldName = "filedName";
        String fieldId = "fieldId";
        String description = "Description";
        Map<String, List<AirtableUtils.AirtableTable>> tablesMap = Map.of(
            "tables", List.of(new AirtableUtils.AirtableTable("table123", "tableName",
                List.of(
                    new AirtableField(fieldId, fieldName, description, "autoNumber", null),
                    new AirtableField(fieldId, fieldName, description, "percent", null),
                    new AirtableField(fieldId, fieldName, description, "count", null),
                    new AirtableField(fieldId, fieldName, description, "barcode", null),
                    new AirtableField(fieldId, fieldName, description, "button", null),
                    new AirtableField(fieldId, fieldName, description, "createdBy", null),
                    new AirtableField(fieldId, fieldName, description, "createdTime", null),
                    new AirtableField(fieldId, fieldName, description, "currency", null),
                    new AirtableField(fieldId, fieldName, description, "externalSyncSource", null),
                    new AirtableField(fieldId, fieldName, description, "formula", null),
                    new AirtableField(fieldId, fieldName, description, "lastModifiedBy", null),
                    new AirtableField(fieldId, fieldName, description, "lastModifiedTime", null),
                    new AirtableField(fieldId, fieldName, description, "lookup", null),
                    new AirtableField(fieldId, fieldName, description, "multipleAttachments", null),
                    new AirtableField(fieldId, fieldName, description, "multipleLookupValues", null),
                    new AirtableField(fieldId, fieldName, description, "multipleRecordLinks", null),
                    new AirtableField(fieldId, fieldName, description, "rating", null),
                    new AirtableField(fieldId, fieldName, description, "richText", null),
                    new AirtableField(fieldId, fieldName, description, "rollup", null),
                    new AirtableField(fieldId, fieldName, description, "singleLineText", null),
                    new AirtableField(fieldId, fieldName, description, "date", null),
                    new AirtableField(fieldId, fieldName, description, "dateTime", null),
                    new AirtableField(fieldId, fieldName, description, "duration", null),
                    new AirtableField(fieldId, fieldName, description, "checkbox", null),
                    new AirtableField(fieldId, fieldName, description, "email", null),
                    new AirtableField(fieldId, fieldName, description, "multilineText", null),
                    new AirtableField(fieldId, fieldName, description, "number", null),
                    new AirtableField(fieldId, fieldName, description, "phoneNumber", null),
                    new AirtableField(fieldId, fieldName, description, "url", null),
                    new AirtableField(
                        fieldId, fieldName, description, "singleSelect",
                        new AirtableOptions(List.of(new AirtableChoice("123", "abc", "")))),
                    new AirtableField(
                        fieldId, fieldName, description, "multipleSelects",
                        new AirtableOptions(List.of(new AirtableChoice("123", "abc", ""))))),
                "description", "type", List.of())));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(Map.class))
            .thenReturn(tablesMap);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(tablesMap);

        List<? extends ValueProperty<?>> result = AirtableUtils.getFieldsProperties()
            .apply(parameters, parameters, Map.of(), mockedActionContext);

        List<ModifiableObjectProperty> expectedProperties = List.of(object("fields")
            .label("Fields")
            .properties(
                integer(fieldName)
                    .description(description),
                integer(fieldName)
                    .description(description),
                integer(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description + ". Expected format for value: mmmm d,yyyy"),
                string(fieldName)
                    .description(description + ". Expected format for value: mmmm d,yyyy"),
                string(fieldName)
                    .description(description),
                bool(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description)
                    .controlType(ControlType.EMAIL),
                string(fieldName)
                    .description(description)
                    .controlType(ControlType.TEXT_AREA),
                number(fieldName)
                    .description(description),
                string(fieldName)
                    .description(description)
                    .controlType(ControlType.PHONE),
                string(fieldName)
                    .description(description)
                    .controlType(ControlType.URL),
                string(fieldName)
                    .description(description)
                    .options(List.of(option("abc", "123"))),
                array(fieldName)
                    .items(string())
                    .description(description)
                    .options(List.of(option("abc", "123"))))
            .required(false));

        assertEquals(expectedProperties, result);
    }

    @Test
    void testGetRecordIdOptions() {
        Parameters parameters = MockParametersFactory.create(Map.of(BASE_ID, "123"));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("records", List.of(Map.of("name", "abc", "id", "123"))));

        List<Option<String>> result =
            AirtableUtils.getRecordIdOptions(parameters, parameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("123", "123")), result);
    }

    @Test
    void testGetTableIdOptions() {
        Parameters parameters = MockParametersFactory.create(Map.of(BASE_ID, "123"));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("tables", List.of(Map.of("name", "abc", "id", "123"))));

        List<Option<String>> result =
            AirtableUtils.getTableIdOptions(parameters, parameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("abc", "123")), result);
    }
}
