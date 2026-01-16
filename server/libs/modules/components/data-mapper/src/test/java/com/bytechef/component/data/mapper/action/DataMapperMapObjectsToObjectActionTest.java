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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_EMPTY_STRINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_NULLS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INCLUDE_UNMAPPED;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.data.mapper.constant.InputType;
import com.bytechef.component.data.mapper.model.RequiredStringMapping;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 */
class DataMapperMapObjectsToObjectActionTest {

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
    void testPerformWithEmptyMappingsNullFlagsObject() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key", "value");

        setupAndAssertTest(
            false, inputJson, List.of(),
            (Map<String, Object> result) -> assertTrue(result.isEmpty(), "Result should be empty."));
    }

    @Test
    void testPerformWithEmptyMappingsNullFlagsArray() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key", "value");

        setupAndAssertTest(
            true, inputJson, List.of(),
            (List<Map<String, Object>> result) -> assertTrue(result.isEmpty(), "Result should be empty."));
    }

    @Test
    void testPerformNullFlagsObject() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "value");

        setupAndAssertTest(
            false, inputJson, List.of(new RequiredStringMapping("oldKey", "newKey", false)),
            (Map<String, Object> result) -> {
                assertTrue(result.containsKey("newKey"), "Result should contain new key");
                assertFalse(result.containsKey("oldKey"), "Result should not contain old key");
            });
    }

    @Test
    void testPerformNullFlagsArray() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "value");

        setupAndAssertTest(
            true, inputJson, List.of(new RequiredStringMapping("oldKey", "newKey", false)),
            (List<Map<String, Object>> result) -> {
                assertTrue(result.getFirst()
                    .containsKey("newKey"), "Result should contain new key");
                assertFalse(result.getFirst()
                    .containsKey("oldKey"), "Result should not contain old key");
            });
    }

    @Test
    void testPerformMultipleMappingNullFlagsObject() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey1", "oldValue1");
        inputJson.put("oldKey2", "oldValue2");

        setupAndAssertTest(
            false, inputJson,
            List.of(
                new RequiredStringMapping("oldKey1", "newKey1", false),
                new RequiredStringMapping("oldKey2", "newKey2", false)),
            (Map<String, Object> result) -> {
                assertTrue(result.containsKey("newKey1"), "Result should contain new key1");
                assertFalse(result.containsKey("oldKey1"), "Result should not contain old key1");
                assertTrue(result.containsKey("newKey2"), "Result should contain new key2");
                assertFalse(result.containsKey("oldKey2"), "Result should not contain old key2");
            });
    }

    @Test
    void testPerformMultipleMappingNullFlagsArray() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey1", "oldValue1");
        inputJson.put("oldKey2", "oldValue2");

        setupAndAssertTest(
            true, inputJson,
            List.of(
                new RequiredStringMapping("oldKey1", "newKey1", false),
                new RequiredStringMapping("oldKey2", "newKey2", false)),
            (List<Map<String, Object>> result) -> {
                assertTrue(result.getFirst()
                    .containsKey("newKey1"), "Result should contain new key1");
                assertFalse(result.getFirst()
                    .containsKey("oldKey1"), "Result should not contain old key1");
                assertTrue(result.getFirst()
                    .containsKey("newKey2"), "Result should contain new key2");
                assertFalse(result.getFirst()
                    .containsKey("oldKey2"), "Result should not contain old key2");
            });
    }

    @Test
    void testPerformNestedNullFlagsObject() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("innerKey", "value");
        Map<String, Object> inputJson = Map.of("outerKey", inner);
        setupAndAssertTest(
            false, inputJson, List.of(new RequiredStringMapping("outerKey.innerKey", "newKey", false)),
            (Map<String, Object> result) -> {
                assertTrue(result.containsKey("newKey"), "Result should contain new key");
                assertFalse(result.containsKey("outerKey"), "Result should not contain old key");
                assertFalse(result.containsKey("innerKey"), "Result should not contain old key");
            });
    }

    @Test
    void testPerformNestedNullFlagsArray() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("innerKey", "value");
        Map<String, Object> inputJson = Map.of("outerKey", inner);

        setupAndAssertTest(
            true, inputJson, List.of(new RequiredStringMapping("outerKey.innerKey", "newKey", false)),
            (List<Map<String, Object>> result) -> {
                assertTrue(result.getFirst()
                    .containsKey("newKey"), "Result should contain new key");
                assertFalse(result.getFirst()
                    .containsKey("outerKey"), "Result should not contain old key");
                assertFalse(result.getFirst()
                    .containsKey("innerKey"), "Result should not contain old key");
            });
    }

    @Test
    void testPerformNestedNullFlagsMultipleObjectsArray() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("innerKey", "value");
        List<Object> inputValue = List.of(Map.of("outerKey", inner), Map.of("outerKey", inner));

        setupAndAssertTest(inputValue, List.of(new RequiredStringMapping("outerKey.innerKey", "newKey", false)),
            result -> {
                assertTrue(result.getFirst()
                    .containsKey("newKey"), "Result should contain new key");
                assertFalse(result.getFirst()
                    .containsKey("outerKey"), "Result should not contain old key");
                assertFalse(result.getFirst()
                    .containsKey("innerKey"), "Result should not contain old key");
                assertTrue(result.get(1)
                    .containsKey("newKey"), "Result should contain new key");
                assertFalse(result.get(1)
                    .containsKey("outerKey"), "Result should not contain old key");
                assertFalse(result.get(1)
                    .containsKey("innerKey"), "Result should not contain old key");
            }, null, null, null);
    }

    @Test
    void testPerformNotIncludeNullsNotRequiredObject() {
        testPerformIncludeNulls(false, false, false);
    }

    @Test
    void testPerformNotIncludeNullsRequiredObject() {
        testPerformIncludeNulls(false, false, true);
    }

    @Test
    void testPerformNotIncludeNullsNotRequiredArray() {
        testPerformIncludeNulls(true, false, false);
    }

    @Test
    void testPerformNotIncludeNullsRequiredArray() {
        testPerformIncludeNulls(true, false, true);
    }

    @Test
    void testPerformIncludeNullsNotRequiredObject() {
        testPerformIncludeNulls(false, true, false);
    }

    @Test
    void testPerformIncludeNullsRequiredObject() {
        testPerformIncludeNulls(false, true, true);
    }

    @Test
    void testPerformIncludeNullsNotRequiredArray() {
        testPerformIncludeNulls(true, true, false);
    }

    @Test
    void testPerformIncludeNullsRequiredArray() {
        testPerformIncludeNulls(true, true, true);
    }

    void testPerformIncludeNulls(boolean isArray, boolean includeNulls, boolean isRequired) {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", null);

        List<RequiredStringMapping> requiredStringMappings = List.of(
            new RequiredStringMapping("oldKey", "newKey", isRequired));

        if (!includeNulls) {
            if (isArray) {
                setupAndAssertTest(
                    List.of(inputJson), requiredStringMappings,
                    (List<Map<String, Object>> result) -> assertTrue(result.isEmpty(),
                        "Result should be an empty array"),
                    null, false, null);
            } else {
                setupAndAssertTest(
                    inputJson, requiredStringMappings,
                    (Map<String, Object> result) -> {
                        assertFalse(result.containsKey("newKey"), "Result should not contain new key");
                        assertFalse(result.containsKey("oldKey"), "Result should not contain old key");
                        assertTrue(result.isEmpty(), "Result should be an empty map");
                    },
                    null, false, null);
            }
        } else if (isRequired) {
            assertThrows(
                NullPointerException.class,
                () -> setupAndAssertTest(
                    isArray, inputJson, requiredStringMappings, result -> {}, null, true, null),
                "Required field oldKey cannot be null.");
        } else {
            if (isArray) {
                setupAndAssertTest(
                    List.of(inputJson), requiredStringMappings,
                    (List<Map<String, Object>> result) -> {
                        assertTrue(result.getFirst()
                            .containsKey("newKey"), "Result should contain new key");
                        assertFalse(result.getFirst()
                            .containsKey("oldKey"), "Result should not contain old key");
                    },
                    null, true, null);
            } else {
                setupAndAssertTest(
                    inputJson, requiredStringMappings,
                    (Map<String, Object> result) -> {
                        assertTrue(result.containsKey("newKey"), "Result should contain new key");
                        assertFalse(result.containsKey("oldKey"), "Result should not contain old key");
                    },
                    null, true, null);
            }
        }
    }

    @Test
    void testPerformNotIncludeEmptyStringNotRequiredObject() {
        testPerformIncludeEmptyString(false, false, false);
    }

    @Test
    void testPerformNotIncludeEmptyStringRequiredObject() {
        testPerformIncludeEmptyString(false, false, true);
    }

    @Test
    void testPerformNotIncludeEmptyStringNotRequiredArray() {
        testPerformIncludeEmptyString(true, false, false);
    }

    @Test
    void testPerformNotIncludeEmptyStringRequiredArray() {
        testPerformIncludeEmptyString(true, false, true);
    }

    @Test
    void testPerformIncludeEmptyStringNotRequiredObject() {
        testPerformIncludeEmptyString(false, true, false);
    }

    @Test
    void testPerformIncludeEmptyStringRequiredObject() {
        testPerformIncludeEmptyString(false, true, true);
    }

    @Test
    void testPerformIncludeEmptyStringNotRequiredArray() {
        testPerformIncludeEmptyString(true, true, false);
    }

    @Test
    void testPerformIncludeEmptyStringRequiredArray() {
        testPerformIncludeEmptyString(true, true, true);
    }

    void testPerformIncludeEmptyString(boolean isArray, boolean includeEmpty, boolean isRequired) {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", null);

        List<RequiredStringMapping> requiredStringMappings = List.of(
            new RequiredStringMapping("oldKey", "newKey", isRequired));

        if (!includeEmpty) {
            if (isArray) {
                setupAndAssertTest(
                    List.of(inputJson), requiredStringMappings,
                    (List<Map<String, Object>> result) -> assertTrue(result.isEmpty(),
                        "Result should be an empty array"),
                    null, null, false);
            } else {
                setupAndAssertTest(
                    inputJson, requiredStringMappings,
                    (Map<String, Object> result) -> {
                        assertFalse(result.containsKey("newKey"), "Result should not contain new key");
                        assertFalse(result.containsKey("oldKey"), "Result should not contain old key");
                        assertTrue(result.isEmpty(), "Result should be an empty map");
                    },
                    null, null, false);
            }
        } else if (isRequired) {
            assertThrows(
                NullPointerException.class,
                () -> setupAndAssertTest(
                    isArray, inputJson, requiredStringMappings,
                    result -> {}, null, null, true),
                "Required field oldKey cannot be empty.");
        } else {
            if (isArray) {
                setupAndAssertTest(
                    List.of(inputJson), requiredStringMappings,
                    (List<Map<String, Object>> result) -> {
                        assertTrue(result.getFirst()
                            .containsKey("newKey"), "Result should contain new key");
                        assertFalse(result.getFirst()
                            .containsKey("oldKey"), "Result should not contain old key");
                    },
                    null, true, null);
            } else {
                setupAndAssertTest(
                    inputJson, requiredStringMappings,
                    (Map<String, Object> result) -> {
                        assertTrue(result.containsKey("newKey"), "Result should contain new key");
                        assertFalse(result.containsKey("oldKey"), "Result should not contain old key");
                    },
                    null, true, null);
            }
        }
    }

    @Test
    void testPerformIncludeUnmappedObject() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key", null);

        setupAndAssertTest(
            false, inputJson, List.of(new RequiredStringMapping("oldKey", "newKey", false)),
            (Map<String, Object> result) -> assertTrue(result.containsKey("key"), "Result should contain new key"),
            true, null, null);
    }

    @Test
    void testPerformNotIncludeUnmappedObject() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key", null);

        setupAndAssertTest(
            false, inputJson, List.of(new RequiredStringMapping("oldKey", "newKey", false)),
            (Map<String, Object> result) -> assertFalse(result.containsKey("key"), "Result should contain new key"),
            false, null, null);
    }

    @Test
    void testPerformIncludeUnmappedArray() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key", null);

        setupAndAssertTest(
            true, inputJson, List.of(new RequiredStringMapping("oldKey", "newKey", false)),
            (List<Map<String, Object>> result) -> assertTrue(result.getFirst()
                .containsKey("key"), "Result should contain new key"),
            true, null, null);
    }

    @Test
    void testPerformNotIncludeUnmappedArray() {
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key", null);

        setupAndAssertTest(
            true, inputJson, List.of(new RequiredStringMapping("oldKey", "newKey", false)),
            (List<Map<String, Object>> result) -> assertTrue(result.isEmpty(), "Result should be an empty array"),
            false, null, null);
    }

    private void setupAndAssertTest(
        boolean isArray, Map<String, Object> inputValue, List<RequiredStringMapping> mappings,
        Consumer<?> consumer) {

        if (isArray) {
            setupAndAssertTest(List.of(inputValue), mappings, (Consumer<List<Map<String, Object>>>) consumer, null,
                null, null);
        } else {
            setupAndAssertTest(inputValue, mappings, (Consumer<Map<String, Object>>) consumer, null, null, null);
        }
    }

    private void setupAndAssertTest(
        boolean isArray, Map<String, Object> inputValue, List<RequiredStringMapping> mappings,
        Consumer<?> consumer, Boolean includeUnmapped, Boolean includeNulls,
        Boolean includeEmptyStrings) {

        if (isArray) {
            setupAndAssertTest(
                List.of(inputValue), mappings, (Consumer<List<Map<String, Object>>>) consumer, includeUnmapped,
                includeNulls, includeEmptyStrings);
        } else {
            setupAndAssertTest(
                inputValue, mappings, (Consumer<Map<String, Object>>) consumer, includeUnmapped, includeNulls,
                includeEmptyStrings);
        }
    }

    private void setupAndAssertTest(
        List<Object> inputValue, List<RequiredStringMapping> mappings, Consumer<List<Map<String, Object>>> consumer,
        Boolean includeUnmapped, Boolean includeNulls, Boolean includeEmptyStrings) {

        when(inputParameters.getBoolean(INCLUDE_UNMAPPED)).thenReturn(includeUnmapped);
        when(inputParameters.getBoolean(INCLUDE_NULLS)).thenReturn(includeNulls);
        when(inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS)).thenReturn(includeEmptyStrings);

        when(inputParameters.getList(MAPPINGS, RequiredStringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.getList(INPUT, Object.class, List.of())).thenReturn(inputValue);
        when(inputParameters.getInteger(INPUT_TYPE)).thenReturn(2);

        List<Map<String, Object>> result = (List<Map<String, Object>>) DataMapperMapObjectsToObjectAction.perform(
            inputParameters, connectionParameters, context);

        consumer.accept(result);
    }

    private void setupAndAssertTest(
        Map<String, Object> inputValue, List<RequiredStringMapping> mappings, Consumer<Map<String, Object>> consumer,
        Boolean includeUnmapped, Boolean includeNulls, Boolean includeEmptyStrings) {

        when(inputParameters.getBoolean(INCLUDE_UNMAPPED)).thenReturn(includeUnmapped);
        when(inputParameters.getBoolean(INCLUDE_NULLS)).thenReturn(includeNulls);
        when(inputParameters.getBoolean(INCLUDE_EMPTY_STRINGS)).thenReturn(includeEmptyStrings);

        when(inputParameters.getList(MAPPINGS, RequiredStringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.getMap(INPUT, Object.class, Map.of())).thenReturn(inputValue);
        when(inputParameters.get(INPUT_TYPE, InputType.class)).thenReturn(InputType.OBJECT);

        Map<String, Object> result = (Map<String, Object>) DataMapperMapObjectsToObjectAction.perform(
            inputParameters, connectionParameters, context);

        consumer.accept(result);
    }
}
