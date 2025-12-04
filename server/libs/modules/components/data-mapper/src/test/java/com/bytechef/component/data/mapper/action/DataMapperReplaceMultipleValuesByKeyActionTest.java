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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.OUTPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.data.mapper.model.StringMapping;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 */
class DataMapperReplaceMultipleValuesByKeyActionTest {

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
    void testPerformWithEmptyMappings() {
        // Setup
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("key1", "value1");

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("key1", "value1");

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(List.of());
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals(inputJson, result, "Result should be unchanged when mappings are empty");
    }

    @Test
    void testPerformReplaceValuesBase() {
        // Setup
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "value");

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("newKey", "value2");

        List<StringMapping> mappings = List.of(new StringMapping("oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals("value2", result.get("oldKey"), "Result should contain new value");
    }

    @Test
    void testPerformReplaceValuesMultipleMapping() {
        // Setup
        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey1", "oldValue1");
        inputJson.put("oldKey2", "oldValue2");

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("newKey1", "newValue1");
        outputJson.put("newKey2", "newValue2");

        List<StringMapping> mappings = List.of(
            new StringMapping("oldKey1", "newKey1"), new StringMapping("oldKey2", "newKey2"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result =
            DataMapperReplaceMultipleValuesByKeyAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertEquals("newValue1", result.get("oldKey1"), "Result should contain new value1");
        assertEquals("newValue2", result.get("oldKey2"), "Result should contain new value2");
    }

    @Test
    void testPerformReplaceValuesNestedInputKey() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("oldKey", "oldValue");

        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("parent", secondJson);

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("newKey", "newValue");

        List<StringMapping> mappings = List.of(new StringMapping("parent.oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals(
            "newValue", ((Map<?, ?>) result.get("parent")).get("oldKey"), "Result should contain new nested value");
    }

    @Test
    void testPerformReplaceValuesNestedOutputKey() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("newKey", "newValue");

        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "oldValue");

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("parent", secondJson);

        List<StringMapping> mappings = List.of(new StringMapping("oldKey", "parent.newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals("newValue", result.get("oldKey"), "Result should contain new value");
    }

    @Test
    void testPerformReplaceValuesNestedInputArray() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("oldKey", "oldValue");

        Map<String, List<Map<String, String>>> inputJson = new LinkedHashMap<>();

        inputJson.put("parent", List.of(secondJson));

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("newKey", "newValue");

        List<StringMapping> mappings = List.of(new StringMapping("parent[0].oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals(
            "newValue", ((Map<?, ?>) ((List<?>) result.get("parent")).getFirst()).get("oldKey"),
            "Result should contain new value");
    }

    @Test
    void testPerformReplaceValuesNestedOutputArray() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("newKey", "newValue");

        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "oldValue");

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("parent", List.of(secondJson));

        List<StringMapping> mappings = List.of(new StringMapping("oldKey", "parent[0].newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals("newValue", result.get("oldKey"), "Result should contain new value");
    }

    @Test
    void testPerformReplaceValuesLargeObject() {
        // Setup
        List<Object> largeObject1 = List.of(
            Map.of("prvi", Map.of("podprvi", 1)), "drugi", Map.of("poddrugi", Map.of("podpoddrugi", 2)));
        List<Object> largeObject2 = List.of(
            Map.of("first", Map.of("underFirst", 10)), "second", Map.of("underSecond", Map.of("underunderSecond", 20)));
        List<Object> array = new ArrayList<>();

        array.add(largeObject1);

        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "oldValue");
        inputJson.put("oldLargeKey", array);

        Map<String, Object> outputJson = new LinkedHashMap<>();

        outputJson.put("newKey", "newValue");
        outputJson.put("newLargeKey", List.of(largeObject2));

        List<StringMapping> mappings = List.of(new StringMapping(
            "oldKey", "newLargeKey[0]"), new StringMapping("oldLargeKey[0]", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);
        when(inputParameters.get(OUTPUT)).thenReturn(outputJson);

        // Execute
        Map<String, Object> result = DataMapperReplaceMultipleValuesByKeyAction.perform(
            inputParameters, connectionParameters, context);

        // Verify
        assertEquals("newValue", ((List<?>) result.get("oldLargeKey")).getFirst(), "Result should contain new value");
        assertEquals(largeObject2, result.get("oldKey"), "Result should contain new large value");
    }
}
