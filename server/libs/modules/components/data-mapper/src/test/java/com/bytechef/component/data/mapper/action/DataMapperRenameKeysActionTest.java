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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class DataMapperRenameKeysActionTest {

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
        Map<String, String> inputJson = new LinkedHashMap<>();

        inputJson.put("key", "value");

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(List.of());
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertEquals(inputJson, result, "Result should be unchanged when mappings are empty");
    }

    @Test
    void testPerformRenameKeysSingleKey() {
        // Setup
        Map<String, String> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey", "value");

        List<StringMapping> mappings = List.of(new StringMapping("oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertTrue(result.containsKey("newKey"), "Result should contain new key");
        assertFalse(result.containsKey("oldKey"), "Result should not contain old key");
    }

    @Test
    void testPerformRenameKeysMultipleMapping() {
        // Setup
        Map<String, String> inputJson = new LinkedHashMap<>();

        inputJson.put("oldKey1", "value1");
        inputJson.put("oldKey2", "value2");

        List<StringMapping> mappings =
            List.of(new StringMapping("oldKey1", "newKey1"), new StringMapping("oldKey2", "newKey2"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertTrue(result.containsKey("newKey1"), "Result should contain new key");
        assertTrue(result.containsKey("newKey2"), "Result should contain new key");
        assertFalse(result.containsKey("oldKey1"), "Result should not contain old key");
        assertFalse(result.containsKey("oldKey2"), "Result should not contain old key");
    }

    @Test
    void testPerformRenameKeysNestedKey() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("oldKey", "value");

        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("parent", secondJson);

        List<StringMapping> mappings = List.of(new StringMapping("parent.oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertTrue(((Map<?, ?>) result.get("parent")).containsKey("newKey"), "Result should contain new nested key");
        assertFalse(
            ((Map<?, ?>) result.get("parent")).containsKey("oldKey"), "Result should not contain old nested key");
    }

    @Test
    void testPerformRenameKeysNestedKeyMultipleMappings() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("oldKey", "value");

        Map<String, Object> inputJson = new LinkedHashMap<>();

        inputJson.put("parent", secondJson);

        List<StringMapping> mappings = List.of(
            new StringMapping("parent", "father"), new StringMapping("father.oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertTrue(result.containsKey("father"), "Result should contain renamed parent key");
        assertTrue(((Map<?, ?>) result.get("father")).containsKey("newKey"), "Result should contain new nested key");
        assertFalse(
            ((Map<?, ?>) result.get("father")).containsKey("oldKey"), "Result should not contain old nested key");
    }

    @Test
    void testPerformRenameKeysNestedArray() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("oldKey", "value");

        List<Map<String, String>> list = new ArrayList<>();

        list.add(secondJson);

        Map<String, List<Map<String, String>>> inputJson = new LinkedHashMap<>();

        inputJson.put("parent", list);

        List<StringMapping> mappings = List.of(new StringMapping("parent[0].oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertTrue(
            ((Map<?, ?>) ((List<?>) result.get("parent")).getFirst()).containsKey("newKey"),
            "Result should contain new nested key");
        assertFalse(
            ((Map<?, ?>) ((List<?>) result.get("parent")).getFirst()).containsKey("oldKey"),
            "Result should not contain old nested key");
    }

    @Test
    void testPerformRenameKeysNestedArrayMultipleMappings() {
        // Setup
        Map<String, String> secondJson = new LinkedHashMap<>();

        secondJson.put("oldKey", "value");

        List<Map<String, String>> list = new ArrayList<>();

        list.add(secondJson);

        Map<String, List<Map<String, String>>> inputJson = new LinkedHashMap<>();

        inputJson.put("parent", list);

        List<StringMapping> mappings = List.of(
            new StringMapping("parent", "father"), new StringMapping("father[0].oldKey", "newKey"));

        when(inputParameters.getList(MAPPINGS, StringMapping.class, List.of())).thenReturn(mappings);
        when(inputParameters.get(INPUT)).thenReturn(inputJson);

        // Execute
        Map<String, Object> result = DataMapperRenameKeysAction.perform(inputParameters, connectionParameters, context);

        // Verify
        assertTrue(result.containsKey("father"), "Result should contain renamed parent key");
        assertTrue(
            ((Map<?, ?>) ((List<?>) result.get("father")).getFirst()).containsKey("newKey"),
            "Result should contain new nested key");
        assertFalse(
            ((Map<?, ?>) ((List<?>) result.get("father")).getFirst()).containsKey("oldKey"),
            "Result should not contain old nested key");
    }
}
