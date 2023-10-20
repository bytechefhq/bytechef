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

package com.integri.atlas.task.handler.xml.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.file.storage.base64.service.Base64FileStorageService;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import com.integri.atlas.task.handler.xml.helper.XmlHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class XmlFileTaskHandlerTest {

    private static final JSONHelper jsonHelper = new JSONHelper(new ObjectMapper());
    private static final FileStorageService fileStorageService = new Base64FileStorageService();
    private static final XmlHelper xmlHelper = new XmlHelper();
    private static final XmlFileTaskHandler xmlFileTaskHandler = new XmlFileTaskHandler(
        jsonHelper,
        fileStorageService,
        xmlHelper
    );

    @Test
    @SuppressWarnings("unchecked")
    public void testRead() throws Exception {
        File file = getFile("sample.xml");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("isArray", false);
        taskExecution.put("operation", "READ");

        assertThat((Map<String, ?>) xmlFileTaskHandler.handle(taskExecution))
            .isEqualTo(xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), Map.class));
    }

    @Test
    public void testReadArray() throws Exception {
        File file = getFile("sample_array.xml");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");

        assertThat((List<?>) xmlFileTaskHandler.handle(taskExecution))
            .isEqualTo(xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");
        taskExecution.put("pageNumber", 1);
        taskExecution.put("pageSize", 2);

        assertThat(((List<?>) xmlFileTaskHandler.handle(taskExecution)).size()).isEqualTo(2);
    }

    @Test
    public void testWrite() throws Exception {
        File file = getFile("sample.xml");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("source", xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), Map.class));
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) xmlFileTaskHandler.handle(taskExecution);

        assertThat(xmlHelper.read(fileStorageService.readFileContent(fileEntry.getUrl()), List.class))
            .isEqualTo(xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        assertThat(fileEntry.getName()).isEqualTo("file.xml");

        taskExecution.put("fileName", "test.xml");
        taskExecution.put("source", xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), Map.class));
        taskExecution.put("operation", "WRITE");

        fileEntry = (FileEntry) xmlFileTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.xml");
    }

    @Test
    public void testWriteArray() throws Exception {
        File file = getFile("sample_array.xml");

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("source", xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), List.class));
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) xmlFileTaskHandler.handle(taskExecution);

        assertThat(xmlHelper.read(fileStorageService.readFileContent(fileEntry.getUrl()), List.class))
            .isEqualTo(xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        assertThat(fileEntry.getName()).isEqualTo("file.xml");

        taskExecution.put("fileName", "test.xml");
        taskExecution.put("source", xmlHelper.read(Files.contentOf(file, Charset.defaultCharset()), List.class));
        taskExecution.put("operation", "WRITE");

        fileEntry = (FileEntry) xmlFileTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.xml");
    }

    private File getFile(String filename) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + filename);

        return classPathResource.getFile();
    }
}
