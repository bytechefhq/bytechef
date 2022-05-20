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

package com.integri.atlas.task.handler.json.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.file.storage.base64.service.Base64FileStorageService;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import com.integri.atlas.test.json.JSONArrayUtil;
import com.integri.atlas.test.json.JSONObjectUtil;
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
    private static final JSONFileTaskHandler jsonFileTaskHandler = new JSONFileTaskHandler(
        fileStorageService,
        jsonHelper
    );

    @Test
    @SuppressWarnings("unchecked")
    public void testReadJSON() throws Exception {
        File file = getFile("sample.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("isArray", false);
        taskExecution.put("operation", "READ");

        assertEquals(
            JSONObjectUtil.of(Files.contentOf(file, Charset.defaultCharset())),
            JSONObjectUtil.of((Map<String, ?>) jsonFileTaskHandler.handle(taskExecution)),
            true
        );
    }

    @Test
    public void testReadJSONArray() throws Exception {
        File file = getFile("sample_array.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "READ");

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(file, Charset.defaultCharset())),
            JSONArrayUtil.of((List<?>) jsonFileTaskHandler.handle(taskExecution)),
            true
        );

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "READ");
        taskExecution.put("pageNumber", 1);
        taskExecution.put("pageSize", 2);

        Assertions.assertThat(((List<?>) jsonFileTaskHandler.handle(taskExecution)).size()).isEqualTo(2);
    }

    @Test
    public void testReadJSONL() throws Exception {
        File file = getFile("sample.jsonl");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSONL");
        taskExecution.put("operation", "READ");

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())),
            JSONArrayUtil.of((List<?>) jsonFileTaskHandler.handle(taskExecution)),
            true
        );

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("fileType", "JSONL");
        taskExecution.put("operation", "READ");
        taskExecution.put("pageNumber", 1);
        taskExecution.put("pageSize", 2);

        Assertions.assertThat(((List<?>) jsonFileTaskHandler.handle(taskExecution)).size()).isEqualTo(2);
    }

    @Test
    public void testWriteJSON() throws Exception {
        File file = getFile("sample.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("source", JSONObjectUtil.toMap(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertEquals(
            JSONObjectUtil.of(Files.contentOf(file, Charset.defaultCharset())),
            JSONObjectUtil.of(fileStorageService.readFileContent(fileEntry.getUrl())),
            true
        );

        assertThat(fileEntry.getName()).isEqualTo("file.json");
    }

    @Test
    public void testWriteJSONArray() throws Exception {
        File file = getFile("sample_array.json");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("source", JSONArrayUtil.toList(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("fileType", "JSON");
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(file, Charset.defaultCharset())),
            JSONArrayUtil.of(fileStorageService.readFileContent(fileEntry.getUrl())),
            true
        );

        assertThat(fileEntry.getName()).isEqualTo("file.json");

        taskExecution.put("fileName", "test.json");
        taskExecution.put("fileType", "JSON");
        taskExecution.put("source", JSONArrayUtil.toList(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("operation", "WRITE");

        fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.json");
    }

    @Test
    public void testWriteJSONL() throws Exception {
        File file = getFile("sample.jsonl");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(
            "source",
            JSONArrayUtil.toList(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset()))
        );
        taskExecution.put("fileType", "JSONL");
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertEquals(
            JSONArrayUtil.ofLines(Files.contentOf(file, Charset.defaultCharset())),
            JSONArrayUtil.ofLines(fileStorageService.readFileContent(fileEntry.getUrl())),
            true
        );

        assertThat(fileEntry.getName()).isEqualTo("file.jsonl");

        taskExecution.put("fileName", "test.jsonl");
        taskExecution.put("fileType", "JSONL");
        taskExecution.put(
            "source",
            JSONArrayUtil.toList(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset()))
        );
        taskExecution.put("operation", "WRITE");

        fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.jsonl");
    }

    private File getFile(String filename) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + filename);

        return classPathResource.getFile();
    }
}
