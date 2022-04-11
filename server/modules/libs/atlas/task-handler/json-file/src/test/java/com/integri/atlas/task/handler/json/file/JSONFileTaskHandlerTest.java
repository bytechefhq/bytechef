/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import com.integri.atlas.json.JSONArrayUtil;
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
    public void testRead() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(file, Charset.defaultCharset())),
            JSONArrayUtil.of((List<?>) jsonFileTaskHandler.handle(taskExecution)),
            true
        );

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");
        taskExecution.put("range", Map.of("startIndex", 1, "endIndex", 3));

        Assertions.assertThat(((List<?>) jsonFileTaskHandler.handle(taskExecution)).size()).isEqualTo(2);
    }

    @Test
    public void testWrite() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("items", JSONArrayUtil.toList(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(file, Charset.defaultCharset())),
            JSONArrayUtil.of(fileStorageService.readFileContent(fileEntry.getUrl())),
            true
        );

        assertThat(fileEntry.getName()).isEqualTo("file.json");

        taskExecution.put("fileName", "test.json");
        taskExecution.put("items", JSONArrayUtil.toList(Files.contentOf(file, Charset.defaultCharset())));
        taskExecution.put("operation", "WRITE");

        fileEntry = (FileEntry) jsonFileTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.json");
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.json");

        return classPathResource.getFile();
    }
}
