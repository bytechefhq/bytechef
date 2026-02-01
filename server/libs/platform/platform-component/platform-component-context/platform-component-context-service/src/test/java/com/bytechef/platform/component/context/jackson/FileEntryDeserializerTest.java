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

package com.bytechef.platform.component.context.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.FileEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * @author Ivica Cardic
 */
class FileEntryDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SimpleModule module = new SimpleModule();

        module.addDeserializer(FileEntry.class, new FileEntryDeserializer());

        objectMapper = JsonMapper.builder()
            .addModule(module)
            .build();
    }

    @Test
    void testDeserializeWithAllFields() throws Exception {
        String json = """
            {
                "name": "test-file.csv",
                "url": "https://example.com/files/test-file.csv",
                "extension": "csv",
                "mimeType": "text/csv"
            }
            """;

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNotNull(fileEntry);
        assertEquals("test-file.csv", fileEntry.getName());
        assertEquals("https://example.com/files/test-file.csv", fileEntry.getUrl());
        assertEquals("csv", fileEntry.getExtension());
        assertEquals("text/csv", fileEntry.getMimeType());
    }

    @Test
    void testDeserializeWithRequiredFieldsOnly() throws Exception {
        String json = """
            {
                "name": "document.pdf",
                "url": "file:///tmp/document.pdf"
            }
            """;

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNotNull(fileEntry);
        assertEquals("document.pdf", fileEntry.getName());
        assertEquals("file:///tmp/document.pdf", fileEntry.getUrl());
        assertNull(fileEntry.getExtension());
        assertNull(fileEntry.getMimeType());
    }

    @Test
    void testDeserializeWithMissingNameReturnsNull() throws Exception {
        String json = """
            {
                "url": "https://example.com/files/test.txt",
                "extension": "txt"
            }
            """;

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNull(fileEntry);
    }

    @Test
    void testDeserializeWithMissingUrlReturnsNull() throws Exception {
        String json = """
            {
                "name": "test-file.csv",
                "extension": "csv"
            }
            """;

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNull(fileEntry);
    }

    @Test
    void testDeserializeWithEmptyObjectReturnsNull() throws Exception {
        String json = "{}";

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNull(fileEntry);
    }

    @Test
    void testDeserializeWithNullNameFieldTreatsAsEmptyString() throws Exception {
        String json = """
            {
                "name": null,
                "url": "https://example.com/file.txt"
            }
            """;

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNotNull(fileEntry);
        assertEquals("", fileEntry.getName());
        assertEquals("https://example.com/file.txt", fileEntry.getUrl());
    }

    @Test
    void testDeserializeWithNullUrlFieldTreatsAsEmptyString() throws Exception {
        String json = """
            {
                "name": "test.txt",
                "url": null
            }
            """;

        FileEntry fileEntry = objectMapper.readValue(json, FileEntry.class);

        assertNotNull(fileEntry);
        assertEquals("test.txt", fileEntry.getName());
        assertEquals("", fileEntry.getUrl());
    }
}
