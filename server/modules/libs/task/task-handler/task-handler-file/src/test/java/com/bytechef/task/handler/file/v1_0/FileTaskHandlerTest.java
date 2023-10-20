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

package com.bytechef.task.handler.file.v1_0;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.task.commons.file.storage.FileStorageHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class FileTaskHandlerTest {

    private static final FileStorageHelper fileStorageHelper = new FileStorageHelper(new Base64FileStorageService());
    private static final FileTaskHandler.FileReadTaskHandler fileFileReadTaskHandler =
            new FileTaskHandler.FileReadTaskHandler(fileStorageHelper);
    private static final FileTaskHandler.FileWriteTaskHandler fileWriteTaskHandler =
            new FileTaskHandler.FileWriteTaskHandler(fileStorageHelper);

    @Test
    public void testRead() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileStorageHelper.storeFileContent(file.getName(), new FileInputStream(file)));
        taskExecution.put("operation", "READ");

        assertThat(fileFileReadTaskHandler.handle(taskExecution))
                .isEqualTo(Files.contentOf(file, Charset.defaultCharset()));
    }

    @Test
    public void testWrite() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("content", Files.contentOf(file, Charset.defaultCharset()));
        taskExecution.put("operation", "WRITE");

        FileEntry fileEntry = fileWriteTaskHandler.handle(taskExecution);

        assertThat(fileStorageHelper.readFileContent(fileEntry))
                .isEqualTo(Files.contentOf(file, Charset.defaultCharset()));

        assertThat(fileEntry.getName()).isEqualTo("file.txt");

        taskExecution.put("fileName", "test.txt");
        taskExecution.put("content", Files.contentOf(file, Charset.defaultCharset()));
        taskExecution.put("operation", "WRITE");

        fileEntry = fileWriteTaskHandler.handle(taskExecution);

        assertThat(fileEntry.getName()).isEqualTo("test.txt");
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }
}
