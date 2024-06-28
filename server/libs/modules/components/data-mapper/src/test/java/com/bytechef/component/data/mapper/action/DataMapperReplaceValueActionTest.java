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
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.data.mapper.util.mapping.ObjectMapping;
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

class DataMapperReplaceValueActionTest {
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ActionContext context;

    @BeforeEach
    public void setUp() {
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(ActionContext.class);
    }

    @Test
    void testPerformWithStringType() {
        setupAndAssertTestForType("inputString", "outputString");
    }

    @Test
    void testPerformWithBooleanType() {
        setupAndAssertTestForType(true, false);
    }

    @Test
    void testPerformWithDateType() {
        setupAndAssertTestForType(LocalDate.now(), LocalDate.now()
            .plusDays(1));
    }

    @Test
    void testPerformWithDateTimeType() {
        setupAndAssertTestForType(LocalDateTime.now(), LocalDateTime.now()
            .plusDays(1));
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
        Object inputObject = new Object();
        Object outputObject = new Object();
        setupAndAssertTestForType(inputObject, outputObject);
    }

    @Test
    void testPerformWithListType() {
        setupAndAssertTestForType(List.of("item1"), List.of("item2"));
    }

    @Test
    void testPerformWithTimeType() {
        setupAndAssertTestForType(LocalTime.now(), LocalTime.now()
            .plusHours(1));
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
        assertEquals("defaultValue", result, "Result should be defalut value due to empty mapping.");
    }

    @Test
    void testPerformDefaultValue() {
        setupAndAssertTest(3, 1, 2, result -> assertEquals("defaultValue", result, "Result should be defalut value."));
    }

    @Test
    void testPerformWithUnconvertibleType() {
        setupAndAssertTest("1", 1, 2,
            result -> assertEquals("defaultValue", result, "Result should be defalut value due to unconvertible type"));
    }

    private <T> void setupAndAssertTestForType(T inputMapping, T outputMapping) {
        setupAndAssertTest(inputMapping, inputMapping, outputMapping, result -> assertEquals(outputMapping, result,
            "Result should match the expected output value for type: " + result.getClass()));
    }

    private <T, V> void setupAndAssertTest(V inputValue, T inputMapping, T outputMapping, Consumer<Object> consumer) {
        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of())).thenReturn(List.of(new ObjectMapping(inputMapping, outputMapping)));
        when(inputParameters.get(eq(VALUE), any())).thenReturn(inputValue);
        when((String) inputParameters.get(eq(DEFAULT_VALUE), any())).thenReturn("defaultValue");
        try (MockedStatic<ConvertUtils> convertUtilsMockedStatic = mockStatic(ConvertUtils.class)) {
            convertUtilsMockedStatic.when(() -> ConvertUtils.canConvert(eq(inputValue), any())).thenReturn(true);
            convertUtilsMockedStatic.when(() -> ConvertUtils.convertValue(eq(inputMapping), any())).thenReturn(inputMapping);
            convertUtilsMockedStatic.when(() -> ConvertUtils.convertValue(eq(outputMapping), any())).thenReturn(outputMapping);

            Object result = DataMapperReplaceValueAction.perform(inputParameters, connectionParameters, context);

            consumer.accept(result);
        }
    }
}
