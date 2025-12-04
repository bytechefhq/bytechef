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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.DEFAULT_VALUE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.data.mapper.constant.ValueType;
import com.bytechef.component.data.mapper.model.ObjectMapping;
import com.bytechef.component.data.mapper.util.DataMapperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Marko Kriskovic
 */
class DataMapperReplaceValueActionTest {

    private Parameters connectionParameters;
    private ActionContext context;
    private Parameters inputParameters;

    @BeforeEach
    void beforeEach() {
        connectionParameters = mock(Parameters.class);
        context = mock(ActionContext.class);
        inputParameters = mock(Parameters.class);
    }

    @Test
    void testPerformWithArrayType() {
        setupAndAssertTestForType(List.of("item1"), List.of("item2"), "defaultValue", ValueType.ARRAY);
    }

    @Test
    void testPerformWithBooleanType() {
        setupAndAssertTestForType(true, false, "defaultValue", ValueType.BOOLEAN);
    }

    @Test
    void testPerformWithDateType() {
        LocalDate now = LocalDate.now();

        setupAndAssertTestForType(LocalDate.now(), now.plusDays(1), "defaultValue", ValueType.DATE);
    }

    @Test
    void testPerformWithDateTimeType() {
        LocalDateTime now = LocalDateTime.now();

        setupAndAssertTestForType(now, now.plusDays(1), "defaultValue", ValueType.DATE_TIME);
    }

    @Test
    void testPerformWithIntegerType() {
        setupAndAssertTestForType(1, 2, "defaultValue", ValueType.INTEGER);
    }

    @Test
    void testPerformWithNumberType() {
        setupAndAssertTestForType(1.5, 2.5, "defaultValue", ValueType.NUMBER);
    }

    @Test
    void testPerformWithObjectType() {
        setupAndAssertTestForType(Map.of(), Map.of(), "defaultValue", ValueType.OBJECT);
    }

    @Test
    void testPerformWithTimeType() {
        LocalTime now = LocalTime.now();

        setupAndAssertTestForType(now, now.plusHours(1), "defaultValue", ValueType.TIME);
    }

    @Test
    void testPerformWithStringType() {
        setupAndAssertTestForType("inputString", "outputString", "defaultValue", ValueType.STRING);
    }

    @Test
    void testPerformWithStringTypeRegex() {
        String inputValue = "input value";

        when(inputParameters.getString(VALUE)).thenReturn(inputValue);

        setupAndAssertTest(
            inputValue, " ", "_", "defaultValue", ValueType.STRING, result -> assertEquals(
                "input_value", result, "The ' ' should be replaced by '_'"));
    }

    @Test
    void testPerformWithStringTypeRegexNotExists() {
        String inputValue = "input value";

        when(inputParameters.getString(VALUE)).thenReturn(inputValue);

        setupAndAssertTest(
            inputValue, "m", "_", "defaultValue", ValueType.STRING, result -> assertEquals(
                "input value", result, "Result should match the expected output value for type: " + result.getClass()));
    }

    @Test
    void testPerformEmptyMapping() {
        // Setup
        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of())).thenReturn(List.of());
        when(inputParameters.getRequired(eq(TYPE), eq(ValueType.class))).thenReturn(ValueType.INTEGER);
        when(inputParameters.get(eq(VALUE), any())).thenReturn(1);
        when((String) inputParameters.get(eq(DEFAULT_VALUE), any())).thenReturn("defaultValue");

        // Execute
        Object result = DataMapperReplaceValueAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertEquals("defaultValue", result, "Result should be default value due to empty mapping.");
    }

    @Test
    void testPerformDefaultValue() {
        setupAndAssertTest(
            3, 1, 2, "defaultValue", ValueType.INTEGER,
            result -> assertEquals("defaultValue", result, "Result should be default value."));
    }

    @Test
    void testPerformWithNotConvertibleType() {
        setupAndAssertTest(
            "1", 1, 2, "defaultValue", ValueType.INTEGER,
            result -> assertEquals(
                "defaultValue", result, "Result should be default value due to not convertible type"));
    }

    private <T> void setupAndAssertTestForType(T inputMapping, T outputMapping, T defaultValue, ValueType type) {
        setupAndAssertTest(
            inputMapping, inputMapping, outputMapping, defaultValue, type, result -> assertEquals(
                outputMapping, result, "Result should match the expected output value for type: " + result.getClass()));
    }

    private <T, V> void setupAndAssertTest(
        V inputValue, T inputMapping, T outputMapping, T defaultValue, ValueType type, Consumer<Object> consumer) {

        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of()))
            .thenReturn(List.of(new ObjectMapping(inputMapping, outputMapping)));
        when(inputParameters.get(eq(VALUE), any())).thenReturn(inputValue);
        when(inputParameters.getString(eq(VALUE))).thenReturn(inputValue.toString());
        when(inputParameters.get(eq(DEFAULT_VALUE), any())).thenReturn(defaultValue);
        when(inputParameters.getRequired(eq(TYPE), eq(ValueType.class))).thenReturn(type);

        try (MockedStatic<DataMapperUtils> dataMapperUtilsMockedStatic = mockStatic(
            DataMapperUtils.class, Mockito.CALLS_REAL_METHODS)) {

            dataMapperUtilsMockedStatic
                .when(() -> DataMapperUtils.canConvert(any(), any(), any()))
                .thenReturn(true);
            dataMapperUtilsMockedStatic
                .when(() -> DataMapperUtils.convertFrom(any(), any(), any()))
                .thenReturn(inputMapping);
            dataMapperUtilsMockedStatic
                .when(() -> DataMapperUtils.convertTo(any(), any(), any()))
                .thenReturn(outputMapping);

            Object result = DataMapperReplaceValueAction.perform(inputParameters, connectionParameters, context);

            consumer.accept(result);
        }
    }
}
