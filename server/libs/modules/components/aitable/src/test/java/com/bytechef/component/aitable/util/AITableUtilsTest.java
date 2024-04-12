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

package com.bytechef.component.aitable.util;

import static com.bytechef.component.aitable.constant.AITableConstants.DATASHEET_ID;
import static com.bytechef.component.aitable.constant.AITableConstants.FIELDS;
import static com.bytechef.component.aitable.constant.AITableConstants.MAX_RECORDS;
import static com.bytechef.component.aitable.constant.AITableConstants.RECORD_IDS;
import static com.bytechef.component.aitable.constant.AITableConstants.SPACE_ID;
import static com.bytechef.component.aitable.constant.FieldType.EMAIL;
import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class AITableUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void createPropertiesForRecord() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> fieldMap = new LinkedHashMap<>();

        fieldMap.put("name", "name");
        fieldMap.put("type", EMAIL.name());
        fieldMap.put("property", Map.of("someProperty", "someValue"));

        fields.add(fieldMap);
        dataMap.put("fields", fields);
        map.put("data", dataMap);

        when(mockedParameters.getRequiredString(DATASHEET_ID))
            .thenReturn("datasheetId");
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<? extends Property.ValueProperty<?>> result =
            AITableUtils.createPropertiesForRecord(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, result.size());

        Property.ValueProperty<?> first = result.getFirst();

        assertEquals("name", first.getName());
        assertEquals("name", first.getLabel()
            .get());
        assertEquals(false, first.getRequired()
            .get());
    }

    @Test
    void testCreateQuery() {
        List<String> fields = List.of("Name", "Url", "Phone", "Rating");
        List<String> recordIds = List.of("123", "234", "345");

        when(mockedParameters.getList(FIELDS, String.class, List.of()))
            .thenReturn(fields);
        when(mockedParameters.getList(RECORD_IDS, String.class, List.of()))
            .thenReturn(recordIds);
        when(mockedParameters.getInteger(MAX_RECORDS))
            .thenReturn(2);

        String result = AITableUtils.createQuery(mockedParameters);

        String expectedQuery = "fields=Name,Url,Phone,Rating&recordIds=123,234,345&maxRecords=2";

        assertEquals(expectedQuery, result);
    }

    @Test
    void testGetDatasheetIdOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> datasheetMap = new LinkedHashMap<>();

        datasheetMap.put("name", "name");
        datasheetMap.put("id", "id");

        nodes.add(datasheetMap);
        dataMap.put("nodes", nodes);
        map.put("data", dataMap);

        when(mockedParameters.getRequiredString(SPACE_ID))
            .thenReturn("spaceId");
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", "id"));

        assertEquals(
            expectedOptions,
            AITableUtils.getDatasheetIdOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetDatasheetRecordIdOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> recordMap = new LinkedHashMap<>();

        recordMap.put("recordId", "recordId");

        records.add(recordMap);
        dataMap.put("records", records);
        map.put("data", dataMap);

        when(mockedParameters.getRequiredString(DATASHEET_ID))
            .thenReturn("datasheetId");
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("recordId", "recordId"));

        assertEquals(
            expectedOptions,
            AITableUtils.getDatasheetRecordIdOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetFieldNamesOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> fieldMap = new LinkedHashMap<>();

        fieldMap.put("name", "name");
        fieldMap.put("type", "type");

        fields.add(fieldMap);
        dataMap.put("fields", fields);
        map.put("data", dataMap);

        when(mockedParameters.getRequiredString(DATASHEET_ID))
            .thenReturn("datasheetId");
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", "type"));

        assertEquals(
            expectedOptions,
            AITableUtils.getFieldNamesOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetSpaceIdOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        List<Map<String, Object>> spaces = new ArrayList<>();
        Map<String, Object> spaceMap = new LinkedHashMap<>();

        spaceMap.put("name", "name");
        spaceMap.put("id", "id");

        spaces.add(spaceMap);
        dataMap.put("spaces", spaces);
        map.put("data", dataMap);

        when(mockedParameters.getRequiredString(DATASHEET_ID))
            .thenReturn("datasheetId");
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", "id"));

        assertEquals(
            expectedOptions,
            AITableUtils.getSpaceIdOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

}
