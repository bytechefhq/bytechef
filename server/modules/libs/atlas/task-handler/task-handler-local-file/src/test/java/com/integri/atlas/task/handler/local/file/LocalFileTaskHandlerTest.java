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
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.integri.atlas.engine.core.binary.Binary;
import com.integri.atlas.engine.core.binary.BinaryHelper;
import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.storage.base64.Base64StorageService;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
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

    private static final StorageService storageService = new Base64StorageService();
    private static final BinaryHelper binaryHelper = new BinaryHelper(storageService);
    private static final LocalFileTaskHandler localFileTaskHandler = new LocalFileTaskHandler(binaryHelper);

    @Test
    public void testReadOperation() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = getSimpleTaskExecution(file.getAbsolutePath(), "READ", null);

        assertThat(localFileTaskHandler.handle(taskExecution))
            .hasFieldOrPropertyWithValue(
                "data",
                storageService.write("bucketName", Files.contentOf(file, Charset.defaultCharset()))
            )
            .hasFieldOrPropertyWithValue("extension", FilenameUtils.getExtension(file.getAbsolutePath()))
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", FilenameUtils.getName(file.getAbsolutePath()));
    }

    @Test
    public void testWriteOperation() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = getSimpleTaskExecution(
            file.getAbsolutePath(),
            "WRITE",
            Binary.of(file.getAbsolutePath(), storageService.write("bucketName", new FileInputStream(file)))
        );

        assertThat(localFileTaskHandler.handle(taskExecution)).hasFieldOrPropertyWithValue("bytes", 5L);
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }

    private SimpleTaskExecution getSimpleTaskExecution(String fileName, String operation, Binary binary) {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("binary", binary);
        taskExecution.put("fileName", fileName);
        taskExecution.put("operation", operation);

        return taskExecution;
    }
}
