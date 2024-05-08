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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUE;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class MicrosoftExcelUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testCreateInputPropertyForRowWhereFirstRowIsHeader() {
        List<Object> row = List.of("firstName", "lastName", 1111, true);

        Map<String, Object> valuesMap = Map.of(VALUES, List.of(row));

        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(true);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(valuesMap);

        List<Property.ValueProperty<?>> result = MicrosoftExcelUtils.createInputPropertyForRow(
            mockedParameters, mockedParameters, Map.of(), mockedContext);

        assertEquals(1, result.size());

        ModifiableObjectProperty first = (ModifiableObjectProperty) result.getFirst();

        assertEquals(VALUES, first.getName());
        assertEquals("Values", first.getLabel()
            .get());

        List<? extends Property.ValueProperty<?>> properties = first.getProperties()
            .get();

        assertEquals(4, properties.size());

        IntStream.range(0, properties.size())
            .forEach(i -> {
                assertEquals(row.get(i)
                    .toString(),
                    properties.get(i)
                        .getName());
                assertEquals("", properties.get(i)
                    .getDefaultValue()
                    .get());
            });

    }

    @Test
    void testCreateInputPropertyForRowWhereFirstRowNotHeader() {
        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(false);

        List<Property.ValueProperty<?>> result = MicrosoftExcelUtils.createInputPropertyForRow(
            mockedParameters, mockedParameters, Map.of(), mockedContext);

        assertEquals(1, result.size());

        Property.ValueProperty<?> array = result.getFirst();

        assertEquals(3, ((ModifiableArrayProperty) array).getItems().get().size());

        assertEquals(VALUES, array.getName());
        assertEquals("Values", array.getLabel().get());
        assertEquals(true, array.getRequired().get());
    }

    @Test
    void testGetLastUsedColumnLabel() {
        Map<String, Object> map = Map.of("columnCount", 3);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        String result = MicrosoftExcelUtils.getLastUsedColumnLabel(mockedParameters, mockedContext);

        assertEquals("C", result);
    }

    @Test
    void getLastUsedRowIndex() {
        Map<String, Object> map = new HashMap<>();

        map.put("rowCount", 2);
        map.put(VALUES, List.of(List.of("A", "B", "C"), List.of("D", "E", "F")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Integer result = MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedContext);

        assertEquals(2, result);
    }

    @Test
    void testGetLastUsedRowIndexWithEmptySheet() {
        Map<String, Object> map = new HashMap<>();

        map.put("rowCount", 1);
        map.put(VALUES, List.of(List.of()));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Integer result = MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedContext);

        assertEquals(0, result);
    }

    @Test
    void testGetLastUsedRowIndexWithOneRow() {
        Map<String, Object> map = new HashMap<>();

        map.put("rowCount", 1);
        map.put(VALUES, List.of(List.of("A", "B")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Integer result = MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedContext);

        assertEquals(1, result);
    }

    @Test
    void testGetMapOfValuesForRowWhereFirstRowIsHeader() {
        List<Object> row = Arrays.asList("value1", "value2", "value3");

        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(true);

        try (MockedStatic<MicrosoftExcelRowUtils> rowUtilsMockedStatic = mockStatic(MicrosoftExcelRowUtils.class)) {
            rowUtilsMockedStatic
                .when(() -> MicrosoftExcelRowUtils.getRowFromWorksheet(mockedParameters, mockedContext, 1))
                .thenReturn(List.of("header1", "header2", "header3"));

            Map<String, Object> result =
                MicrosoftExcelUtils.getMapOfValuesForRow(mockedParameters, mockedContext, row);

            Map<String, Object> expectedValuesMap = new LinkedHashMap<>();

            expectedValuesMap.put("header1", "value1");
            expectedValuesMap.put("header2", "value2");
            expectedValuesMap.put("header3", "value3");

            assertEquals(expectedValuesMap, result);
        }
    }

    @Test
    void testGetMapOfValuesForRowWhereFirstRowIsNotHeader() {
        List<Object> row = Arrays.asList("value1", "value2", "value3");

        when(mockedParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER))
            .thenReturn(false);

        Map<String, Object> result = MicrosoftExcelUtils.getMapOfValuesForRow(mockedParameters, mockedContext, row);

        Map<String, Object> expectedValuesMap = new LinkedHashMap<>();

        expectedValuesMap.put("column_A", "value1");
        expectedValuesMap.put("column_B", "value2");
        expectedValuesMap.put("column_C", "value3");

        assertEquals(expectedValuesMap, result);
    }

    @Test
    void testGetRowInputValuesForMap() {
        Map<String, String> valuesMap = new LinkedHashMap<>();

        valuesMap.put("name", "A");
        valuesMap.put("age", "B");

        when(mockedParameters.getRequiredMap(ROW, Object.class))
            .thenReturn(Map.of(VALUES, valuesMap));

        List<Object> result = MicrosoftExcelUtils.getRowInputValues(mockedParameters);

        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void testGetRowInputValuesForList() {
        when(mockedParameters.getRequiredMap(ROW, Object.class))
            .thenReturn(Map.of(VALUES, List.of("A", "B")));

        List<Object> result = MicrosoftExcelUtils.getRowInputValues(mockedParameters);

        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void testGetWorkbookIdOptions() {
        Map<String, Object> map = Map.of(VALUE, List.of(Map.of(NAME, "abc", ID, "123")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("abc", "123"));

        List<Option<String>> result = MicrosoftExcelUtils.getWorkbookIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetWorksheetNameOptions() {
        Map<String, Object> map = Map.of(VALUE, List.of(Map.of(NAME, "abc")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("abc", "abc"));

        List<Option<String>> result = MicrosoftExcelUtils.getWorksheetNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
    }

}
