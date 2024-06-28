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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.data.mapper.util.mapping.ObjectMapping;
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

class DataMapperReplaceAllSpecifiedValuesActionTest {
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
    void testPerformWithStringTypeObject() {
        setupAndAssertTestForType("inputString", "outputString", false);
    }

    @Test
    void testPerformWithBooleanTypeObject() {
        setupAndAssertTestForType(true, false, false);
    }

    @Test
    void testPerformWithDateTypeObject() {
        setupAndAssertTestForType(LocalDate.now(), LocalDate.now()
            .plusDays(1), false);
    }

    @Test
    void testPerformWithDateTimeTypeObject() {
        setupAndAssertTestForType(LocalDateTime.now(), LocalDateTime.now()
            .plusDays(1), false);
    }

    @Test
    void testPerformWithIntegerTypeObject() {
        setupAndAssertTestForType(1, 2, false);
    }

    @Test
    void testPerformWithNumberTypeObject() {
        setupAndAssertTestForType(1.5, 2.5, false);
    }

    @Test
    void testPerformWithObjectTypeObject() {
        Object inputObject = new Object();
        Object outputObject = new Object();
        setupAndAssertTestForType(inputObject, outputObject, false);
    }

    @Test
    void testPerformWithListTypeObject() {
        setupAndAssertTestForType(List.of("item1"), List.of("item2"), false);
    }

    @Test
    void testPerformWithTimeTypeObject() {
        setupAndAssertTestForType(LocalTime.now(), LocalTime.now()
            .plusHours(1), false);
    }

    @Test
    void testPerformWithStringTypeArray() {
        setupAndAssertTestForType("inputString", "outputString", true);
    }

    @Test
    void testPerformWithBooleanTypeArray() {
        setupAndAssertTestForType(true, false, true);
    }

    @Test
    void testPerformWithDateTypeArray() {
        setupAndAssertTestForType(LocalDate.now(), LocalDate.now()
            .plusDays(1), true);
    }

    @Test
    void testPerformWithDateTimeTypeArray() {
        setupAndAssertTestForType(LocalDateTime.now(), LocalDateTime.now()
            .plusDays(1), true);
    }

    @Test
    void testPerformWithIntegerTypeArray() {
        setupAndAssertTestForType(1, 2, true);
    }

    @Test
    void testPerformWithNumberTypeArray() {
        setupAndAssertTestForType(1.5, 2.5, true);
    }

    @Test
    void testPerformWithObjectTypeArray() {
        Object inputObject = new Object();
        Object outputObject = new Object();
        setupAndAssertTestForType(inputObject, outputObject, true);
    }

    @Test
    void testPerformWithListTypeArray() {
        setupAndAssertTestForType(List.of("item1"), List.of("item2"), true);
    }

    @Test
    void testPerformWithTimeTypeArray() {
        setupAndAssertTestForType(LocalTime.now(), LocalTime.now()
            .plusHours(1), true);
    }

    @Test
    void testPerformChangeToNullObject() {
        setupAndAssertTestForType(1.5, null, false);
    }

    @Test
    void testPerformChangeToNullArray() {
        setupAndAssertTestForType(1.5, null, true);
    }

    @Test
    void testPerformChangeFromNullObject() {
        setupAndAssertTestForType(null, 1.5, false);
    }

    @Test
    void testPerformChangeFromNullArray() {
        setupAndAssertTestForType(null, 1.5, true);
    }

    private <T> void setupAndAssertTestForType(T inputMapping, T outputMapping, boolean isArray) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", inputMapping);

        if (isArray) {
            setupAndAssertTest(List.of(map),
                List.of(new ObjectMapping(inputMapping, outputMapping)),
                (List<?> result) -> assertEquals(outputMapping,
                    ((Map<?, ?>) result.getFirst()).get("key"),
                    "Value in result should match the expected output value."));
        } else {
            setupAndAssertTest(map,
                List.of(new ObjectMapping(inputMapping, outputMapping)),
                result -> assertEquals(outputMapping,
                    ((Map<?, ?>) result).get("key"),
                    "Value in result should match the expected output value."));
        }
    }

    @Test
    void testPerformEmptyMappingObject() {
        Map<String, Object> map = Map.of("key", "value");
        setupAndAssertTest(map, List.of(),
            result -> assertEquals(map, result, "Result should be the same as input."));
    }

    @Test
    void testPerformEmptyMappingArray() {
        List<Object> list = List.of(Map.of("key", "value"));
        setupAndAssertTest(list, List.of(),
            (List<?> result) -> assertEquals(list, result, "Result should be the same as input."));
    }

    @Test
    void testPerformMultipleMappingObject() {
        setupAndAssertTest(Map.of("key1", 1, "key2", 2),
            List.of(new ObjectMapping(1, 3), new ObjectMapping(2, 4)),
            result -> {
                assertEquals(3, ((Map<?, ?>) result).get("key1"),
                    "Value of result should match the expected output value.");
                assertEquals(4, ((Map<?, ?>) result).get("key2"),
                    "Value of result should match the expected output value.");
            });
    }

    @Test
    void testPerformMultipleMappingArray() {
        setupAndAssertTest(List.of(Map.of("key1", 1), Map.of("key2", 2)),
            List.of(new ObjectMapping(1, 3), new ObjectMapping(2, 4)),
            result -> {
                assertEquals(3, ((Map<?, ?>) result.getFirst()).get("key1"),
                    "Value of result should match the expected output value.");
                assertEquals(4, ((Map<?, ?>) result.get(1)).get("key2"),
                    "Value of result should match the expected output value.");
            });
    }

    @Test
    void testPerformReplaceMultipleValuesObject() {
        setupAndAssertTest(Map.of("key1", 1, "key2", 1),
            List.of(new ObjectMapping(1, 3)),
            result -> {
                assertEquals(3, ((Map<?, ?>) result).get("key1"),
                    "Value of result should match the expected output value.");
                assertEquals(3, ((Map<?, ?>) result).get("key2"),
                    "Value of result should match the expected output value.");
            });
    }

    @Test
    void testPerformReplaceMultipleValuesArray() {
        setupAndAssertTest(List.of(Map.of("key1", 1), Map.of("key2", 1)),
            List.of(new ObjectMapping(1, 3)),
            result -> {
                assertEquals(3, ((Map<?, ?>) result.getFirst()).get("key1"),
                    "Value of result should match the expected output value.");
                assertEquals(3, ((Map<?, ?>) result.get(1)).get("key2"),
                    "Value of result should match the expected output value.");
            });
    }

    private void setupAndAssertTest(List<Object> inputValue, List<ObjectMapping> mappings, Consumer<List<?>> consumer) {
        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.getList(INPUT, Object.class, List.of())).thenReturn(inputValue);
        when(inputParameters.getInteger(INPUT_TYPE)).thenReturn(2);

        Object result = DataMapperReplaceAllSpecifiedValuesAction.perform(inputParameters, connectionParameters, context);

        consumer.accept((List<?>)result);
    }

    private void setupAndAssertTest(Map<String, Object> inputValue,  List<ObjectMapping> mappings, Consumer<Object> consumer) {
        when(inputParameters.getList(MAPPINGS, ObjectMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.getMap(INPUT, Object.class, Map.of())).thenReturn(inputValue);
        when(inputParameters.getInteger(INPUT_TYPE)).thenReturn(1);

        Object result = DataMapperReplaceAllSpecifiedValuesAction.perform(inputParameters, connectionParameters, context);

        consumer.accept(result);
    }

}
