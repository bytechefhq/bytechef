/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.handler.jsonfile.v1_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.commons.json.JSONHelper;
import com.bytechef.test.support.json.JSONArrayUtils;
import com.bytechef.test.support.json.JSONObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class JSONFileTaskHandlerTest {

    private static final FileStorageService fileStorageService = new Base64FileStorageService();
    private static final JSONHelper jsonHelper = new JSONHelper(new ObjectMapper());
    private static final JSONFileTaskHandler.JSONFileReadTaskHandler jsonFileReadTaskHandler =
            new JSONFileTaskHandler.JSONFileReadTaskHandler(fileStorageService, jsonHelper);
    private static final JSONFileTaskHandler.JSONFileWriteTaskHandler jsonFileWriteTaskHandler =
            new JSONFileTaskHandler.JSONFileWriteTaskHandler(fileStorageService, jsonHelper);

    @Test
    @SuppressWarnings("unchecked")
    public void testReadJSON() throws Exception {
        File file = getFile("sample.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("isArray", false);
        taskExecution.put("operation", "READ");

        assertEquals(
                JSONObjectUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JSONObjectUtils.of((Map<String, ?>) jsonFileReadTaskHandler.handle(taskExecution)),
                true);
    }

    @Test
    public void testReadJSONArray() throws Exception {
        File file = getFile("sample_array.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "READ");

        assertEquals(
                JSONArrayUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JSONArrayUtils.of((List<?>) jsonFileReadTaskHandler.handle(taskExecution)),
                true);

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "READ");
        taskExecution.put("pageNumber", 1);
        taskExecution.put("pageSize", 2);

        Assertions.assertThat(((List<?>) jsonFileReadTaskHandler.handle(taskExecution)).size())
                .isEqualTo(2);
    }

    @Test
    public void testReadJSONL() throws Exception {
        File file = getFile("sample.jsonl");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSONL");
        taskExecution.put("operation", "READ");

        assertEquals(
                JSONArrayUtils.of(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())),
                JSONArrayUtils.of((List<?>) jsonFileReadTaskHandler.handle(taskExecution)),
                true);

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSONL");
        taskExecution.put("operation", "READ");
        taskExecution.put("pageNumber", 1);
        taskExecution.put("pageSize", 2);

        Assertions.assertThat(((List<?>) jsonFileReadTaskHandler.handle(taskExecution)).size())
                .isEqualTo(2);
    }

    @Test
    public void testWriteJSON() throws Exception {
        File file = getFile("sample.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("source", JSONObjectUtils.toMap(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = jsonFileWriteTaskHandler.handle(taskExecution);

        assertEquals(
                JSONObjectUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JSONObjectUtils.of(fileStorageService.readFileContent(fileEntry.getUrl())),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.json");
    }

    @Test
    public void testWriteJSONArray() throws Exception {
        File file = getFile("sample_array.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("source", JSONArrayUtils.toList(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = jsonFileWriteTaskHandler.handle(taskExecution);

        assertEquals(
                JSONArrayUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JSONArrayUtils.of(fileStorageService.readFileContent(fileEntry.getUrl())),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.json");

        taskExecution.put("fileName", "test.json");
        taskExecution.put("fileType", "JSON");
        taskExecution.put("source", JSONArrayUtils.toList(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("operation", "WRITE");

        fileEntry = jsonFileWriteTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.json");
    }

    @Test
    public void testWriteJSONL() throws Exception {
        File file = getFile("sample.jsonl");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(
                "source",
                JSONArrayUtils.toList(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())));
        taskExecution.put("fileType", "JSONL");
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = jsonFileWriteTaskHandler.handle(taskExecution);

        assertEquals(
                JSONArrayUtils.ofLines(Files.contentOf(file, Charset.defaultCharset())),
                JSONArrayUtils.ofLines(fileStorageService.readFileContent(fileEntry.getUrl())),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.jsonl");

        taskExecution.put("fileName", "test.jsonl");
        taskExecution.put("fileType", "JSONL");
        taskExecution.put(
                "source",
                JSONArrayUtils.toList(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())));
        taskExecution.put("operation", "WRITE");

        fileEntry = jsonFileWriteTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.jsonl");
    }

    private File getFile(String filename) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + filename);

        return classPathResource.getFile();
    }
}
