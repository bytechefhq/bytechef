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

import static com.integri.atlas.task.handler.json.file.JsonFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.json.file.JsonFileTaskConstants.PROPERTY_FILE_TYPE;
import static com.integri.atlas.task.handler.json.file.JsonFileTaskConstants.PROPERTY_SOURCE;
import static com.integri.atlas.task.handler.json.file.JsonFileTaskConstants.TASK_JSON_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.json.file.JsonFileTaskConstants.FileType;
import com.integri.atlas.task.handler.json.helper.JsonHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_JSON_FILE + "/write")
public class JsonFileWriteTaskHandler implements TaskHandler<FileEntry> {

    private final FileStorageService fileStorageService;
    private final JsonHelper jsonHelper;

    public JsonFileWriteTaskHandler(FileStorageService fileStorageService, JsonHelper jsonHelper) {
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileEntry handle(TaskExecution taskExecution) throws Exception {
        FileType fileType = FileType.valueOf(
            StringUtils.upperCase(taskExecution.get(PROPERTY_FILE_TYPE, String.class, FileType.JSON.name()))
        );
        String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, getDefaultFileName(fileType));
        Object source = taskExecution.getRequired(PROPERTY_SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if (fileType == FileType.JSON) {
            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                printWriter.println(jsonHelper.write(source));
            }
        } else {
            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                for (Map<String, ?> item : (List<Map<String, ?>>) source) {
                    printWriter.println(jsonHelper.write(item));
                }
            }
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return fileStorageService.storeFileContent(fileName, inputStream);
        }
    }

    private String getDefaultFileName(FileType fileType) {
        return "file." + (fileType == FileType.JSON ? "json" : "jsonl");
    }
}
