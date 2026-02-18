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

package com.bytechef.component.datastream.processor.datastream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class FieldMapperItemProcessorTest {

    private FieldMapperItemProcessor fieldMapperItemProcessor;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ClusterElementContext context;

    @BeforeEach
    void setUp() {
        fieldMapperItemProcessor = new FieldMapperItemProcessor();
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(ClusterElementContext.class);

        when(context.nested(any())).thenAnswer(invocation -> {
            Context.ContextFunction<ClusterElementContext.Nested, ?> function = invocation.getArgument(0);
            ClusterElementContext.Nested mockNested = mock(ClusterElementContext.Nested.class);

            when(mockNested.containsPath(any(), any())).thenAnswer(containsInvocation -> {
                Map<String, Object> map = containsInvocation.getArgument(0);
                String path = containsInvocation.getArgument(1);

                return containsNestedPath(map, path);
            });

            when(mockNested.getValue(any(), any())).thenAnswer(getInvocation -> {
                Map<String, Object> map = getInvocation.getArgument(0);
                String path = getInvocation.getArgument(1);

                return getNestedValue(map, path);
            });

            doAnswer(setInvocation -> {
                Map<String, Object> map = setInvocation.getArgument(0);
                String path = setInvocation.getArgument(1);
                Object value = setInvocation.getArgument(2);

                setNestedValue(map, path, value);

                return map;
            }).when(mockNested)
                .setValue(any(), any(), any());

            return function.apply(mockNested);
        });
    }

    @SuppressWarnings("unchecked")
    private boolean containsNestedPath(Map<String, Object> map, String path) {
        if (map == null || path == null || path.isEmpty()) {
            return false;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (int i = 0; i < parts.length; i++) {
            if (current instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) current;

                if (i == parts.length - 1) {
                    return currentMap.containsKey(parts[i]);
                }

                current = currentMap.get(parts[i]);

                if (current == null) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String path) {
        if (map == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);

                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        if (map == null || path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);

            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                Map<String, Object> newMap = new HashMap<>();

                current.put(parts[i], newMap);

                current = newMap;
            }
        }

        current.put(parts[parts.length - 1], value);
    }

    @Test
    void testProcessMapsFieldsCorrectly() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "firstName", "destinationField", "first_name"),
            Map.of("sourceField", "lastName", "destinationField", "last_name"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe", "email", "john@example.com");

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals("John", result.get("first_name"));
        assertEquals("Doe", result.get("last_name"));
        assertFalse(result.containsKey("email"), "Unmapped fields should be dropped");
    }

    @Test
    void testProcessUsesDefaultValueForMissingField() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "firstName", "destinationField", "first_name"),
            Map.of("sourceField", "middleName", "destinationField", "middle_name", "defaultValue", "N/A"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> input = Map.of("firstName", "John");

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals("John", result.get("first_name"));
        assertEquals("N/A", result.get("middle_name"));
    }

    @Test
    void testProcessUsesNullForMissingFieldWithNoDefault() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "firstName", "destinationField", "first_name"),
            Map.of("sourceField", "middleName", "destinationField", "middle_name"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> input = Map.of("firstName", "John");

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals("John", result.get("first_name"));
        assertNull(result.get("middle_name"));
    }

    @Test
    void testProcessWithEmptyMappings() throws Exception {
        doReturn(List.of()).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        assertTrue(result.isEmpty());
    }

    @Test
    void testProcessWithNestedSourceField() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "user.name", "destinationField", "name"),
            Map.of("sourceField", "user.email", "destinationField", "email"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> user = Map.of("name", "John", "email", "john@example.com");
        Map<String, Object> input = Map.of("user", user);

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals("John", result.get("name"));
        assertEquals("john@example.com", result.get("email"));
    }

    @Test
    void testProcessWithNestedDestinationField() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "firstName", "destinationField", "user.name"),
            Map.of("sourceField", "userEmail", "destinationField", "user.email"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> input = Map.of("firstName", "John", "userEmail", "john@example.com");

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.get("user");

        assertEquals("John", user.get("name"));
        assertEquals("john@example.com", user.get("email"));
    }

    @Test
    void testProcessWithBothNestedSourceAndDestinationFields() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "source.data.name", "destinationField", "target.info.fullName"),
            Map.of("sourceField", "source.data.age", "destinationField", "target.info.userAge"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> data = Map.of("name", "John", "age", 30);
        Map<String, Object> source = Map.of("data", data);
        Map<String, Object> input = Map.of("source", source);

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) result.get("target");

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) target.get("info");

        assertEquals("John", info.get("fullName"));
        assertEquals(30, info.get("userAge"));
    }

    @Test
    void testProcessWithMissingNestedSourceFieldUsesDefault() throws Exception {
        List<Map<String, Object>> mappings = List.of(
            Map.of("sourceField", "user.name", "destinationField", "name"),
            Map.of("sourceField", "user.email", "destinationField", "email", "defaultValue", "unknown@example.com"));

        doReturn(mappings).when(inputParameters)
            .getList(eq("mappings"), any(TypeReference.class), eq(List.of()));

        Map<String, Object> user = Map.of("name", "John");
        Map<String, Object> input = Map.of("user", user);

        Map<String, Object> result = fieldMapperItemProcessor.process(
            input, inputParameters, connectionParameters, context);

        assertEquals(2, result.size());
        assertEquals("John", result.get("name"));
        assertEquals("unknown@example.com", result.get("email"));
    }
}
