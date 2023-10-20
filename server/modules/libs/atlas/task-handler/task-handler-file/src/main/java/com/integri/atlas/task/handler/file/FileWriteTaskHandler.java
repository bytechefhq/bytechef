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

package com.integri.atlas.task.handler.file;

import static com.integri.atlas.task.handler.file.FileTaskConstants.PROPERTY_CONTENT;
import static com.integri.atlas.task.handler.file.FileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.file.FileTaskConstants.TASK_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_FILE + "/write")
public class FileWriteTaskHandler implements TaskHandler<Object> {

    private final FileStorageService fileStorageService;

    public FileWriteTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public FileEntry handle(TaskExecution taskExecution) {
        Object content = taskExecution.getRequired(PROPERTY_CONTENT);
        String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, "file.txt");

        return fileStorageService.storeFileContent(
            fileName,
            content instanceof String ? (String) content : content.toString()
        );
    }
}
