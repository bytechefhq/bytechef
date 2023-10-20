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

package com.integri.atlas.task.handler.xml.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.xml.XMLHelper;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
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
public class XMLFileTaskHandlerTest {

    private static final FileStorageService fileStorageService = new Base64FileStorageService();
    private static final XMLHelper xmlHelper = new XMLHelper();
    private static final XMLFileTaskHandler xmlFileTaskHandler = new XMLFileTaskHandler(fileStorageService, xmlHelper);

    @Test
    public void testRead() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFile(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");

        assertThat((List<?>) xmlFileTaskHandler.handle(taskExecution))
            .isEqualTo(xmlHelper.deserialize(Files.contentOf(file, Charset.defaultCharset()), List.class));

        taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageService.storeFile(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");
        taskExecution.put("range", Map.of("startIndex", 1, "endIndex", 2));

        assertThat(((List<?>) xmlFileTaskHandler.handle(taskExecution)).size()).isEqualTo(1);
    }

    @Test
    public void testWrite() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("items", xmlHelper.deserialize(Files.contentOf(file, Charset.defaultCharset()), List.class));
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = (FileEntry) xmlFileTaskHandler.handle(taskExecution);

        assertThat(xmlHelper.deserialize(fileStorageService.readFileContent(fileEntry.getUrl()), List.class))
            .isEqualTo(xmlHelper.deserialize(Files.contentOf(file, Charset.defaultCharset()), List.class));

        assertThat(fileEntry.getName()).isEqualTo("file.xml");

        taskExecution.put("fileName", "test.xml");
        taskExecution.put("items", xmlHelper.deserialize(Files.contentOf(file, Charset.defaultCharset()), List.class));
        taskExecution.put("operation", "WRITE");

        fileEntry = (FileEntry) xmlFileTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.xml");
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.xml");

        return classPathResource.getFile();
    }
}
