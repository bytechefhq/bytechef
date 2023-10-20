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

package com.integri.atlas.task.handler.local.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FilenameUtils;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class LocalFileTaskHandlerTest {

    private static final FileStorageService fileStorageService = new Base64FileStorageService();
    private static final LocalFileTaskHandler localFileTaskHandler = new LocalFileTaskHandler(fileStorageService);

    @Test
    public void testReadOperation() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = getSimpleTaskExecution(file.getAbsolutePath(), "READ", null);

        FileEntry fileEntry = fileStorageService.storeFile(
            file.getName(),
            Files.contentOf(file, Charset.defaultCharset())
        );

        assertThat(localFileTaskHandler.handle(taskExecution))
            .hasFieldOrPropertyWithValue("extension", FilenameUtils.getExtension(file.getAbsolutePath()))
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", FilenameUtils.getName(file.getAbsolutePath()))
            .hasFieldOrPropertyWithValue("url", fileEntry.getUrl());
    }

    @Test
    public void testWriteOperation() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = getSimpleTaskExecution(
            file.getAbsolutePath(),
            "WRITE",
            fileStorageService.storeFile(file.getName(), new FileInputStream(file))
        );

        assertThat(localFileTaskHandler.handle(taskExecution)).hasFieldOrPropertyWithValue("bytes", 5L);
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }

    private SimpleTaskExecution getSimpleTaskExecution(String fileName, String operation, FileEntry fileEntry) {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileEntry);
        taskExecution.put("fileName", fileName);
        taskExecution.put("operation", operation);

        return taskExecution;
    }
}
