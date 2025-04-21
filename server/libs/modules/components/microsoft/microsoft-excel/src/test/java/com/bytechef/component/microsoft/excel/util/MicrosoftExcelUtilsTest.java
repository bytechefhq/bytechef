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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.COLUMN;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.UPDATE_WHOLE_ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUE;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftExcelUtilsTest {

    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor =
        ArgumentCaptor.forClass(ActionContext.class);
    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testCreatePropertiesForNewRowWhereFirstNewRowIsHeader() {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, true));
        List<Object> firstRow = List.of("firstName", "lastName", 1111, true);

        try (MockedStatic<MicrosoftExcelRowUtils> microsoftExcelRowUtilsMockedStatic =
            mockStatic(MicrosoftExcelRowUtils.class)) {

            microsoftExcelRowUtilsMockedStatic
                .when(() -> MicrosoftExcelRowUtils.getRowFromWorksheet(
                    parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(firstRow);

            List<Property.ValueProperty<?>> result = MicrosoftExcelUtils.createPropertiesForNewRow(
                mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            ModifiableObjectProperty expectedProperty = object(VALUES)
                .label("Values")
                .properties(
                    string("firstName")
                        .label("firstName")
                        .defaultValue(""),
                    string("lastName")
                        .label("lastName")
                        .defaultValue(""),
                    string("1111")
                        .label("1111")
                        .defaultValue(""),
                    string("true")
                        .label("true")
                        .defaultValue(""))
                .required(true);

            assertEquals(List.of(expectedProperty), result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void testCreatePropertiesForNewRowWhereFirstNewRowNotHeader() {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false));

        List<Property.ValueProperty<?>> result = MicrosoftExcelUtils.createPropertiesForNewRow(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        ModifiableArrayProperty expectedProperty = array(VALUES)
            .label("Values")
            .items(bool(), number(), string())
            .required(true);

        assertEquals(List.of(expectedProperty), result);
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsHeaderAndUpdatingWholeRow() {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, UPDATE_WHOLE_ROW, true, WORKBOOK_ID, "workbookId", WORKSHEET_NAME,
                "sheetName"));

        try (MockedStatic<MicrosoftExcelRowUtils> microsoftExcelRowUtilsMockedStatic =
            mockStatic(MicrosoftExcelRowUtils.class)) {

            microsoftExcelRowUtilsMockedStatic
                .when(() -> MicrosoftExcelRowUtils.getRowFromWorksheet(
                    parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(List.of("header 1", "header2", "header3"));

            List<Property.ValueProperty<?>> propertiesToUpdateRow = MicrosoftExcelUtils
                .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            List<ModifiableObjectProperty> expectedProperties = List.of(
                object(VALUES)
                    .label("Values")
                    .properties(
                        string("header_1")
                            .label("header 1")
                            .defaultValue(""),
                        string("header2")
                            .label("header2")
                            .defaultValue(""),
                        string("header3")
                            .label("header3")
                            .defaultValue(""))
                    .required(true));

            assertEquals(expectedProperties, propertiesToUpdateRow);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsHeaderAndUpdatingSelectedColumns() throws Exception {
        mockedParameters = MockParametersFactory.create(
            Map.of(IS_THE_FIRST_ROW_HEADER, true, UPDATE_WHOLE_ROW, false, WORKBOOK_ID, "workbookId", WORKSHEET_NAME,
                "sheetName"));

        try (MockedStatic<MicrosoftExcelRowUtils> microsoftExcelRowUtilsMockedStatic =
            mockStatic(MicrosoftExcelRowUtils.class)) {

            microsoftExcelRowUtilsMockedStatic
                .when(() -> MicrosoftExcelRowUtils.getRowFromWorksheet(
                    parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(List.of("header 1", "header2", "header3"));

            List<Property.ValueProperty<?>> propertiesToUpdateRow = MicrosoftExcelUtils
                .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            List<ModifiableArrayProperty> expectedProperties = List.of(
                array(VALUES)
                    .label("Values")
                    .items(
                        object()
                            .properties(
                                string(COLUMN)
                                    .label("Column")
                                    .description("Column to update.")
                                    .options(
                                        option("header 1", "header 1"),
                                        option("header2", "header2"),
                                        option("header3", "header3"))
                                    .required(true),
                                string(VALUE)
                                    .label("Column Value")
                                    .defaultValue("")
                                    .required(true)))
                    .required(true));

            assertEquals(expectedProperties, propertiesToUpdateRow);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
            assertEquals(1, integerArgumentCaptor.getValue());
        }
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsNotHeaderAndUpdatingWholeRow() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false, UPDATE_WHOLE_ROW, true));

        List<Property.ValueProperty<?>> propertiesToUpdateRow = MicrosoftExcelUtils
            .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        List<ModifiableArrayProperty> expectedProperties = List.of(
            array(VALUES)
                .label("Values")
                .items(bool(), number(), string())
                .required(true));

        assertEquals(expectedProperties, propertiesToUpdateRow);
    }

    @Test
    void createPropertiesToUpdateRowWhenFirstRowIsNotHeaderAndUpdatingSelectedColumns() {
        mockedParameters =
            MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false, UPDATE_WHOLE_ROW, false));

        List<Property.ValueProperty<?>> propertiesToUpdateRow = MicrosoftExcelUtils
            .createPropertiesToUpdateRow(mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        List<ModifiableArrayProperty> expectedProperties = List.of(
            array(VALUES)
                .label("Values")
                .items(
                    object()
                        .properties(
                            string(COLUMN)
                                .label("Column Label")
                                .description("Label of the column to update. Example: A, B, C, ...")
                                .exampleValue("A")
                                .required(true),
                            string(VALUE)
                                .label("Column Value")
                                .defaultValue("")
                                .required(true))));

        assertEquals(expectedProperties, propertiesToUpdateRow);
    }

    @Test
    void testGetLastUsedColumnLabel() {
        Map<String, Object> map = Map.of("columnCount", 3);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        String result = MicrosoftExcelUtils.getLastUsedColumnLabel(mockedParameters, mockedActionContext);

        assertEquals("C", result);
    }

    @Test
    void getLastUsedRowIndex() {
        Map<String, Object> map = new HashMap<>();

        map.put("rowCount", 2);
        map.put(VALUES, List.of(List.of("A", "B", "C"), List.of("D", "E", "F")));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Integer result = MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedActionContext);

        assertEquals(2, result);
    }

    @Test
    void testGetLastUsedRowIndexWithEmptySheet() {
        Map<String, Object> map = new HashMap<>();

        map.put("rowCount", 1);
        map.put(VALUES, List.of(List.of()));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Integer result = MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedActionContext);

        assertEquals(0, result);
    }

    @Test
    void testGetLastUsedRowIndexWithOneRow() {
        Map<String, Object> map = new HashMap<>();

        map.put("rowCount", 1);
        map.put(VALUES, List.of(List.of("A", "B")));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        Integer result = MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedActionContext);

        assertEquals(1, result);
    }

    @Test
    void testGetMapOfValuesForRowWhereFirstRowIsHeader() {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, true));
        List<Object> row = Arrays.asList("value1", "value2", "value3");

        try (MockedStatic<MicrosoftExcelRowUtils> rowUtilsMockedStatic = mockStatic(MicrosoftExcelRowUtils.class)) {
            rowUtilsMockedStatic
                .when(() -> MicrosoftExcelRowUtils.getRowFromWorksheet(mockedParameters, mockedActionContext, 1))
                .thenReturn(List.of("header1", "header2", "header3"));

            Map<String, Object> result =
                MicrosoftExcelUtils.getMapOfValuesForRow(mockedParameters, mockedActionContext, row);

            Map<String, Object> expectedValuesMap = new LinkedHashMap<>();

            expectedValuesMap.put("header1", "value1");
            expectedValuesMap.put("header2", "value2");
            expectedValuesMap.put("header3", "value3");

            assertEquals(expectedValuesMap, result);
        }
    }

    @Test
    void testGetMapOfValuesForRowWhereFirstRowIsNotHeader() {
        mockedParameters = MockParametersFactory.create(Map.of(IS_THE_FIRST_ROW_HEADER, false));
        List<Object> row = Arrays.asList("value1", "value2", "value3");

        Map<String, Object> result =
            MicrosoftExcelUtils.getMapOfValuesForRow(mockedParameters, mockedActionContext, row);

        Map<String, Object> expectedValuesMap = new LinkedHashMap<>();

        expectedValuesMap.put("column_A", "value1");
        expectedValuesMap.put("column_B", "value2");
        expectedValuesMap.put("column_C", "value3");

        assertEquals(expectedValuesMap, result);
    }

    @Test
    void testGetRowValuesForMap() {
        Map<String, String> valuesMap = new LinkedHashMap<>();

        valuesMap.put("name", "A");
        valuesMap.put("age", "B");

        when(mockedParameters.getRequiredMap(ROW, Object.class))
            .thenReturn(Map.of(VALUES, valuesMap));

        List<Object> result = MicrosoftExcelUtils.getRowValues(mockedParameters);

        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void testGetRowValuesForList() {
        when(mockedParameters.getRequiredMap(ROW, Object.class))
            .thenReturn(Map.of(VALUES, List.of("A", "B")));

        List<Object> result = MicrosoftExcelUtils.getRowValues(mockedParameters);

        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void testGetWorkbookIdOptions() {
        Map<String, Object> map = Map.of(VALUE, List.of(Map.of(NAME, "abc", ID, "123")));

        when(mockedActionContext.http(any()))
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
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetWorksheetNameOptions() {
        Map<String, Object> map = Map.of(VALUE, List.of(Map.of(NAME, "abc")));

        when(mockedActionContext.http(any()))
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
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(expectedOptions, result);
    }

}
