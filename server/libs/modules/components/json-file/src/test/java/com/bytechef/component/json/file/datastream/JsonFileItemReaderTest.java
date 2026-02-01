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

package com.bytechef.component.json.file.datastream;

import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_ENTRY;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.component.json.file.constant.FileType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JsonFileItemReaderTest {

    private JsonFileItemReader jsonFileItemReader;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private ClusterElementContext context;
    private FileEntry mockFileEntry;

    @BeforeEach
    void setUp() {
        jsonFileItemReader = new JsonFileItemReader();
        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(ClusterElementContext.class);
        mockFileEntry = mock(FileEntry.class);

        when(inputParameters.getRequiredFileEntry(FILE_ENTRY)).thenReturn(mockFileEntry);
    }

    @Test
    void testGetFieldsWithJsonArray() {
        String jsonContent = "[{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}]";

        setupMocks(jsonContent, FileType.JSON.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.stream()
            .map(FieldDefinition::name)
            .toList())
                .containsExactlyInAnyOrder("name", "age", "email");
    }

    @Test
    void testGetFieldsWithJsonl() {
        String jsonlContent =
            "{\"id\":1,\"name\":\"Test\",\"active\":true}\n{\"id\":2,\"name\":\"Demo\",\"active\":false}";

        setupMocks(jsonlContent, FileType.JSONL.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.stream()
            .map(FieldDefinition::name)
            .toList())
                .containsExactlyInAnyOrder("id", "name", "active");
    }

    @Test
    void testGetFieldsWithNestedJson() {
        String jsonContent = "[{\"user\":{\"name\":\"John\",\"email\":\"john@example.com\"},\"status\":\"active\"}]";

        setupMocks(jsonContent, FileType.JSON.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);
        assertThat(fields.stream()
            .map(FieldDefinition::name)
            .toList())
                .containsExactlyInAnyOrder("user.name", "user.email", "status");
    }

    @Test
    void testGetFieldsWithPath() {
        String jsonContent = "{\"data\":[{\"id\":1,\"value\":\"test\"}]}";

        setupMocks(jsonContent, FileType.JSON.name(), "data");

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(2);
        assertThat(fields.stream()
            .map(FieldDefinition::name)
            .toList())
                .containsExactlyInAnyOrder("id", "value");
    }

    @Test
    void testGetFieldsWithEmptyJsonArray() {
        String jsonContent = "[]";

        setupMocks(jsonContent, FileType.JSON.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).isEmpty();
    }

    @Test
    void testGetFieldsWithEmptyJsonlFile() {
        String jsonlContent = "";

        setupMocks(jsonlContent, FileType.JSONL.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).isEmpty();
    }

    @Test
    void testGetFieldsInfersTypeFromValues() {
        String jsonContent = "[{\"stringField\":\"text\",\"numberField\":42,\"booleanField\":true}]";

        setupMocks(jsonContent, FileType.JSON.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(3);

        FieldDefinition stringField = fields.stream()
            .filter(field -> "stringField".equals(field.name()))
            .findFirst()
            .orElseThrow();
        assertThat(stringField.type()).isEqualTo(String.class);

        FieldDefinition numberField = fields.stream()
            .filter(field -> "numberField".equals(field.name()))
            .findFirst()
            .orElseThrow();
        assertThat(numberField.type()).isEqualTo(Integer.class);

        FieldDefinition booleanField = fields.stream()
            .filter(field -> "booleanField".equals(field.name()))
            .findFirst()
            .orElseThrow();
        assertThat(booleanField.type()).isEqualTo(Boolean.class);
    }

    @Test
    void testGetFieldsWithDeeplyNestedJson() {
        String jsonContent = "[{\"level1\":{\"level2\":{\"level3\":\"value\"}}}]";

        setupMocks(jsonContent, FileType.JSON.name(), null);

        List<FieldDefinition> fields = jsonFileItemReader.getFields(inputParameters, connectionParameters, context);

        assertThat(fields).hasSize(1);
        assertThat(fields.getFirst()
            .name()).isEqualTo("level1.level2.level3");
    }

    @SuppressWarnings("unchecked")
    private void setupMocks(String jsonContent, String fileType, String path) {
        when(inputParameters.getString(FILE_TYPE, FileType.JSON.name())).thenReturn(fileType);
        when(inputParameters.getString(PATH)).thenReturn(path);

        when(context.file(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.File, ?> function = invocation.getArgument(0);
            Context.File mockFile = mock(Context.File.class);

            when(mockFile.getInputStream(any(FileEntry.class)))
                .thenReturn(new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8)));

            return function.apply(mockFile);
        });

        when(context.json(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.Json, ?> function = invocation.getArgument(0);
            Context.Json mockJson = mock(Context.Json.class);

            when(mockJson.read(any(String.class))).thenAnswer(readInvocation -> {
                String content = readInvocation.getArgument(0);

                return parseJson(content);
            });

            when(mockJson.read(any(InputStream.class), any(String.class))).thenAnswer(readInvocation -> {
                String pathArg = readInvocation.getArgument(1);
                Map<String, Object> parsed = (Map<String, Object>) parseJson(jsonContent);

                return parsed.get(pathArg);
            });

            when(mockJson.stream(any(InputStream.class))).thenAnswer(streamInvocation -> {
                List<Map<String, ?>> items = (List<Map<String, ?>>) parseJson(jsonContent);

                return items != null ? items.stream() : Stream.empty();
            });

            return function.apply(mockJson);
        });

        when(context.nested(any())).thenAnswer(invocation -> {
            Context.ContextFunction<ClusterElementContext.Nested, ?> function = invocation.getArgument(0);
            ClusterElementContext.Nested mockNested = mock(ClusterElementContext.Nested.class);

            when(mockNested.flatten(any())).thenAnswer(flattenInvocation -> {
                Map<String, Object> map = flattenInvocation.getArgument(0);

                return flattenMap(map);
            });

            return function.apply(mockNested);
        });
    }

    private Map<String, Object> flattenMap(Map<String, Object> map) {
        Map<String, Object> result = new java.util.HashMap<>();

        flattenMapRecursive(map, "", result);

        return result;
    }

    @SuppressWarnings("unchecked")
    private void flattenMapRecursive(Map<String, Object> map, String prefix, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMapRecursive((Map<String, Object>) value, key, result);
            } else {
                result.put(key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object parseJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        content = content.trim();

        if (content.startsWith("[")) {
            return parseJsonArray(content);
        } else if (content.startsWith("{")) {
            return parseJsonObject(content);
        }

        return null;
    }

    private List<Map<String, Object>> parseJsonArray(String content) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                content, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Object> parseJsonObject(String content) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                content, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
