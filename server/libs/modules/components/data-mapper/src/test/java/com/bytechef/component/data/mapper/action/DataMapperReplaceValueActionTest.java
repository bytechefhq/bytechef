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

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.data.mapper.model.ObjectMapping;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Marko Kriskovic
 */
class DataMapperReplaceValueActionTest {

    private Parameters connectionParameters;
    private ActionContext context;
    private Parameters inputParameters;

    @BeforeEach
    public void setUp() {
        connectionParameters = mock(Parameters.class);
        context = mock(ActionContext.class);
        inputParameters = mock(Parameters.class);
    }

    @Test
    void testPerformWithBooleanType() {
        setupAndAssertTestForType(true, false);
    }

    @Test
    void testPerformWithDateType() {
        LocalDate now = LocalDate.now();

        setupAndAssertTestForType(LocalDate.now(), now.plusDays(1));
    }

    @Test
    void testPerformWithDateTimeType() {
        LocalDateTime now = LocalDateTime.now();

        setupAndAssertTestForType(now, now.plusDays(1));
    }

    @Test
    void testPerformWithIntegerType() {
        setupAndAssertTestForType(1, 2);
    }

    @Test
    void testPerformWithNumberType() {
        setupAndAssertTestForType(1.5, 2.5);
    }

    @Test
    void testPerformWithObjectType() {
        setupAndAssertTestForType(new Object(), new Object());
    }

    @Test
    void testPerformWithListType() {
        setupAndAssertTestForType(List.of("item1"), List.of("item2"));
    }

    @Test
    void testPerformWithTimeType() {
        LocalTime now = LocalTime.now();

        setupAndAssertTestForType(now, now.plusHours(1));
    }

    @Test
    void testPerformWithStringType() {
        setupAndAssertTestForType("inputString", "outputString");
    }

    @Test
    void testPerformWithStringTypeRegex() {
        String inputValue = "input value";

        when(inputParameters.getRequiredInteger(TYPE)).thenReturn(9);
        when(inputParameters.getString(VALUE)).thenReturn(inputValue);

        setupAndAssertTest(
            inputValue, " ", "_", result -> assertEquals("input_value", result,
                "The ' ' should be replaced by '_'"));
    }

    @Test
    void testPerformWithStringTypeRegexNotExists() {
        String inputValue = "input value";

        when(inputParameters.getRequiredInteger(TYPE)).thenReturn(9);
        when(inputParameters.getString(VALUE)).thenReturn(inputValue);

        setupAndAssertTest(
            inputValue, "m", "_", result -> assertEquals("input value", result,
                "Result should match the expected output value for type: " + result.getClass()));
    }

    @Test
    void testPerformEmptyMapping() {
        // Setup
        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of())).thenReturn(List.of());
        when(inputParameters.get(eq(VALUE), any())).thenReturn(1);
        when((String) inputParameters.get(eq(DEFAULT_VALUE), any())).thenReturn("defaultValue");

        // Execute
        Object result = DataMapperReplaceValueAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertEquals("defaultValue", result, "Result should be default value due to empty mapping.");
    }

    @Test
    void testPerformDefaultValue() {
        setupAndAssertTest(3, 1, 2, result -> assertEquals("defaultValue", result, "Result should be default value."));
    }

    @Test
    void testPerformWithNotConvertibleType() {
        setupAndAssertTest(
            "1", 1, 2,
            result -> assertEquals(
                "defaultValue", result, "Result should be default value due to not convertible type"));
    }

    private <T> void setupAndAssertTestForType(T inputMapping, T outputMapping) {
        setupAndAssertTest(
            inputMapping, inputMapping, outputMapping, result -> assertEquals(
                outputMapping, result, "Result should match the expected output value for type: " + result.getClass()));
    }

    private <T, V> void setupAndAssertTest(
        V inputValue, T inputMapping, T outputMapping, Consumer<Object> consumer) {

        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of()))
            .thenReturn(List.of(new ObjectMapping(inputMapping, outputMapping)));
        when(inputParameters.get(eq(VALUE), any())).thenReturn(inputValue);
        when((String) inputParameters.get(eq(DEFAULT_VALUE), any())).thenReturn("defaultValue");

        try (MockedStatic<ConvertUtils> convertUtilsMockedStatic = mockStatic(ConvertUtils.class)) {
            convertUtilsMockedStatic.when(() -> ConvertUtils.canConvert(eq(inputMapping), any())).thenReturn(true);
            convertUtilsMockedStatic.when(
                () -> ConvertUtils.convertValue(eq(inputMapping), any())).thenReturn(inputMapping);
            convertUtilsMockedStatic.when(
                () -> ConvertUtils.convertValue(eq(outputMapping), any())).thenReturn(outputMapping);

            Object result = DataMapperReplaceValueAction.perform(inputParameters, connectionParameters, context);

            consumer.accept(result);
        }
    }
}
