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
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.data.mapper.constant.InputType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 */
class DataMapperMapObjectsToArrayActionTest {

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
    void testPerformWithStringTypeObject() {
        setupAndAssertTestForType("inputString", false);
    }

    @Test
    void testPerformWithBooleanTypeObject() {
        setupAndAssertTestForType(true, false);
    }

    @Test
    void testPerformWithDateTypeObject() {
        setupAndAssertTestForType(LocalDate.now(), false);
    }

    @Test
    void testPerformWithDateTimeTypeObject() {
        setupAndAssertTestForType(LocalDateTime.now(), false);
    }

    @Test
    void testPerformWithIntegerTypeObject() {
        setupAndAssertTestForType(1, false);
    }

    @Test
    void testPerformWithNumberTypeObject() {
        setupAndAssertTestForType(1.5, false);
    }

    @Test
    void testPerformWithObjectTypeObject() {
        Object inputObject = new Object();

        setupAndAssertTestForType(inputObject, false);
    }

    @Test
    void testPerformWithListTypeObject() {
        setupAndAssertTestForType(List.of("item1"), false);
    }

    @Test
    void testPerformWithTimeTypeObject() {
        setupAndAssertTestForType(LocalTime.now(), false);
    }

    @Test
    void testPerformWithStringTypeArray() {
        setupAndAssertTestForType("inputString", true);
    }

    @Test
    void testPerformWithBooleanTypeArray() {
        setupAndAssertTestForType(true, true);
    }

    @Test
    void testPerformWithDateTypeArray() {
        setupAndAssertTestForType(LocalDate.now(), true);
    }

    @Test
    void testPerformWithDateTimeTypeArray() {
        setupAndAssertTestForType(LocalDateTime.now(), true);
    }

    @Test
    void testPerformWithIntegerTypeArray() {
        setupAndAssertTestForType(1, true);
    }

    @Test
    void testPerformWithNumberTypeArray() {
        setupAndAssertTestForType(1.5, true);
    }

    @Test
    void testPerformWithObjectTypeArray() {
        Object inputObject = new Object();

        setupAndAssertTestForType(inputObject, true);
    }

    @Test
    void testPerformWithListTypeArray() {
        setupAndAssertTestForType(List.of("item1"), true);
    }

    @Test
    void testPerformWithTimeTypeArray() {
        setupAndAssertTestForType(LocalTime.now(), true);
    }

    @Test
    void testPerformWithTypeNullObject() {
        setupAndAssertTestForType(null, false);
    }

    @Test
    void testPerformWithTypeNullArray() {
        setupAndAssertTestForType(null, true);
    }

    private <T> void setupAndAssertTestForType(T input, boolean isArray) {
        Map<String, Object> map = new HashMap<>();

        map.put("key", input);

        if (isArray) {
            setupAndAssertTest(
                List.of(map),
                (List<?> result) -> {
                    assertEquals(
                        "key", ((Map<?, ?>) result.getFirst()).get("fieldKey"),
                        "'fieldKey' value in result with should be 'key'.");
                    assertEquals(
                        input, ((Map<?, ?>) result.getFirst()).get("fieldValue"),
                        "'fieldValue' value in result with should match the expected key input value.");
                });
        } else {
            setupAndAssertTest(
                map,
                (List<?> result) -> {
                    assertEquals(
                        "key", ((Map<?, ?>) result.getFirst()).get("fieldKey"),
                        "'fieldKey' value in result with should 'key'.");
                    assertEquals(
                        input, ((Map<?, ?>) result.getFirst()).get("fieldValue"),
                        "'fieldValue' value in result with should match the expected key output value.");
                });
        }
    }

    @Test
    void testPerformReplaceMultipleValuesObject() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("key1", "value1");
        map.put("key2", "value2");

        setupAndAssertTest(map,
            (List<?> result) -> {
                assertEquals(
                    "key1", ((Map<?, ?>) result.getFirst()).get("fieldKey"),
                    "'fieldKey' value in result with should be 'key1'.");
                assertEquals(
                    "value1", ((Map<?, ?>) result.getFirst()).get("fieldValue"),
                    "'fieldValue' value in result with should be 'value1.");
                assertEquals(
                    "key2", ((Map<?, ?>) result.get(1)).get("fieldKey"),
                    "'fieldKey' value in result with should be 'key2'.");
                assertEquals(
                    "value2", ((Map<?, ?>) result.get(1)).get("fieldValue"),
                    "'fieldValue' value in result with should be 'value2'.");
            });
    }

    @Test
    void testPerformReplaceMultipleValuesArray() {
        setupAndAssertTest(
            List.of(Map.of("key1", "value1"), Map.of("key2", "value2")),
            (List<?> result) -> {
                assertEquals(
                    "key1", ((Map<?, ?>) result.getFirst()).get("fieldKey"),
                    "'fieldKey' value in result with should be 'key1'.");
                assertEquals(
                    "value1", ((Map<?, ?>) result.getFirst()).get("fieldValue"),
                    "'fieldValue' value in result with should be 'value1.");
                assertEquals(
                    "key2", ((Map<?, ?>) result.get(1)).get("fieldKey"),
                    "'fieldKey' value in result with should be 'key2'.");
                assertEquals(
                    "value2", ((Map<?, ?>) result.get(1)).get("fieldValue"),
                    "'fieldValue' value in result with should be 'value2'.");
            });
    }

    private void setupAndAssertTest(List<Object> inputValue, Consumer<List<?>> consumer) {
        when(inputParameters.getRequiredString(FIELD_KEY)).thenReturn("fieldKey");
        when(inputParameters.getRequiredString(VALUE_KEY)).thenReturn("fieldValue");
        when(inputParameters.getList(INPUT, Object.class, List.of())).thenReturn(inputValue);
        when(inputParameters.get(INPUT_TYPE, InputType.class)).thenReturn(InputType.ARRAY);

        List<Map<String, Object>> result = DataMapperMapObjectsToArrayAction.perform(
            inputParameters, connectionParameters, context);

        consumer.accept(result);
    }

    private void setupAndAssertTest(Map<String, Object> inputValue, Consumer<List<?>> consumer) {
        when(inputParameters.getRequiredString(FIELD_KEY)).thenReturn("fieldKey");
        when(inputParameters.getRequiredString(VALUE_KEY)).thenReturn("fieldValue");
        when(inputParameters.getMap(INPUT, Object.class, Map.of())).thenReturn(inputValue);
        when(inputParameters.get(INPUT_TYPE, InputType.class)).thenReturn(InputType.OBJECT);

        List<Map<String, Object>> result = DataMapperMapObjectsToArrayAction.perform(
            inputParameters, connectionParameters, context);

        consumer.accept(result);
    }
}
