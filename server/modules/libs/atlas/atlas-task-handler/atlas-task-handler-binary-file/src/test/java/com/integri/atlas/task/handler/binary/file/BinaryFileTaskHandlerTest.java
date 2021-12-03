/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.binary.file;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.storage.base64.Base64StorageService;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.json.item.BinaryItem;
import com.integri.atlas.json.item.BinaryItemHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class BinaryFileTaskHandlerTest {

    private static final StorageService storageService = new Base64StorageService();
    private static final BinaryItemHelper binaryItemHelper = new BinaryItemHelper(storageService);
    private static final BinaryFileTaskHandler binaryFileTaskHandler = new BinaryFileTaskHandler(binaryItemHelper);

    @Test
    public void testReadOperation() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = getSimpleTaskExecution(file.getAbsolutePath(), "READ", null);

        assertEquals(
            new JSONObject()
                .put("data", storageService.write("bucketName", file.getAbsolutePath(), new FileInputStream(file)))
                .put("extension", "txt")
                .put("mimeType", "text/plain")
                .put("name", FilenameUtils.getName(file.getAbsolutePath())),
            binaryFileTaskHandler.handle(taskExecution),
            true
        );
    }

    @Test
    public void testWriteOperation() throws Exception {
        File file = getFile();

        SimpleTaskExecution taskExecution = getSimpleTaskExecution(
            file.getAbsolutePath(),
            "WRITE",
            BinaryItem
                .of(
                    file.getAbsolutePath(),
                    storageService.write("bucketName", file.getAbsolutePath(), new FileInputStream(file))
                )
                .toString()
        );

        assertEquals(new JSONObject().put("bytes", 5), binaryFileTaskHandler.handle(taskExecution), true);
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }

    private SimpleTaskExecution getSimpleTaskExecution(String fileName, String operation, String binaryItem) {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("binaryItem", binaryItem);
        taskExecution.put("fileName", fileName);
        taskExecution.put("operation", operation);

        return taskExecution;
    }
}
