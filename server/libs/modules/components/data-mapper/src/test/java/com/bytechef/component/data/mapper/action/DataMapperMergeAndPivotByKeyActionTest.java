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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_KEY;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_VALUE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 */
class DataMapperMergeAndPivotByKeyActionTest {

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
    void testPerformWithStringType() {
        setupAndAssertTestForType("inputString");
    }

    @Test
    void testPerformWithBooleanType() {
        setupAndAssertTestForType(true);
    }

    @Test
    void testPerformWithDateType() {
        setupAndAssertTestForType(LocalDate.now());
    }

    @Test
    void testPerformWithDateTimeType() {
        setupAndAssertTestForType(LocalDateTime.now());
    }

    @Test
    void testPerformWithIntegerType() {
        setupAndAssertTestForType(1);
    }

    @Test
    void testPerformWithNumberType() {
        setupAndAssertTestForType(1.5);
    }

    @Test
    void testPerformWithObjectType() {
        setupAndAssertTestForType(new Object());
    }

    @Test
    void testPerformWithListType() {
        setupAndAssertTestForType(List.of("item1"));
    }

    @Test
    void testPerformWithTimeType() {
        setupAndAssertTestForType(LocalTime.now());
    }

    @Test
    void testPerformWithTypeNull() {
        setupAndAssertTestForType(null);
    }

    private <T> void setupAndAssertTestForType(T input) {
        Map<String, Object> map = new HashMap<>();

        map.put("key", input);

        setupAndAssertTest(
            List.of(map),
            result -> {
                Map<Object, Object> key = result.get("key");

                assertTrue(key.containsKey(input), "Result should contain input as key");
                assertEquals("value", key.get(input), "Value in result with input key should be 'value'.");
            });
    }

    @Test
    void testPerformEmptyList() {
        setupAndAssertTest(List.of(),
            result -> {
                Map<Object, Object> key = result.get("key");

                assertTrue(key.isEmpty(), "Result should be empty.");
            });
    }

    @Test
    void testPerformReplaceNotSameKey() {
        setupAndAssertTest(List.of(Map.of("key1", "value")),
            result -> {
                Map<Object, Object> key = result.get("key");

                assertFalse(key.containsKey("value"), "Result should not contain 'value' as key");
            });
    }

    @Test
    void testPerformReplaceMultipleValues() {
        setupAndAssertTest(List.of(Map.of("key", "value1"), Map.of("key", "value2")),
            result -> {
                Map<Object, Object> key = result.get("key");

                assertTrue(key.containsKey("value1"), "Result should contain 'value1' as key");
                assertEquals("value", key.get("value1"), "Value in result with 'value1' key should be 'value'.");
                assertTrue(key.containsKey("value2"), "Result should contain 'value2' as key");
                assertEquals("value", key.get("value2"), "Value in result with 'value2' key should be 'value'.");
            });
    }

    @Test
    void testPerformReplaceMultipleValuesNotSameKey() {
        setupAndAssertTest(List.of(Map.of("key", "value1"), Map.of("key1", "value2")),
            result -> {
                Map<Object, Object> key = result.get("key");

                assertTrue(key.containsKey("value1"), "Result should contain 'value1' as key");
                assertEquals("value", key.get("value1"), "Value in result with 'value1' key should be 'value'.");
                assertFalse(key.containsKey("value2"), "Result should not contain 'value2' as key");
            });
    }

    private void setupAndAssertTest(List<Object> inputValue, Consumer<Map<String, Map<Object, Object>>> consumer) {
        when(inputParameters.getRequiredString(FIELD_KEY)).thenReturn("key");
        when(inputParameters.getRequiredString(FIELD_VALUE)).thenReturn("value");
        when(inputParameters.getList(INPUT, Object.class, List.of())).thenReturn(inputValue);

        Map<String, Map<Object, Object>> result = DataMapperMergeAndPivotByKeyAction.perform(
            inputParameters, connectionParameters, context);

        consumer.accept(result);
    }
}
